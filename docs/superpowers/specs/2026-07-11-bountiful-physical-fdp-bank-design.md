# Monnaie physique FDP et Banquier magique — Conception

**Date :** 2026-07-11  
**Cible :** NeoForge 1.21.1, STAT Mod 1.2.0, Bountiful 8.0.0-beta.2, SDM Economy/Shop

## Objectif

Ajouter une représentation physique de la devise numérique SDM `FDP_cfa`, distribuée notamment
par les quêtes Bountiful, et un Banquier magique qui échange cette monnaie physique contre le solde
numérique dans les deux sens sans duplication ni perte.

## Coupures

STAT Mod enregistre huit objets empilables par 64 :

| Identifiant | Type | Valeur FDP |
|---|---|---:|
| `fdp_coin_50` | Pièce | 50 |
| `fdp_coin_100` | Pièce | 100 |
| `fdp_coin_200` | Pièce | 200 |
| `fdp_coin_500` | Pièce | 500 |
| `fdp_note_1000` | Billet | 1 000 |
| `fdp_note_2000` | Billet | 2 000 |
| `fdp_note_5000` | Billet | 5 000 |
| `fdp_note_10000` | Billet | 10 000 |

Chaque objet possède un modèle, une texture, un nom français et anglais et apparaît dans l’onglet
créatif STAT Mod. Les objets n’accordent aucun solde par clic droit et ne sont pas fabricables : le
Banquier magique reste le seul point de conversion.

## Récompenses Bountiful

Un datapack intégré à STAT Mod ajoute le pool `statmod:fdp_rewards`, marqué `currency: true`,
contenant les huit objets. La valeur
`unitWorth` correspond exactement à la valeur FDP de chaque coupure. Les montants de chaque entrée
sont bornés pour éviter les piles excessives. STAT Mod surcharge les décrets Bountiful standards en
préservant leurs objectifs et récompenses existants, puis ajoute `statmod:fdp_rewards` à chaque liste
de récompenses. Les quêtes peuvent ainsi sélectionner les petites coupures pour les récompenses
modestes et les billets élevés pour les récompenses de grande valeur.

Bountiful reste optionnel au chargement : les objets et le banquier fonctionnent sans lui. Le JAR
`bountiful-neoforge-8.0.0-beta.2.jar` doit être présent dans le dossier `mods` du client pour que les
quêtes soient disponibles.

## Banquier magique

Le Banquier magique est un villageois dédié, identifié par un marqueur persistant STAT Mod. Il est
immobile, invulnérable, silencieux, persistant, nommé et exclu :

- des échanges vanilla ;
- de l’ouverture automatique de SDM Shop ;
- du système du changeur de points du donjon ;
- du nettoyage et du comptage des monstres du donjon.

Un exemplaire est garanti dans le hub du Trial Dungeon, à l’étage 0. Un exemplaire au maximum est
également généré par village vanilla. Lors du chargement d’un chunk serveur, le générateur recherche
le point d’intérêt `minecraft:meeting` (la cloche) du village. Il enregistre la dimension et la
position de cette cloche dans un `SavedData`, puis crée le banquier à côté uniquement si cette clé
n’a jamais été traitée. Le marqueur persistant du PNJ permet de le retrouver après rechargement ; sa
mort ou sa suppression administrative ne déclenche pas de duplication automatique.

## Interface bancaire

Un clic droit ouvre une interface serveur autorisée par une session courte liée au joueur et au
banquier exact. Le joueur doit rester dans la même dimension et à huit blocs maximum.

L’interface affiche :

- le solde numérique `FDP_cfa` ;
- la valeur totale des coupures dans l’inventaire ;
- un bouton « Tout déposer » ;
- un champ de montant et un bouton « Retirer » ;
- un bouton « Retrait max » limité par le solde et la capacité de l’inventaire ;
- un message explicite pour montant invalide, SDM indisponible ou inventaire insuffisant.

## Dépôt

Le serveur recense les huit coupures dans l’inventaire et calcule leur valeur avec une table unique.
Il crédite d’abord `FDP_cfa`. Les objets ne sont retirés qu’après confirmation du crédit. Si le
crédit échoue, aucun objet n’est consommé. Le dépôt concerne toutes les coupures présentes afin de
rester simple et prévisible.

## Retrait

Le montant demandé doit être positif, inférieur ou égal au solde et multiple de 50. Le serveur
calcule une décomposition gloutonne avec les coupures 10 000, 5 000, 2 000, 1 000, 500, 200, 100,
50. Avant tout débit, il simule l’insertion complète dans l’inventaire. Si toutes les coupures ne
tiennent pas, l’opération est refusée sans modifier le solde. Sinon, le solde est débité puis les
objets sont insérés. Toute erreur SDM annule l’opération avant remise des objets.

## Cohérence et sécurité

Tous les calculs et mutations sont réalisés sur le thread serveur. Le client n’envoie qu’un type
d’action et un montant ; le serveur relit le solde et l’inventaire. Les montants utilisent `long`,
les taux flottants sont interdits et la valeur minimale est 50 FDP. La table des coupures est la
source unique utilisée par le dépôt, le retrait, l’affichage et les tests.

## Vérification

- Tests unitaires de la table des coupures et de la décomposition des retraits.
- Tests du dépôt, des multiples de 50, du solde insuffisant et de l’inventaire plein.
- Test source garantissant l’exclusion du Banquier magique des autres ponts PNJ.
- Validation JSON de tous les modèles, langues et fichiers Bountiful.
- Compilation, suite de tests et build Gradle complets.
- Essai en jeu : quête Bountiful → récompense physique → dépôt → achat SDM → retrait physique.

## Hors périmètre

- Paiement direct du shop avec des objets physiques.
- Conversion automatique au ramassage ou au clic droit.
- Recettes de fabrication ou fonte des pièces et billets.
- Intérêts, prêts, comptes partagés ou frais bancaires.
