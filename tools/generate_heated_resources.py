#!/usr/bin/env python
"""
Mission M5 — Phase α (resources for heated_*_ingot).

Génère pour chaque heated_<material>_ingot :
- son modèle JSON item (parent: generated, layer0: statmod:item/<name>)
- son entrée lang en_us + fr_fr
- sa recette minecraft:blasting (ingot froid → heated, cookingtime par tier)

Usage: python tools/generate_heated_resources.py
"""

import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "statmod"
DATA = REPO / "src" / "main" / "resources" / "data" / "statmod"

# (statmod_id, source_ingot_id, cookingtime_ticks, en_us, fr_fr)
MATERIALS = [
    ("heated_gold_ingot",           "minecraft:gold_ingot",                150, "Heated Gold Ingot",            "Lingot d'Or Chauffé"),
    ("heated_tin_ingot",            "magistuarmory:tin_ingot",             100, "Heated Tin Ingot",             "Lingot d'Étain Chauffé"),
    ("heated_bronze_ingot",         "magistuarmory:bronze_ingot",          100, "Heated Bronze Ingot",          "Lingot de Bronze Chauffé"),
    ("heated_diamond",              "minecraft:diamond",                   200, "Heated Diamond",               "Diamant Chauffé"),
    ("heated_pyrium_ingot",         "irons_spellbooks:pyrium_ingot",       200, "Heated Pyrium Ingot",          "Lingot de Pyrium Chauffé"),
    ("heated_arcane_ingot",         "irons_spellbooks:arcane_ingot",       250, "Heated Arcane Ingot",          "Lingot Arcanique Chauffé"),
    ("heated_mithril_ingot",        "irons_spellbooks:mithril_ingot",      250, "Heated Mithril Ingot",         "Lingot de Mithril Chauffé"),
    ("heated_low_magisteel_ingot",  "tensura:low_magisteel_ingot",         300, "Heated Low Magisteel Ingot",   "Lingot de Magiacier Bas Chauffé"),
    ("heated_magisteel_ingot",      "tensura:magisteel_ingot",             300, "Heated Magisteel Ingot",       "Lingot de Magiacier Chauffé"),
    ("heated_pure_magisteel_ingot", "tensura:pure_magisteel_ingot",        300, "Heated Pure Magisteel Ingot",  "Lingot de Magiacier Pur Chauffé"),
    ("heated_high_magisteel_ingot", "tensura:high_magisteel_ingot",        350, "Heated High Magisteel Ingot",  "Lingot de Haut Magiacier Chauffé"),
    ("heated_orichalcum_ingot",     "tensura:orichalcum_ingot",            350, "Heated Orichalcum Ingot",      "Lingot d'Orichalque Chauffé"),
    ("heated_adamantite_ingot",     "tensura:adamantite_ingot",            400, "Heated Adamantite Ingot",      "Lingot d'Adamantite Chauffé"),
    ("heated_hihiirokane_ingot",    "tensura:hihiirokane_ingot",           400, "Heated Hihi'irokane Ingot",    "Lingot d'Hihi'irokane Chauffé"),
]


def write_model(name: str) -> None:
    path = ASSETS / "models" / "item" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"statmod:item/{name}"},
    }
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_recipe(name: str, source: str, cookingtime: int) -> None:
    path = DATA / "recipe" / "heating" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "minecraft:blasting",
        "category": "misc",
        "cookingtime": cookingtime,
        "experience": 0.5,
        "ingredient": {"item": source},
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
    en_entries = {}
    fr_entries = {}
    for name, source, cookingtime, en, fr in MATERIALS:
        write_model(name)
        write_recipe(name, source, cookingtime)
        en_entries[f"item.statmod.{name}"] = en
        fr_entries[f"item.statmod.{name}"] = fr
    merge_lang(ASSETS / "lang" / "en_us.json", en_entries)
    merge_lang(ASSETS / "lang" / "fr_fr.json", fr_entries)
    print(f"  models: {len(MATERIALS)}")
    print(f"  recipes: {len(MATERIALS)}")
    print(f"  lang en/fr: {len(MATERIALS)} entries each")


if __name__ == "__main__":
    main()
