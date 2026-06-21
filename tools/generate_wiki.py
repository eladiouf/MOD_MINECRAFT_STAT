#!/usr/bin/env python3
"""Generate STAT Mod wiki from Java source — MkDocs multi-page output."""
import re
from pathlib import Path

SRC = Path("src/main/java/tong/statmod")
DOCS = Path("docs/wiki")


def parse_stat_type():
    """Parse StatType.java to extract stat definitions."""
    content = (SRC / "stats/StatType.java").read_text()
    stats = []
    for match in re.finditer(
        r'(\w+)\((\d+),\s*StatFamily\.(\w+),\s*"([^"]+)"', content
    ):
        stats.append({
            "name": match.group(1),
            "index": int(match.group(2)),
            "category": match.group(3),
            "display": match.group(4),
        })
    return stats


def parse_perks():
    """Parse Perk.java to extract perk definitions."""
    content = (SRC / "perks/Perk.java").read_text()
    perks = []
    for match in re.finditer(
        r'(\w+)\((\d+),\s*StatType\.(\w+),\s*\w+\.(\w+),\s*"([^"]+)"', content
    ):
        perks.append({
            "name": match.group(1),
            "id": int(match.group(2)),
            "stat": match.group(3),
            "tier": match.group(4),
            "display": match.group(5),
        })
    return perks


def front_matter(title, description, nav_order):
    return f"""---
title: {title}
description: "{description}"
nav_order: {nav_order}
---"""


def gen_stats():
    stats = parse_stat_type()
    active = stats[:14]
    magic = stats[14:]

    lines = [front_matter("Stats", "Liste complète des 22 stats", 4)]
    lines.append("")
    lines.append("# Stats")
    lines.append("")
    lines.append(f"## Stats Actives ({len(active)})")
    lines.append("| Stat | Catégorie | Index |")
    lines.append("|------|-----------|-------|")
    for s in active:
        lines.append(f"| {s['display']} | {s['category']} | {s['index']} |")
    lines.append("")
    lines.append(f"## Stats Magiques ({len(magic)})")
    lines.append("| Stat | Catégorie | Index |")
    lines.append("|------|-----------|-------|")
    for s in magic:
        lines.append(f"| {s['display']} | {s['category']} | {s['index']} |")
    lines.append("")

    (DOCS / "stats.md").write_text("\n".join(lines), newline="\n")
    print(f"Generated docs/wiki/stats.md ({len(stats)} stats)")


def gen_perks():
    perks = parse_perks()

    lines = [front_matter("Perks", "Liste des 84 perks", 5)]
    lines.append("")
    lines.append("# Perks")
    lines.append("")
    lines.append("| Perk | Stat | Tier |")
    lines.append("|------|------|------|")
    for p in perks:
        lines.append(f"| {p['display']} | {p['stat']} | {p['tier']} |")
    lines.append("")

    (DOCS / "perks.md").write_text("\n".join(lines), newline="\n")
    print(f"Generated docs/wiki/perks.md ({len(perks)} perks)")


def gen_keybinds():
    lines = [front_matter("Raccourcis", "Touches par défaut", 9)]
    lines.append("")
    lines.append("# Raccourcis")
    lines.append("")
    lines.append("| Touche | Action |")
    lines.append("|--------|--------|")
    lines.append("| P | Écran de personnage |")
    lines.append("| O | Écran des perks |")
    lines.append("| F8 | Écran de debug |")
    lines.append("")

    (DOCS / "keybinds.md").write_text("\n".join(lines), newline="\n")
    print("Generated docs/wiki/keybinds.md")


def gen_magic_branches():
    (DOCS / "magic").mkdir(parents=True, exist_ok=True)

    lines = [front_matter(
        "Branches Magiques", "Les 8 écoles de l'arbre magique unifié", 8
    )]
    lines.append("")
    lines.append("# Branches Magiques")
    lines.append("")
    lines.append("| Branche | Statut | Description |")
    lines.append("|---------|--------|-------------|")
    lines.append("| Fire | **Actif** | Sorts de feu, dégâts directs |")
    lines.append("| Water | Verrouillé | Soins, buffs |")
    lines.append("| Earth | Verrouillé | Protection, contrôle |")
    lines.append("| Air | Verrouillé | Vélocité, furtivité |")
    lines.append("| Lightning | Verrouillé | Dégâts rapides |")
    lines.append("| Ice | Verrouillé | Contrôle, ralentissements |")
    lines.append("| Arcane | Verrouillé | Magie pure, altération |")
    lines.append("| Holy | Verrouillé | Lumière, purification |")
    lines.append("")

    (DOCS / "magic/branches.md").write_text("\n".join(lines), newline="\n")
    print("Generated docs/wiki/magic/branches.md")


def main():
    DOCS.mkdir(parents=True, exist_ok=True)
    gen_stats()
    gen_perks()
    gen_keybinds()
    gen_magic_branches()
    print("\nAll wiki files generated.")


if __name__ == "__main__":
    main()
