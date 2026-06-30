# STAT-DEC-COMMONNETWORK-SHIM-REMOVAL — Suppression des shims commonnetwork

**Date :** 2026-06-30
**Mission :** Hotfix runtime boot
**Auteur :** Onivo Studio runtime agent
**Branche :** `neoforge-1.21.1`

## Symptôme

`runClient` plantait au boot sur :

```
java.lang.module.ResolutionException:
  Modules statmod and commonnetworking export package commonnetwork.networking to module mixin_synthetic
```

(voir `runs/client/logs/latest.log` ligne 168-170, snapshot du 2026-06-30 19:05)

## Cause

`src/main/java/commonnetwork/api/` et `src/main/java/commonnetwork/networking/` contenaient
des shims écrits autrefois pour permettre la compilation quand `libs/common-network.jar`
n'était pas disponible. Maintenant que `libs/` est restauré, le runtime charge deux modules
qui exportent le même package — Java ModuleLayer refuse.

Les shims n'étaient utilisés qu'à un seul endroit :
`NetworkHandler.onRegisterPayloads` appelait `Network.registerCompatPayloads(event)`. Mais
**aucune** ligne de code n'appelait `Network.registerPacket(...)` ni
`Network.getNetworkHandler()` → le registre interne `REGISTRATIONS` était toujours vide → le
call était un no-op.

## Décision

Supprimer les 5 fichiers shim + 1 test + retirer l'appel no-op dans `NetworkHandler`.

Fichiers supprimés :
- `src/main/java/commonnetwork/api/Network.java`
- `src/main/java/commonnetwork/api/NetworkHandler.java`
- `src/main/java/commonnetwork/networking/PacketRegistrar.java`
- `src/main/java/commonnetwork/networking/data/PacketContext.java`
- `src/main/java/commonnetwork/networking/data/Side.java`
- `src/test/java/commonnetwork/api/NetworkCompatTest.java`
- Dossiers vides : `src/main/java/commonnetwork/**`, `src/test/java/commonnetwork/**`

Modifs :
- `src/main/java/tong/statmod/network/NetworkHandler.java` :
  - retrait de `import commonnetwork.api.Network;`
  - retrait de la ligne `Network.registerCompatPayloads(event);`

## Vérification

- `./gradlew compileJava compileTestJava` → BUILD SUCCESSFUL
- `./gradlew test` → BUILD SUCCESSFUL
- À valider en jeu : `./gradlew runClient` ne doit plus crasher sur ModuleLayer.

## Garde

Si une intégration future a besoin de l'API CommonNetworking, **dépendre du vrai mod** via
`libs/common-network.jar` (déjà sur le classpath), pas de shim local. Toute tentative de
réintroduire un package `commonnetwork.*` dans `src/main/java/` du mod doit être refusée et
renvoyer à cette décision.
