#!/usr/bin/env python
"""
Mission M5 — Phase γ (Universal Assembly).

Scanne libs/*.jar pour extraire toutes les armes des mods tiers et génère une recette
crafting_shapeless qui assemble chaque arme à partir d'un rough_<class>_<material>
(STAT MOD ou Overgeared natif) + un grip.

Output : src/main/resources/data/statmod/recipe/assembly/<modid>/<item>.json

Whitelist Tensura : `tools/tensura_whitelist.txt` — un id par ligne. Vide par défaut.
On exclut aussi overgeared (host), minecraft (vanilla), statmod (nous).

Usage : python tools/generate_assembly_recipes.py
"""

import json
import re
import sys
import zipfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
LIBS = REPO / "libs"
OUT = REPO / "src" / "main" / "resources" / "data" / "statmod" / "recipe" / "assembly"
WHITELIST_FILE = REPO / "tools" / "tensura_whitelist.txt"
STATS_OUT = REPO / "tools" / "assembly_recipes_stats.json"

EXCLUDED_NAMESPACES = {"overgeared", "minecraft", "statmod"}

STATION_MANAGED_RESULTS = {
    "simplyswords:runic_rapier",
    "simplyswords:runic_katana",
    "simplyswords:runic_claymore",
    "simplyswords:runic_spear",
    "simplyswords:runic_longsword",
    "simplyswords:runic_glaive",
    "simplyswords:runic_greathammer",
    "simplyswords:runic_greataxe",
}

# Mods qui demandent une whitelist explicite. Par défaut, on les exclut sauf entrée
# dans tools/tensura_whitelist.txt
NEEDS_WHITELIST = {"tensura"}

# ─── Classification arme : path regex → weapon class ──────────────────────────────

CLASS_REGEXES = [
    ("blade",        re.compile(r"(sword|katana|rapier|cutlass|saber|sabre|claymore|estoc|zweihander|bastardsword|longsword|scimitar|tachi|kodachi|odachi|wakizashi|gladius|messer|falchion|sax|short_sword|long_sword|great_sword|broadsword|flamebladedsword|katzbalger)")),
    ("axe_head",     re.compile(r"(axe|hammer|mace|warhammer|warmaul|maul|heavymace|heavywarhammer|lochaberaxe|battleaxe|tabar|tomahawk|halberd_axe)")),
    ("spear_tip",    re.compile(r"(spear|halberd|glaive|polearm|pike|naginata|ranseur|trident|lance|ahlspiess|concavehalberd|chivalrylance|partizan|guisarme|fauchard|bardiche|voulge)")),
    ("bow_limb",     re.compile(r"(crossbow|longbow|shortbow|bow)")),
    ("staff_core",   re.compile(r"(staff|wand|scepter|sceptre|rod)")),
    ("dagger_blade", re.compile(r"(dagger|knife|shuriken|kunai|stylet|stiletto|dirk|tanto|kris|misericorde|cinquedea)")),
]

# ─── Extraction matériau : path prefix → material id ───────────────────────────────

# Ordre matters : plus spécifique d'abord (high_magisteel avant magisteel).
# Alias : préfixes thématiques mappés vers les 14 matériaux STAT MOD pour étendre la
# couverture sans créer de nouveaux items.
MATERIAL_PREFIXES = [
    # Tier matériau canonique
    ("hihiirokane",     "hihiirokane"),
    ("high_magisteel",  "high_magisteel"),
    ("pure_magisteel",  "pure_magisteel"),
    ("low_magisteel",   "low_magisteel"),
    ("magisteel",       "magisteel"),
    ("adamantite",      "adamantite"),
    ("orichalcum",      "orichalcum"),
    ("mithril",         "mithril"),
    ("arcane",          "arcane"),
    ("pyrium",          "pyrium"),
    ("bronze",          "bronze"),
    ("netherite",       "netherite"),
    ("diamond",         "diamond"),
    ("steel",           "steel"),
    ("silver",          "silver"),
    ("golden",          "gold"),
    ("gold",            "gold"),
    ("iron",            "iron"),
    ("copper",          "copper"),
    ("tin",             "tin"),
    ("stone",           "stone"),
    ("wooden",          "wood"),
    ("wood",            "wood"),
    # ─── Alias thématiques → matériau STAT MOD ─────────────────────────────
    # Simply Swords tiers
    ("runic",           "arcane"),
    ("netherfused",     "high_magisteel"),
    ("runefused",       "arcane"),
    ("soulkeeper",      "high_magisteel"),
    ("brimstone",       "pure_magisteel"),
    ("watcher",         "adamantite"),
    ("fiery",           "magisteel"),
    ("soul",            "high_magisteel"),
    ("wraith",          "adamantite"),
    ("frostbound",      "mithril"),
    ("frost",           "mithril"),
    ("ash",             "magisteel"),
    ("ashen",           "magisteel"),
    ("ancient",         "orichalcum"),
    ("legendary",       "hihiirokane"),
    # Iron's Spellbooks
    ("priest",          "arcane"),
    ("paladin",         "mithril"),
    ("infernal",        "pyrium"),
    # SLU spécifiques
    ("knight",          "steel"),
    ("samurai",         "steel"),
    # Tensura crystals (les armes Tensura sont bloquées par whitelist de toute façon)
]

# ─── Sources rough disponibles ─────────────────────────────────────────────────────

STATMOD_MATERIALS = {
    "gold", "tin", "bronze", "diamond",
    "pyrium", "arcane", "mithril",
    "low_magisteel", "magisteel", "pure_magisteel", "high_magisteel",
    "orichalcum", "adamantite", "hihiirokane",
}

# Overgeared natif — material → class → overgeared rough id
OVERGEARED_ROUGH = {
    "copper": {
        "blade":    "overgeared:copper_sword_blade",
        "axe_head": "overgeared:copper_axe_head",
    },
    "iron": {
        "blade":    "overgeared:iron_sword_blade",
        "axe_head": "overgeared:iron_axe_head",
    },
    "steel": {
        "blade":    "overgeared:steel_sword_blade",
        "axe_head": "overgeared:steel_axe_head",
    },
}

DESCRIPTION_SUFFIX = re.compile(r"\.description_\d+$")


def load_whitelist() -> set[str]:
    if not WHITELIST_FILE.exists():
        return set()
    return {
        line.strip()
        for line in WHITELIST_FILE.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.startswith("#")
    }


def detect_class(path: str) -> str | None:
    """Retourne la classe d'arme ou None si pas reconnaissable."""
    for cls, rx in CLASS_REGEXES:
        if rx.search(path):
            return cls
    return None


def detect_material(path: str) -> str | None:
    """Match le prefix matériau le plus spécifique."""
    for prefix, mat in MATERIAL_PREFIXES:
        if path.startswith(prefix + "_") or path == prefix:
            return mat
    # Aussi check si le préfixe apparaît n'importe où séparé par _
    for prefix, mat in MATERIAL_PREFIXES:
        if f"_{prefix}_" in f"_{path}_":
            return mat
    return None


def rough_for(material: str, weapon_class: str) -> str | None:
    """Retourne l'id rough_intermediate à utiliser, ou None si pas dispo.

    Pour les armes vanilla (silver/netherite/wood/stone) qui n'ont pas de rough natif,
    on retombe sur le diamond statmod pour fournir une couverture minimale. C'est lossy
    (la teinte ne matchera pas le matériau d'origine) mais ça débloque des centaines
    d'armes en plus, et le gating FORGING reste pertinent (via le material gate appliqué
    sur le résultat final, pas sur l'intermédiaire).
    """
    if material in STATMOD_MATERIALS:
        return f"statmod:rough_{weapon_class}_{material}"
    if material in OVERGEARED_ROUGH and weapon_class in OVERGEARED_ROUGH[material]:
        return OVERGEARED_ROUGH[material][weapon_class]
    # Fallback : silver/netherite/wood/stone/iron-pour-classes-non-overgeared
    # → utilise diamond rough comme placeholder universel
    if material in ("silver", "netherite"):
        return f"statmod:rough_{weapon_class}_diamond"
    # iron + classes non couvertes par overgeared (spear/bow/staff/dagger)
    if material == "iron" and weapon_class not in OVERGEARED_ROUGH["iron"]:
        return f"statmod:rough_{weapon_class}_diamond"
    return None


def grip_for(modid: str, item_path: str) -> str:
    """Choisit le grip d'assemblage selon le tier thématique de l'arme."""
    if modid == "simplyswords" and item_path.startswith(("runic_", "brimstone_", "watcher_")):
        return "statmod:runic_grip"
    return "statmod:wooden_grip"


def parse_jar_items(jar_path: Path) -> tuple[str, list[str]] | None:
    """Retourne (modid, item_paths) en lisant lang/en_us et toml."""
    try:
        zf = zipfile.ZipFile(jar_path)
    except zipfile.BadZipFile:
        return None
    modid = None
    try:
        toml = zf.read("META-INF/neoforge.mods.toml").decode("utf-8", errors="ignore")
        m = re.search(r'modId\s*=\s*"([^"]+)"', toml)
        if m:
            modid = m.group(1)
    except KeyError:
        pass
    if not modid:
        return None
    try:
        lang = zf.read(f"assets/{modid}/lang/en_us.json").decode("utf-8")
        data = json.loads(lang)
    except (KeyError, json.JSONDecodeError):
        return None
    item_paths: list[str] = []
    prefix = f"item.{modid}."
    for key in data:
        if key.startswith(prefix):
            item_path = key[len(prefix):]
            if DESCRIPTION_SUFFIX.search(item_path):
                continue
            item_paths.append(item_path)
    return (modid, item_paths)


def write_assembly_recipe(modid: str, item_path: str, rough_id: str, grip_id: str) -> Path:
    out_path = OUT / modid / f"{item_path}.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "minecraft:crafting_shapeless",
        "category": "equipment",
        "ingredients": [
            {"item": rough_id},
            {"item": grip_id},
        ],
        "result": {"count": 1, "id": f"{modid}:{item_path}"},
    }
    out_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    return out_path


def main() -> int:
    if not LIBS.exists():
        print(f"ERROR: libs/ not found at {LIBS}", file=sys.stderr)
        return 1

    # Nettoyer l'output dir précédent pour éviter les recettes orphelines
    if OUT.exists():
        for f in OUT.rglob("*.json"):
            f.unlink()
        for d in sorted(OUT.rglob("*"), reverse=True):
            if d.is_dir():
                try:
                    d.rmdir()
                except OSError:
                    pass

    whitelist = load_whitelist()
    stats = {
        "by_mod": {},
        "skipped_no_class": 0,
        "skipped_no_material": 0,
        "skipped_no_rough": 0,
        "skipped_whitelist": 0,
        "skipped_excluded_mod": 0,
        "generated": 0,
    }

    for jar_path in sorted(LIBS.glob("*.jar")):
        result = parse_jar_items(jar_path)
        if result is None:
            continue
        modid, item_paths = result
        if modid in EXCLUDED_NAMESPACES:
            stats["skipped_excluded_mod"] += len(item_paths)
            continue
        mod_count = 0
        for item_path in item_paths:
            full_id = f"{modid}:{item_path}"
            if full_id in STATION_MANAGED_RESULTS:
                continue
            if modid in NEEDS_WHITELIST and full_id not in whitelist:
                stats["skipped_whitelist"] += 1
                continue
            weapon_class = detect_class(item_path)
            if not weapon_class:
                stats["skipped_no_class"] += 1
                continue
            material = detect_material(item_path)
            if not material:
                stats["skipped_no_material"] += 1
                continue
            rough_id = rough_for(material, weapon_class)
            if not rough_id:
                stats["skipped_no_rough"] += 1
                continue
            write_assembly_recipe(modid, item_path, rough_id, grip_for(modid, item_path))
            stats["generated"] += 1
            mod_count += 1
        if mod_count:
            stats["by_mod"][modid] = mod_count

    STATS_OUT.write_text(json.dumps(stats, indent=2) + "\n", encoding="utf-8")

    print("=== Assembly Recipes Generation ===")
    print(f"  Generated: {stats['generated']}")
    print(f"  Skipped (no class match):    {stats['skipped_no_class']}")
    print(f"  Skipped (no material match): {stats['skipped_no_material']}")
    print(f"  Skipped (no rough available):{stats['skipped_no_rough']}")
    print(f"  Skipped (whitelist filter):  {stats['skipped_whitelist']}")
    print(f"  Skipped (excluded mod):      {stats['skipped_excluded_mod']}")
    print(f"\nBy mod:")
    for mod, n in sorted(stats["by_mod"].items(), key=lambda x: -x[1]):
        print(f"  {mod:30s} {n:4d} recipes")
    print(f"\nStats dumped to {STATS_OUT.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
