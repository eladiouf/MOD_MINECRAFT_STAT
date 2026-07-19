# Iron's Spells Addons Forge 1.20.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Télécharger, contrôler et cataloguer le maximum d'addons publics d'Iron's Spells 'n Spellbooks pour Minecraft Forge 1.20.1 sans inclure Tensura.

**Architecture:** Un catalogue CSV pilotera un script PowerShell de téléchargement reproductible. Les JAR valides seront séparés entre addons activables, addons à tester et dépendances, sans toucher aux bibliothèques NeoForge 1.21.1 du dépôt principal.

**Tech Stack:** PowerShell 7/Windows PowerShell, API Modrinth, liens officiels CurseForge, archives JAR/ZIP, Git.

## Global Constraints

- Minecraft 1.20.1 et Forge uniquement.
- Tensura et toutes ses intégrations sont exclus.
- Les fichiers sont confinés dans `external-mods/irons-spells-forge-1.20.1/`.
- Un addon incertain est conservé dans `needs-testing/`, jamais activé automatiquement.
- Chaque entrée conserve sa source officielle, sa version, ses dépendances connues et son état de validation.

---

### Task 1: Catalogue officiel des projets

**Files:**
- Create: `external-mods/irons-spells-forge-1.20.1/catalog.csv`
- Create: `external-mods/irons-spells-forge-1.20.1/README.md`

**Interfaces:**
- Consumes: pages officielles CurseForge et Modrinth publiant des fichiers Forge 1.20.1.
- Produces: colonnes CSV `project,kind,version,loader,minecraft,url,filename,dependencies,classification` consommées par `download.ps1`.

- [ ] **Step 1: Créer les dossiers et le catalogue avec tous les projets déjà identifiés**

Créer `catalog.csv` en UTF-8 avec une ligne par projet et seulement des URL de fichiers ou d'API officielles. Inclure au minimum Iron's Spells, T.O Magic 'n Extras, GTBC's Geomancy Plus, Dark Doppelganger, Illage and Spell-age, Sanctified Legacy, Iron's Epic Spell Slinger, Iron's Spell's Delight, Spells Gone Wrong, Dungeons And Combat x Iron's Spells, Shoulder Surfing Integration, KubeJS Iron's Spells, Gambler Illager, Iotic Spellbooks, Interlace SpellWeaves, Alex's Caves: Spellbooks, Iron's Spellbooks Tweaks, Project MMO compat, Elder Tales, Dungeons and Minecraft, Mobbility, Restrictions, Osmium's Magic, IMA et IronsArms.

- [ ] **Step 2: Vérifier l'absence de Tensura et les plateformes**

Run:

```powershell
$rows = Import-Csv external-mods/irons-spells-forge-1.20.1/catalog.csv
if ($rows | Where-Object { $_.project -match 'tensura' -or $_.url -match 'tensura' }) { throw 'Tensura détecté' }
if ($rows | Where-Object { $_.minecraft -ne '1.20.1' -or $_.loader -ne 'forge' }) { throw 'Mauvaise plateforme détectée' }
```

Expected: aucune sortie et code 0.

- [ ] **Step 3: Documenter les catégories**

Dans `README.md`, expliquer que `active/` est le lot de départ, `needs-testing/` contient les versions incertaines et `dependencies/` les prérequis externes non activés.

- [ ] **Step 4: Commit**

```powershell
git add external-mods/irons-spells-forge-1.20.1/catalog.csv external-mods/irons-spells-forge-1.20.1/README.md
git commit -m "docs: catalog Iron's Spells Forge addons"
```

### Task 2: Téléchargeur reproductible et contrôles

**Files:**
- Create: `scripts/irons-addons/download.ps1`
- Create: `scripts/irons-addons/verify.ps1`

**Interfaces:**
- Consumes: `catalog.csv` selon le schéma de Task 1.
- Produces: JAR rangés dans `active/`, `needs-testing/` ou `dependencies/`, plus `manifest.csv` avec `project,version,filename,classification,source,sha256,size,status,dependencies`.

- [ ] **Step 1: Écrire le contrôle d'entrée avant téléchargement**

`download.ps1` doit refuser toute ligne qui n'a pas `loader=forge`, `minecraft=1.20.1`, une URL HTTPS, un nom finissant par `.jar`, ou qui contient `tensura` dans un champ.

- [ ] **Step 2: Tester le refus d'une ligne interdite**

Run:

```powershell
Copy-Item external-mods/irons-spells-forge-1.20.1/catalog.csv $env:TEMP/catalog-invalid.csv
Add-Content $env:TEMP/catalog-invalid.csv 'Tensura,test,1.0,forge,1.20.1,https://example.invalid/tensura.jar,tensura.jar,,active'
& scripts/irons-addons/download.ps1 -Catalog $env:TEMP/catalog-invalid.csv -DryRun
if ($LASTEXITCODE -eq 0) { throw 'Le contrôle Tensura aurait dû échouer' }
```

Expected: échec explicite contenant `Tensura`.

- [ ] **Step 3: Implémenter le téléchargement atomique**

Pour chaque ligne, télécharger vers `<filename>.part`, vérifier une taille supérieure à zéro, puis renommer en `.jar`. Réessayer trois fois avec délais de 2, 5 et 10 secondes. Ne jamais écraser un fichier dont le SHA-256 correspond déjà au manifeste.

- [ ] **Step 4: Implémenter la validation ZIP/JAR**

`verify.ps1` doit ouvrir chaque JAR avec `System.IO.Compression.ZipFile`, exiger `META-INF/mods.toml` ou `META-INF/neoforge.mods.toml`, calculer SHA-256 et taille, puis marquer `valid`, `invalid-archive`, `missing-metadata` ou `download-failed`.

- [ ] **Step 5: Vérifier les scripts en mode simulation**

Run:

```powershell
& scripts/irons-addons/download.ps1 -Catalog external-mods/irons-spells-forge-1.20.1/catalog.csv -DryRun
```

Expected: une destination valide par ligne, zéro téléchargement et code 0.

- [ ] **Step 6: Commit**

```powershell
git add scripts/irons-addons/download.ps1 scripts/irons-addons/verify.ps1
git commit -m "build: add verified Iron's addon downloader"
```

### Task 3: Téléchargement et manifeste final

**Files:**
- Create: `external-mods/irons-spells-forge-1.20.1/manifest.csv`
- Create: `external-mods/irons-spells-forge-1.20.1/active/*.jar`
- Create: `external-mods/irons-spells-forge-1.20.1/needs-testing/*.jar`
- Create: `external-mods/irons-spells-forge-1.20.1/dependencies/*.jar`
- Modify: `external-mods/irons-spells-forge-1.20.1/README.md`

**Interfaces:**
- Consumes: catalogue et scripts validés.
- Produces: collection locale contrôlée et inventaire complet prêt pour les tests Forge 1.20.1.

- [ ] **Step 1: Télécharger tous les fichiers accessibles**

Run:

```powershell
& scripts/irons-addons/download.ps1 -Catalog external-mods/irons-spells-forge-1.20.1/catalog.csv
```

Expected: chaque entrée devient `valid` ou conserve un statut d'échec explicite dans `manifest.csv`.

- [ ] **Step 2: Contrôler tous les JAR**

Run:

```powershell
& scripts/irons-addons/verify.ps1 -Root external-mods/irons-spells-forge-1.20.1
```

Expected: code 0 si tous les fichiers présents sont des archives de mod lisibles ; les téléchargements impossibles restent signalés dans le manifeste.

- [ ] **Step 3: Rechercher doublons et contenu interdit**

Run:

```powershell
$manifest = Import-Csv external-mods/irons-spells-forge-1.20.1/manifest.csv
if ($manifest | Group-Object sha256 | Where-Object { $_.Name -and $_.Count -gt 1 }) { throw 'JAR dupliqué' }
if (Get-ChildItem external-mods/irons-spells-forge-1.20.1 -Recurse -File | Where-Object Name -Match 'tensura') { throw 'Tensura détecté' }
```

Expected: aucune sortie et code 0.

- [ ] **Step 4: Résumer les résultats dans README**

Ajouter le nombre de JAR valides dans chaque catégorie, la liste des échecs et les dépendances encore introuvables. Ne pas déclarer compatibles les éléments de `needs-testing/`.

- [ ] **Step 5: Commit des métadonnées uniquement**

Ajouter les JAR à `.gitignore`, puis versionner le catalogue, le manifeste, le README et les scripts sans committer les binaires téléchargés.

```powershell
git add .gitignore external-mods/irons-spells-forge-1.20.1/catalog.csv external-mods/irons-spells-forge-1.20.1/manifest.csv external-mods/irons-spells-forge-1.20.1/README.md scripts/irons-addons
git commit -m "chore: record Forge Iron's addon downloads"
```

### Task 4: Préparation du test Forge

**Files:**
- Create: `external-mods/irons-spells-forge-1.20.1/test-order.md`

**Interfaces:**
- Consumes: manifeste validé et catégories de téléchargement.
- Produces: ordre de test incrémental qui isole rapidement les conflits.

- [ ] **Step 1: Générer les lots de test**

Documenter un premier lot contenant Iron's Spells et ses dépendances directes, puis des lots d'au plus cinq addons. Mettre chaque addon ayant de grosses dépendances externes dans son propre lot.

- [ ] **Step 2: Vérifier la couverture**

Chaque JAR de `active/` et `needs-testing/` doit apparaître exactement une fois dans `test-order.md`; les bibliothèques partagées restent dans le lot de base.

- [ ] **Step 3: Commit**

```powershell
git add external-mods/irons-spells-forge-1.20.1/test-order.md
git commit -m "test: define incremental addon compatibility batches"
```
