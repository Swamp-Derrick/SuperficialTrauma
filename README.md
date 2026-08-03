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

Phase 0's CGM recognition slice is also implemented:

- optional CGM projectile inspection without a hard runtime dependency;
- server-side capture of the projectile, ammunition, and weapon registry IDs;
- data-pack ammunition groups for low-velocity, high-velocity, and shotgun rounds;
- version-2 `BodyState` diagnostics with safe migration from version 1;
- `/superficialtrauma classifyammo` for checking the held ammunition item.

CGM shots are identified and logged, but they intentionally do not create gunshot wounds until the gun-wound rules are implemented. Blunt-wound healing, gameplay effects, treatments, the two-player target HUD, and polished HUD art are not active yet.

### Manual persistence check

1. Start `runClient` and enter a test world.
2. Take ordinary blunt or fall damage. Fire, drowning, magic, sharp weapons, and unarmored projectiles are deferred until their wound types exist.
3. Press `H` and confirm the final damage and wound card appear.
4. Run `/superficialtrauma selftest`; the command should report that the BodyState/NBT round trip passed.
5. Save and quit the world, re-enter it, and press `H` again. The wound UUID, severity, `A`, and `H` are stored in the player's capability data and should remain unchanged.

### Manual CGM recognition check

1. Start the compatibility client with `runClient -Penable_compatibility_mods=true`.
2. Hold `cgm:basic_bullet`, `cgm:advanced_bullet`, `cgm:shell`, or `nzgexpansion:medium_bullet` and run `/superficialtrauma classifyammo`.
3. In a two-player test, shoot the second player and open the victim's HUD with `H`.
4. Confirm that the HUD classification and ammunition ID match the fired round. `/superficialtrauma status` and `latest.log` also include the classification, ammunition ID, and weapon ID.

## License

Copyright (c) 2026 Swamp_D. All Rights Reserved. The repository may be publicly viewable, but reuse and redistribution require separate permission from the copyright holder.
