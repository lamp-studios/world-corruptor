# WorldCorruptor

WorldCorruptor is a configurable Paper plugin for Minecraft 1.21.11 that makes the world feel unstable: blocks can be replaced, removed, displaced, or launched as falling blocks, and hostile entities can spawn as part of corruption pulses.

## Features

- Scheduled corruption around online players.
- Manual `/worldcorruptor pulse [radius] [blocks]` command for instant corruption.
- Configurable corruption amount, radius, interval, block palette, protected materials, and operation weights.
- Optional corruption-themed entity spawning.
- Admin commands for start, stop, reload, status, and pulse.

## Build

```bash
mvn package
```

The plugin jar is written to `target/WorldCorruptor-1.0.0.jar`.

## Install

1. Build the jar.
2. Copy `target/WorldCorruptor-1.0.0.jar` into your Paper 1.21.11 server's `plugins/` folder.
3. Start the server once to generate `plugins/WorldCorruptor/config.yml`.
4. Edit the config to choose how much corruption you want, then run `/worldcorruptor reload`.

> Back up your worlds before enabling destructive corruption on an important server.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/worldcorruptor start` | `worldcorruptor.admin` | Starts scheduled corruption. |
| `/worldcorruptor stop` | `worldcorruptor.admin` | Stops scheduled corruption. |
| `/worldcorruptor reload` | `worldcorruptor.admin` | Reloads `config.yml`. |
| `/worldcorruptor status` | `worldcorruptor.admin` | Shows current runtime settings. |
| `/worldcorruptor pulse [radius] [blocks]` | `worldcorruptor.admin` | Mutates blocks around the command sender once. |

Aliases: `/wcorrupt`, `/corruptor`.
