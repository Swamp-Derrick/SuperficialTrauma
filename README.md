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

Phase 0's first vertical slice is implemented:

- versioned `BodyState` player capability with NBT persistence;
- post-armor, post-effect, post-absorption final damage capture;
- a 20-second blunt-damage accumulation window;
- blunt wounds using the reviewed half-open severity ranges;
- server-to-client body-state snapshots;
- a first-pass three-column health screen, opened with `H`;
- `/superficialtrauma status` and `/superficialtrauma selftest` diagnostics.

Blunt-wound healing, gameplay effects, treatments, and polished HUD art are intentionally not active yet.

### Manual persistence check

1. Start `runClient` and enter a test world.
2. Take ordinary blunt or fall damage. Fire, drowning, magic, sharp weapons, and unarmored projectiles are deferred until their wound types exist.
3. Press `H` and confirm the final damage and wound card appear.
4. Run `/superficialtrauma selftest`; the command should report that the BodyState/NBT round trip passed.
5. Save and quit the world, re-enter it, and press `H` again. The wound UUID, severity, `A`, and `H` are stored in the player's capability data and should remain unchanged.

## License

Copyright (c) 2026 Swamp_D. All Rights Reserved. The repository may be publicly viewable, but reuse and redistribution require separate permission from the copyright holder.
