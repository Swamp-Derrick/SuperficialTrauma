# Superficial Trauma 0.0.5 compatibility baseline

Updated 2026-10-03. Release target: Minecraft **1.21.1**, NeoForge **21.1.252**, **Java 21**.
Artifact: `superficialtrauma-neoforge-1.21.1-0.0.5.jar`, from the `1.21.1-dev` branch.
Install the same Superficial Trauma version on the server and participating clients. Forge 1.20.1 is discontinued; old-world migration is outside this release's supported baseline.

The exact baseline below comes from the user's local test instance:
`E:\.minecraft\versions\1.21.1 neoforge test\mods`.

These third-party JARs are **not redistributed** in this repository or the Superficial Trauma JAR. CGM and Simple Voice Chat remain optional.

| Role | Mod ID | Version | Filename |
|---|---|---|---|
| Library | `framework` | 0.13.11 | `framework-neoforge-1.21.1-0.13.11.jar` |
| Gun system | `cgm` | 1.4.4 | `cgm-1.4.4.jar` |
| Gun expansion | `nzgmaddon` | 1.5.0-port.1+1.21.1 | `nzgExpansion-neoforge-1.5.0-port.1+1.21.1.jar` |
| Voice chat | `voicechat` / `voicechat_api` | 1.21.1-2.6.24 / 2.6.24 | `[简单的语音聊天] voicechat-neoforge-1.21.1-2.6.24.jar` |

The instance's installed NeoForge version matches the build baseline. This table records exact tested files, not a promise that every newer version or gun addon is compatible.

## Ammunition classification

Classification inspects the supplied CGM projectile's actual `getItem()` and `getWeapon()` results. Tags are in the 1.21 singular `tags/item` directory.

| Group | Tag | Optional entries |
|---|---|---|
| Low velocity | `superficialtrauma:ammo/low_velocity` | `cgm:basic_bullet` |
| High velocity | `superficialtrauma:ammo/high_velocity` | `cgm:advanced_bullet`, `nzgmaddon:medium_bullet` |
| Shotgun | `superficialtrauma:ammo/shotgun` | `cgm:shell` |

The supplied NineZero port uses **nzgmaddon**, not the former **nzgexpansion** namespace. Other compatible projectile-based addons can extend these tags with a data pack. Unknown ammunition is not silently classified as blunt trauma.

CGM fire/reload cancellation hooks are registered reflectively against the concrete NeoForge events. No CGM classes are required to load Superficial Trauma without CGM.

## Reproduce compatibility checks

PowerShell, from the repository, with Java 21:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer -Penable_migration_gametests=true
.\gradlew.bat runGameTestServer -Penable_migration_gametests=true -Penable_compatibility_mods=true '-Pcompatibility_mods_dir=E:\.minecraft\versions\1.21.1 neoforge test\mods'
.\gradlew.bat runGameTestServer -Penable_migration_gametests=true -Penable_voicechat_mod=true '-Pvoicechat_mod_jar=E:\.minecraft\versions\1.21.1 neoforge test\mods\[简单的语音聊天] voicechat-neoforge-1.21.1-2.6.24.jar' -Penable_compatibility_mods=true '-Pcompatibility_mods_dir=E:\.minecraft\versions\1.21.1 neoforge test\mods'
.\gradlew.bat runClient -Penable_migration_gametests=true -Penable_migration_client_smoke=true -Penable_compatibility_mods=true '-Pcompatibility_mods_dir=E:\.minecraft\versions\1.21.1 neoforge test\mods'
```

The client smoke option creates a new disposable development world in `run-1.21.1`, opens the main medical interfaces, captures screenshots and exits. It is not a multiplayer playtest. Test code is in a separate source set and is not packaged into the mod JAR.

Without compatibility flags, development runs do not load the gun mods. SVC has a separate `enable_voicechat_mod` flag; see [SVC behavior and checks](VOICECHAT_COMPATIBILITY.md). The old Forge SRG-refmap workaround is no longer applied.

## Verification coverage

Automated results and user feedback are deliberately listed separately. A passing callback or synthetic-audio test is not a full multiplayer listening test.

| Area | Evidence | Scope / limit |
|---|---|---|
| Standalone mod | Build, 17 JUnit tests, dedicated GameTest server and isolated client checks passed | SVC and gun mods are optional; checks needing absent optional mods are skipped internally |
| CGM / NineZero | Actual close-range lethal shotgun hit callback and ammunition tags passed | Creates shotgun trauma, preserves the downed player and records the weapon; not an all-weapons playtest |
| Server with optional mods | Dedicated GameTest server passed with Framework, CGM, NineZero and SVC | Uses the exact versions above; SVC server event dispatch is exercised |
| Main medical interfaces | Integrated login, health payload, charging station, workbench, loot and autopsy smoke checks passed | Automated UI / payload checks, not a complete multiplayer regression |
| SVC speech / menus | Automated 3-second speech grace, server rejection, client microphone callbacks and menu blocking passed | Covers the API paths for proximity, whisper and group handling; all real microphone modes and network conditions still need a multiplayer matrix |
| SVC hearing | Synthetic PCM callbacks and state transitions passed; user reported satisfactory in-game echo / strengthened muffling | User listening approval is not presented as exhaustive two-player coverage |
| World audio | Native OpenAL low-pass / echo, ten-second threshold, medical / GUI exclusions, recovery and repeated device-context reload checks passed | Tested with and without SVC / gun baseline; final checks had no OpenAL errors |
| World-audio listening | User reported the final in-game effect works well | Other hardware and third-party acoustic/filter mods remain unverified |

See [downed audio behavior and detailed checks](VOICECHAT_COMPATIBILITY.md) for timing, filter parameters and reproducible client tests.

## Remaining multiplayer / compatibility checks

- CGM aiming/reloading during treatment, every addon weapon, latency-sensitive dragging, shore edges and two-person resuscitation.
- Real SVC conversations across push-to-talk, voice activation, open mic, whisper and groups; reconnects, packet loss and high latency.
- Other mods that replace Minecraft audio processing or attach OpenAL EFX filters. If a device lacks EFX, world audio falls back to volume attenuation without muffling / echo.
- Real-account skins, server restarts, dimension changes and long-running sessions. Existing persistence tests do not cover every account / network combination.
- No standalone Corpse JAR was supplied or tested. The built-in corpse system does not require it. The old Forge compatibility table is historical, not a claim of 1.21 support.

## Observed third-party / development warnings

CGM contains the invalid resource path `cgm:sounds/SOUND-LICENSE.txt`; Minecraft ignores that text file. This warning did not prevent the client or server tests. Development resource-URL and vanilla shader/sound warnings are tracked separately from Superficial Trauma failures. No missing Superficial Trauma model or sound was reported in the final smoke run.

## SHA-256

```text
429EA90A162D7C25C1463EE60979E4D7B1DDB525D9384A3A2A2F45D70BDA03F5  framework-neoforge-1.21.1-0.13.11.jar
50370FC5FDBD39406C8DF50604324C4AA8C6C5C24E4A2AA38F057F6D3CBD9B4F  cgm-1.4.4.jar
A2A08393F35F3E1031C8C3D544644B038B9DCBA302DFD4A381363ABBEA016435  nzgExpansion-neoforge-1.5.0-port.1+1.21.1.jar
7AF3BBC34948CC0F833946FD4B0D2DB7ED608F67A12702BE4DF6F731A4B66290  [简单的语音聊天] voicechat-neoforge-1.21.1-2.6.24.jar
```
