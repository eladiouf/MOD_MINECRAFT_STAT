# Task 4 — rapport

## Résultat

- Document créé : `external-mods/irons-spells-forge-1.20.1/test-order.md`.
- 44 lots au total : lot 0 et lots 1 à 43.
- Lot 0 : les 4 JAR classés `dependencies`, chacun exactement une fois.
- Lots addons : 70 JAR `active` ou `needs-testing`, au plus 5 par lot.
- Les addons déclarant de gros mods de contenu externes ont un lot isolé et leurs dépendances connues sont indiquées.
- Aucun JAR n'a été déplacé ou modifié.

## Preuve de couverture

La commande PowerShell intégrée au document a été exécutée depuis la racine du dépôt et a terminé avec le code 0 :

```text
Expected     : 74
Listed       : 74
Unique       : 74
Lots         : 44
Lot0         : 4
MaxAddonLot  : 5
CoverageDiff : 0
Duplicates   : 0
Oversized    : 0
BadLotZero   : 0
DiskDiff     : 0
```

Ainsi, les 74 noms du manifeste correspondent aux 74 JAR présents sur disque et apparaissent chacun exactement une fois dans l'ordre de test.
