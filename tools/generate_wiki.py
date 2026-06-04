#!/usr/bin/env python3
"""Generate STAT Mod wiki from Java source code."""
import os, re, json
from pathlib import Path

SRC = Path("src/main/java/tong/statmod")
DOCS = Path("docs/wiki")
DOCS.mkdir(parents=True, exist_ok=True)

def parse_stat_type():
    """Parse StatType.java to extract stat definitions."""
    content = (SRC / "stats/StatType.java").read_text()
    stats = []
    for match in re.finditer(r'(\w+)\((\d+),\s*StatCategory\.(\w+),\s*"([^"]+)"', content):
        stats.append({
            "name": match.group(1),
            "index": int(match.group(2)),
            "category": match.group(3),
            "display": match.group(4)
        })
    return stats

def parse_perks():
    """Parse Perk.java to extract perk definitions."""
    content = (SRC / "perks/Perk.java").read_text()
    perks = []
    for match in re.finditer(r'(\w+)\((\d+),\s*StatType\.(\w+),\s*(\d+),\s*"([^"]+)"', content):
        perks.append({
            "name": match.group(1),
            "id": int(match.group(2)),
            "stat": match.group(3),
            "level": int(match.group(4)),
            "display": match.group(5)
        })
    return perks

def gen_index():
    """Generate wiki index page."""
    stats = parse_stat_type()
    perks = parse_perks()

    lines = []
    lines.append("# STAT Mod Wiki")
    lines.append("")
    lines.append("Auto-generated from source code. Last updated: " + os.popen("date").read().strip())
    lines.append("")

    lines.append("## Stats (23)")
    lines.append("| Stat | Category | Index |")
    lines.append("|------|----------|-------|")
    for s in stats:
        lines.append(f"| {s['display']} | {s['category']} | {s['index']} |")
    lines.append("")

    lines.append("## Perks (42)")
    lines.append("| Perk | Stat | Level Required |")
    lines.append("|------|------|----------------|")
    for p in perks:
        lines.append(f"| {p['display']} | {p['stat']} | {p['level']} |")
    lines.append("")

    lines.append("## Commands")
    lines.append("| Command | Description | Permission |")
    lines.append("|---------|-------------|------------|")
    lines.append("| `/statmod list [player]` | List all stats | Player / Admin |")
    lines.append("| `/statmod get <stat>` | Get stat level | Player |")
    lines.append("| `/statmod set <stat|all> <level>` | Set stat level | Admin (2) |")
    lines.append("| `/statmod xp <stat> <amount>` | Add XP | Admin (2) |")
    lines.append("| `/statmod reset` | Reset all stats | Admin (2) |")
    lines.append("| `/statmod backup` | Save stats backup | Admin (2) |")
    lines.append("| `/statmod restore` | Restore from backup | Admin (2) |")
    lines.append("| `/statmod preset <easy|normal|hard>` | Apply config preset | Admin (2) |")
    lines.append("| `/statmod profile` | Toggle profiler | Admin (2) |")
    lines.append("| `/statmod benchmark` | Run balance benchmark | Admin (2) |")
    lines.append("")

    lines.append("## Keybindings")
    lines.append("| Key | Action |")
    lines.append("|-----|--------|")
    lines.append("| P | Character Screen |")
    lines.append("| O | Perk Screen |")
    lines.append("| F8 | Debug Screen |")
    lines.append("")

    outline = "\n".join(lines)
    (DOCS / "index.md").write_text(outline)
    print(f"Wiki generated at {DOCS}/index.md")

if __name__ == "__main__":
    gen_index()
