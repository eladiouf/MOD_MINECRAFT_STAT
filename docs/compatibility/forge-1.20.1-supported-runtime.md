# Forge 1.20.1 supported runtime

This record separates required runtime providers from integrations that remain
prepared, optional, or excluded.

| Component | Version | Status | Notes |
|---|---:|---|---|
| Minecraft | 1.20.1 | verified | Compiles and passes the automated standalone test suite. |
| Forge | 47.4.10 | verified | Compiles and passes the automated suite and required-provider GameTest profile. |
| Java | 17 | verified | Required build and runtime toolchain. |
| Iron's Spells 'n Spellbooks | 3.16.2 | prepared | Minimal runtime is deployed and server-smoked; STAT Mod integration and spell gameplay are not implemented yet. |
| Epic Fight | 20.14.17 | required | Mandatory provider for stamina and attack-speed attributes. |
| Pufferfish's Attributes | 0.8.2 | required | Mandatory provider for specialized movement attributes. |
| ParCool | 3.4.3.3 | prepared | Optional provider for the existing Endurance stamina targets; minimal runtime is server-smoked. |
| Curios API | 5.14.1 | dependency | Required by Iron's Spells. |
| GeckoLib | 4.8.4 | dependency | Required by Iron's Spells and Iron's Lib. |
| Iron's Lib | 2.1.0 | dependency | Required by Iron's Spells. |
| Player Animator | 1.0.2-rc1 | dependency | Required by Iron's Spells. |
| Patchouli | 85 | dependency | Retained because ParCool's guide recipe and loot reference Patchouli resources. |
| Tensura | excluded | unsupported | Deliberately absent from the Forge remake. |

## Status meanings

- `verified`: the stated scope has passed the build and automated tests.
- `required`: Forge must reject world loading when this provider or its minimum
  supported version is missing.
- `dependency`: support library required by one component in the selected
  development profile.
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

## Minimal `test-vrai` development profile

The deployed `test-vrai` profile contains exactly these ten JARs:

- STAT Mod 0.1.0+1.20.1;
- Epic Fight 20.14.17;
- Pufferfish's Attributes 0.8.2;
- ParCool 3.4.3.3;
- Iron's Spells 3.16.2;
- Iron's Lib 2.1.0;
- Curios API 5.14.1;
- GeckoLib 4.8.4;
- Player Animator 1.0.2-rc1;
- Patchouli 85.

The complete set passes the Forge GameTest startup smoke: all mod IDs load,
the overworld and Iron's Spells pocket dimension are created and saved, and the
server shuts down normally. This is a server-startup result, not a completed
client gameplay validation.

Iron's Spells 3.16.2 currently logs two non-fatal loot-table parse errors for
`chests/catacombs/crypt_loot` and `chests/citadel/citadel_tomes`. Both invalid
entries are present inside the unmodified upstream JAR. STAT Mod does not patch
that third-party JAR. Patchouli removes the separate ParCool guide recipe and
loot errors that occur when ParCool is loaded without its guide provider.
