# DriscolMod

Fabric mod for Minecraft **26.1.2** (mod id: `dm`).

Edit the first-person held item's position (XYZ) and rotation (XYZ) with a live hand preview.

## Commands

- `/dm`
- `/Driscolmod`

Opens the editor on the left. The right side stays clear so your hand/item updates in real time. Attack to check how the swing follows your rotations.

## Config

Saved automatically to `.minecraft/config/dm.json` (also when you hit Save or close the menu).

## Build

```bat
gradlew.bat build
```

Jar output: `build/libs/`.

## Requirements

- JDK 25
- Fabric Loader 0.19.3+
- Fabric API

## License

CC0-1.0
