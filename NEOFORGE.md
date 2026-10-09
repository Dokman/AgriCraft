# Minecraft 1.21.1 / NeoForge

This branch builds on the existing `origin/1.21` NeoForge port and brings forward the irrigation system and fixes from `1.20.1`.

- Connected tanks retain individual blocks and share their water storage.
- Channels, valves, visible water levels, rotating sprinklers and falling water particles are included.
- Sprinklers hydrate farmland and provide additional crop growth ticks.
- Jade displays server-synchronized irrigation contents and sprinkler/valve status.
- Create harvesters collect mature crops, reset their configured growth stage and preserve crop sticks. Create is optional.
- Pam's HarvestCraft 2 Crops data and Spanish translations are retained.

## Build

Use Java 21. Gradle can provision the toolchain through the configured Foojay resolver.

On the first checkout, generate the base and optional compatibility resources before building:

```powershell
.\gradlew.bat runData
.\gradlew.bat build
python src/test/python/test_tank_models.py
```

Output: `build/libs/agricraft-1.21.1-4.0.6.jar`.

The build runs reservoir conservation, atomic bucket transfer and Jade tooltip regression checks. Resource generation also starts the NeoForge mod loader. Shader visuals and a moving Create harvester still require verification in a game instance.

Irrigation settings are in the `irrigation` section of the common configuration. Create support can be disabled with `compat.enable_create`.
