# Task 4 — rapport

## Résultat

- Document créé : `external-mods/irons-spells-forge-1.20.1/test-order.md`.
- 44 lots au total : lot 0 et lots 1 à 43.
- Lot 0 : les 4 JAR classés `dependencies`, chacun exactement une fois.
- Lots addons : 70 JAR `active` ou `needs-testing`, au plus 5 par lot.
- Les addons déclarant de gros mods de contenu externes ont un lot isolé et leurs dépendances connues sont indiquées.
- Aucun JAR n'a été déplacé ou modifié.

## Preuve de couverture

La commande PowerShell intégrée au document a été exécutée depuis la racine du dépôt et a terminé avec le code 0. Sa sortie exacte est :

```text
OK: 74 JAR uniques; 44 lots; lots addons <= 5; 4 dependencies dans le lot 0.
```

Ainsi, les 74 noms du manifeste correspondent aux 74 JAR présents sur disque et apparaissent chacun exactement une fois dans l'ordre de test.
