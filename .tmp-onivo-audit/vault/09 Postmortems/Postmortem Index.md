# Postmortem Index

This page indexes studio and project postmortems.

| Id | Date | Project | Subject | Status |
|---|---|---|---|---|
| `STAT-PM-001` | 2026-06-21 | STAT Mod | Mahou + Elementals integration removal | Closed-Provisional |

```dataview
TABLE date, status, related_projects
FROM "vault/09 Postmortems"
WHERE type = "postmortem" AND file.name != "Postmortem Index"
SORT date DESC
```
