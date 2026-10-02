# NeoForge 1.21.1 compatibility baseline

The exact baseline below comes from the user's local test instance:
`E:\.minecraft\versions\1.21.1 neoforge test\mods`.

These third-party JARs are **not redistributed** in this repository or the Superficial Trauma JAR. CGM remains optional.

| Role | Mod ID | Version | Filename |
|---|---|---|---|
| Library | `framework` | 0.13.11 | `framework-neoforge-1.21.1-0.13.11.jar` |
| Gun system | `cgm` | 1.4.4 | `cgm-1.4.4.jar` |
| Gun expansion | `nzgmaddon` | 1.5.0-port.1+1.21.1 | `nzgExpansion-neoforge-1.5.0-port.1+1.21.1.jar` |

Target: Minecraft 1.21.1, NeoForge 21.1.252, Java 21. The instance's installed NeoForge version matches the build baseline.

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
.\gradlew.bat runGameTestServer -Penable_migration_gametests=true -Penable_compatibility_mods=true '-Pcompatibility_mods_dir=E:\.minecraft\versions\1.21.1 neoforge test\mods'
.\gradlew.bat runClient -Penable_migration_gametests=true -Penable_migration_client_smoke=true -Penable_compatibility_mods=true '-Pcompatibility_mods_dir=E:\.minecraft\versions\1.21.1 neoforge test\mods'
```

The client smoke option creates a new disposable development world in `run-1.21.1`, opens the main medical interfaces, captures screenshots and exits. It is not a multiplayer playtest. Test code is in a separate source set and is not packaged into the mod JAR.

Without compatibility flags, the build and development runs do not load any of these three mods. The old Forge SRG-refmap workaround is no longer applied.

## Verified and not verified

- Actual CGM shotgun hit callback: close-range lethal volley produces shotgun trauma, preserves the downed player, and records the weapon for forensics.
- Actual CGM shell and NineZero medium-ammunition tag lookup: passed.
- Dedicated GameTest server with all three supplied JARs: passed.
- Integrated client/server login, health-state payload, station inventory, loot inventory and autopsy report delivery: passed.
- CGM aiming/reloading during treatment, all addon guns, latency-sensitive dragging and two-person resuscitation: require multiplayer regression testing.
- No NeoForge Simple Voice Chat or standalone Corpse JAR was supplied or tested. The built-in corpse system does not require the Corpse mod. The old Forge compatibility table is historical, not a claim of 1.21 support.

## Observed third-party / development warnings

CGM contains the invalid resource path `cgm:sounds/SOUND-LICENSE.txt`; Minecraft ignores that text file. This warning did not prevent the client or server tests. Development resource-URL and vanilla shader/sound warnings are tracked separately from Superficial Trauma failures. No missing Superficial Trauma model or sound was reported in the final smoke run.

## SHA-256

```text
429EA90A162D7C25C1463EE60979E4D7B1DDB525D9384A3A2A2F45D70BDA03F5  framework-neoforge-1.21.1-0.13.11.jar
50370FC5FDBD39406C8DF50604324C4AA8C6C5C24E4A2AA38F057F6D3CBD9B4F  cgm-1.4.4.jar
A2A08393F35F3E1031C8C3D544644B038B9DCBA302DFD4A381363ABBEA016435  nzgExpansion-neoforge-1.5.0-port.1+1.21.1.jar
```
