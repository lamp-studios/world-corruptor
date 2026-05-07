package dev.lampyt.worldcorruptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

final class CorruptionEngine {
    private static final int DISPLACE_ATTEMPTS = 8;

    private final CorruptionSettings settings;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    CorruptionEngine(CorruptionSettings settings) {
        this.settings = settings;
    }

    int pulse(World world, Location center, int radius, int blocks) {
        int mutated = 0;
        for (int index = 0; index < blocks; index++) {
            Block block = randomBlockAround(world, center, radius);
            if (block != null && corruptBlock(block)) {
                mutated++;
            }
        }
        return mutated;
    }

    int tick(List<? extends Player> onlinePlayers) {
        List<? extends Player> eligiblePlayers = onlinePlayers.stream()
                .filter(player -> settings.allowsWorld(player.getWorld().getName()))
                .toList();
        if (eligiblePlayers.isEmpty()) {
            return 0;
        }

        int mutated = 0;
        for (int index = 0; index < settings.blocksPerCycle(); index++) {
            Player player = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));
            Block block = randomBlockAround(player.getWorld(), player.getLocation(), settings.radius());
            if (block != null && corruptBlock(block)) {
                mutated++;
            }
        }

        maybeSpawnEntity(eligiblePlayers);
        return mutated;
    }

    private boolean corruptBlock(Block block) {
        if (!isMutable(block)) {
            return false;
        }

        Operation operation = chooseOperation();
        return switch (operation) {
            case REPLACE -> replace(block);
            case REMOVE -> remove(block);
            case DISPLACE -> displace(block);
            case FALLING_BLOCK -> fallingBlock(block);
        };
    }

    private Operation chooseOperation() {
        int totalWeight = settings.totalOperationWeight();
        if (totalWeight <= 0) {
            return Operation.REPLACE;
        }

        int roll = random.nextInt(totalWeight);
        if (roll < settings.replaceWeight()) {
            return Operation.REPLACE;
        }
        roll -= settings.replaceWeight();
        if (roll < settings.removeWeight()) {
            return Operation.REMOVE;
        }
        roll -= settings.removeWeight();
        if (roll < settings.displaceWeight()) {
            return Operation.DISPLACE;
        }
        return Operation.FALLING_BLOCK;
    }

    private boolean replace(Block block) {
        Material material = settings.palette().get(random.nextInt(settings.palette().size()));
        if (material == block.getType() || settings.protectedMaterials().contains(material)) {
            return false;
        }
        block.setType(material, false);
        return true;
    }

    private boolean remove(Block block) {
        block.setType(Material.AIR, false);
        return true;
    }

    private boolean displace(Block block) {
        Block target = findNearbyAir(block);
        if (target == null) {
            return replace(block);
        }

        BlockData sourceData = block.getBlockData();
        target.setBlockData(sourceData, false);
        block.setType(Material.AIR, false);
        return true;
    }

    private boolean fallingBlock(Block block) {
        BlockData sourceData = block.getBlockData();
        block.setType(Material.AIR, false);
        block.getWorld().spawnFallingBlock(block.getLocation().add(0.5D, 0.25D, 0.5D), sourceData);
        return true;
    }

    private void maybeSpawnEntity(List<? extends Player> eligiblePlayers) {
        if (!settings.entitySpawningEnabled() || random.nextDouble() > settings.entitySpawnChance()) {
            return;
        }

        Player player = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));
        Location location = randomSurfaceLocation(player.getWorld(), player.getLocation(), settings.entitySpawnRadius());
        if (location == null) {
            return;
        }

        EntityType type = settings.entityTypes().get(random.nextInt(settings.entityTypes().size()));
        player.getWorld().spawnEntity(location, type);
    }

    private Location randomSurfaceLocation(World world, Location center, int radius) {
        int x = center.getBlockX() + randomOffset(radius);
        int z = center.getBlockZ() + randomOffset(radius);
        int y = world.getHighestBlockYAt(x, z) + 1;
        if (y < world.getMinHeight() || y > world.getMaxHeight()) {
            return null;
        }
        return new Location(world, x + 0.5D, y, z + 0.5D);
    }

    private Block randomBlockAround(World world, Location center, int radius) {
        int x = center.getBlockX() + randomOffset(radius);
        int z = center.getBlockZ() + randomOffset(radius);
        int minY = Math.max(world.getMinHeight(), center.getBlockY() - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, center.getBlockY() + radius);
        if (minY > maxY) {
            return null;
        }
        int y = random.nextInt(minY, maxY + 1);
        return world.getBlockAt(x, y, z);
    }

    private int randomOffset(int radius) {
        return random.nextInt(-radius, radius + 1);
    }

    private Block findNearbyAir(Block block) {
        List<Block> candidates = new ArrayList<>();
        World world = block.getWorld();
        for (int attempt = 0; attempt < DISPLACE_ATTEMPTS; attempt++) {
            int x = block.getX() + random.nextInt(-3, 4);
            int y = clamp(block.getY() + random.nextInt(-2, 3), world.getMinHeight(), world.getMaxHeight() - 1);
            int z = block.getZ() + random.nextInt(-3, 4);
            Block candidate = world.getBlockAt(x, y, z);
            if (candidate.getType().isAir()) {
                candidates.add(candidate);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private boolean isMutable(Block block) {
        Material material = block.getType();
        if (material.isAir() || material == Material.WATER || material == Material.LAVA) {
            return false;
        }
        if (!material.isBlock() || settings.protectedMaterials().contains(material)) {
            return false;
        }
        BlockState state = block.getState(false);
        return !(state instanceof Container);
    }

    private enum Operation {
        REPLACE,
        REMOVE,
        DISPLACE,
        FALLING_BLOCK
    }
}
