---
type: risk
status: Open
severity: S2-High
owner: Executive Orchestrator
date: 2026-06-21
next_review: 2026-06-22
related_projects:
  - STAT Mod
tags:
  - risk
  - statmod
---
# 2026-06-21 - STAT Mod Dirty Worktree Masks Release Readiness

## Description

The project worktree contains multiple unrelated local changes, logs, and scratch surfaces that make release-readiness judgment noisier.

## Impact

Verification and release calls can be polluted by unrelated edits or runtime artifacts.

## Signals

- modified log files
- untracked local exploration and dependency surfaces
- active code changes outside the studio bridge work

## Mitigation

- classify release blockers separately from ongoing local exploration
- use the studio release and verification tracks as curated control surfaces

## Escalation

Escalate if a release decision cannot be made without first separating durable changes from local noise.
