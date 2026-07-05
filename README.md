# Particle Muffler

Particle Muffler is a Minecraft 1.20.1 mod that suppresses particles around placed muffler blocks on the client side.

It is intended for performance-focused setups such as factories, farms, mob grinders, and magic or tech mod builds where local particle spam becomes distracting or expensive.

## Supported Platforms

- Minecraft 1.20.1
- Fabric
- Forge
- Java 17
- Architectury

## Blocks

### Particle Muffler

Suppresses all particles in the section where the block is placed.

- Fast path for performance-focused use.
- Does not inspect particle type.
- Redstone signal disables suppression.

### Filtered Particle Muffler

Suppresses particles by particle ID.

- Right-click to open the editor screen.
- Supports blacklist mode and whitelist mode.
- Particle IDs can be typed directly, for example `minecraft:flame`.
- Press `Tab` in the input field to cycle particle ID completions from the runtime particle registry, including modded particle types.
- Redstone signal disables suppression.

## Range Model

Particle Muffler uses Minecraft sections, not an exact sphere or cube.

The current default range is:

```text
sectionRadius = 0
```

That means only the 16x16x16 section containing the muffler block is affected.

## Config

The config file is generated at:

```text
config/particlemuffler.toml
```

Current options:

```toml
[client]
# How often the client removes stale Particle Muffler entries from its local registry.
# 20 ticks = 1 second. Minimum: 1. Maximum: 1200.
pruningIntervalTicks = 20
```

## Compatibility Notes

Particle suppression is implemented by injecting into `ParticleEngine#createParticle`.

Particles that do not go through that path may not be suppressed. This can include some special effects such as certain block break, firework, or weather particle paths.

The basic Particle Muffler keeps the fast path simple: if no filtered muffler is active, particle type IDs are not looked up.

## Development

Build all modules:

```bash
./gradlew build
```

The project uses an Architectury multi-loader layout:

```text
common/
fabric/
forge/
```

## Release

GitHub Actions builds with Java 21 while compiling Java 17 compatible mod jars.

To publish releases from GitHub Actions, configure repository variables:

```text
MODRINTH_PROJECT_ID
CURSEFORGE_PROJECT_ID
```

And repository secrets:

```text
MODRINTH_TOKEN
CURSEFORGE_TOKEN
```

Publishing runs when a GitHub Release is published. It can also be started manually from the `Publish` workflow.

## License

This project is licensed under the GNU Lesser General Public License v3.0.

Particle suppression behavior is designed with reference to the general approach used by global particle suppression mods. Block-based area suppression is inspired by Sound Muffler-style mods.

No source code from Sodium Extra or other particle suppression mods is included unless explicitly noted in file headers.
