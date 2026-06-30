# Projects Index

Connected project adapters and project records are indexed here.

```dataview
TABLE status, stage, owner, repository
FROM "vault/04 Projects"
WHERE type = "project" AND file.name != "Projects Index"
SORT file.name ASC
```

## Related Surfaces

- [[Current Portfolio]]
- [[Template Index]]
- [[Playbook - Project Onboarding]]
