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
- blunt, sharp, burn, explosion, low-velocity gunshot, high-velocity gunshot, and shotgun wounds using reviewed half-open severity ranges;
- server-authoritative natural healing that updates `H` once per complete second and removes wounds at `H = 0`;
- version-9 body state with persistent pain, stress, shock warnings, collapse reasons, downed deadlines, downed-pose snapshots, per-wound bleeding clocks, gunshot severity context, and safe migration from earlier saves;
- server-authoritative bleeding pulses for bleeding levels 1-4, including the two-second movement-bleeding linger on level-3 blunt wounds;
- silent blood-loss health deduction that bypasses the vanilla hurt animation and instead sends a short two-or-three-spot blood overlay to the affected client;
- traumatic-shock progression: stress suppresses collapse, pain 20 starts a ten-second warning, and pain still at 20 incapacitates the player at the deadline;
- lethal final damage is clamped to preserve up to one vanilla health point and moves an active player into the incapacitated state;
- incapacitation starts a 180-second blood-oxygen countdown; expiry enters cardiac arrest and starts the separate 180-second brain-death deadline;
- final damage while downed skips trauma, pain, and stress, then shortens the current danger countdown by `10 × D` seconds;
- collapse captures one fixed server-owned snapshot containing downing time, body yaw, standing/crouching/sprinting posture, and forward/backward/left/right fall direction; unsafe, swimming, and crawling states select fade-only;
- compact downed-pose snapshots are synchronized to the victim, current tracking players, and players who begin tracking later;
- a first-pass client presentation applies a render-only rigid horizontal transform to the complete third-person player render and fades the victim into a persistent dark downed overlay in about 0.9 seconds;
- standing, crouching, and sprinting collapse snapshots drive first-person forward/backward/left/right fall angles while swimming, crawling, and unsafe postures retain direct fade-only handling;
- the horizontal model is centered over a server-authoritative `1.8 x 0.6 x 0.6` directional bounding volume, with the same bounds reconstructed on tracking clients for aiming, F3+B diagnostics, and centered shadow placement;
- downed input suppresses movement, inventory/container screens, chat, item dropping, and offhand swapping on the client, while the server independently freezes movement, closes open containers, and rejects item tosses;
- the completed downed fade is fully opaque, covers the hotbar and chat HUD, hides the first-person held item, and still permits the dedicated `H` health screen;
- brain-death expiry performs one normal server death handoff so later corpse compatibility can remain downstream of the life-state machine;
- server-side incapacitation restrictions for movement, attacks, block breaking, interaction, and item use;
- server-to-client body-state snapshots;
- network protocol 4, requiring the current JAR on the server and every test client because gunshot wound types are synchronized in body-state snapshots;
- a first-pass three-column health screen, opened with `H`;
- `/superficialtrauma status` and `/superficialtrauma selftest` diagnostics;
- `/superficialtrauma reset [player]` completely clears the mod body state and restores vanilla survival health, hunger, air, effects, absorption, fire, freezing, embedded arrows/stingers, hurt cooldowns, and movement for repeatable cross-version testing; `/recover` remains an alias.

Phase 0's CGM recognition slice is also implemented:

- optional CGM projectile inspection without a hard runtime dependency;
- server-side capture of the projectile, ammunition, and weapon registry IDs;
- data-pack ammunition groups for low-velocity, high-velocity, and shotgun rounds;
- version-2 `BodyState` diagnostics with safe migration from version 1;
- `/superficialtrauma classifyammo` for checking the held ammunition item.

Explosion damage is checked before CGM projectile damage so rockets and explosive projectiles cannot be misclassified as ordinary gunshots. Classified CGM low-velocity, high-velocity, and shotgun hits now create separate gunshot wounds. Individual hits below `D = 4` convert to blunt trauma; qualifying gunshot hits accumulate for twenty seconds, retain the reviewed `V > 10` fragmentation or `L <= 3` close-shot context, and roll the reviewed debridement chance once when the wound is created. Natural healing is active for the reviewed wound rates; wounds that cannot naturally heal remain at their current `H`. Pain accumulation, wound-tag contributions, stress refresh, natural base-pain recovery, external bleeding damage, the hidden-duration pain-20 shock warning, traumatic-shock incapacitation, lethal-hit downing, blood-oxygen countdown, downed-damage deadline reduction, cardiac arrest, brain-death expiry, synchronized downed-pose data, the rigid third-person downed transform, the first directional camera pass, the low directional hitbox, and the opaque victim overlay are active. Awakening through treatment, CPR, ventricular fibrillation, internal bleeding derivation, treatments, the two-player target HUD, corpse handoff verification, camera collision polish, accessibility settings, and polished HUD art are not active yet. The agreed downed camera and third-person pose direction is recorded in [DESIGN_NOTES.md](DESIGN_NOTES.md).

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
8. During development, an operator can run `/superficialtrauma reset` or `/superficialtrauma reset <player>` to clear all mod state and restore vanilla survival state. `/superficialtrauma recover` remains an alias.
9. Take final damage equal to or greater than current vanilla health. Health stops at one and the collapse reason becomes lethal injury; ordinary external-hit feedback is retained.

### Manual downed countdown check

1. Become incapacitated and open the health HUD. It must show a 180-second danger countdown instead of exposing exact blood oxygen.
2. Wait nine seconds. The server-side oxygen reserve must lose one point while the displayed countdown continues toward cardiac arrest.
3. While downed, take a controlled `D = 2` final-damage hit. The danger countdown must immediately lose 20 seconds, while the wound count, wound accumulation, base pain, and stress remain unchanged.
4. Repeat with blunt, sharp, burn, explosion, and CGM-classified damage. Classification may remain in latest-hit diagnostics, but none may create or update a wound while downed.
5. Let the first countdown expire. The life state must become cardiac arrest and a new 180-second brain-death countdown must begin with a persistent cardiac-arrest event ID.
6. Take another `D = 2` hit during cardiac arrest. This time the brain-death countdown must lose 20 seconds.
7. Run `/superficialtrauma status` during both states and confirm that `danger` matches the HUD in server ticks. NBT deadline migration and round trips are covered by `bodyStateSelfTest`; the reviewed disconnect-immediately-dies rule is not connected yet.
8. At brain-death expiry, exactly one normal player death must occur. Corpse-mod ownership and item handoff still require a later compatibility test.
9. On the first collapse, `/superficialtrauma status` must report a fixed `downedPose` containing posture, fall direction, body yaw, and downing game time. Later mouse movement and further damage must not change it.

### Manual downed presentation check

1. Replace the mod JAR on both clients because the downed-pose network protocol is required on both sides.
2. Down one standing, crouching, and sprinting player. The observer must see the complete player render lying horizontally, including armor and held-item layers, without changing the server hitbox.
3. Move the downed player's camera. The rendered body direction must remain fixed and the nameplate must remain hidden.
4. Enter a one-block-high space using a trapdoor and become incapacitated. `/superficialtrauma status` must report `crawling/fade_only`, not `standing`.
5. Swim and become incapacitated. Status must report `swimming/fade_only`. Riding, climbing, sleeping, and elytra cases remain `unsafe/fade_only`; this first pass deliberately keeps their vanilla third-person pose instead of applying a potentially conflicting rigid transform.
6. In first person, repeat standing, crouching, and sprinting collapses for all four directions. The camera must descend and pitch or roll toward the captured fall direction before the screen becomes black; swimming, crawling, and unsafe cases must fade without forced camera rotation.
7. After about 0.9 seconds the black overlay must completely cover the hotbar, received-chat background, and held item. `E`, `T`, `/`, movement, jumping, sprinting, sneaking, `Q`, and offhand swap must do nothing, while `H` must still open the health screen.
8. Enable F3+B. The downed box must be 0.6 blocks high and extend along the fixed body direction. Shoot or melee the visible torso and head area and confirm that the hit shortens the correct danger countdown.
9. The body must be centered over its shadow instead of extending outward from the old standing feet origin.
10. Let the oxygen countdown reach cardiac arrest. The health display may change to cardiac arrest, but no separate heart-stopped action-bar message should appear.
11. `/superficialtrauma reset <player>` must immediately restore the ordinary hitbox and remove the overlay, camera lock, and third-person transform.

### Manual cross-version reset check

1. Create several wounds, pain, stress, bleeding, effects, missing health, hunger, fire, freezing, and a downed state.
2. Run `/superficialtrauma reset <player>` as an operator. The no-argument form targets the command sender.
3. Confirm full vanilla health, hunger, saturation, air, no absorption/effects/fire/freezing/arrows/stingers, active life state, no wounds, no pain, no pending damage windows, and no downed-pose snapshot.
4. Inventory, experience, game mode, location, and advancements are deliberately preserved.

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
5. Confirm that the hit creates the matching low-velocity, high-velocity, or shotgun wound card. A single classified hit below `D = 4` must create or update blunt trauma instead.
6. For low/high velocity, repeat the upper-threshold hit with `V <= 10` and `V > 10`; only the latter may reach severity 3. For shotgun `A >= 8`, shoot from just over three blocks for severity 2 and from three blocks or less for severity 3.
7. Save and rejoin. The gunshot wound, debridement result, armor-qualified flag, and close-range flag must retain the same severity and tags.

### Implemented non-gun trauma ranges

| Type | No wound | Level 1 | Level 2 | Level 3 |
|---|---:|---:|---:|---:|
| Blunt | `[0, 1.5)` | `[1.5, 4)` | `[4, 13)` | `[13, +∞)` |
| Sharp | `[0, 0.5)` | `[0.5, 5)` | `[5, 15)` | `[15, +∞)` |
| Burn | `D = 0` | `(0, 5)` | `[5, 16)` | `[16, +∞)` |
| Explosion | `[0, 4)` | `[4, 8)` | `[8, 16)` | `[16, +∞)` |

Vanilla swords and axes are classified as sharp weapons. Modded sharp weapons can be appended through the `superficialtrauma:weapons/sharp` item tag.

### Implemented gunshot trauma ranges

Every classified gunshot with single-hit `D` in `[0, 4)` converts to blunt trauma instead of entering a gunshot accumulation window.

| Type | Level 1 | Level 2 | Level 3 |
|---|---:|---:|---:|
| Low velocity | `A in [4, 6)` | `A in [6, +inf)`, capped here when `V <= 10` | `A in [15, +inf)` and at least one hit had `V > 10` |
| High velocity | `A in [4, 10)` | `A in [10, +inf)`, capped here when `V <= 10` | `A in [12, +inf)` and at least one hit had `V > 10` |
| Shotgun | `A in [4, 8)` | `A in [8, +inf)` and all contributing hits had `L > 3` | `A in [8, +inf)` and at least one hit had `L <= 3` |

## License

Copyright (c) 2026 Swamp_D. All Rights Reserved. The repository may be publicly viewable, but reuse and redistribution require separate permission from the copyright holder.
