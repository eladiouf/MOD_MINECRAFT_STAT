# Enchantment Anvil Forge Tools Design

Date: 2026-07-01
Repo: `MOD_MINECRAFT_STAT`
Scope: `statmod` forge support tools for `enchantment_anvil`

## Objective

Turn the `enchantment_anvil` support slot into a real forge-tool slot by introducing two `statmod` items:

- `statmod:basic_forge_tongs`
- `statmod:basic_smithing_hammer`

These tools are required by advanced essence recipes, are not consumed on craft, and give the station a stronger Overgeared-like identity without introducing full durability or tier progression yet.

## Final Decision

- Add two forge support items now.
- Make them real registered items, visible in the creative tab, with lang/model/recipe resources.
- Keep them non-consumable.
- Require them in the `enchantment_anvil` support slot for all current essence recipes.
- Split recipe families by weapon profile:
  - `basic_forge_tongs`
    - `runic_katana`
    - `runic_rapier`
    - `runic_spear`
    - `runic_longsword`
    - `runic_glaive`
  - `basic_smithing_hammer`
    - `runic_claymore`
    - `runic_greathammer`
    - `runic_greataxe`

## Constraints

- Do not add durability yet.
- Do not add tool tiers yet.
- Do not consume the tool on output pickup.
- Keep `infusion_forge` grip-based.
- Update current essence JSON fallback resources so they stay aligned with the Java anvil catalog.

## Testing

- Item rule tests must recognize the new support tools.
- Anvil recipe catalog tests must prove recipes no longer match old grips.
- Resource consistency tests must stay green after the recipe migration.
