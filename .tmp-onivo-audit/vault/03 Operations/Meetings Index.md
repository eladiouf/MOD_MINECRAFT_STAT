# Meetings Index

Use this page to track review, planning, plenary, and ops session records.

```dataview
TABLE date, type, session_type, meeting_type, status, related_projects
FROM "vault"
WHERE (type = "meeting" OR type = "meeting-minutes" OR type = "ops-review") AND file.name != "Meetings Index"
SORT date DESC
```
