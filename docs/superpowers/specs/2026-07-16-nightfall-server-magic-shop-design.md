# Nightfall Server Magic Shop Design

## Goal

Configure the NightfallCraft dedicated server's SDM Economy and SDMShop stack as a single permanent magic-scroll market. The shop sells every Iron's Spells 'n Spellbooks spell and every compatible installed addon spell at each valid spell level. Players fund purchases by selling a curated set of raw resources, crops, ores, and monster drops.

## Shop structure

- The shop ID is `default`, matching `config/SDMShop/sdmshop-common.snbt`.
- The shop is always available; it does not use rotating stock.
- Scroll entries are grouped by their registered magic school, not by addon or rarity.
- Addon schools receive their own category automatically when their spell data declares a distinct school.
- Search remains enabled so players can find a spell by translated name.
- Every installed spell appears immediately; there are no level, quest, floor, or class gates.
- Each valid spell level is a separate purchasable entry.
- Scrolls are purchase-only and can never be sold back to the shop.

## Currency and starting balance

- SDM Economy is the only currency used by this shop.
- Every new player begins with exactly 1,000 currency units.
- Stat Mod dungeon points and dungeon coins remain independent and are not converted by SDMShop.
- Existing player balances are never reset when the catalog is regenerated.

## Scroll pricing

The price for a scroll is `rarity base × level multiplier`, rounded to the nearest whole currency unit.

| Rarity | Base price |
|---|---:|
| Common | 500 |
| Uncommon | 1,200 |
| Rare | 3,000 |
| Epic | 7,500 |
| Legendary | 20,000 |

| Spell level | Multiplier |
|---|---:|
| 1 | 1 |
| 2 | 2 |
| 3 | 4 |
| 4 | 7 |
| 5 | 11 |
| 6 | 16 |
| 7 | 22 |
| 8 | 29 |
| 9 | 37 |
| 10 | 46 |

Spells with a lower maximum level only receive their valid levels. Unknown rarities fall back to the Common base. Levels above 10 continue with `round(0.5 × level²)` as the multiplier, never decreasing below the level-10 multiplier.

## Player sales

The shop accepts sell-only bundles from four categories:

1. Basic materials: logs, cobblestone, stone, sand, gravel, clay, and other abundant construction resources.
2. Agriculture: raw crops, seeds, sugar cane, cactus, kelp, and unprocessed animal products.
3. Ores and mining: coal, raw copper, raw iron, raw gold, redstone, lapis, quartz, diamonds, and similarly stable addon raw materials.
4. Creature loot: stackable vanilla and addon monster drops that do not contain unique state.

The catalog rejects weapons, armor, tools, enchanted books, spell scrolls, curios, containers, potions, named items, damaged items, and any item stack with custom NBT. Emeralds are excluded to prevent villager-trading loops. Finished products are excluded when a raw form can be sold.

Bundle prices target slow, predictable income:

| Bundle | Sale value |
|---|---:|
| 64 common blocks | 8 |
| 16 logs | 20 |
| 32 common crops | 25 |
| 16 coal | 40 |
| 8 raw copper | 60 |
| 8 raw iron | 80 |
| 4 raw gold | 100 |
| 16 common monster drops | 20–70 |
| 1 diamond | 180 |
| 1 rare curated addon drop | 100–500 |

No item may be sold to the shop for enough money to buy its ingredients back at a profit. Automated-farm items use the bottom of their range.

## Generation and maintenance

- The authoritative catalog is generated from the installed server JARs, not handwritten spell lists.
- Spell registry data determines spell ID, school, rarity, minimum level, and maximum level.
- The generator creates exact Iron scroll ItemStacks with the spell ID and selected level preserved in NBT.
- Re-running generation is deterministic: identical mods and rules produce identical shop data.
- Generation backs up the previous SDMShop data and writes an inventory report containing counts by addon, school, rarity, and level.
- Missing or malformed spell data is reported and skipped; it never corrupts the rest of the shop.

## Server behavior and safety

- SDMShop purchase notifications remain enabled.
- Admin debug messages remain enabled during setup and are disabled after validation.
- Client keybind opening remains enabled and opens shop ID `default`.
- Shop edits are server-authoritative.
- The final validation must load the dedicated server through Forge, confirm SDMShop and SDM Economy initialize, and confirm the generated scroll stacks deserialize without registry errors.
- The server EULA, `online-mode=false`, OP list, worlds, and unrelated configuration are outside this shop change and remain untouched.

## Acceptance criteria

- A new player receives exactly 1,000 SDM currency units once.
- The default shop contains every installed Iron/addon spell at every valid level.
- Categories match registered magic schools.
- Scroll prices match the rarity/level tables exactly.
- Scrolls cannot be sold.
- Accepted resource bundles pay their configured values.
- Disallowed, damaged, named, or NBT-bearing items cannot be sold.
- Regeneration preserves balances and creates a rollback backup.
- The dedicated server reaches its ready state without shop-related missing dependencies, registry errors, or crashes.
