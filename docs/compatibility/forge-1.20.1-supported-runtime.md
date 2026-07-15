# Forge 1.20.1 supported runtime

This record separates versions exercised by the standalone STAT Mod build from
optional integrations that are only prepared or not yet tested.

| Component | Version | Status | Notes |
|---|---:|---|---|
| Minecraft | 1.20.1 | verified | Compiles and passes the automated standalone test suite. |
| Forge | 47.4.10 | verified | Compiles and passes the automated standalone test suite. |
| Java | 17 | verified | Required build and runtime toolchain. |
| Iron's Spells 'n Spellbooks | 3.16.2 | prepared | Runtime batch is catalogued; STAT Mod integration is not implemented yet. |
| Epic Fight | unpinned | untested | The exact Forge 1.20.1 version will be pinned before direct integration. |
| Tensura | excluded | unsupported | Deliberately absent from the Forge remake. |

## Status meanings

- `verified`: the stated scope has passed the build and automated tests.
- `prepared`: files and dependencies are catalogued, but the integration is not
  implemented or validated end to end.
- `untested`: no compatibility claim is made.
- `unsupported`: the component is deliberately excluded.

Client startup, normal world creation, dedicated-server gameplay, Iron's
Spells, Epic Fight, and addon profiles are promoted to `verified` only after
their matching smoke procedures pass. Reaching the title screen alone is not a
compatibility result.
