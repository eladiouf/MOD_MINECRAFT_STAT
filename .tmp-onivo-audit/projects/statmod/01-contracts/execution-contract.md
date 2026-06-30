# Execution Contract

## Studio Expectations

The studio may inspect this project broadly across:

- source code
- generated and hand-authored resources
- tests
- plans and specs
- logs and run outputs
- local dependency jars

## Preferred Work Pattern

1. use specs for unclear directional work
2. write plans before large implementation slices
3. execute in scoped commits
4. verify with targeted tests before claiming closure

## Safe Assumptions

- integration work should be isolated behind compat layers where possible
- native UX from upstream mods should be reused before custom UI is introduced
- runtime and release claims must reflect the current NeoForge branch, not stale docs
