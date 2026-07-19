# Enchanted Book Activation Visual Fix Design

**Date:** 2026-07-15  
**Target:** Minecraft 1.20.1, Forge 47.4.10, Java 17

## 1. Problem

Successful enchanted-book study currently broadcasts vanilla entity event
`35`. Minecraft handles that event as a Totem of Undying activation and chooses
a Totem stack for the fullscreen animation. The result is functionally correct
but visually wrong: the consumed enchanted book must appear in the animation,
not a Totem.

## 2. Selected approach

STAT Mod will stop broadcasting entity event `35` for book study. After a
successful server-side XP transaction, the server sends a dedicated S2C message
containing a count-one copy of the exact enchanted book that was studied. The
client validates the payload and calls vanilla
`GameRenderer.displayItemActivation(ItemStack)` with that book.

This reuses the complete vanilla activation motion and timing while changing
only the rendered item. Real Totem behavior is untouched. No mixin and no
custom fullscreen renderer are introduced.

## 3. Transaction and data flow

Before consumption, the server copies the validated held enchanted book and
forces the copy count to one. The existing transactional order remains:

`validate -> accept XP -> consume outside Creative -> synchronize -> send visual`

The visual message is sent only when `XpAwardService.awardBookStudy` returns
true. Failed, interrupted, capped, invalid, or max-level studies send no visual
message and play no sound.

The S2C payload contains only one `ItemStack`. Encoding uses Forge/Minecraft's
standard item-stack codec. Decoding normalizes the count to one and marks the
message invalid unless the item is `Items.ENCHANTED_BOOK`. The client ignores
invalid messages.

## 4. Sound

The Totem activation sound remains part of successful book study. Because
entity event `35` is removed, the server plays `SoundEvents.TOTEM_USE` with
`SoundSource.PLAYERS` at the studying player's position after the award
succeeds. Nearby players may hear it according to normal Minecraft sound
rules; only the studying player receives the fullscreen book animation.

## 5. Networking

The new S2C message uses the next free STAT Mod message ID after the current
snapshot, progress notice, and C2S study-input messages. `NETWORK_PROTOCOL`
increases from `"3"` to `"4"` so older clients cannot connect without the
book-animation handler.

Client-only rendering is invoked behind the existing `DistExecutor` pattern.
No `net.minecraft.client` class is loaded on a dedicated server.

## 6. Verification

Tests must prove:

- the message round-trips the stored enchanted book and normalizes count one;
- non-enchanted and empty stacks are rejected client-side;
- the client handler calls `gameRenderer.displayItemActivation(book)`;
- successful study sends the dedicated message and plays `TOTEM_USE`;
- the study handler no longer contains event byte `35` or
  `broadcastEntityEvent`;
- the network protocol is exactly `"4"`;
- ordinary Totem code and behavior are not modified.

Completion requires the complete test/build suite, JAR inspection, the
required-provider Forge GameTest smoke, and deployment to `test-vrai` with a
timestamped backup and matching SHA-256 hash.

## 7. Out of scope

- changing the XP formula, study duration, consumption, or Creative behavior;
- changing real Totem animations or sounds;
- adding a custom renderer, texture, model, or mixin;
- showing the animation to players other than the studying player.
