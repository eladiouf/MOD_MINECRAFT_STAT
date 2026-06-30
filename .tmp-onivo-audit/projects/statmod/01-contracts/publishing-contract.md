# Publishing Contract

## Publishing Rule

Do not publish from this project using stale branch assumptions.

## Release Preconditions

- runtime target and dependency floor are accurate
- changelog reflects real feature state
- required dependency mods are validated on the release branch
- store copy matches the active loader and version family

## Current Publishing Concern

The public `README.md` still advertises a Forge 1.20.1 release posture while the active branch targets NeoForge 1.21.1.
