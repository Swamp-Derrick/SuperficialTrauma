# Superficial Trauma

Superficial Trauma is a server-authoritative trauma, treatment, and resuscitation mod for Minecraft Forge 1.20.1.

## Development baseline

- Minecraft 1.20.1
- Forge 47.4.20
- Java 17
- Official 1.20.1 mappings
- Gradle 8.8
- ForgeGradle 6.0.54

Run a standalone build with:

```powershell
.\gradlew.bat clean build
```

The local compatibility pack is kept in the sibling directory `../Compatiblemods` and is not bundled into the Superficial Trauma JAR. Verify it with:

```powershell
.\gradlew.bat verifyCompatibilityMods
```

Enable the exact compatibility pack for a development run with:

```powershell
.\gradlew.bat runClient -Penable_compatibility_mods=true
```

Dedicated-server startup uses the same property. The first local run creates `run/eula.txt`; the project does not accept the Minecraft EULA on the developer's behalf.

See [COMPATIBILITY.md](COMPATIBILITY.md) for the pinned filenames, mod IDs, versions, and checksums.

## Project status

The project is entering phase 0 technical validation. The first vertical slice is final damage to wound creation, persistence across reconnects, and the player's own health HUD.

## License

Copyright (c) 2026 Swamp_D. All Rights Reserved. The repository may be publicly viewable, but reuse and redistribution require separate permission from the copyright holder.
