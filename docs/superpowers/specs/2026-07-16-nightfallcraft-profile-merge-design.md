# NightfallCraft Profile Merge Design

## Goal

Use `NightfallCraft - The Casket of Reveries (2)` as the base Forge 1.20.1 pack and add the RPG development stack currently validated in `test-vrai` without replacing NightfallCraft's worlds, options, resource packs, or configuration tree.

## Compatibility policy

- Keep NightfallCraft's 196-mod inventory as the base.
- Compare JARs by primary Forge mod ID, not filename.
- Add the 29 `test-vrai` JARs whose primary mod ID is absent from NightfallCraft.
- Keep NightfallCraft's copy for the 14 duplicate mod IDs.
- Exception: replace Iron's Spellbooks `3.15.4` with `3.16.2`, because Stat Mod requires `3.16.2` or newer.
- Keep Epic Fight `20.14.17`, Curios `5.14.1`, FTB Teams `2001.3.2`, Lootr `0.7.35.94`, and the other already-compatible NightfallCraft dependencies.
- Declare Stat Mod compatible with Forge `47.4.4`, matching the pack loader; verify the complete project against the existing Forge build and test suite before deployment.

## Safety and rollback

- Never delete or overwrite saves, configs, default configs, resource packs, shaders, or options.
- Create a timestamped `statmod-backups` directory inside the target profile.
- Back up every pre-existing JAR that is replaced.
- Record SHA-256 inventories before and after the merge plus the list of added files.
- Copy the freshly built Stat Mod JAR rather than the stale `test-vrai` copy.

## Verification

- Run the Forge dependency contract test red/green for the `47.4.4` floor.
- Run `gradlew clean build` and require zero test failures.
- Verify exactly one JAR per primary mod ID after the merge.
- Verify all mandatory Stat Mod dependencies and minimum versions are present.
- Confirm the target mod count is 225: 196 base + 29 absent primary mod IDs.

