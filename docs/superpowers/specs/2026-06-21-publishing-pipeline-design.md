# Publishing Pipeline — STAT Mod v1.2.0

**Date :** 2026-06-21  
**Auteur :** Onivo Studio — Innovation & Research Lead  
**Statut :** Design validé, prêt pour implémentation  
**Mission liée :** Nouvelle mission (post-M4/M9)

---

## 1. Objectif

Automatiser la publication de STAT Mod sur **GitHub Releases**, **CurseForge** et **Modrinth** via une pipeline CI/CD corrigée et un workflow de release par tag git.

**Cible :** v1.2.0 (inclut arbre magique Iron's Spellbooks Phase 1, audit M1, port NeoForge finalisé)

---

## 2. Problèmes Identifiés sur le CI Existant

Le fichier `.github/workflows/build.yml` contient des configurations périmées du port Forge 1.20.1 vers NeoForge 1.21.1 :

| # | Problème | Fichier | Gravité |
|---|----------|---------|---------|
| 1 | JDK 17 au lieu de JDK 21 | `build.yml` line 20 | 🔴 Bloquant |
| 2 | `game_versions: '1.20.1'` au lieu de `1.21.1` | `build.yml` CurseForge | 🔴 Bloquant |
| 3 | `loaders: forge` au lieu de `neoforge` | `build.yml` Modrinth | 🔴 Bloquant |
| 4 | `game-versions: '1.20.1'` au lieu de `1.21.1` | `build.yml` Modrinth | 🔴 Bloquant |
| 5 | Epic Fight en `requiredDependency` au lieu d'optional | `build.yml` CurseForge + Modrinth | 🟡 Medium |
| 6 | Absence de dépendance Tensura (hard dependency) | `build.yml` les deux stores | 🟡 Medium |
| 7 | Aucun tag git existant | Repo | 🟡 Medium |
| 8 | Version `1.0.0` périmée | `gradle.properties` | 🟢 Low |

---

## 3. Corrections CI

### 3.1 JDK Version

```yaml
# build.yml — avant
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'

# build.yml — après
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
```

### 3.2 CurseForge — Versions et Dépendances

```yaml
# build.yml — avant
game_versions: '1.20.1'
relations: epic-fight:requiredDependency

# build.yml — après
game_versions: '1.21.1'
relations: |
    tensura-reincarnated:requiredDependency
    irons-spellbooks:optionalDependency
    epic-fight:optionalDependency
```

### 3.3 Modrinth — Versions, Loader et Dépendances

```yaml
# build.yml — avant
game-versions: '1.20.1'
loaders: forge
dependencies: |
    epic-fight(required)

# build.yml — après
game-versions: '1.21.1'
loaders: neoforge
dependencies: |
    tensura-reincarnated(required)
    irons-spellbooks(optional)
    epic-fight(optional)
```

### 3.4 Ajout — Support des dépendences multiples (relations multiligne)

Le format `itsmeow/curseforge-upload@v3` supporte les relations multilignes en YAML. Utiliser la syntaxe block `|` pour lister plusieurs dépendances.

---

## 4. Version & CHANGELOG

### 4.1 Bump de version

Fichier : `gradle.properties`

```properties
mod_version=1.2.0
```

### 4.2 Mise à jour CHANGELOG

Ajouter en-tête dans `CHANGELOG.md` :

```markdown
## v1.2.0 (2026-06-21)

### ✨ Nouvelles fonctionnalités
- Arbre magique unifié Iron's Spellbooks (Phase 1) — tronc commun + Fire actif
- 8 écoles magiques structurellement présentes (verrouillées)
- Système de points arcanes et d'école
- Bridge évènementiel IronSpellEventBridge
- Intégration Puffish Skills — miroir UI arbre magique
- Commande `/magic` pour la gestion arbre

### 🐛 Corrections
- Audit M1 — 10 correctifs (Forge→NeoForge résidus)
- Correction mixins SwordSoaring
- Synchronisation état magique à la connexion

### 🔧 Maintenance
- Port NeoForge 1.21.1 finalisé
- Dépendances mises à jour
```

---

## 5. Métadonnées Stores

### 5.1 Fiche CurseForge

| Champ | Valeur |
|-------|--------|
| **Name** | STAT Mod |
| **Description** | Standalone stat system with XP, leveling, 84 perks, and unified magic tree. |
| **Category** | Adventure and RPG |
| **Game Version** | Minecraft 1.21.1 |
| **Loader** | NeoForge |
| **License** | MIT |
| **Client/Server** | Both |

**Description longue (markdown) :**

```markdown
# STAT Mod

**STAT Mod** is a comprehensive stat and progression system for Minecraft NeoForge 1.21.1. It serves as the **progression authority** in a multi-mod RPG stack.

## Features

- **22 Stats** — 14 active stats (covered by 84 perks) + 8 magic stats (covered by the unified magic tree)
- **84 Perks** — 6 perks per active stat, with tiered progression
- **XP & Leveling** — Combat and non-combat XP with balanced progression curves
- **Unified Magic Tree** — Integrated with Iron's Spellbooks (Phase 1: common trunk + Fire school)
- **Racial System** — Full integration with Tensura Reincarnated (race modifiers, soul level sync, skill gates)
- **Puffish Skills UI** — Mirror UI for perk and magic tree management

## Dependencies

- **Tensura Reincarnated** (required)
- **Iron's Spellbooks** (recommended)
- **Puffish Skills** (recommended)
- **Epic Fight** (optional)

## License

MIT — Free to use, modify, and distribute.
```

### 5.2 Fiche Modrinth

| Champ | Valeur |
|-------|--------|
| **Title** | STAT Mod |
| **Description** | Standalone stat system with XP, leveling, 84 perks, and unified magic tree. |
| **Categories** | adventure, rpg, magic |
| **Game Versions** | 1.21.1 |
| **Loaders** | NeoForge |
| **License** | MIT |
| **Client/Server** | Both |

Utiliser la même description longue que CurseForge.

---

## 6. Workflow de Release

### 6.1 Déclencheur

Un tag git `v*` sur la branche `neoforge-1.21.1` déclenche :

1. **Build & Test** — `./gradlew build test --no-daemon`
2. **GitHub Release** — auto-generated notes, upload du `.jar`
3. **CurseForge Publish** — upload vers le projet lié
4. **Modrinth Publish** — upload vers le projet lié

### 6.2 Procédure manuelle (Founder)

```bash
# 1. S'assurer que la branche est propre
git checkout neoforge-1.21.1
git status

# 2. Version bump déjà commité
git add gradle.properties CHANGELOG.md
git commit -m "bump: v1.2.0"

# 3. Tagger et pusher
git tag v1.2.0
git push origin v1.2.0

# 4. Vérifier l'exécution sur GitHub Actions
```

### 6.3 Secrets GitHub Requis

| Secret | Source |
|--------|--------|
| `CURSEFORGE_TOKEN` | CurseForge → API Keys |
| `MODRINTH_TOKEN` | Modrinth → Settings → API |
| `CURSEFORGE_PROJECT_ID` | URL du projet CurseForge (numérique) |
| `MODRINTH_PROJECT_ID` | URL du projet Modrinth (slug textuel) |

---

## 7. Étapes Manuelles (Founder)

### 7.1 Créer compte CurseForge

1. Aller sur https://www.curseforge.com/
2. Créer un compte ou se connecter
3. Aller dans **Settings → API Keys**
4. Générer un nouveau token API → copier dans `CURSEFORGE_TOKEN`

### 7.2 Créer le projet CurseForge

1. Aller sur https://authors.curseforge.com/
2. Cliquer **Create a Project**
3. Remplir les champs avec les métadonnées de la section 5.1
4. Une fois créé, noter l'ID du projet dans l'URL (ex: `123456`) → `CURSEFORGE_PROJECT_ID`

### 7.3 Créer compte Modrinth

1. Aller sur https://modrinth.com/
2. Créer un compte ou se connecter
3. Aller dans **Settings → API tokens**
4. Générer un nouveau token avec les scopes : `upload_file`, `create_version` → `MODRINTH_TOKEN`

### 7.4 Créer le projet Modrinth

1. Aller sur https://modrinth.com/ → **Dashboard → Create Project**
2. Remplir avec les métadonnées de la section 5.2
3. Noter le slug du projet (ex: `statmod`) → `MODRINTH_PROJECT_ID`

### 7.5 Ajouter les secrets GitHub

1. Aller sur https://github.com/eladiouf/MOD_MINECRAFT_STAT/settings/secrets/actions
2. Ajouter **New repository secret** pour chaque token :
   - `CURSEFORGE_TOKEN`
   - `MODRINTH_TOKEN`
3. Ajouter **New variable** pour chaque ID :
   - `CURSEFORGE_PROJECT_ID`
   - `MODRINTH_PROJECT_ID`

---

## 8. Validation

Avant le tag release :

1. `./gradlew build test --no-daemon` passe en local
2. CI sur push (sans tag) build et test passe
3. Les tokens et IDs sont configurés dans GitHub
4. Le CHANGELOG est à jour

---

## 9. Post-Release

Après publication réussie :

1. Vérifier la page GitHub Release
2. Vérifier la fiche CurseForge (téléchargement, description)
3. Vérifier la fiche Modrinth (téléchargement, description)
4. Mettre à jour les badges dans README.md si nécessaire
5. Annoncer la release

---

## 10. Questions Ouvertes / Risques

| Risque | Mitigation |
|--------|------------|
| Token API invalide ou expiré | Documenter le renouvellement dans la procédure |
| Build échoue en CI mais pas en local | Vérifier les différences d'environnement (JDK, OS) |
| Dépendances optionnelles mal déclarées | Tester le tag en dry-run d'abord |
| Fichier `.jar` trop volumineux (59 libs embarquées) | Vérifier la taille avant upload, optimiser si > 10MB |
