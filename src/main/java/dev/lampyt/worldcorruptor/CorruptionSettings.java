package dev.lampyt.worldcorruptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

final class CorruptionSettings {
    private final boolean enabled;
    private final long intervalTicks;
    private final Set<String> worlds;
    private final int radius;
    private final int blocksPerCycle;
    private final Set<Material> protectedMaterials;
    private final List<Material> palette;
    private final int replaceWeight;
    private final int removeWeight;
    private final int displaceWeight;
    private final int fallingBlockWeight;
    private final boolean entitySpawningEnabled;
    private final double entitySpawnChance;
    private final int entitySpawnRadius;
    private final List<EntityType> entityTypes;

    private CorruptionSettings(
            boolean enabled,
            long intervalTicks,
            Set<String> worlds,
            int radius,
            int blocksPerCycle,
            Set<Material> protectedMaterials,
            List<Material> palette,
            int replaceWeight,
            int removeWeight,
            int displaceWeight,
            int fallingBlockWeight,
            boolean entitySpawningEnabled,
            double entitySpawnChance,
            int entitySpawnRadius,
            List<EntityType> entityTypes) {
        this.enabled = enabled;
        this.intervalTicks = intervalTicks;
        this.worlds = worlds;
        this.radius = radius;
        this.blocksPerCycle = blocksPerCycle;
        this.protectedMaterials = protectedMaterials;
        this.palette = palette;
        this.replaceWeight = replaceWeight;
        this.removeWeight = removeWeight;
        this.displaceWeight = displaceWeight;
        this.fallingBlockWeight = fallingBlockWeight;
        this.entitySpawningEnabled = entitySpawningEnabled;
        this.entitySpawnChance = entitySpawnChance;
        this.entitySpawnRadius = entitySpawnRadius;
        this.entityTypes = entityTypes;
    }

    static CorruptionSettings fromConfig(FileConfiguration config) {
        return new CorruptionSettings(
                config.getBoolean("enabled", true),
                Math.max(1L, config.getLong("interval-ticks", 100L)),
                normalizeWorldNames(config.getStringList("worlds")),
                Math.max(1, config.getInt("corruption.radius", 18)),
                Math.max(1, config.getInt("corruption.blocks-per-cycle", 18)),
                materials(config.getStringList("corruption.protected-materials"), defaultProtectedMaterials()),
                palette(config.getStringList("corruption.palette"), defaultPalette()),
                Math.max(0, config.getInt("corruption.operations.replace-weight", 55)),
                Math.max(0, config.getInt("corruption.operations.remove-weight", 15)),
                Math.max(0, config.getInt("corruption.operations.displace-weight", 20)),
                Math.max(0, config.getInt("corruption.operations.falling-block-weight", 10)),
                config.getBoolean("entities.enabled", true),
                clamp(config.getDouble("entities.spawn-chance", 0.18D), 0.0D, 1.0D),
                Math.max(1, config.getInt("entities.spawn-radius", 14)),
                entityTypes(config.getStringList("entities.types")));
    }

    boolean enabled() {
        return enabled;
    }

    long intervalTicks() {
        return intervalTicks;
    }

    boolean allowsWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    int radius() {
        return radius;
    }

    int blocksPerCycle() {
        return blocksPerCycle;
    }

    Set<Material> protectedMaterials() {
        return protectedMaterials;
    }

    List<Material> palette() {
        return palette;
    }

    int replaceWeight() {
        return replaceWeight;
    }

    int removeWeight() {
        return removeWeight;
    }

    int displaceWeight() {
        return displaceWeight;
    }

    int fallingBlockWeight() {
        return fallingBlockWeight;
    }

    int totalOperationWeight() {
        return replaceWeight + removeWeight + displaceWeight + fallingBlockWeight;
    }

    boolean entitySpawningEnabled() {
        return entitySpawningEnabled;
    }

    double entitySpawnChance() {
        return entitySpawnChance;
    }

    int entitySpawnRadius() {
        return entitySpawnRadius;
    }

    List<EntityType> entityTypes() {
        return entityTypes;
    }

    private static Set<String> normalizeWorldNames(List<String> configuredWorlds) {
        Set<String> names = new java.util.HashSet<>();
        for (String world : configuredWorlds) {
            if (world != null && !world.isBlank()) {
                names.add(world.toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(names);
    }

    private static Set<Material> materials(List<String> names, Set<Material> fallback) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material material = parseMaterial(name);
            if (material != null) {
                materials.add(material);
            }
        }
        return materials.isEmpty() ? fallback : Set.copyOf(materials);
    }

    private static List<Material> palette(List<String> names, List<Material> fallback) {
        List<Material> materials = new ArrayList<>();
        for (String name : names) {
            Material material = parseMaterial(name);
            if (material != null) {
                materials.add(material);
            }
        }
        return materials.isEmpty() ? fallback : List.copyOf(materials);
    }

    private static List<EntityType> entityTypes(List<String> names) {
        List<EntityType> types = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                EntityType type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
                if (type.isAlive() && type.isSpawnable()) {
                    types.add(type);
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid config entries are skipped so one typo does not disable the plugin.
            }
        }
        return types.isEmpty() ? List.of(EntityType.ZOMBIE, EntityType.SKELETON) : List.copyOf(types);
    }

    private static Material parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return material != null && material.isBlock() ? material : null;
    }

    private static Set<Material> defaultProtectedMaterials() {
        return Set.of(
                Material.BEDROCK,
                Material.BARRIER,
                Material.COMMAND_BLOCK,
                Material.CHAIN_COMMAND_BLOCK,
                Material.REPEATING_COMMAND_BLOCK,
                Material.STRUCTURE_BLOCK,
                Material.JIGSAW,
                Material.END_PORTAL,
                Material.END_PORTAL_FRAME);
    }

    private static List<Material> defaultPalette() {
        return List.of(
                Material.SCULK,
                Material.BLACKSTONE,
                Material.CRYING_OBSIDIAN,
                Material.OBSIDIAN,
                Material.NETHERRACK,
                Material.SOUL_SAND,
                Material.MAGMA_BLOCK);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
