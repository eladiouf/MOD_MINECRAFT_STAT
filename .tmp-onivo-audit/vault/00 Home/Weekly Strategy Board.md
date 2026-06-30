# Weekly Strategy Board

This page is the founder-facing strategy checkpoint surface.

## Active Projects

```dataview
TABLE status, stage, owner
FROM "vault/04 Projects"
WHERE type = "project"
SORT stage ASC, file.name ASC
```

## Open Strategy Reviews

```dataview
TABLE date, review_window, status, owners, focus_areas
FROM "vault/02 Strategy"
WHERE type = "strategy_review"
SORT date DESC
```

## Active Decisions

```dataview
TABLE date, area, status, related_projects
FROM "vault/08 Decisions"
WHERE type = "decision"
SORT date DESC
```

## Highest Attention Risks

```dataview
TABLE severity, status, owner, related_projects, next_review
FROM "vault/03 Operations"
WHERE type = "risk"
SORT severity ASC, next_review ASC
```

## Review Prompts

- What should be accelerated this week
- What should be frozen this week
- Which missions are creating the most leverage
- Which risks are becoming structural
