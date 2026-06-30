# Mission Index

Use this page to track mission packages across the studio.

```dataview
TABLE mission_type, status, owner, target_project, priority, next_review
FROM "vault/03 Operations"
WHERE type = "mission" AND file.name != "Mission Index"
SORT next_review ASC, priority ASC
```

## Related Surfaces

- [[Mission Radar]]
- [[Risk Register]]
- [[Weekly Strategy Board]]
