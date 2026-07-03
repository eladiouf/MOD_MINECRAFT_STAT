#!/usr/bin/env python
"""
Mission M5 — Phase δ resources generator.

Génère pour la Magic Forge :
- blockstate + 2 modèles JSON (block + item) + texture du infusion_forge
- 3 textures + modèles + lang pour rune_essence_arcane/_pyrium/_mithril
- 1 loot table pour le bloc (drops self)
- 1 tag minecraft:mineable/pickaxe
- 1 recette d'artisanat pour fabriquer infusion_forge
- 3 recettes d'artisanat pour les rune essences
- 5 recettes magic-infused (crafting_shapeless) consommant rough + rune_essence
- Lang en/fr pour tout

Usage : python tools/generate_phase_delta_resources.py
"""

import json
from pathlib import Path
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
BASE_TEX = REPO / "tools" / ".heated_iron_base.png"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "statmod"
DATA = REPO / "src" / "main" / "resources" / "data" / "statmod"

# Tinted teintes pour les nouveaux items/bloc
TINTS = {
    "infusion_forge":      (88, 28, 135),   # obsidian-amethyst
    "rune_essence_arcane": (157, 78, 221),
    "rune_essence_pyrium": (255, 107, 53),
    "rune_essence_mithril":(192, 192, 224),
}

# Lang en/fr
LANG_EN = {
    "block.statmod.infusion_forge":        "Infusion Forge",
    "item.statmod.infusion_forge":         "Infusion Forge",
    "item.statmod.rune_essence_arcane":    "Arcane Rune Essence",
    "item.statmod.rune_essence_pyrium":    "Pyrium Rune Essence",
    "item.statmod.rune_essence_mithril":   "Mithril Rune Essence",
    "itemGroup.statmod.delta_subtitle":    "Magic Forge",
}
LANG_FR = {
    "block.statmod.infusion_forge":        "Forge d'Infusion",
    "item.statmod.infusion_forge":         "Forge d'Infusion",
    "item.statmod.rune_essence_arcane":    "Essence Runique Arcanique",
    "item.statmod.rune_essence_pyrium":    "Essence Runique de Pyrium",
    "item.statmod.rune_essence_mithril":   "Essence Runique de Mithril",
    "itemGroup.statmod.delta_subtitle":    "Forge Magique",
}


def tint(base: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    base = base.convert("RGBA")
    r, g, b, a = base.split()
    tr = r.point(lambda v: max(0, min(255, int(v * color[0] / 255))))
    tg = g.point(lambda v: max(0, min(255, int(v * color[1] / 255))))
    tb = b.point(lambda v: max(0, min(255, int(v * color[2] / 255))))
    return Image.merge("RGBA", (tr, tg, tb, a))


def write_texture_block(name: str, color: tuple[int, int, int], base: Image.Image) -> None:
    out_path = ASSETS / "textures" / "block" / f"{name}.png"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tint(base, color).save(out_path)


def write_texture_item(name: str, color: tuple[int, int, int], base: Image.Image) -> None:
    out_path = ASSETS / "textures" / "item" / f"{name}.png"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tint(base, color).save(out_path)


def write_block_model(name: str) -> None:
    path = ASSETS / "models" / "block" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"statmod:block/{name}"},
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_block_state(name: str) -> None:
    path = ASSETS / "blockstates" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "variants": {"": {"model": f"statmod:block/{name}"}},
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_block_item_model(name: str) -> None:
    path = ASSETS / "models" / "item" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {"parent": f"statmod:block/{name}"}
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_item_model(name: str) -> None:
    path = ASSETS / "models" / "item" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"statmod:item/{name}"},
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_loot_table_self_drop(name: str) -> None:
    path = DATA / "loot_table" / "blocks" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"statmod:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_pickaxe_tag(blocks: list[str]) -> None:
    path = DATA.parent.parent / "data" / "minecraft" / "tags" / "block" / "mineable" / "pickaxe.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    # On ne touche pas le tag global de minecraft : on ajoute notre bloc via un tag
    # statmod:forge qui sera référencé. Pour la simplicité du MVP, écrivons un tag
    # statmod-specific et laissons le pickaxe vanilla tag tranquille — c'est cleaner.
    statmod_tag = DATA / "tags" / "block" / "mineable_with_pickaxe.json"
    statmod_tag.parent.mkdir(parents=True, exist_ok=True)
    statmod_tag.write_text(json.dumps({
        "replace": False,
        "values": [f"statmod:{b}" for b in blocks],
    }, indent=2) + "\n", encoding="utf-8")


def write_infusion_forge_recipe() -> None:
    """4 obsidian + 1 amethyst_block + 1 blueprint_runic_blade → infusion_forge"""
    path = DATA / "recipe" / "infusion_forge.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["OOO", "OBO", "OAO"],
        "key": {
            "O": {"item": "minecraft:obsidian"},
            "B": {"item": "statmod:blueprint_runic_blade"},
            "A": {"item": "minecraft:amethyst_block"},
        },
        "result": {"count": 1, "id": "statmod:infusion_forge"},
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_rune_essence_recipes() -> None:
    """4 amethyst + 1 [ingot type] → rune_essence_<flavor>"""
    recipes = [
        ("rune_essence_arcane",  "irons_spellbooks:arcane_ingot"),
        ("rune_essence_pyrium",  "irons_spellbooks:pyrium_ingot"),
        ("rune_essence_mithril", "irons_spellbooks:mithril_ingot"),
    ]
    for name, ingot in recipes:
        path = DATA / "recipe" / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [
                {"item": "minecraft:amethyst_shard"},
                {"item": "minecraft:amethyst_shard"},
                {"item": "minecraft:amethyst_shard"},
                {"item": "minecraft:amethyst_shard"},
                {"item": ingot},
            ],
            "result": {"count": 2, "id": f"statmod:{name}"},
        }
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_magic_infusion_recipes() -> None:
    """
    5 recettes infusion magic-infused : consomme rough + rune_essence + runic_grip,
    produit une arme spécifique d'un mod magique (Iron's Spellbooks / Simply Swords runic).
    Les targets sont des armes déjà couvertes par Phase γ — Phase δ propose un chemin
    alternatif "premium" qui consomme plus mais qui (à terme — Phase ε) enchantera.
    """
    # (name, rough_intermediate, rune_essence_type, grip, target_weapon)
    infusions = [
        ("infusion_runic_rapier",   "statmod:rough_blade_arcane",        "statmod:rune_essence_arcane",  "statmod:runic_grip",  "simplyswords:runic_rapier"),
        ("infusion_runic_katana",   "statmod:rough_blade_arcane",        "statmod:rune_essence_arcane",  "statmod:runic_grip",  "simplyswords:runic_katana"),
        ("infusion_runic_claymore", "statmod:rough_blade_mithril",       "statmod:rune_essence_mithril", "statmod:runic_grip",  "simplyswords:runic_claymore"),
        ("infusion_runic_spear",    "statmod:rough_spear_tip_arcane",    "statmod:rune_essence_arcane",  "statmod:runic_grip",  "simplyswords:runic_spear"),
        ("infusion_brimstone_cutlass", "statmod:rough_blade_pure_magisteel", "statmod:rune_essence_pyrium", "statmod:runic_grip", "simplyswords:brimstone_cutlass"),
    ]
    for name, rough, essence, grip, target in infusions:
        path = DATA / "recipe" / "infusion" / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "type": "minecraft:crafting_shapeless",
            "category": "equipment",
            "ingredients": [
                {"item": rough},
                {"item": essence},
                {"item": grip},
            ],
            "result": {"count": 1, "id": target},
        }
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


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

    # 1 block infusion_forge
    write_texture_block("infusion_forge", TINTS["infusion_forge"], base)
    write_block_model("infusion_forge")
    write_block_state("infusion_forge")
    write_block_item_model("infusion_forge")
    write_loot_table_self_drop("infusion_forge")
    write_pickaxe_tag(["infusion_forge"])
    write_infusion_forge_recipe()

    # 3 rune essences (items)
    for name in ["rune_essence_arcane", "rune_essence_pyrium", "rune_essence_mithril"]:
        write_texture_item(name, TINTS[name], base)
        write_item_model(name)
    write_rune_essence_recipes()

    # 5 magic infusion recipes (datapack)
    write_magic_infusion_recipes()

    # Lang
    merge_lang(ASSETS / "lang" / "en_us.json", LANG_EN)
    merge_lang(ASSETS / "lang" / "fr_fr.json", LANG_FR)

    print("=== Phase δ resources ===")
    print("  blocks:          1 (infusion_forge)")
    print("  items:           3 (rune_essence_*)")
    print("  crafting recipes: 4 (block + 3 essences)")
    print("  infusion recipes: 5")
    print("  loot table:       1")
    print("  tag mineable_with_pickaxe: 1")
    print("  lang en/fr:       6 entries each")


if __name__ == "__main__":
    main()
