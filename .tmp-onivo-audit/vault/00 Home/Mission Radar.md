# Mission Radar

This page is the central execution radar for active work.

## All Missions

```dataview
TABLE mission_type, status, owner, target_project, priority, next_review
FROM "vault/03 Operations"
WHERE type = "mission" AND file.name != "Mission Index"
SORT next_review ASC, file.name ASC
```

## Blocked Missions

```dataview
TABLE mission_type, owner, target_project, escalation_rule
FROM "vault/03 Operations"
WHERE type = "mission" AND status = "Blocked"
SORT file.name ASC
```

## Review Prompts

- Which missions need escalation
- Which missions are waiting on verification
- Which missions should be split or closed
