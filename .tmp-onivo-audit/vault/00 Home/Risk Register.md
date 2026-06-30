# Risk Register

This page tracks durable studio and project risks.

## Open Risks

```dataview
TABLE severity, status, owner, related_projects, next_review
FROM "vault/03 Operations"
WHERE type = "risk" AND file.name != "Risk Register"
SORT severity ASC, next_review ASC
```

## Risks Needing Review

```dataview
TABLE severity, owner, next_review, related_projects
FROM "vault/03 Operations"
WHERE type = "risk" AND next_review AND file.name != "Risk Register"
SORT severity ASC, next_review ASC
```

## Review Prompts

- Which risk can be retired
- Which risk needs mitigation now
- Which risk should escalate to council
