# Matador

Client-side Fabric 26.1.1 mod with a visual Mob Hunter panel.

## Requirements

- Minecraft 26.1.1
- Fabric Loader 0.19.x
- Fabric API 0.145.1 or newer for 26.1.x
- Java 25
- Gradle, or import the project in an IDE that can run Gradle

## Usage

- Press `J` in game to open the `Mob Hunter` panel.
- Select the target mob using the visual spawn egg cards.
- Toggle the bot with `Ativar` / `Desativar`.
- Adjust search radius, attack distance, and attack delay from the panel.

## Build

```bash
gradlew.bat build
```

If Windows still points to Java 8, use the local portable JDK installed with this project:

```bash
build-local.bat
```

The built jar will be generated under `build/libs/`.
