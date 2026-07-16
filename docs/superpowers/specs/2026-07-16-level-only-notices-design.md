# Level-only notices and binding search design

## Scope

This change removes ordinary XP gain notices while retaining level-up notices,
makes the retained overlay less visually dominant, and prevents the learned
spell search box from extending too far to the right.

## Behavior

- The server creates and sends a progress notice only when at least one level
  was gained.
- The client queue independently rejects messages with no gained level so stale
  or malformed senders cannot restore ordinary XP notices.
- Level-up messages continue to merge by statistic inside the existing merge
  window and retain the highest resulting level.
- The overlay is reduced to a 160 by 20 pixel row, uses lower alpha for its
  background, border, and text, and remains visible for 60 ticks with the
  existing bounded fade.
- The XP-only translation and transport fields remain wire-compatible; they are
  no longer rendered or emitted by normal gameplay.
- The learned-spell search box width is reduced from 116 to 112 pixels.

## Verification

Tests cover server-side level-only calculation, defensive client filtering,
the compact overlay constants and rendering path, and the search-box width.
The complete unit suite, production build, required-provider Forge smoke, JAR
inspection, deployment hash, and remote commit must pass before handoff.
