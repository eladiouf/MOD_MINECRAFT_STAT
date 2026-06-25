# Wiki MkDocs Material + Vercel — Design

**Date :** 2026-06-21  
**Auteur :** Onivo Studio — Innovation & Research Lead  
**Statut :** Design validé  

---

## 1. Objectif

Remplacer le déploiement GitHub Pages du wiki de STAT Mod par un site MkDocs Material hébergé sur Vercel, avec :
- Pages auto-générées depuis le code source (stats, perks, keybinds)
- Pages rédigées manuellement (installation, commandes, intégrations)
- Thème Material Design avec navigation, recherche, onglets
- Déploiement automatique via GitHub Actions → Vercel

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions                            │
│                                                              │
│  src/main/java/**  ──►  generate_wiki.py  ──►  docs/wiki/*.md│
│  (code source)               ↑                               │
│                        pages manuscrites                     │
│                    (installation.md, etc.)                    │
│                                                              │
│  mkdocs build ──► site/ ──► Vercel Deploy                    │
└─────────────────────────────────────────────────────────────┘
```

### Flux de déploiement

1. Push sur `neoforge-1.21.1` avec changements dans `src/main/java/`, `tools/generate_wiki.py`, `docs/wiki/`, ou `mkdocs.yml`
2. GitHub Actions déclenche le workflow
3. Python génère les pages Markdown dans `docs/wiki/`
4. MkDocs Material compile le site dans `site/`
5. Vercel reçoit le build et déploie

---

## 3. Structure des Fichiers

```
docs/
├── wiki/
│   ├── index.md              # Accueil (manuel)
│   ├── installation.md       # Installation (manuel)
│   ├── commands.md           # Commandes (manuel)
│   ├── dependencies.md       # Dépendances (manuel)
│   ├── stats.md              # Stats (auto-généré)
│   ├── perks.md              # Perks (auto-généré)
│   ├── keybinds.md           # Raccourcis (auto-généré)
│   ├── tensura/
│   │   ├── index.md          # Présentation Tensura (manuel)
│   │   ├── races.md          # Races (manuel)
│   │   └── soul-level.md     # Soul Level (manuel)
│   └── magic/
│       ├── index.md          # Présentation arbre magique (manuel)
│       └── branches.md       # Branches magiques (manuel)
├── mkdocs.yml                # Configuration MkDocs
└── overrides/                # Surcharge thème Material (optionnel)
    ├── main.html
    └── extra.css
```

---

## 4. Adaptation du Générateur Python

### 4.1 Modifications de `tools/generate_wiki.py`

Le script actuel génère un seul `index.md`. Il faut :

1. **Ajouter front-matter YAML** pour MkDocs (titre, description, page rank)
2. **Éclater en fichiers séparés** : `stats.md`, `perks.md`, `keybinds.md`
3. **Corriger les comptes** : 22 stats (14 actives + 8 magiques), 84 perks

### 4.2 Format attendu

```markdown
---
title: Stats
description: Liste complète des 22 stats
nav_order: 4
---

# Stats

## Stats Actives (14)

| Stat | Catégorie | Index |
|------|-----------|-------|
| Strength | COMBAT | 0 |
| ...

## Stats Magiques (8)

| Stat | École | Index |
|------|-------|-------|
| Fire Affinity | FIRE | 14 |
| ...
```

---

## 5. Pages Manuelles

### 5.1 index.md (Accueil)
- Présentation générale de STAT Mod
- Badges : version, licence, CurseForge, Modrinth
- Liens rapides vers les sections

### 5.2 installation.md
- Prérequis : NeoForge 1.21.1, JDK 21
- Installation du mod
- Dépendances requises et optionnelles
- Vérification de l'installation

### 5.3 commands.md
- `/statmod` — commandes admin
- `/magic` — commandes arbre magique

### 5.4 dependencies.md
- Tableau des dépendances (requises, recommandées, optionnelles)
- Versions minimales
- Liens de téléchargement

### 5.5 tensura/
- **index.md** : Présentation de l'intégration Tensura Reincarnated
- **races.md** : Races disponibles et leurs modifieurs de stats
- **soul-level.md** : Système de Soul Level et gates de perks

### 5.6 magic/
- **index.md** : Présentation de l'arbre magique unifié
- **branches.md** : Les 8 écoles (Fire actif, 7 verrouillées)

---

## 6. Configuration MkDocs

`mkdocs.yml` :

```yaml
site_name: STAT Mod Wiki
site_url: https://stat-mod-wiki.vercel.app/
repo_url: https://github.com/eladiouf/MOD_MINECRAFT_STAT
repo_name: eladiouf/MOD_MINECRAFT_STAT
edit_uri: edit/neoforge-1.21.1/docs/wiki/

theme:
  name: material
  language: fr
  logo: assets/logo.png
  favicon: assets/favicon.ico
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

---

## 7. Pipeline de Déploiement

### 7.1 GitHub Actions

Fichier : `.github/workflows/wiki.yml` (remplace l'actuel)

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
      - name: Install MkDocs + plugins
        run: pip install mkdocs mkdocs-material mkdocs-awesome-pages
      - name: Generate wiki pages from source
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

### 7.2 Secrets Vercel Requis

| Secret | Source |
|--------|--------|
| `VERCEL_TOKEN` | Vercel Dashboard → Settings → Tokens → Create Token |
| `VERCEL_ORG_ID` | Vercel — ID de l'équipe (récupérable via `vercel whoami` ou Dashboard) |
| `VERCEL_PROJECT_ID` | Vercel — ID du projet (créé via Import Git ou `vercel link`) |

### 7.3 Procédure de Configuration Vercel

1. Aller sur https://vercel.com/ → Import Git Repository
2. Connecter GitHub et choisir `eladiouf/MOD_MINECRAFT_STAT`
3. Config :
   - Framework Preset : **Other**
   - Root Directory : `./` (racine du projet)
   - Build Command : `pip install mkdocs mkdocs-material mkdocs-awesome-pages && mkdocs build --site-dir site`
   - Output Directory : `site`
4. Copier `VERCEL_ORG_ID` et `VERCEL_PROJECT_ID`
5. Générer un `VERCEL_TOKEN` depuis https://vercel.com/account/tokens
6. Ajouter les 3 secrets dans GitHub Settings → Secrets and variables → Actions

---

## 8. Dépendances Python (requirements)

`requirements-wiki.txt` :

```
mkdocs>=1.6
mkdocs-material>=9.5
mkdocs-awesome-pages>=2.9
```

---

## 9. Validation

1. `python tools/generate_wiki.py` — génère les fichiers Markdown sans erreur
2. `pip install -r requirements-wiki.txt && mkdocs build` — build réussi
3. `mkdocs serve` — site accessible en local sur `http://localhost:8000`
4. CI GitHub Actions → déploiement Vercel réussi
5. URL publique accessible

---

## 10. Post-Mise en Ligne

- Désactiver ou supprimer l'ancien `wiki.yml` (GitHub Pages) après validation
- Mettre à jour README.md avec le nouveau lien wiki
- Ajouter le badge Vercel dans le README
