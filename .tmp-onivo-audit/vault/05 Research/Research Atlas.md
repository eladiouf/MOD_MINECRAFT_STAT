# Research Atlas

This page is the navigation surface for durable research notes and synthesis.

```dataview
TABLE date, area, status, related_projects
FROM "vault/05 Research"
WHERE type = "research" AND file.name != "Research Atlas"
SORT date DESC
```
