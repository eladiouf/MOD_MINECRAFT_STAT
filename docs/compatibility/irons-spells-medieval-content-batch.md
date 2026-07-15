# Iron's Spells Medieval Content Batch — Forge 1.20.1

## Installed transaction

The batch contains the seven prepared addon filenames in `scripts/irons-addons/medieval-content-batch.json`, AzureLib 3.0.9, and Deeper and Darker 1.3.3. The latest complete `disabled-mods/irons-content-batch-*.json` is the authoritative source and SHA-256 manifest.

The installation performed on 2026-07-15 changed the active JAR count from 139 to 148. Static verification reported nine matching hashes, zero missing mandatory dependencies, zero introduced primary mod-ID conflicts, zero absent protected IDs, and zero versions below the required floors.

Magic Circles and Darker Magic do not declare a `minecraft` dependency in their Forge metadata. Their catalog entries therefore use a narrow documented exception that still requires `javafml`, a Forge range containing 47.4.10, and an exact internal version containing `1.20.1`.

## Commands

Dry run:

`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1`

Apply:

`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1 -Apply`

## Runtime acceptance

- Reach the main menu without a new crash report or fatal mod-loading error.
- Open or create a world without registry, datapack, recipe, or mixin failure.
- Cast one previously working Iron's Spells spell.
- Exercise one Cataclysm Spellbooks feature.
- Exercise one Darker Magic feature.
- Exercise one Geomancy Plus feature.
- Exercise one feature from Familiars, Arcanist's Equipage, Constructs' Casting, or Magic Circles.

## Recovery

Close Minecraft. Read the latest complete manifest and move only its nine listed filenames from `mods` to a new timestamped folder below `disabled-mods`. If logs identify one incompatible addon, quarantine that addon and any addon whose mandatory dependency requires it; otherwise quarantine all nine. Never remove STAT Mod, Epic Fight, Iron's Spells, or Project Babylon as part of this recovery.
