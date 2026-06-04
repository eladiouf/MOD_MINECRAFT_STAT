# Contributing to STAT Mod

## Development Setup

```bash
git clone -b experiment https://github.com/eladiouf/MOD_MINECRAFT_STAT.git
cd MOD_MINECRAFT_STAT
./gradlew build
```

## Project Structure

| Package | Purpose |
|---------|---------|
| `capability` | Player stat storage, capability providers |
| `stats` | Stat definitions, calculators, effect appliers |
| `progression` | XP awards, level-up handling, combat/non-combat XP |
| `skills` | Epic Fight skill registration, passive/active skills |
| `perks` | 42 perks system, tick handlers, damage effects |
| `weapon` | Weapon mastery XP and levels |
| `fatigue` | Fatigue accumulation, debuffs |
| `thirst` | Thirst system, dehydration effects |
| `client` | GUI, HUD, keybindings, rendering |
| `network` | Client-server packet sync |
| `integration` | Epic Fight, ParCool compatibility |
| `world` | Day length, potions, effects |
| `command` | `/statmod` admin commands |
| `api` | Public API for addon mods |

## Code Style

- Java 17, Forge 1.20.1, Parchment mappings
- Use tabs for indentation (Minecraft standard)
- Class names: PascalCase, methods/vars: camelCase
- Constants: UPPER_SNAKE_CASE
- Log with `STATMod.LOGGER`, never `System.out`
- All messages in English

## Adding Features

### Adding a new stat
1. Add enum entry in `StatType.java`
2. Add effects in `StatCalculator.java`
3. Apply in `StatEffectApplier.java`
4. Add XP sources in `CombatXPHandler` / `NonCombatXPHandler`
5. Add config options in `Config.java`

### Adding a new perk
1. Add enum entry in `Perk.java`
2. Implement effect in `PerkDamageHandler` or `PerkTickHandler`
3. Add icon mapping in `ClientPerkCache.java`

### Adding a new skill
1. Implement in `SkillRegistry.onSkillBuild()`
2. Add requirements in `SkillRequirementRegistry.init()`

## Testing

```bash
./gradlew test        # Unit tests
./gradlew runClient   # Manual testing
```

GameTests are in `src/test/java/tong/statmod/gametest/`.

## Pull Requests

1. Branch from `experiment`
2. Include tests for new features
3. Run `./gradlew build` before pushing
4. Update CHANGELOG.md
