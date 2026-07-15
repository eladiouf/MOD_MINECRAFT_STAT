# Forge 1.20.1 supported runtime

This record separates required runtime providers from integrations that remain
prepared, optional, or excluded.

| Component | Version | Status | Notes |
|---|---:|---|---|
| Minecraft | 1.20.1 | verified | Compiles and passes the automated standalone test suite. |
| Forge | 47.4.10 | verified | Compiles and passes the automated suite and required-provider GameTest profile. |
| Java | 17 | verified | Required build and runtime toolchain. |
| Iron's Spells 'n Spellbooks | 3.16.2 | prepared | Runtime batch is catalogued; STAT Mod integration is not implemented yet. |
| Epic Fight | 20.14.17 | required | Mandatory provider for stamina and attack-speed attributes. |
| Pufferfish's Attributes | 0.8.2 | required | Mandatory provider for specialized movement attributes. |
| Tensura | excluded | unsupported | Deliberately absent from the Forge remake. |

## Status meanings

- `verified`: the stated scope has passed the build and automated tests.
- `required`: Forge must reject world loading when this provider or its minimum
  supported version is missing.
- `prepared`: files and dependencies are catalogued, but the integration is not
  implemented or validated end to end.
- `untested`: no compatibility claim is made.
- `unsupported`: the component is deliberately excluded.

Client startup, normal world creation, dedicated-server gameplay, Iron's
Spells, and addon profiles are promoted to `verified` only after
their matching smoke procedures pass. Reaching the title screen alone is not a
compatibility result.

## Verified runtime profiles

The required-provider Forge GameTest dedicated-server profile is verified. It
loads STAT Mod with Epic Fight 20.14.17 and Pufferfish's Attributes 0.8.2,
creates all three vanilla dimensions, completes the finite GameTest run, saves
them, and shuts down without a fatal log signature. It does not verify normal
client world creation, combat feel, or Iron's Spells gameplay.

Run the reproducible profile with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-gametest-server.ps1 `
  -ProviderModsDirectory "C:\path\to\the\validated\client\mods"
```

The script requires the exact audited provider filenames, remaps their classes
and Mixin refmaps for the ForgeGradle development runtime, and removes its
temporary copies after the run.
