# Plan d'apprentissage : Créer un style de combat custom avec Epic Fight

## Objectif
Créer un mod (intégré à STAT_MOD ou séparé) avec un style de combat personnalisé, des armes uniques, des animations 3D et des skills Epic Fight.

---

## PHASE 1 : Théorie et exploration

### 1.1 Lire la doc API Epic Fight
- https://epicfight-docs.readthedocs.io/API/Starting/
- Comprendre : AnimationRegistryEvent, SkillBuilder, WeaponCapability, SkillCategories

### 1.2 Étudier le code de Battle-Arts (1.20.1)
- https://github.com/Forixaim/Battle-Arts/tree/1.20.1
- Fichiers clés à lire absolument :
  - `BattleArts.java` — classe principale, registrations
  - `BattleStyleCategories.java` — exemple de catégorie d'arme custom
  - `BattleBowItem.java` — item d'arc avec intégration EF
  - `FixedArrow.java` — entité flèche custom
  - `Ronin.java` — style de combat complet (plus complexe)
  - `SkillRegistry.java` — enregistrement des skills
  - `ItemRegistry.java` — enregistrement des items

### 1.3 Étudier notre propre code STAT_MOD
- `SkillRegistry.java` — nos 28 skills déjà enregistrés
- `SkillUnlockHandler.java` — système de déblocage
- `PlaceholderSkill.java` — notre classe Skill vide

### 1.4 Étudier les Battle Styles (EF-BS)
- https://www.curseforge.com/minecraft/mc-mods/ef-bs
- Télécharger et tester en jeu pour comprendre le concept

---

## PHASE 2 : Installation des outils

### 2.1 Blender
- Télécharger Blender 3.6+ : https://www.blender.org/
- Optionnel : Blender 2.79 (mieux documenté pour EF)

### 2.2 Plugin d'export Epic Fight
- https://github.com/Epic-Fight/blender-json-addon
- Installation :
  1. Télécharger le repo
  2. Copier le dossier dans `Blender/scripts/addons/`
  3. Activer dans Blender : File → User Preferences → Add-ons → "Minecraft Model Json Exporter"

### 2.3 Rig du joueur Epic Fight
- https://github.com/Epic-Fight/EpicFight-Files
- Télécharger le fichier `.blend` du squelette du joueur
- C'est la base de TOUTE animation Epic Fight

---

## PHASE 3 : Apprendre Blender pour Epic Fight

### 3.1 Suivre le tutorial officiel
- https://epicfight-docs.readthedocs.io/Guides/page1/
- Concepts à maîtriser :
  - Navigation 3D (vue, rotation, zoom)
  - Mode Object vs Mode Edit
  - Armature (squelette) et bones
  - Keyframes (position, rotation, scale)
  - Visual keyframes vs standard keyframes
  - Export JSON avec le plugin

### 3.2 Pratiques guidées
1. Ouvrir le rig du joueur dans Blender
2. Créer une animation simple (ex: vague de la main)
3. L'exporter en JSON
4. Créer un datapack de test pour voir l'animation en jeu

### 3.3 Ressources Blender générales (si débutant)
- https://www.youtube.com/playlist?list=PLjEaoINr3zgFX8sChb62oQ2Gj9P3BQZQt  (Blender Basics)
- Chercher "Blender armature animation tutorial" sur YouTube

---

## PHASE 4 : Créer une arme custom (tutoriel vidéo)

### 4.1 Regarder la vidéo de Yesman
- https://www.youtube.com/watch?v=iysWR_dSic4
- Sujet : Créer un type d'arme avec le Weapon Type Editor
- Concepts : WeaponCategory, Styles, Combos, Colliders, Innate Skills

### 4.2 Pratique : Datapack d'abord
1. Créer une catégorie d'arme (ex: "longbow", "rapier")
2. Lui assigner un style et des combos
3. Tester en jeu avec un item vanilla assigné à cette catégorie
4. Vérifier que les animations EF natives s'appliquent

### 4.3 Pratique : Mod Java
1. Créer un item custom avec `WeaponCapability.builder()`
2. Lui assigner la catégorie créée
3. Lui donner une `SkillCategory` et un moveset
4. Tester en jeu

---

## PHASE 5 : Création d'animations custom

### 5.1 Workflow complet
1. **Blender** : Ouvrir le rig EF
2. **Animer** : Créer les keyframes pour l'attaque (auto1, auto2, auto3, dash, etc.)
3. **Exporter** : Plugin → Export Animation JSON
4. **Placer** : `assets/monmod/animmodels/animations/biped/skill/mon_attaque.json`
5. **Enregistrer** dans le code Java :

```java
@SubscribeEvent
public static void registerAnimations(AnimationRegistryEvent event) {
    event.newBuilder(STATMod.MODID, Animations::build);
}

public static AnimationAccessor<AttackAnimation> MON_ATTACK;

private static void build(AnimationManager.AnimationBuilder builder) {
    MON_ATTACK = builder.addAttackAnimation("mon_attaque", Armatures.BIPED, new AttackAnimation.Phase(...));
}
```

### 5.2 Types d'animations
- `AttackAnimation` : combo d'attaque
- `StaticAnimation` : mouvement/idle
- `GuardAnimation` : blocage
- `DodgeAnimation` : esquive
- `MovementAnimation` : marche/course

### 5.3 Référence
- Les animations vanilla EF sont dans le JAR : `assets/epicfight/animmodels/animations/biped/skill/`
- On peut les extraire et les étudier pour comprendre le format

---

## PHASE 6 : Création du style de combat complet

### 6.1 Ce qu'il faut coder (je peux le faire)
| Fichier | Contenu |
|---------|---------|
| `MonStyleCategories.java` | Enum WeaponCategory custom |
| `MonStyleItem.java` | Item avec WeaponCapability |
| `MonStyles.java` | Enum Style pour les combos |
| `MonStyleSkill.java` | extends BattleStyle (ou Skill) |
| `MonWeaponInnate.java` | Skill arme innée |
| `MonCombatArt.java` | Skill ultimate |
| `MonPassive.java` | Skill passif |
| `MonAnimations.java` | Enregistrement des animations |
| `MonStyleRegistry.java` | DeferredRegister pour skills/items |
| `data/monmod/epicfight/skill/*.json` | Data JSON des skills |
| `assets/monmod/textures/gui/skill/*.png` | Icônes 16×16 |

### 6.2 Ce qu'il faut créer toi-même (ou trouver un artiste)
- Fichiers `.blend` avec les animations
- Export JSON des animations
- Textures des items et icônes (IA possible)

---

## PHASE 7 : Intégration dans STAT_MOD

### 7.1 Si intégré dans STAT_MOD
- Package : `tong.statmod.battlestyles.monstyle/`
- Les skills sont enregistrés dans `SkillRegistry.java`
- Les items dans un nouvel `ItemRegistry`
- Les animations dans `Animations.java`

### 7.2 Si mod séparé
- Dépendance : STAT_MOD + Epic Fight
- Communication via `StatUpdateHandler` ou capabilities
- Package indépendant

---

## RÉFÉRENCES RAPIDES

| Ressource | URL |
|-----------|-----|
| Wiki EF | https://epicfight-docs.readthedocs.io/ |
| API Guide | https://epicfight-docs.readthedocs.io/API/Starting/ |
| Blender Tutorial | https://epicfight-docs.readthedocs.io/Guides/page1/ |
| Weapon Editor | https://epicfight-docs.readthedocs.io/Guides/Weapons/page2/ |
| Vidéo Yesman | https://www.youtube.com/watch?v=iysWR_dSic4 |
| Blender Exporter | https://github.com/Epic-Fight/blender-json-addon |
| Rig du joueur | https://github.com/Epic-Fight/EpicFight-Files |
| Battle-Arts (réf) | https://github.com/Forixaim/Battle-Arts/tree/1.20.1 |
| epicskills (réf) | https://github.com/Antikythera-Studios/epicskills/tree/1.20.1 |

---

## CHECKLIST PROGRESSION

- [ ] Lire la doc API EF
- [ ] Étudier Battle-Arts code source
- [ ] Installer Blender + plugin export
- [ ] Télécharger le rig EF
- [ ] Suivre le Blender Tutorial (wiki)
- [ ] Créer une animation simple → exporter → tester
- [ ] Regarder la vidéo Yesman sur les armes
- [ ] Créer un datapack d'arme de test
- [ ] Créer un item custom en Java
- [ ] Animer une attaque complète (auto1→auto2→auto3)
- [ ] Créer le style de combat complet
- [ ] Intégrer dans STAT_MOD ou mod séparé

---

Quand tu veux commencer une phase, tu me dis et je t'accompagne étape par étape.
