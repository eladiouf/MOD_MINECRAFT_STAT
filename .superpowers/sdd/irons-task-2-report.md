# Task 2 — Téléchargeur reproductible et contrôles

## Statut

`DONE`

Commit livré : `build: add verified Iron's addon downloader`

## Implémentation

- `scripts/irons-addons/download.ps1` valide le catalogue complet avant écriture, refuse Tensura, impose Forge 1.20.1/HTTPS/JAR/classification, gère le dry-run, les téléchargements `.part`, les trois tentatives avec délais 2/5/10 s, le saut par SHA-256 existant et le manifeste à neuf colonnes.
- `scripts/irons-addons/verify.ps1` inspecte seulement les trois répertoires autorisés, reconnaît les métadonnées Forge/NeoForge, calcule SHA-256 et taille, préserve les métadonnées du manifeste et retourne un code non nul pour les JAR présents non valides.
- Les noms d’entrées ZIP sont normalisés de `\` vers `/` pour la compatibilité Windows PowerShell 5.1.

## TDD et auto-revue

RED observé avant implémentation : le test Tensura échouait parce que `download.ps1` était absent et le message attendu n’était pas produit.

GREEN observé après implémentation : rejet Tensura avec code 1, message contenant `Tensura` et aucune écriture.

Une revue indépendante a ensuite détecté le cas Windows où `Compress-Archive` produit `META-INF\mods.toml`. La fixture échouait avec `missing-metadata`; la normalisation des séparateurs a été ajoutée puis le test a passé avec `valid` et code 0. Aucun autre problème critique ou important n’a été relevé.

## Commandes et résultats

Validation finale regroupée avec roots temporaires :

```text
PASS Tensura: exit=1 message=yes writes=0
PASS DryRun: exit=0 destinations=74 rows=74 dry-run=74 jars=0
PASS Verify: exit=1 valid=valid missing=missing-metadata invalid=invalid-archive absent=download-failed
PASS Parser: scripts/irons-addons/download.ps1 errors=0
PASS Parser: scripts/irons-addons/verify.ps1 errors=0
PASS git diff --check: exit=0
```

Test complémentaire de réutilisation :

```text
Exit=0 status=skipped-existing shaMatch=True dirs=3 output=1
PASS: matching existing JAR is not downloaded.
```

Test complémentaire de compatibilité ZIP Windows :

```text
Exit=0 status=valid
PASS: Windows ZIP separator normalization.
```

## Préoccupations

Aucune préoccupation bloquante. Aucun JAR réel n’a été téléchargé ou committé, conformément au périmètre de la tâche.

## Correctif après revue indépendante

Statut : `DONE`

- Après normalisation de `\` vers `/`, les entrées sont maintenant comparées avec `StringComparison.Ordinal` : les chemins exacts avec slash ou antislash sont valides, tandis que `meta-inf/mods.toml` reste `missing-metadata` et provoque un code non nul.
- Les deux scripts écrivent `manifest.csv.part` dans le même répertoire, puis utilisent `File.Move` lors de la création ou `File.Replace` lors d’une mise à jour. Les fichiers temporaires et sauvegardes sont nettoyés.
- `download.ps1` sauvegarde atomiquement le manifeste après chaque résultat de catalogue; `verify.ps1` le sauvegarde après chaque JAR contrôlé.

Preuves RED :

```text
Exit=0 forward=valid backslash=valid lowercase=valid
RED confirmed: lowercase metadata is incorrectly accepted.

Exit=0 tempObserved=False rows=1 status=dry-run partRemains=False
RED confirmed: atomic manifest temp file was not observed.
```

Preuves GREEN :

```text
Exit=1 forward=valid backslash=valid lowercase=missing-metadata partFiles=0
PASS: exact case enforced after separator normalization.

Exit=0 tempObserved=True rows=1 status=dry-run partRemains=0
PASS: manifest written through same-directory temp and remains valid.

PASS DryRun exit=0 destinations=74 rows=74 dry-run=74 parts=0
PASS Parser scripts/irons-addons/download.ps1 errors=0
PASS Parser scripts/irons-addons/verify.ps1 errors=0
PASS git diff --check exit=0
```
