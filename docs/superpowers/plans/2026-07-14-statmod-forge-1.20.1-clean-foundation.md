# STAT Mod Forge 1.20.1 Clean Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remplacer le projet NeoForge 1.21.1 de la branche `forge-1.20.1` par un socle minimal STAT Mod compilable sous Minecraft Forge 1.20.1.

**Architecture:** Le nettoyage est précédé d'un contrôle de conservation des 74 JAR Iron's Spells et d'un garde-fou sur le worktree. Le socle est reconstruit depuis le MDK officiel Forge, puis réduit à un point d'entrée, des métadonnées et un test structurel sans fonctionnalité métier.

**Tech Stack:** Minecraft 1.20.1, Minecraft Forge 47.4.10, ForgeGradle 6, Gradle 8, Java 17, JUnit 5, PowerShell.

## Global Constraints

- Travailler uniquement dans `.worktrees/forge-1.20.1` sur la branche `forge-1.20.1`.
- Ne jamais modifier le worktree principal ni la branche `neoforge-1.21.1`.
- Minecraft `1.20.1`, Forge `47.4.10`, Java `17`.
- Mod ID `statmod`, groupe `tong.statmod`, version `0.1.0+1.20.1`, nom `STAT Mod`.
- Zéro référence active à NeoForge ou Tensura dans le nouveau socle.
- Conserver `external-mods/irons-spells-forge-1.20.1/`, ses 74 JAR locaux, `manifest.csv`, `catalog.csv`, `test-order.md` et `scripts/irons-addons/`.
- Aucun JAR externe ne doit être committé ni ajouté au classpath de compilation initial.
- Chaque suppression récursive doit vérifier que sa cible absolue reste sous le worktree Forge.

---

### Task 1: Garde-fous et état de conservation

**Files:**
- Create: `scripts/verify-clean-foundation.ps1`
- Create: `docs/migration/forge-1.20.1-preservation-baseline.md`

**Interfaces:**
- Consumes: branche courante, chemin du worktree, catalogue et manifeste Iron's Spells.
- Produces: `verify-clean-foundation.ps1 -Mode Before|After` et une preuve versionnée de l'état à préserver.

- [ ] **Step 1: Écrire le test de garde-fou avant nettoyage**

Créer `scripts/verify-clean-foundation.ps1` avec les paramètres et contrôles suivants :

```powershell
param([ValidateSet('Before','After')][string]$Mode = 'Before')
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((git rev-parse --show-toplevel))
$branch = (git branch --show-current).Trim()
if ($branch -ne 'forge-1.20.1') { throw "Wrong branch: $branch" }
if ((Split-Path $root -Leaf) -ne 'forge-1.20.1' -or $root -notmatch '[\\/]\.worktrees[\\/]forge-1\.20\.1$') {
    throw "Unsafe worktree: $root"
}
$external = Join-Path $root 'external-mods/irons-spells-forge-1.20.1'
$manifest = Import-Csv (Join-Path $external 'manifest.csv')
$jars = @(Get-ChildItem (Join-Path $external 'active'),(Join-Path $external 'needs-testing'),(Join-Path $external 'dependencies') -File -Filter '*.jar')
if ($manifest.Count -ne 74 -or $jars.Count -ne 74) { throw 'Iron addon baseline must contain 74 entries and 74 JARs.' }
foreach ($row in $manifest) {
    $jar = $jars | Where-Object Name -CEQ $row.filename
    if (@($jar).Count -ne 1) { throw "Missing or duplicated JAR: $($row.filename)" }
    if ((Get-FileHash $jar.FullName -Algorithm SHA256).Hash -ne $row.sha256) { throw "SHA mismatch: $($row.filename)" }
}
if ($Mode -eq 'After') {
    $forbidden = rg -n -i 'net\.neoforged|neoforge|tensura|tensura_iron_spells' src build.gradle gradle.properties settings.gradle
    if ($LASTEXITCODE -eq 0 -and $forbidden) { throw "Forbidden active references:`n$forbidden" }
    if ((Get-Content -Raw gradle.properties) -notmatch 'minecraft_version=1\.20\.1') { throw 'Wrong Minecraft version.' }
    if ((Get-Content -Raw gradle.properties) -notmatch 'forge_version=47\.4\.10') { throw 'Wrong Forge version.' }
}
"OK mode=$Mode branch=$branch jars=$($jars.Count) manifest=$($manifest.Count)"
```

- [ ] **Step 2: Exécuter le garde-fou en mode Before**

Run:

```powershell
& scripts/verify-clean-foundation.ps1 -Mode Before
```

Expected: `OK mode=Before branch=forge-1.20.1 jars=74 manifest=74`.

- [ ] **Step 3: Enregistrer le baseline**

Créer `docs/migration/forge-1.20.1-preservation-baseline.md` avec le commit de départ, le chemin absolu du worktree, `74` lignes de manifeste, `74` JAR, `74` SHA-256 uniques et la somme des tailles issue du manifeste.

- [ ] **Step 4: Commit**

```powershell
git add scripts/verify-clean-foundation.ps1 docs/migration/forge-1.20.1-preservation-baseline.md
git commit -m "test: guard Forge foundation cleanup"
```

### Task 2: Nettoyage contrôlé et import du MDK Forge

**Files:**
- Delete: ancien `src/`, `libs/`, `runs/`, `io/`, `META-INF/`, `data/`, `site/`, `tools/`, `tmp/`
- Delete: anciens outils, archives, images temporaires et documentation produit NeoForge à la racine
- Replace: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: garde-fou Task 1 et MDK officiel `forge-1.20.1-47.4.10-mdk.zip`.
- Produces: build ForgeGradle propre, sans anciennes sources, prêt à recevoir le mod minimal.

- [ ] **Step 1: Vérifier les chemins puis supprimer les sorties non suivies**

Exécuter un script PowerShell inline qui résout `$root`, exige la branche `forge-1.20.1`, puis traite seulement cette liste : `.gradle`, `build`, `runs`, `logs`, `tmp`, `io`, `META-INF`, `.tmp-onivo-audit`. Pour chaque cible, exiger que `[IO.Path]::GetFullPath($target).StartsWith($root + [IO.Path]::DirectorySeparatorChar)` avant `Remove-Item -Recurse -Force`.

- [ ] **Step 2: Supprimer les éléments suivis devenus obsolètes**

Utiliser `git rm -r --ignore-unmatch` sur :

```text
.claude .onivo-project .opencode .superpowers .tmp-onivo-audit .vscode
src libs runs io META-INF data site tmp tools
docs/generated docs/stores docs/wiki
scripts/tests scripts/render_dungeon_map.py scripts/render_magic_tree.py scripts/__init__.py
ARCHITECTURE.md CHANGELOG.md CURSEFORGE_DESCRIPTION.html MODRINTH_DESCRIPTION.md README.md
SECURITY.md STAT_MOD_TEST_PLAN.md STAT_MOD_v2.zip SDMCurrencyInspect.class
claude.md crowdin.yml mkdocs.yml modrinth_icon.png opencode.json requirements-wiki.txt
_all_strings.py _check_currency.py _inspect_sdm.py _strings.py
analyze_slots.py analyze_slots2.py analyze_slots3.py analyze_slots4.py analyze_slots5.py
analyze_slots6.py analyze_slots7.py analyze_slots8.py analyze_slots9.py
tmp_halberd_socket_source.png tmp_hammer_source.png tmp_tongs_source.png tmp_warhammer_core_source.png
```

Conserver explicitement `.github`, `LICENSE`, `CONTRIBUTING.md`, `docs/superpowers`, `docs/migration`, `external-mods` et `scripts/irons-addons`.

- [ ] **Step 3: Télécharger et contrôler le MDK officiel**

```powershell
$version = '1.20.1-47.4.10'
$url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$version/forge-$version-mdk.zip"
$shaUrl = "$url.sha1"
$zip = Join-Path $env:TEMP "forge-$version-mdk.zip"
Invoke-WebRequest $url -OutFile $zip
$expected = (Invoke-WebRequest $shaUrl).Content.Trim()
$actual = (Get-FileHash $zip -Algorithm SHA1).Hash.ToLowerInvariant()
if ($actual -ne $expected.ToLowerInvariant()) { throw 'MDK SHA1 mismatch' }
```

Expected: SHA-1 local identique au SHA-1 officiel.

- [ ] **Step 4: Extraire seulement le socle MDK**

Extraire dans un dossier temporaire puis copier vers le worktree : `gradle/`, `gradlew`, `gradlew.bat`, `build.gradle`, `settings.gradle`, `gradle.properties`. Ne pas copier les sources d'exemple ni les fichiers de licence du MDK.

- [ ] **Step 5: Écrire les propriétés exactes**

Remplacer `gradle.properties` par :

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
org.gradle.java.installations.auto-detect=true
org.gradle.java.installations.auto-download=true

minecraft_version=1.20.1
minecraft_version_range=[1.20.1,1.21)
forge_version=47.4.10
forge_version_range=[47.4.10,)
loader_version_range=[47,)
mapping_channel=official
mapping_version=1.20.1

mod_id=statmod
mod_name=STAT Mod
mod_license=MIT
mod_version=0.1.0+1.20.1
mod_group_id=tong.statmod
mod_authors=ela_juff
mod_description=Standalone statistics foundation for Forge 1.20.1.
```

- [ ] **Step 6: Réduire le build Gradle au minimum**

Remplacer `settings.gradle` par :

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
        mavenCentral()
    }
}

rootProject.name = 'statmod'
```

Remplacer `build.gradle` par :

```groovy
plugins {
    id 'java'
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: mapping_channel, version: mapping_version
    copyIdeResources = true

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                "${mod_id}" {
                    source sourceSets.main
                }
            }
        }

        client {}
        server { args '--nogui' }
        gameTestServer {}
        data {
            workingDirectory project.file('run-data')
            args '--mod', mod_id, '--all', '--output', file('src/generated/resources/'), '--existing', file('src/main/resources/')
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

repositories {
    mavenCentral()
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

tasks.named('processResources', ProcessResources).configure {
    def replacements = [
        minecraft_version_range: minecraft_version_range,
        forge_version_range: forge_version_range,
        loader_version_range: loader_version_range,
        mod_id: mod_id,
        mod_name: mod_name,
        mod_license: mod_license,
        mod_version: mod_version,
        mod_authors: mod_authors,
        mod_description: mod_description
    ]
    inputs.properties replacements
    filesMatching(['META-INF/mods.toml', 'pack.mcmeta']) {
        expand replacements
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test', Test).configure {
    useJUnitPlatform()
}

jar {
    manifest {
        attributes([
            'Specification-Title': mod_id,
            'Specification-Vendor': mod_authors,
            'Specification-Version': '1',
            'Implementation-Title': project.name,
            'Implementation-Version': project.jar.archiveVersion,
            'Implementation-Vendor': mod_authors
        ])
    }
}
```

Ne conserver aucun plugin de publication tiers ni aucune dépendance locale sous `libs/`.

- [ ] **Step 7: Adapter `.gitignore`**

Conserver les règles des JAR externes et ajouter au minimum : `.gradle/`, `build/`, `run/`, `runs/`, `logs/`, `*.part`, `out/`, `.idea/`, `*.iml`.

- [ ] **Step 8: Vérifier l'absence de sources actives**

Run:

```powershell
if (Test-Path src) { throw 'src must be recreated only in Task 3' }
& scripts/verify-clean-foundation.ps1 -Mode Before
```

Expected: aucune source active et baseline Iron intact.

- [ ] **Step 9: Commit**

```powershell
git add -A
git commit -m "build: replace NeoForge workspace with Forge 1.20.1 MDK"
```

### Task 3: Mod Forge minimal et tests structurels

**Files:**
- Create: `src/main/java/tong/statmod/StatMod.java`
- Create: `src/main/resources/META-INF/mods.toml`
- Create: `src/main/resources/pack.mcmeta`
- Create: `src/main/resources/assets/statmod/lang/en_us.json`
- Create: `src/main/resources/assets/statmod/lang/fr_fr.json`
- Create: `src/test/java/tong/statmod/FoundationIdentityTest.java`

**Interfaces:**
- Consumes: propriétés Gradle Task 2.
- Produces: JAR `build/libs/statmod-0.1.0+1.20.1.jar` chargé par Forge sous le mod ID `statmod`.

- [ ] **Step 1: Écrire le test d'identité avant le point d'entrée**

Créer `FoundationIdentityTest.java` :

```java
package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class FoundationIdentityTest {
    @Test
    void exposesStableModIdentity() {
        assertEquals("statmod", StatMod.MOD_ID);
        assertEquals("STAT Mod", StatMod.MOD_NAME);
    }
}
```

- [ ] **Step 2: Vérifier l'échec initial**

Run: `./gradlew test --tests tong.statmod.FoundationIdentityTest --console=plain`

Expected: échec de compilation car `StatMod` n'existe pas.

- [ ] **Step 3: Créer le point d'entrée minimal**

```java
package tong.statmod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(StatMod.MOD_ID)
public final class StatMod {
    public static final String MOD_ID = "statmod";
    public static final String MOD_NAME = "STAT Mod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public StatMod() {
        LOGGER.info("Initializing {} for Forge 1.20.1", MOD_NAME);
    }
}
```

- [ ] **Step 4: Créer `mods.toml`**

Utiliser exactement :

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
modId="forge"
mandatory=true
versionRange="${forge_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
```

- [ ] **Step 5: Créer les ressources minimales**

`pack.mcmeta` utilise `pack_format: 15` et la description `STAT Mod resources`. `en_us.json` contient `{"mod.statmod.name":"STAT Mod"}` et `fr_fr.json` contient `{"mod.statmod.name":"STAT Mod"}`.

- [ ] **Step 6: Exécuter le test puis le build**

```powershell
./gradlew test --tests tong.statmod.FoundationIdentityTest --console=plain
./gradlew clean build --console=plain
```

Expected: test réussi et `BUILD SUCCESSFUL`.

- [ ] **Step 7: Contrôler le contenu du JAR**

Ouvrir `build/libs/statmod-0.1.0+1.20.1.jar` comme ZIP et exiger `META-INF/mods.toml` ainsi que `tong/statmod/StatMod.class`.

- [ ] **Step 8: Commit**

```powershell
git add src
git commit -m "feat: add minimal STAT Mod Forge entrypoint"
```

### Task 4: Documentation, CI et validation finale

**Files:**
- Create: `README.md`
- Modify: `.github/workflows/*`
- Modify: `scripts/verify-clean-foundation.ps1`
- Modify: `docs/migration/forge-1.20.1-preservation-baseline.md`

**Interfaces:**
- Consumes: socle compilable Task 3.
- Produces: dépôt documenté et CI Java 17 reproductible.

- [ ] **Step 1: Écrire le README du nouveau socle**

Documenter Minecraft 1.20.1, Forge 47.4.10, Java 17, `./gradlew build`, le statut « socle uniquement », l'emplacement du catalogue Iron's Spells et l'exclusion de Tensura.

- [ ] **Step 2: Remplacer les workflows hérités**

Supprimer les workflows strictement NeoForge/publication 1.21.1 de cette branche et créer `.github/workflows/build.yml` :

```yaml
name: Forge 1.20.1 Build

on:
  push:
    branches: [forge-1.20.1]
  pull_request:
    branches: [forge-1.20.1]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: gradle
      - uses: gradle/actions/setup-gradle@v4
      - name: Build
        run: ./gradlew clean build --console=plain
```

- [ ] **Step 3: Étendre la validation After**

Dans `verify-clean-foundation.ps1`, exiger aussi : JAR produit présent, entrée `META-INF/mods.toml`, classe `tong/statmod/StatMod.class`, aucun JAR sous `external-mods` suivi par Git, et `git diff --check` réussi.

- [ ] **Step 4: Exécuter toute la validation fraîche**

```powershell
& scripts/verify-clean-foundation.ps1 -Mode After
./gradlew clean build --console=plain
git diff --check
git status --short
```

Expected: garde-fou `OK mode=After`, `BUILD SUCCESSFUL`, aucun avertissement de whitespace et seulement les fichiers intentionnels avant commit.

- [ ] **Step 5: Vérifier l'isolation de la branche NeoForge**

Depuis le worktree principal, exécuter `git status --short` et confirmer qu'aucun changement nouveau n'a été créé par cette migration. Ne pas nettoyer ses modifications préexistantes.

- [ ] **Step 6: Commit final**

```powershell
git add README.md .github scripts/verify-clean-foundation.ps1 docs/migration/forge-1.20.1-preservation-baseline.md
git commit -m "ci: verify Forge 1.20.1 clean foundation"
```

- [ ] **Step 7: Mettre à jour la Pull Request existante**

Pousser `forge-1.20.1` vers `origin`; la PR existante doit recevoir les nouveaux commits sans committer les 74 JAR.
