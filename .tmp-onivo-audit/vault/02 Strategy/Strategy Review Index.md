# Strategy Review Index

Use this page to track weekly and milestone strategy reviews.

```dataview
TABLE date, review_window, status, owners, focus_areas
FROM "vault/02 Strategy"
WHERE type = "strategy_review" AND file.name != "Strategy Review Index"
SORT date DESC
```
