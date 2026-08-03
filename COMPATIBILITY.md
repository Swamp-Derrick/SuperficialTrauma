# Compatibility baseline

The files below are the exact local development baseline. They live in `../Compatiblemods` and are not redistributed inside this repository or the Superficial Trauma JAR.

| Role | Mod ID | Version | Filename |
|---|---|---:|---|
| CGM library | `framework` | 0.6.16 | `framework-forge-1.20.1-0.6.16.jar` |
| Gun system | `cgm` | 1.4.20 | `CGM-Unofficial-1.4.20+Forge+1.20.1+valfucked.jar` |
| Medium-ammo gun expansion | `nzgexpansion` | 1.4.4 | `nzgExpansionUnofficialSD-1.4.4-1.20.1.jar` |
| Additional gun expansion | `redundantguns` | 1.0.0 | `redundantGunsUnofficialSD-1.0.0-1.20.1.jar` |
| Corpse integration | `corpse` | 1.20.1-1.0.23 | `corpse-forge-1.20.1-1.0.23.jar` |
| Voice integration | `voicechat`, `voicechat_api` | 1.20.1-2.6.21 / API 2.6.20 | `voicechat-forge-1.20.1-2.6.21.jar` |

## Dependency chain

```text
Framework 0.6.16
└─ CGM Unofficial 1.4.20
   └─ NineZero's Gun Expansion Unofficial 1.4.4
      └─ Redundant Guns Unofficial 1.0.0
```

Corpse and Simple Voice Chat are independent optional integrations. Superficial Trauma must continue to load when any optional integration is absent.

## Development-run requirements

CGM's production JAR contains an SRG refmap. The Gradle run configuration therefore disables that production refmap when the compatibility pack is enabled, allowing the original Mojang-mapped Mixin annotation names to resolve in Forge userdev. This setting affects development runs only and is not written into the released mod JAR.

## Known baseline warnings

The 2026-08-04 compatibility-enabled client smoke test reached the main-menu resource and sound initialization stage. The following warnings originate in the pinned compatibility JARs and did not prevent startup:

- CGM contains the invalid uppercase resource path `assets/cgm/sounds/SOUND-LICENSE.txt`; Minecraft ignores that license text resource.
- CGM declares an optional Simple Planes Mixin target while Simple Planes is absent.
- Redundant Guns references a missing `carbine_rifle_rearsight_folded` special model.
- NineZero's Gun Expansion reports a missing `cgm:item/pump_shotgun` texture for the double-barreled shotgun model.
- NineZero's Gun Expansion registers four Bullpup sound events whose sound files are absent.

These warnings are tracked separately from Superficial Trauma. They should be repaired in the respective compatibility projects before a polished modpack release.

## Checksums

SHA-256 checksums:

```text
9C92918E6D4B3BB78D45CEEE532941D709C0E37ABA79E70FC76BE91A3DAF4EAD  framework-forge-1.20.1-0.6.16.jar
779988F5029EE895068675D08B5A86947238583D014C84C2A4B352A875EA1436  CGM-Unofficial-1.4.20+Forge+1.20.1+valfucked.jar
002498E23B960643E4413359906CDCE2D596F45FF6C272EDF5CD078069A0DB3F  nzgExpansionUnofficialSD-1.4.4-1.20.1.jar
5A24D80E0A88AFA1C5FBB08F519D96BA9862E852EEEB9880C802BC2AD3FF4EC4  redundantGunsUnofficialSD-1.0.0-1.20.1.jar
8D77C5FC8DC981C750A8E66D4665F055457F748F0CE49A303D9BC50347BDBF7C  corpse-forge-1.20.1-1.0.23.jar
D752FC395F5485DE6636FAAF629599BF3C04C95077B017041AD0FE456B32833B  voicechat-forge-1.20.1-2.6.21.jar
```
