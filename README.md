# DriscolMod

Fabric mod for Minecraft **26.1.2** (mod id: `dm`).

Edit first-person held item position, rotation, scale, and swing speed with a live hand preview.

## Commands

- `/dm`
- `/Driscolmod`

## Editor

- **Pos / Rot** — fine-tuned units (not raw world units), so small numbers stay controllable
- **Scale** — weapon size (`1.0` = normal)
- **Swing** — swing animation speed (`1.0` = normal, higher = faster)
- **Copy Preset** — copies a `dm1:...` code to clipboard to share
- **Apply Preset** — paste a friend's code and apply it

Settings save to `.minecraft/config/dm.json`.

## Build

```bat
gradlew.bat build
```

Jar: `build/libs/dm-1.1.0.jar`

## Requirements

- JDK 25
- Fabric Loader 0.19.3+
- Fabric API

## License

CC0-1.0

