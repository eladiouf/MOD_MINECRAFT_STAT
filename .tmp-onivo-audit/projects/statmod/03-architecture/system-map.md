# System Map

## Core Layers

- entry point: `tong.statmod.STATMod`
- storage: `PlayerStatData`, `ModAttachments`
- progression: stats, perks, mastery, unlock routing
- stamina and time: `tong.statmod.stamina.*`, `tong.statmod.time.*`
- compat: `integration/` package per external mod family
- client: screens, cache, toasts, HUD routing

## Architecture Notes

- the project already treats optional mod support as explicit integration surfaces
- tests exist across stamina, perks, magic, Puffish, Epic Fight, Tensura, Overgeared, and networking
- mixins are present and should be treated as fragile interfaces

## Current Mismatch To Track

The architecture has moved to NeoForge 1.21.1 while some public-facing documentation still describes the older release identity.
