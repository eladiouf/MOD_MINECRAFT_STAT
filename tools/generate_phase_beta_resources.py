#!/usr/bin/env python
"""
Mission M5 — Phase β resources generator.

Génère pour les 84 rough_<class>_<material> + 4 grips + 4 blueprints :
- textures placeholder (tint du base heated_iron + variation par classe d'arme)
- modèles JSON item
- entries lang en_us + fr_fr
- recettes overgeared:forging pour les 84 rough_*
- recettes crafting_shaped/shapeless pour grips + blueprints

Usage: python tools/generate_phase_beta_resources.py
"""

import json
from pathlib import Path
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
BASE_TEX = REPO / "tools" / ".heated_iron_base.png"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "statmod"
DATA = REPO / "src" / "main" / "resources" / "data" / "statmod"

WEAPON_CLASSES = ["blade", "axe_head", "spear_tip", "bow_limb", "staff_core", "dagger_blade"]

FORGING_BLUEPRINT_TOOLTYPES = {
    "blade": "sword",
    "axe_head": "axe",
    "spear_tip": "pickaxe",
    "bow_limb": "hoe",
    "staff_core": "multitool",
    "dagger_blade": "shovel",
}

# (material_id, anvil_tier, hammering_count, source_heated_id, base_color, en_us_name, fr_fr_name)
MATERIALS = [
    ("gold",            "iron",    5,  "statmod:heated_gold_ingot",            (255, 215, 0),    "Gold",          "Or"),
    ("tin",             "stone",   3,  "statmod:heated_tin_ingot",             (208, 208, 208),  "Tin",           "Étain"),
    ("bronze",          "stone",   4,  "statmod:heated_bronze_ingot",          (205, 127, 50),   "Bronze",        "Bronze"),
    ("diamond",         "above_a", 6,  "statmod:heated_diamond",               (185, 242, 255),  "Diamond",       "Diamant"),
    ("pyrium",          "above_a", 6,  "statmod:heated_pyrium_ingot",          (255, 107, 53),   "Pyrium",        "Pyrium"),
    ("arcane",          "above_a", 7,  "statmod:heated_arcane_ingot",          (157, 78, 221),   "Arcane",        "Arcanique"),
    ("mithril",         "above_a", 7,  "statmod:heated_mithril_ingot",         (192, 192, 224),  "Mithril",       "Mithril"),
    ("low_magisteel",   "above_a", 7,  "statmod:heated_low_magisteel_ingot",   (112, 128, 144),  "Low Magisteel", "Magiacier Bas"),
    ("magisteel",       "above_a", 8,  "statmod:heated_magisteel_ingot",       (70, 130, 180),   "Magisteel",     "Magiacier"),
    ("pure_magisteel",  "above_b", 9,  "statmod:heated_pure_magisteel_ingot",  (30, 144, 255),   "Pure Magisteel", "Magiacier Pur"),
    ("high_magisteel",  "above_b", 10, "statmod:heated_high_magisteel_ingot",  (0, 191, 255),    "High Magisteel", "Haut Magiacier"),
    ("orichalcum",      "above_b", 10, "statmod:heated_orichalcum_ingot",      (255, 193, 7),    "Orichalcum",    "Orichalque"),
    ("adamantite",      "above_b", 11, "statmod:heated_adamantite_ingot",      (106, 13, 173),   "Adamantite",    "Adamantite"),
    ("hihiirokane",     "above_b", 12, "statmod:heated_hihiirokane_ingot",     (220, 20, 60),    "Hihi'irokane",  "Hihi'irokane"),
]

# Patterns par classe : (pattern[], ingot_count, en_label, fr_label)
WEAPON_CLASS_SPECS = {
    "blade":         (["#", "#"],         2, "Blade",       "Lame"),
    "axe_head":      (["##", "# "],       3, "Axe Head",    "Tête de Hache"),
    "spear_tip":     (["#", "#", "#"],    3, "Spear Tip",   "Pointe de Lance"),
    "bow_limb":      (["##", "# "],       3, "Bow Limb",    "Branche d'Arc"),
    "staff_core":    (["#", "#", "#"],    3, "Staff Core",  "Cœur de Bâton"),
    "dagger_blade":  (["#"],              1, "Dagger Blade","Lame de Dague"),
}

# Brightness multiplier par classe (différentier visuellement)
CLASS_BRIGHTNESS = {
    "blade":         1.00,
    "axe_head":      0.85,
    "spear_tip":     1.10,
    "bow_limb":      0.75,
    "staff_core":    1.20,
    "dagger_blade":  0.90,
}

# Blueprints : (name, en_label, fr_label, recipe_shapeless_ingredients)
BLUEPRINTS = [
    ("blueprint_universal_blade", "Universal Blade Blueprint", "Plan de Lame Universel",
     [{"item": "minecraft:paper"}, {"item": "minecraft:iron_nugget"}]),
    ("blueprint_universal_pole",  "Universal Pole Blueprint",  "Plan de Hampe Universel",
     [{"item": "minecraft:paper"}, {"item": "minecraft:stick"}, {"item": "minecraft:iron_nugget"}]),
    ("blueprint_runic_blade",     "Runic Blade Blueprint",     "Plan de Lame Runique",
     [{"item": "minecraft:paper"}, {"item": "minecraft:amethyst_shard"}, {"item": "minecraft:gold_nugget"}]),
    ("blueprint_legendary",       "Legendary Blueprint",       "Plan Légendaire",
     [{"item": "minecraft:paper"}, {"item": "minecraft:nether_star"}]),
]

# Grips : (name, en_label, fr_label, ingredients_shapeless)
GRIPS = [
    ("wooden_grip", "Wooden Grip",   "Manche en Bois",
     [{"item": "minecraft:stick"}, {"item": "minecraft:stick"}]),
    ("leather_wrap", "Leather Wrap", "Manche en Cuir",
     [{"item": "minecraft:leather"}, {"item": "minecraft:string"}]),
    ("wire_wrap", "Wire Wrap",       "Manche Filetée",
     [{"item": "minecraft:iron_nugget"}, {"item": "minecraft:string"}]),
    ("runic_grip", "Runic Grip",     "Manche Runique",
     [{"item": "minecraft:leather"}, {"item": "minecraft:amethyst_shard"}]),
]


def tint(base: Image.Image, color: tuple[int, int, int], brightness: float = 1.0) -> Image.Image:
    base = base.convert("RGBA")
    r, g, b, a = base.split()
    factor = brightness
    tr = r.point(lambda v: max(0, min(255, int(v * color[0] / 255 * factor))))
    tg = g.point(lambda v: max(0, min(255, int(v * color[1] / 255 * factor))))
    tb = b.point(lambda v: max(0, min(255, int(v * color[2] / 255 * factor))))
    return Image.merge("RGBA", (tr, tg, tb, a))


def write_model(name: str) -> None:
    path = ASSETS / "models" / "item" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"statmod:item/{name}"},
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_texture(name: str, color: tuple[int, int, int], brightness: float, base: Image.Image) -> None:
    out_path = ASSETS / "textures" / "item" / f"{name}.png"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tinted = tint(base, color, brightness)
    tinted.save(out_path)


def write_forging_recipe(rough_name: str, source_heated: str, anvil_tier: str,
                         hammering: int, pattern: list[str], ingot_count: int,
                         class_name: str) -> None:
    path = DATA / "recipe" / "forging" / class_name / f"{rough_name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "overgeared:forging",
        "blueprint": [FORGING_BLUEPRINT_TOOLTYPES[class_name]],
        "category": "TOOL_HEADS",
        "hammering": hammering,
        "key": {"#": {"item": source_heated}},
        "pattern": pattern,
        "result": {"count": 1, "id": f"statmod:{rough_name}"},
        "show_notification": False,
        "tier": anvil_tier,
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_shapeless_recipe(name: str, ingredients: list[dict]) -> None:
    path = DATA / "recipe" / "assembly_components" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": ingredients,
        "result": {"count": 1, "id": f"statmod:{name}"},
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
    en_entries: dict[str, str] = {}
    fr_entries: dict[str, str] = {}

    # 84 rough_<class>_<material>
    rough_count = 0
    for cls in WEAPON_CLASSES:
        pattern, ingot_count, cls_en, cls_fr = WEAPON_CLASS_SPECS[cls]
        brightness = CLASS_BRIGHTNESS[cls]
        for (mat, anvil_tier, hammering, source_heated, color, mat_en, mat_fr) in MATERIALS:
            name = f"rough_{cls}_{mat}"
            write_texture(name, color, brightness, base)
            write_model(name)
            write_forging_recipe(name, source_heated, anvil_tier, hammering,
                                  pattern, ingot_count, cls)
            en_entries[f"item.statmod.{name}"] = f"Rough {mat_en} {cls_en}"
            fr_entries[f"item.statmod.{name}"] = f"{cls_fr} Brut(e) en {mat_fr}"
            rough_count += 1

    # 4 grips
    grip_color = (139, 90, 43)  # tan / wood-leather neutral
    for (name, en, fr, ingredients) in GRIPS:
        write_texture(name, grip_color, 1.0, base)
        write_model(name)
        write_shapeless_recipe(name, ingredients)
        en_entries[f"item.statmod.{name}"] = en
        fr_entries[f"item.statmod.{name}"] = fr

    # 4 blueprints
    blueprint_color = (200, 200, 240)  # blueprint-blue tint
    for (name, en, fr, ingredients) in BLUEPRINTS:
        write_texture(name, blueprint_color, 1.0, base)
        write_model(name)
        write_shapeless_recipe(name, ingredients)
        en_entries[f"item.statmod.{name}"] = en
        fr_entries[f"item.statmod.{name}"] = fr

    merge_lang(ASSETS / "lang" / "en_us.json", en_entries)
    merge_lang(ASSETS / "lang" / "fr_fr.json", fr_entries)

    print(f"  rough intermediates: {rough_count}")
    print(f"  grips: {len(GRIPS)}")
    print(f"  blueprints: {len(BLUEPRINTS)}")
    print(f"  total textures/models: {rough_count + len(GRIPS) + len(BLUEPRINTS)}")
    print(f"  forging recipes: {rough_count}")
    print(f"  shapeless recipes: {len(GRIPS) + len(BLUEPRINTS)}")
    print(f"  lang entries en/fr: {len(en_entries)} each")


if __name__ == "__main__":
    main()
