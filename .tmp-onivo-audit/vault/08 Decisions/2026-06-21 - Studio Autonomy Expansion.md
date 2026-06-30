---
type: decision
status: Accepted
decision_id: STUDIO-DEC-001
date: 2026-06-21
area: governance
owners:
  - Founder Desk
related_projects: []
tags:
  - decision
  - governance
  - autonomy
---

# 2026-06-21 — Studio Autonomy Expansion

## Context

In the plenary session of 2026-06-21 the Founder Desk verbally amended the studio autonomy posture with the directive:

> *"LE STUDIO DOIT ETRE AUTONOME ET PRENDRE SES PROPRES DECISIONS EN REUNION, TOUT DOCUMENTER."*

The prior autonomy policy required escalation for any decision touching documentation taxonomy, integration status, or contradictions between project constitution and code.

## Decision

The studio is now authorized to take and act on the following decisions without prior Founder approval, provided every decision produces a numbered decision record:

- resolve contradictions between project `CLAUDE.md` files and observed runtime by amending the `CLAUDE.md`
- arbitrate documentation taxonomy (plan dirs, spec dirs, indexes)
- open and execute postmortems
- complete onboarding playbooks
- re-order mission packages within an active project
- mark integrations as `experimental optional`, `production optional`, or `removed`

The Founder Desk retains exclusive authority on:

- the studio constitution itself
- external publication go-live
- irreversible product pivots
- monetization and licensing
- any decision the studio explicitly flags as escalated

All studio decisions remain reversible by the Founder.

## Consequences

- the autonomy policy text in `governance/autonomy-policy.md` is updated with a Founder Amendment block
- every plenary session now produces minutes and at least one decision record when contested items are present
- the studio can no longer hide behind "awaiting Founder" when a runtime contradiction is observed

## Follow-up

- `governance/autonomy-policy.md` amended in the same commit
- `vault/08 Decisions/2026-06-21 - STAT Mod Plenary Session Minutes.md` references this decision
