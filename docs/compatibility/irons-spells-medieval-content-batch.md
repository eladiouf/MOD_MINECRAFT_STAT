# Iron's Spells Medieval Content Batch — Forge 1.20.1

## Installed transaction

The stable batch contains the five prepared addons in `scripts/irons-addons/medieval-content-batch.json`, AzureLib 3.1.3, Deeper and Darker 1.3.3, and FamiliarsLib 1.6.

The initial installation performed on 2026-07-15 changed the active JAR count from 139 to 148. Runtime validation then exposed undeclared binary compatibility problems that Forge metadata alone could not detect.

Magic Circles and Darker Magic do not declare a `minecraft` dependency in their Forge metadata. Their catalog entries therefore use a narrow documented exception that still requires `javafml`, a Forge range containing 47.4.10, and an exact internal version containing `1.20.1`.

Alshanex Familiars 1.20.1 v3.8 was quarantined because it calls an Iron's Spells method absent from 3.16.2. Cataclysm Spellbooks 1.2.9 was quarantined because it references a renderer class absent from Cataclysm 3.31. AzureLib 3.1.3 replaces 3.0.9 because the rejected Cataclysm addon revealed that its declared AzureLib floor was inaccurate. FamiliarsLib 1.6 remains because Geomancy Plus uses its classes without declaring the dependency.

## Commands

Dry run:

`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1`

Apply:

`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/irons-addons/install-medieval-content-batch.ps1 -Apply`

## Runtime acceptance

- Reach the main menu without a new crash report or fatal mod-loading error.
- Open or create a world without registry, datapack, recipe, or mixin failure.
- Cast one previously working Iron's Spells spell.
- Exercise one Darker Magic feature.
- Exercise one Geomancy Plus feature.
- Exercise one feature from Arcanist's Equipage, Constructs' Casting, or Magic Circles.

## Recovery

Close Minecraft. Read the stable catalog and move only its eight listed filenames from `mods` to a new timestamped folder below `disabled-mods`. If logs identify one incompatible addon, quarantine that addon and any addon whose mandatory dependency requires it; otherwise quarantine the stable batch. Never remove STAT Mod, Epic Fight, Iron's Spells, or Project Babylon as part of this recovery.
