#!/usr/bin/env python
"""
Mission M5 — Phase ε resources generator (Essences).

Génère pour l'Enchantment Anvil :
- blockstate + modèles + texture placeholder du enchantment_anvil
- loot table + tag pickaxe
- recette d'artisanat pour fabriquer le bloc
- 8 recettes essence : consomme rough + N shards + grip → arme cible d'un mod tiers
  pré-enchantée via components.enchantments

Les shards consommés viennent des mods sources (SLU, Simply Swords, Iron's Spellbooks,
Tensura). STAT MOD ne les crée pas — on les utilise directement.

Usage : python tools/generate_phase_epsilon_resources.py
"""

import json
from pathlib import Path
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
BASE_TEX = REPO / "tools" / ".heated_iron_base.png"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "statmod"
DATA = REPO / "src" / "main" / "resources" / "data" / "statmod"

TINT_ENCHANTMENT_ANVIL = (200, 40, 200)   # magenta / enchanting-purple

LANG_EN = {
    "block.statmod.enchantment_anvil": "Enchantment Anvil",
    "item.statmod.enchantment_anvil":  "Enchantment Anvil",
}
LANG_FR = {
    "block.statmod.enchantment_anvil": "Enclume d'Enchantement",
    "item.statmod.enchantment_anvil":  "Enclume d'Enchantement",
}

# ─── Essence recipes ───────────────────────────────────────────────────────────────
#
# Format : (recipe_id, [ingredients], target_weapon_id, enchantments_dict)
#
# Chaque essence recipe :
# - consomme un rough_<class>_<mat> + N shards + un grip
# - produit l'arme cible avec des enchantements pré-appliqués via components.enchantments
# - le shard source vient d'un mod tiers (SLU, Simply Swords, Iron's, Tensura)
# - le target weapon est déjà couvert par Phase γ → Phase ε est la voie "premium enchanted"

ESSENCE_RECIPES = [
    # (id, rough, shards[], grip, target, enchants)
    ("essence_flame_katana",
     "statmod:rough_blade_arcane",
     [("slu:flame_shard", 2)],
     "statmod:runic_grip",
     "simplyswords:runic_katana",
     {"minecraft:fire_aspect": 2, "minecraft:sharpness": 3}),

    ("essence_shadow_rapier",
     "statmod:rough_blade_adamantite",
     [("slu:shadow_shard", 2)],
     "statmod:wire_wrap",
     "simplyswords:runic_rapier",
     {"minecraft:knockback": 2, "minecraft:sharpness": 4}),

    ("essence_wither_claymore",
     "statmod:rough_blade_high_magisteel",
     [("slu:wither_shard", 3)],
     "statmod:runic_grip",
     "simplyswords:runic_claymore",
     {"minecraft:smite": 4, "minecraft:unbreaking": 2}),

    ("essence_estus_greathammer",
     "statmod:rough_axe_head_mithril",
     [("slu:estus_shard", 2)],
     "statmod:leather_wrap",
     "simplyswords:runic_greathammer",
     {"minecraft:mending": 1, "minecraft:unbreaking": 3}),

    ("essence_magma_greataxe",
     "statmod:rough_axe_head_pure_magisteel",
     [("slu:magma_shard", 2)],
     "statmod:wire_wrap",
     "simplyswords:runic_greataxe",
     {"minecraft:fire_aspect": 2, "minecraft:sharpness": 4}),

    ("essence_permafrost_spear",
     "statmod:rough_spear_tip_mithril",
     [("irons_spellbooks:permafrost_shard", 3)],
     "statmod:runic_grip",
     "simplyswords:runic_spear",
     {"minecraft:sharpness": 3, "minecraft:knockback": 1}),

    ("essence_runefused_longsword",
     "statmod:rough_blade_arcane",
     [("simplyswords:runefused_gem", 1)],
     "statmod:runic_grip",
     "simplyswords:runic_longsword",
     {"minecraft:sharpness": 5, "minecraft:sweeping_edge": 3}),

    ("essence_netherfused_glaive",
     "statmod:rough_spear_tip_pure_magisteel",
     [("simplyswords:netherfused_gem", 1)],
     "statmod:runic_grip",
     "simplyswords:runic_glaive",
     {"minecraft:fire_aspect": 2, "minecraft:sharpness": 4, "minecraft:unbreaking": 2}),
]


def tint(base: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    base = base.convert("RGBA")
    r, g, b, a = base.split()
    tr = r.point(lambda v: max(0, min(255, int(v * color[0] / 255))))
    tg = g.point(lambda v: max(0, min(255, int(v * color[1] / 255))))
    tb = b.point(lambda v: max(0, min(255, int(v * color[2] / 255))))
    return Image.merge("RGBA", (tr, tg, tb, a))


def write_block_resources(name: str, color: tuple[int, int, int], base: Image.Image) -> None:
    # texture
    tex_path = ASSETS / "textures" / "block" / f"{name}.png"
    tex_path.parent.mkdir(parents=True, exist_ok=True)
    tint(base, color).save(tex_path)

    # block model
    model_path = ASSETS / "models" / "block" / f"{name}.json"
    model_path.parent.mkdir(parents=True, exist_ok=True)
    model_path.write_text(json.dumps({
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"statmod:block/{name}"},
    }, indent=2) + "\n", encoding="utf-8")

    # blockstate
    bs_path = ASSETS / "blockstates" / f"{name}.json"
    bs_path.parent.mkdir(parents=True, exist_ok=True)
    bs_path.write_text(json.dumps({
        "variants": {"": {"model": f"statmod:block/{name}"}},
    }, indent=2) + "\n", encoding="utf-8")

    # item model (references block model)
    item_model_path = ASSETS / "models" / "item" / f"{name}.json"
    item_model_path.write_text(json.dumps({
        "parent": f"statmod:block/{name}"
    }, indent=2) + "\n", encoding="utf-8")

    # loot table (drops self)
    loot_path = DATA / "loot_table" / "blocks" / f"{name}.json"
    loot_path.parent.mkdir(parents=True, exist_ok=True)
    loot_path.write_text(json.dumps({
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"statmod:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }, indent=2) + "\n", encoding="utf-8")


def append_pickaxe_tag(new_blocks: list[str]) -> None:
    """Add new blocks to the existing mineable_with_pickaxe tag."""
    tag_path = DATA / "tags" / "block" / "mineable_with_pickaxe.json"
    if tag_path.exists():
        existing = json.loads(tag_path.read_text(encoding="utf-8"))
    else:
        existing = {"replace": False, "values": []}
    values = set(existing.get("values", []))
    for b in new_blocks:
        values.add(f"statmod:{b}")
    existing["values"] = sorted(values)
    tag_path.write_text(json.dumps(existing, indent=2) + "\n", encoding="utf-8")


def write_enchantment_anvil_recipe() -> None:
    """4 amethyst_block + 1 enchanting_table + 1 infusion_forge → enchantment_anvil"""
    path = DATA / "recipe" / "enchantment_anvil.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["AAA", "AEA", "AIA"],
        "key": {
            "A": {"item": "minecraft:amethyst_block"},
            "E": {"item": "minecraft:enchanting_table"},
            "I": {"item": "statmod:infusion_forge"},
        },
        "result": {"count": 1, "id": "statmod:enchantment_anvil"},
    }, indent=2) + "\n", encoding="utf-8")


def write_essence_recipes() -> None:
    """
    Génère les 8 recettes essence.

    Format Minecraft 1.21.1 : le champ `result.components` permet d'appliquer les
    enchantements pré-cuits sur l'output.
    """
    for (name, rough, shards, grip, target, enchants) in ESSENCE_RECIPES:
        ingredients = [{"item": rough}]
        for (shard_id, count) in shards:
            for _ in range(count):
                ingredients.append({"item": shard_id})
        ingredients.append({"item": grip})

        levels = {ench_id: level for (ench_id, level) in enchants.items()}

        path = DATA / "recipe" / "essence" / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({
            "type": "minecraft:crafting_shapeless",
            "category": "equipment",
            "ingredients": ingredients,
            "result": {
                "count": 1,
                "id": target,
                "components": {
                    "minecraft:enchantments": {"levels": levels},
                },
            },
        }, indent=2) + "\n", encoding="utf-8")


def merge_lang(lang_file: Path, entries: dict[str, str]) -> None:
    if lang_file.exists():
        existing = json.loads(lang_file.read_text(encoding="utf-8"))
    else:
        existing = {}
    existing.update(entries)
    lang_file.write_text(
        json.dumps(existing, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def main() -> None:
    base = Image.open(BASE_TEX)

    # 1 bloc enchantment_anvil + toutes ses ressources
    write_block_resources("enchantment_anvil", TINT_ENCHANTMENT_ANVIL, base)
    append_pickaxe_tag(["enchantment_anvil"])
    write_enchantment_anvil_recipe()

    # 8 recettes essence
    write_essence_recipes()

    # Lang
    merge_lang(ASSETS / "lang" / "en_us.json", LANG_EN)
    merge_lang(ASSETS / "lang" / "fr_fr.json", LANG_FR)

    print("=== Phase ε resources ===")
    print("  blocks:               1 (enchantment_anvil)")
    print("  crafting recipes:     1 (block itself)")
    print(f"  essence recipes:      {len(ESSENCE_RECIPES)}")
    print("  loot table:           1")
    print("  tag pickaxe:          appended")
    print("  lang en/fr:           2 entries each")


if __name__ == "__main__":
    main()
