# Identity Contract

## Project Role

`STAT Mod` is the progression authority for the gameplay stack.

It owns:

- player stats and stat families
- perks and progression rewards
- unified stamina and fatigue logic
- core survival overlays such as thirst
- cross-mod gating and identity rules

## Runtime Shape

The project is no longer aligned with the legacy public Forge 1.20.1 identity.

Current local runtime indicators show:

- `NeoForge 21.1.228`
- `Minecraft 1.21.1`
- `Java 21`

## External Identity Risks

- public-facing metadata and docs can become misleading if not refreshed with the actual runtime baseline
- integrations should be described as controlled surfaces, not as the project identity itself
