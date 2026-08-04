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
- independent 20-second accumulation windows for each implemented wound type;
- blunt, sharp, burn, and explosion wounds using reviewed half-open severity ranges;
- server-authoritative natural healing that updates `H` once per complete second and removes wounds at `H = 0`;
- version-6 body state with persistent pain, stress, shock warnings, collapse reasons, per-wound bleeding clocks, and safe migration from earlier saves;
- server-authoritative bleeding pulses for bleeding levels 1-4, including the two-second movement-bleeding linger on level-3 blunt wounds;
- silent blood-loss health deduction that bypasses the vanilla hurt animation and instead sends a short two-or-three-spot blood overlay to the affected client;
- traumatic-shock progression: stress suppresses collapse, pain 20 starts a ten-second warning, and pain still at 20 incapacitates the player at the deadline;
- lethal final damage is clamped to preserve up to one vanilla health point and moves an active player into the incapacitated state;
- server-side incapacitation restrictions for movement, attacks, block breaking, interaction, and item use;
- server-to-client body-state snapshots;
- a first-pass three-column health screen, opened with `H`;
- `/superficialtrauma status` and `/superficialtrauma selftest` diagnostics.

Phase 0's CGM recognition slice is also implemented:

- optional CGM projectile inspection without a hard runtime dependency;
- server-side capture of the projectile, ammunition, and weapon registry IDs;
- data-pack ammunition groups for low-velocity, high-velocity, and shotgun rounds;
- version-2 `BodyState` diagnostics with safe migration from version 1;
- `/superficialtrauma classifyammo` for checking the held ammunition item.

Explosion damage is checked before CGM projectile damage so rockets and explosive projectiles cannot be misclassified as ordinary gunshots. CGM shots are identified and logged, but they intentionally do not create gunshot wounds until the gun-wound rules are implemented. Natural healing is active for the reviewed non-gun wound rates; wounds that cannot naturally heal remain at their current `H`. Pain accumulation, wound-tag contributions, stress refresh, natural base-pain recovery, external bleeding damage, the hidden-duration pain-20 shock warning, traumatic-shock incapacitation, and lethal-hit downing are active. Blood-oxygen countdown, downed-damage deadline reduction, cardiac arrest, awakening through treatment, internal bleeding derivation, treatments, the two-player target HUD, and polished HUD art are not active yet.

Natural healing and pain timers advance only while the injured player is online. Logging out pauses the progression clock, preventing logout time from being used as free treatment or stress recovery. Whole intervals are calculated from server game time, so delayed processing does not lose elapsed progress.

Blood-loss pulses deduct vanilla health directly on the server instead of invoking the vanilla hurt pipeline. They therefore bypass armor, defensive effects, absorption hearts, and hurt cooldowns without producing camera hurt wobble, knockback, hurt sounds, new wounds, pain, stress, or latest-hit diagnostics. Each pulse sends only a brief translucent blood-spot overlay to the injured client. The dedicated bleeding damage types remain reserved for compatibility and future true-death attribution.

### Manual persistence check

1. Start `runClient` and enter a test world.
2. Take blunt, sharp, burn, or explosion damage. Drowning, starvation, magic, wither, and unarmored ordinary projectiles remain non-traumatic or deferred.
3. Press `H` and confirm the final damage and wound card appear.
4. Run `/superficialtrauma selftest`; the command should report that the BodyState/NBT round trip passed.
5. Keep the HUD open and confirm `H` decreases once per second at the displayed natural-healing rate. A level-1 blunt or sharp wound decreases by `1.0 H/s`.
6. Save and quit the world, re-enter it, and press `H` again. The wound UUID, severity, `A`, and current `H` are stored in the player's capability data. Time spent logged out must not decrease `H`.

### Manual pain check

1. Take a known amount of final damage and open the health HUD. Base pain increases by `D`; pain wound tags are then added on top.
2. Confirm the stress row starts near 20 seconds and refreshes to 20 seconds after another traumatic hit.
3. Base pain must remain unchanged throughout stress and for the following 1.5 seconds.
4. After that point, effective pain decreases by one every 1.5 seconds until only the current wound-tag contribution remains.
5. Log out while stress is active, wait on a still-running server, and reconnect. The remaining stress and base pain must resume rather than elapse offline.
6. Reach effective pain 20 and wait for stress to end. The server starts its internal ten-second traumatic-shock warning, but the action bar and health HUD only show the non-numeric warning text; automatic base-pain recovery remains paused for the full warning.
7. If effective pain is still 20 at the deadline, the life state becomes incapacitated and the player can no longer move horizontally, sprint, attack, break blocks, interact, or start using an item.
8. During development, an operator can run `/superficialtrauma recover` or `/superficialtrauma recover <player>` to restore active state and clear base pain.
9. Take final damage equal to or greater than current vanilla health. Health stops at one and the collapse reason becomes lethal injury; ordinary external-hit feedback is retained.

### Manual bleeding check

1. Create a level-2 sharp wound with `5 <= D < 15`. Its `Bleeding 2` tag must remove one vanilla health point every seven seconds.
2. Create a level-3 sharp wound with `D >= 15`. Its `Bleeding 3` tag must remove one point every five seconds.
3. Create a level-2 explosion wound with `8 <= D < 16`. Its `Bleeding 1` tag must remove one point every ten seconds.
4. Confirm that a bleeding pulse does not shake the camera, play the normal hurt animation, create a new wound, add pain, restart stress, or replace the HUD's latest external-damage record. Two or three soft red blood spots should briefly fade over the screen instead.
5. For a level-3 blunt wound, sprint or jump. `Bleeding 1` appears while movement is active, remains for two seconds after stopping, and then hides. Its ten-second pulse timer resets when the movement bleeding stops.
6. Log out before a pulse and reconnect later. The remaining interval must resume instead of catching up offline damage.

### Manual CGM recognition check

1. Start the compatibility client with `runClient -Penable_compatibility_mods=true`.
2. Hold `cgm:basic_bullet`, `cgm:advanced_bullet`, `cgm:shell`, or `nzgexpansion:medium_bullet` and run `/superficialtrauma classifyammo`.
3. In a two-player test, shoot the second player and open the victim's HUD with `H`.
4. Confirm that the HUD classification and ammunition ID match the fired round. `/superficialtrauma status` and `latest.log` also include the classification, ammunition ID, and weapon ID.

### Implemented non-gun trauma ranges

| Type | No wound | Level 1 | Level 2 | Level 3 |
|---|---:|---:|---:|---:|
| Blunt | `[0, 1.5)` | `[1.5, 4)` | `[4, 13)` | `[13, +∞)` |
| Sharp | `[0, 0.5)` | `[0.5, 5)` | `[5, 15)` | `[15, +∞)` |
| Burn | `D = 0` | `(0, 5)` | `[5, 16)` | `[16, +∞)` |
| Explosion | `[0, 4)` | `[4, 8)` | `[8, 16)` | `[16, +∞)` |

Vanilla swords and axes are classified as sharp weapons. Modded sharp weapons can be appended through the `superficialtrauma:weapons/sharp` item tag.

## License

Copyright (c) 2026 Swamp_D. All Rights Reserved. The repository may be publicly viewable, but reuse and redistribution require separate permission from the copyright holder.
