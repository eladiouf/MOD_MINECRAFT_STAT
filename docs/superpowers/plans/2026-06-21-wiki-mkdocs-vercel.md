# Wiki MkDocs Material + Vercel — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy STAT Mod wiki as MkDocs Material site on Vercel with auto-generated + manual pages

**Architecture:** Adapt existing `generate_wiki.py` to output MkDocs-compatible Markdown files, create `mkdocs.yml` with Material theme, write manual pages, replace GitHub Actions workflow to deploy to Vercel

**Tech Stack:** MkDocs, Material for MkDocs, Python 3.11, Vercel, GitHub Actions

---

### Task 1: Adapt generate_wiki.py for MkDocs

**Files:**
- Modify: `tools/generate_wiki.py`

- [ ] **Step 1: Add MkDocs front-matter and split output into files**

Replace the entire `gen_index()` function. The new version:
- Parses stats and perks from source
- Writes separate `docs/wiki/stats.md`, `docs/wiki/perks.md`, `docs/wiki/keybinds.md`
- Each file includes YAML front-matter (title, description, nav_order)

Write to `tools/generate_wiki.py`:

```python
#!/usr/bin/env python3
"""Generate STAT Mod wiki pages for MkDocs."""
import os, re
from pathlib import Path

SRC = Path("src/main/java/tong/statmod")
DOCS = Path("docs/wiki")
DOCS.mkdir(parents=True, exist_ok=True)

def parse_stat_type():
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

def write_page(filename, title, description, nav_order, content_lines):
    lines = [
        "---",
        f"title: {title}",
        f"description: {description}",
        f"nav_order: {nav_order}",
        "---",
        ""
    ]
    lines.extend(content_lines)
    (DOCS / filename).write_text("\n".join(lines))

def gen_stats():
    stats = parse_stat_type()
    lines = ["# Stats", "", "Auto-generated from source code.", "",
             "## Stats Actives (14)", "", "| Stat | Catégorie | Index |",
             "|------|-----------|-------|"]
    for s in stats:
        if s["index"] < 14:
            lines.append(f"| {s['display']} | {s['category']} | {s['index']} |")
    lines += ["", "## Stats Magiques (8)", "", "| Stat | Catégorie | Index |",
              "|------|-----------|-------|"]
    for s in stats:
        if s["index"] >= 14:
            lines.append(f"| {s['display']} | {s['category']} | {s['index']} |")
    write_page("stats.md", "Stats", "Liste complète des 22 stats", 4, lines)

def gen_perks():
    perks = parse_perks()
    lines = ["# Perks", "", "Auto-generated from source code.", "",
             "| Perk | Stat | Niveau Requis |",
             "|------|------|---------------|"]
    for p in perks:
        lines.append(f"| {p['display']} | {p['stat']} | {p['level']} |")
    write_page("perks.md", "Perks", "Liste des 84 perks", 5, lines)

def gen_keybinds():
    lines = ["# Raccourcis Clavier", "", "| Touche | Action |",
             "|--------|--------|",
             "| P | Écran de personnage |",
             "| O | Écran des perks |",
             "| F8 | Écran de debug |"]
    write_page("keybinds.md", "Raccourcis", "Touches par défaut", 9, lines)

def gen_magic_branches():
    lines = ["# Branches Magiques", "",
             "| Branche | Statut | Description |",
             "|---------|--------|-------------|",
             "| Fire | **Actif** | Sorts de feu, dégâts directs |",
             "| Water | Verrouillé | Soins, buffs |",
             "| Earth | Verrouillé | Protection, contrôle |",
             "| Air | Verrouillé | Vélocité, furtivité |",
             "| Lightning | Verrouillé | Dégâts rapides |",
             "| Ice | Verrouillé | Contrôle, ralentissements |",
             "| Arcane | Verrouillé | Magie pure, altération |",
             "| Holy | Verrouillé | Lumière, purification |"]
    write_page("magic/branches.md", "Branches Magiques",
               "Les 8 écoles de l'arbre magique unifié", 8, lines)

if __name__ == "__main__":
    gen_stats()
    gen_perks()
    gen_keybinds()
    gen_magic_branches()
    print("Wiki pages generated in docs/wiki/")
```

- [ ] **Step 2: Test the script**

Run: `python tools/generate_wiki.py`
Expected: Files created in `docs/wiki/` — `stats.md`, `perks.md`, `keybinds.md`, `magic/branches.md`

- [ ] **Step 3: Commit**

```
git add tools/generate_wiki.py docs/wiki/
git commit -m "feat(wiki): adapt generator for MkDocs multi-page output"
```

---

### Task 2: Create mkdocs.yml

**Files:**
- Create: `mkdocs.yml`

- [ ] **Step 1: Write mkdocs.yml**

Create `mkdocs.yml`:

```yaml
site_name: STAT Mod Wiki
site_url: https://stat-mod-wiki.vercel.app/
repo_url: https://github.com/eladiouf/MOD_MINECRAFT_STAT
repo_name: eladiouf/MOD_MINECRAFT_STAT
edit_uri: edit/neoforge-1.21.1/docs/wiki/

theme:
  name: material
  language: fr
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.indexes
    - search.highlight
    - search.share
    - content.tabs.link
  palette:
    - media: "(prefers-color-scheme: light)"
      scheme: default
      primary: indigo
      accent: indigo
      toggle:
        icon: material/weather-night
        name: Mode sombre
    - media: "(prefers-color-scheme: dark)"
      scheme: slate
      primary: indigo
      accent: indigo
      toggle:
        icon: material/weather-sunny
        name: Mode clair

plugins:
  - search
  - awesome-pages

markdown_extensions:
  - admonition
  - pymdownx.details
  - pymdownx.superfences
  - pymdownx.tabbed
  - tables
  - attr_list
  - def_list
  - toc:
      permalink: true

nav:
  - Accueil: index.md
  - Installation: installation.md
  - Commandes: commands.md
  - Dépendances: dependencies.md
  - Stats: stats.md
  - Perks: perks.md
  - Intégration Tensura:
    - Présentation: tensura/index.md
    - Races: tensura/races.md
    - Soul Level: tensura/soul-level.md
  - Arbre Magique:
    - Présentation: magic/index.md
    - Branches: magic/branches.md
  - Raccourcis: keybinds.md
```

- [ ] **Step 2: Create `.markdownlint.json`** (optional config to avoid lint warnings)

Create `.markdownlint.json`:
```json
{
  "MD041": false,
  "MD024": false
}
```

- [ ] **Step 3: Commit**

```
git add mkdocs.yml .markdownlint.json
git commit -m "feat(wiki): add MkDocs Material configuration"
```

---

### Task 3: Create Manual Pages (Accueil, Installation, Commandes, Dépendances)

**Files:**
- Create: `docs/wiki/index.md`
- Create: `docs/wiki/installation.md`
- Create: `docs/wiki/commands.md`
- Create: `docs/wiki/dependencies.md`

- [ ] **Step 1: Create index.md**

Create `docs/wiki/index.md`:

```markdown
---
title: STAT Mod Wiki
description: Wiki officiel de STAT Mod — système de stats et progression NeoForge 1.21.1
nav_order: 0
---

# STAT Mod Wiki

Bienvenue sur le wiki officiel de **STAT Mod**, un système complet de statistiques,
progression et magie pour Minecraft NeoForge 1.21.1.

## Fonctionnalités

- **22 Stats** — 14 stats actives + 8 stats magiques
- **84 Perks** — 6 perks par stat active avec progression par paliers
- **XP & Leveling** — XP de combat et non-combat avec courbes de progression
- **Arbre Magique Unifié** — Intégration Iron's Spellbooks (Phase 1 : tronc commun + Fire)
- **Système Racial** — Intégration Tensura Reincarnated
- **Interface Puffish Skills** — Miroir UI pour perks et arbre magique

## Liens

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/statmod)
- [Modrinth](https://modrinth.com/mod/STAT-Mod_rpg)
- [GitHub](https://github.com/eladiouf/MOD_MINECRAFT_STAT)
- [Signaler un bug](https://github.com/eladiouf/MOD_MINECRAFT_STAT/issues)
```

- [ ] **Step 2: Create installation.md**

Create `docs/wiki/installation.md`:

```markdown
---
title: Installation
description: Procédure d'installation du mod STAT Mod
nav_order: 1
---

# Installation

## Prérequis

- Minecraft 1.21.1
- NeoForge 21.1.228+
- Java 21

## Installation

1. Télécharger NeoForge 1.21.1 et l'installer
2. Télécharger le JAR de STAT Mod depuis CurseForge ou Modrinth
3. Placer le JAR dans le dossier `mods/`
4. Installer les dépendances requises
5. Lancer Minecraft avec le profil NeoForge

## Dépendances

### Requises
- **Tensura Reincarnated** 1.0.2.6+ — système racial et soul level

### Recommandées
- **Iron's Spellbooks** 3.16+ — arbre magique unifié
- **Puffish Skills** — interface utilisateur pour perks et arbre

### Optionnelles
- **Epic Fight** — animations de combat (expérimental)
- **ParCool** — compétences de mouvement
- **Overgeared** — équipement personnalisé
```

- [ ] **Step 3: Create commands.md**

Create `docs/wiki/commands.md`:

```markdown
---
title: Commandes
description: Liste des commandes du mod
nav_order: 2
---

# Commandes

## /statmod

| Sous-commande | Description | Permission |
|---|---|---|
| `list [player]` | Lister toutes les stats | Joueur / Admin |
| `get <stat>` | Voir le niveau d'une stat | Joueur |
| `set <stat\|all> <level>` | Définir le niveau d'une stat | Admin (op 2) |
| `xp <stat> <amount>` | Ajouter de l'XP à une stat | Admin (op 2) |
| `reset` | Réinitialiser toutes les stats | Admin (op 2) |
| `backup` | Sauvegarder les stats | Admin (op 2) |
| `restore` | Restaurer une sauvegarde | Admin (op 2) |
| `profile` | Activer le profiler | Admin (op 2) |
| `benchmark` | Lancer le benchmark d'équilibrage | Admin (op 2) |

## /magic

| Sous-commande | Description | Permission |
|---|---|---|
| `tree` | Afficher l'arbre magique | Joueur |
| `unlock <node>` | Déverrouiller un nœud magique | Joueur |
| `points` | Voir ses points arcanes et d'école | Joueur |
| `reset` | Réinitialiser l'arbre magique | Admin (op 2) |
```

- [ ] **Step 4: Create dependencies.md**

Create `docs/wiki/dependencies.md`:

```markdown
---
title: Dépendances
description: Tableau des dépendances du mod
nav_order: 3
---

# Dépendances

| Mod | Type | Version Min | Lien |
|---|---|---|---|
| Tensura Reincarnated | Requise | 1.0.2.6 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated) |
| Iron's Spellbooks | Recommandée | 3.16 | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) |
| Puffish Skills | Recommandée | — | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/puffish-skills) |
| Epic Fight | Optionnelle | — | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) |
| ParCool | Optionnelle | — | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/parcool) |
| Overgeared | Optionnelle | — | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/overgeared) |
```

- [ ] **Step 5: Commit**

```
git add docs/wiki/index.md docs/wiki/installation.md docs/wiki/commands.md docs/wiki/dependencies.md
git commit -m "feat(wiki): add manual pages (accueil, installation, commandes, dépendances)"
```

---

### Task 4: Create Integration Pages (Tensura, Magic)

**Files:**
- Create: `docs/wiki/tensura/index.md`
- Create: `docs/wiki/tensura/races.md`
- Create: `docs/wiki/tensura/soul-level.md`
- Create: `docs/wiki/magic/index.md`

- [ ] **Step 1: Create tensura/index.md**

Create `docs/wiki/tensura/index.md`:

```markdown
---
title: Intégration Tensura
description: Présentation de l'intégration Tensura Reincarnated
nav_order: 6
---

# Intégration Tensura Reincarnated

STAT Mod s'intègre profondément avec **Tensura Reincarnated** pour fournir
un système racial et de progression lié au niveau d'âme.

## Fonctionnalités

- **Races Tensura** → modifieurs de stats de base
- **Niveau d'âme** → niveau global du joueur
- **Compétences Tensura** → gates de déverrouillage des perks
- **Magie Tensura** → liaison avec l'arbre magique unifié
```

- [ ] **Step 2: Create tensura/races.md**

Create `docs/wiki/tensura/races.md`:

```markdown
---
title: Races
description: Races Tensura et leurs modifieurs de stats
nav_order: 7
---

# Races

Chaque race Tensura applique des modifieurs aux stats de base du joueur.

## Liste des Races

| Race | Force | Agilité | Endurance | Intelligence | Sagesse |
|---|---|---|---|---|---|
| Humain | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 |
| Elfe | 0.8 | 1.2 | 0.9 | 1.1 | 1.1 |
| Nain | 1.2 | 0.8 | 1.3 | 0.9 | 0.8 |
| Ange | 1.1 | 1.0 | 1.0 | 1.1 | 1.3 |
| Démon | 1.3 | 1.1 | 1.1 | 1.0 | 0.9 |
| Dragon | 1.5 | 0.9 | 1.4 | 1.0 | 1.0 |
(Tableau indicatif — les valeurs exactes sont définies dans la configuration)
```

- [ ] **Step 3: Create tensura/soul-level.md**

Create `docs/wiki/tensura/soul-level.md`:

```markdown
---
title: Soul Level
description: Système de niveau d'âme Tensura
nav_order: 8
---

# Soul Level

Le niveau d'âme Tensura est synchronisé avec le niveau global du joueur.

## Paliers

| Niveau d'Âme | Titre | Perks Débloqués |
|---|---|---|
| 1-10 | Éveillé | 1 perk par stat |
| 11-30 | Évolué | 2 perks par stat |
| 31-60 | Transcendant | 3 perks par stat |
| 61-100 | Divin | 4 perks par stat |
| 100+ | Originel | 5-6 perks par stat |
```

- [ ] **Step 4: Create magic/index.md**

Create `docs/wiki/magic/index.md`:

```markdown
---
title: Arbre Magique
description: Présentation de l'arbre magique unifié
nav_order: 8
---

# Arbre Magique Unifié

L'arbre magique unifié intègre **Iron's Spellbooks** dans un système de progression
à 8 écoles.

## Écoles

| École | Statut | Description |
|---|---|---|
| **Fire** | ✅ **Actif** | Sorts de feu, dégâts directs |
| Water | 🔒 Verrouillé | Soins, buffs |
| Earth | 🔒 Verrouillé | Protection, contrôle |
| Air | 🔒 Verrouillé | Vélocité, furtivité |
| Lightning | 🔒 Verrouillé | Dégâts rapides |
| Ice | 🔒 Verrouillé | Contrôle, ralentissements |
| Arcane | 🔒 Verrouillé | Magie pure, altération |
| Holy | 🔒 Verrouillé | Lumière, purification |

## Progression

1. Gagner des **points arcanes** en utilisant la magie
2. Dépenser des points pour déverrouiller des nœuds dans l'arbre
3. Chaque école a son propre **compteur de points**
4. Les races Tensura ont des **affinités racales** (réduisent le coût)
```

- [ ] **Step 5: Commit**

```
git add docs/wiki/tensura/ docs/wiki/magic/
git commit -m "feat(wiki): add integration pages (Tensura, Magic)"
```

---

### Task 5: Create requirements-wiki.txt

**Files:**
- Create: `requirements-wiki.txt`

- [ ] **Step 1: Write requirements file**

Create `requirements-wiki.txt`:
```
mkdocs>=1.6
mkdocs-material>=9.5
mkdocs-awesome-pages>=2.9
```

- [ ] **Step 2: Commit**

```
git add requirements-wiki.txt
git commit -m "chore: add MkDocs Python requirements file"
```

---

### Task 6: Replace GitHub Actions Workflow for Vercel

**Files:**
- Modify: `.github/workflows/wiki.yml`

- [ ] **Step 1: Rewrite wiki.yml for Vercel deployment**

Replace `.github/workflows/wiki.yml`:

```yaml
name: Deploy Wiki to Vercel

on:
  push:
    branches: [neoforge-1.21.1]
    paths:
      - 'src/main/java/**'
      - 'tools/generate_wiki.py'
      - 'docs/wiki/**'
      - 'mkdocs.yml'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - name: Install MkDocs
        run: pip install -r requirements-wiki.txt
      - name: Generate wiki pages
        run: python tools/generate_wiki.py
      - name: Build MkDocs site
        run: mkdocs build --site-dir site
      - name: Deploy to Vercel
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: ./
          vercel-args: '--prod'
```

- [ ] **Step 2: Commit**

```
git add .github/workflows/wiki.yml
git commit -m "feat(ci): replace GitHub Pages with Vercel deploy for wiki"
```

---

### Task 7: Verify Build

- [ ] **Step 1: Install MkDocs dependencies**

Run: `pip install -r requirements-wiki.txt`
Expected: Packages installed successfully

- [ ] **Step 2: Generate wiki pages**

Run: `python tools/generate_wiki.py`
Expected: `docs/wiki/stats.md`, `docs/wiki/perks.md`, `docs/wiki/keybinds.md`, `docs/wiki/magic/branches.md` created

- [ ] **Step 3: Build MkDocs site**

Run: `mkdocs build --site-dir site`
Expected: `site/` directory created with HTML output, no errors

- [ ] **Step 4: Verify the generated files**

Run: `ls docs/wiki/`
Expected: All expected files present

- [ ] **Step 5: Test locally (optional)**

Run: `mkdocs serve`
Expected: Server starts at `http://127.0.0.1:8000`

- [ ] **Step 6: Run project build to ensure no regressions**

Run: `./gradlew build --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Final commit of any generated content**

```
git status
```

Expected: Only `docs/wiki/` auto-generated files may be unstaged. Add them:

```
git add docs/wiki/stats.md docs/wiki/perks.md docs/wiki/keybinds.md docs/wiki/magic/branches.md
git commit -m "chore: add auto-generated wiki pages"
```

---

### Task 8: Final Push

- [ ] **Step 1: Push all changes**

```
git push origin neoforge-1.21.1
```

Expected: All commits pushed successfully
