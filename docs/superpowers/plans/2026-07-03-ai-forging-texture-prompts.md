# AI Forging Texture Prompt Pack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a reusable ChatGPT Images prompt pack for the full `rough_*` forge intermediary family, plus the first ready-to-run prompts grouped by shape and material.

**Architecture:** Keep the work in documentation assets only. One document captures the reusable prompt system and one document captures the exact production prompts the user can paste into ChatGPT Images without extra interpretation.

**Tech Stack:** Markdown docs, ChatGPT Images prompt engineering, Minecraft item texture constraints

## Global Constraints

- Output style: true Minecraft-style pixel-art item textures
- Visual direction: realistic forged metal with a restrained mystical accent
- Generation method: material-specific generation from the start
- Initial scope: the full `rough_*` family
- AI role: generate near-final source images that are designed to downscale cleanly into game textures

---

### Task 1: Define The Prompt System

**Files:**
- Create: `docs/superpowers/specs/2026-07-03-ai-forging-texture-prompt-system.md`
- Modify: `docs/superpowers/specs/2026-07-03-ai-forging-texture-generation-design.md`

**Interfaces:**
- Consumes: visual rules and scope from `docs/superpowers/specs/2026-07-03-ai-forging-texture-generation-design.md`
- Produces: a stable prompt template with shared negative constraints, family modifiers, and material modifiers

- [ ] **Step 1: Write the prompt system document**
- [ ] **Step 2: Add one short handoff note in the generation design spec pointing to the prompt system**
- [ ] **Step 3: Review both docs for contradictions**

### Task 2: Produce The Exact Prompt Pack

**Files:**
- Create: `docs/superpowers/specs/2026-07-03-ai-forging-texture-prompt-pack.md`
- Modify: `docs/superpowers/specs/2026-07-03-ai-forging-texture-prompt-system.md`

**Interfaces:**
- Consumes: prompt template, family modifiers, and material language from Task 1
- Produces: exact copy-paste prompts for the `rough_*` production wave

- [ ] **Step 1: Write the shared master prompt**
- [ ] **Step 2: Write shape-specific prompts for all rough families**
- [ ] **Step 3: Write material-specific prompt blocks for the first production set**
- [ ] **Step 4: Add usage notes explaining how to mix shape and material blocks into final prompts**

### Task 3: Final Consistency Pass

**Files:**
- Modify: `docs/superpowers/specs/2026-07-03-ai-forging-texture-prompt-pack.md`

**Interfaces:**
- Consumes: the full prompt pack from Task 2
- Produces: a cleaner production-ready prompt library with no placeholders

- [ ] **Step 1: Scan for vague wording that could cause image drift**
- [ ] **Step 2: Normalize repeated constraints so all prompts stay visually coherent**
- [ ] **Step 3: Make sure the prompt pack covers the full `rough_*` wave without missing a family**
