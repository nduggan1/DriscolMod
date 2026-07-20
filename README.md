# DriscolMod

Fabric mod for Minecraft **26.1.2** (mod id: `dm`).

Edit first-person held item position, rotation, scale, and swing speed with a live hand preview.

## Commands

- `/dm`
- `/Driscolmod`

Everything the editor changes is purely visual and client-side.

## Editor

- **Pos X/Y/Z** — nudge the item's position (fine-tuned units, so small numbers stay controllable)
- **Pitch / Yaw / Roll** — rotate the item in place
- **Scale** — weapon size (`1.0` = normal, `2` = double, `0.5` = half)
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

