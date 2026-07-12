# Catalogue détaillé du SDM Shop

## Objectif

Transformer le SDM Shop en marché principal du modpack avec environ 350 à 450
articles, sans permettre aux joueurs de contourner les combats de boss, les
quêtes ou les paliers finaux de progression.

## Organisation

Le shop utilise des catégories fonctionnelles plutôt que des onglets par mod.
Un joueur doit pouvoir chercher un type d'objet sans connaître son mod
d'origine.

1. Minerais bruts
2. Lingots et gemmes
3. Matériaux avancés
4. Forge et amélioration
5. Armes légères
6. Armes lourdes
7. Lances et armes d'hast
8. Armes à distance
9. Armures classiques
10. Armures fantastiques
11. Magie et parchemins
12. Runes et composants magiques
13. Potions et soins
14. Composants de monstres
15. Nourriture
16. Construction
17. Utilitaires
18. Objets rares contrôlés

Le catalogue cible environ 400 entrées. Une même ressource ne doit pas être
dupliquée dans plusieurs catégories.

## Sources de contenu

Le catalogue couvre au minimum Vanilla, STAT Mod, Tensura, Iron's Spells 'n
Spellbooks, Ice and Fire, Apotheosis, Epic Fight, Simply Swords, Epic Knights
et Overgeared. D'autres mods installés peuvent contribuer lorsqu'ils possèdent
des ressources utiles à l'économie générale.

Les identifiants sont extraits des JAR réellement présents dans `libs/`.
L'existence d'un modèle d'objet est contrôlée pendant la préparation du
catalogue, puis l'existence dans `BuiltInRegistries.ITEM` est contrôlée à
l'exécution.

## Protection de la progression

Sont exclus :

- spawn eggs et objets créatifs ou administratifs ;
- objets d'invocation de boss ;
- récompenses uniques, artefacts narratifs et clés de progression ;
- équipements finaux qui supprimeraient l'intérêt du craft ou des boss ;
- objets techniques, incomplets ou dépourvus d'entrée de registre ;
- objets dont l'effet économique ne peut pas être évalué raisonnablement.

Les matériaux avancés peuvent être vendus, mais à un prix qui préserve leur
valeur et sans proposer les composants finaux les plus sensibles.

## Prix et quantités

Les prix utilisent cinq paliers communs :

- courant : 1 à 25 FDP ;
- travaillé : 30 à 100 FDP ;
- rare : 120 à 350 FDP ;
- épique : 400 à 900 FDP ;
- exceptionnel contrôlé : 1 000 à 2 500 FDP.

Les valeurs sont ajustées selon la puissance, la rareté naturelle, le coût de
craft et la possibilité de réutilisation. Les blocs de construction, aliments
et munitions sont vendus par lots. Les minerais, composants rares, armes et
armures sont vendus à l'unité ou en petits lots.

## Architecture

`SDMShopCatalog` reste la source déclarative unique. Il expose les onglets et
les articles sans dépendre du serveur. Chaque article contient sa catégorie,
son identifiant namespacé, son prix, sa quantité et ses éventuelles données
spéciales, notamment les variantes de potions.

`SDMShopDatabaseInitializer` résout les identifiants via le registre, fabrique
les `ItemStack` et écrit les fichiers NBT de SDM Shop. Une entrée manquante
est journalisée puis ignorée. Elle ne bloque ni le démarrage ni les autres
articles.

La version du catalogue NBT est augmentée à chaque changement structurel afin
de régénérer automatiquement les anciens fichiers une seule fois.

## Validation

Les tests vérifient :

- la présence exacte des 18 catégories ;
- un volume compris entre 350 et 450 articles ;
- l'unicité des identifiants ;
- la validité des catégories, prix et quantités ;
- la présence de chaque grande famille de contenu ;
- l'absence des motifs interdits comme les spawn eggs et boss summoners ;
- la migration de version du catalogue ;
- la compilation et le build NeoForge.

Un audit des JAR produit la liste des identifiants candidats et rejette les
entrées sans ressource d'objet correspondante. La validation d'exécution par
registre reste nécessaire pour tolérer un mod retiré du modpack.
