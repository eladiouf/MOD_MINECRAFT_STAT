#!/usr/bin/env python
"""
Mission M5 — Phase γ (Universal Assembly).

Scanne libs/*.jar pour extraire toutes les armes des mods tiers et génère une recette
crafting_shapeless qui assemble chaque arme à partir d'un rough_<class>_<material>
(STAT MOD ou Overgeared natif) + un composant de forme + un grip + un accent
de silhouette.

Output : src/main/resources/data/statmod/recipe/assembly/<modid>/<item>.json

Whitelist Tensura : `tools/tensura_whitelist.txt` — un id par ligne. Vide par défaut.
On exclut aussi overgeared (host), minecraft (vanilla), statmod (nous).

Usage : python tools/generate_assembly_recipes.py
"""

import json
import hashlib
import re
import sys
import zipfile
from dataclasses import dataclass
from collections import Counter
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
NEEDS_WHITELIST = set()

# ─── Classification arme : path regex → weapon class ──────────────────────────────

CLASS_REGEXES = [
    ("blade",        re.compile(r"(sword|katana|rapier|cutlass|saber|sabre|claymore|estoc|zweihander|bastardsword|longsword|scimitar|tachi|kodachi|odachi|wakizashi|gladius|messer|falchion|sax|short_sword|shortsword|long_sword|great_sword|greatsword|broadsword|flamebladedsword|katzbalger|bokken|uchigatana|milady|florett|sidesword|swordbreaker)")),
    ("axe_head",     re.compile(r"(axe|hammer|mace|warhammer|warmaul|maul|heavymace|heavywarhammer|lochaberaxe|battleaxe|tabar|tomahawk|halberd_axe|club|morgenstern|chainmorgenstern|bar_mace|round_mace|war_hammer|barbedclub|nailbat|bat|glove|fist)")),
    ("spear_tip",    re.compile(r"(spear|halberd|glaive|polearm|pike|naginata|ranseur|trident|lance|ahlspiess|concavehalberd|chivalrylance|partizan|guisarme|fauchard|bardiche|voulge|boar_spear|hammer_spear|short_spear|poleaxe)")),
    ("bow_limb",     re.compile(r"(crossbow|longbow|shortbow|bow)")),
    ("staff_core",   re.compile(r"(staff|wand|scepter|sceptre|rod)")),
    ("dagger_blade", re.compile(r"(dagger|knife|shuriken|kunai|stylet|stiletto|dirk|tanto|kris|misericorde|cinquedea|sickle|war_sickle|parrying_dagger|rondel_dagger|backhand_blade)")),
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
    ("phantom",         "arcane"),
    ("lich",            "arcane"),
    ("amethyst",        "arcane"),
    ("obsidian",        "arcane"),
    ("magic",           "arcane"),
    ("spell",           "arcane"),
    ("dead_end",        "arcane"),
    ("blood",           "pyrium"),
    ("fiery",           "pyrium"),
    ("ice",             "mithril"),
    ("lightning",       "arcane"),
    ("dragon",          "adamantite"),
    ("kraken",          "mithril"),
    ("pirate",          "steel"),
    ("warrior",         "steel"),
    ("goblin",          "steel"),
    ("beast",           "steel"),
    ("centipede",       "steel"),
    ("rusted",          "iron"),
    ("noble",           "steel"),
    ("black",           "steel"),
    ("blessed",         "mithril"),
    ("toxic",           "low_magisteel"),
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
WEAPON_MODEL_HINT = re.compile(r"(sword|katana|rapier|cutlass|saber|sabre|scimitar|claymore|estoc|zweihander|bastardsword|longsword|short_sword|shortsword|great_sword|greatsword|broadsword|flamebladedsword|katzbalger|messer|falchion|sax|tachi|kodachi|odachi|wakizashi|gladius|warglaive|spear|halberd|glaive|polearm|pike|naginata|ranseur|trident|lance|ahlspiess|concavehalberd|chivalrylance|partizan|guisarme|fauchard|bardiche|voulge|axe|hammer|mace|warhammer|warmaul|maul|heavymace|heavywarhammer|lochaberaxe|battleaxe|tabar|tomahawk|lucernhammer|greataxe|dagger|knife|shuriken|kunai|stylet|stiletto|dirk|tanto|kris|misericorde|cinquedea|wand|staff|scepter|sceptre|rod|bow|crossbow|florett|milady|sickle|bat|glove|fist|bokken|uchigatana|morgenstern|club)")
MODEL_VARIANT_EXCLUDE = re.compile(r"(^|_)(pulling_[0-9]+|blocking|raised|gui|desc|tooltip|exp|powers|firework|spectral_arrow|arrow|model|item|empty|sheath|handheld|inactive)$")
NON_WEAPON_EXCLUDE = re.compile(r"(spawn_egg|helmet|chestplate|leggings|boots|pattern|decoration|template|tome|manual|schematic|haft|glider|ingot|nugget|soul|curio|example_loot_bag|example_passive_ability_spellbook)")


@dataclass(frozen=True)
class JarItems:
    modid: str
    item_paths: list[str]

FORM_COMPONENT_RULES = [
    ("statmod:katana_tsuba", re.compile(r"(katana|tachi|odachi|wakizashi|kodachi)")),
    ("statmod:rapier_guard", re.compile(r"(rapier|estoc|dagger|knife|shuriken|kunai|stylet|stiletto|dirk|tanto|kris|misericorde|cinquedea|cutlass|saber|sabre|scimitar|gladius|short_sword|shortsword)")),
    ("statmod:halberd_socket", re.compile(r"(spear|halberd|glaive|warglaive|polearm|pike|naginata|ranseur|trident|lance|ahlspiess|concavehalberd|chivalrylance|partizan|guisarme|fauchard|bardiche|voulge|axespear)")),
    ("statmod:warhammer_core", re.compile(r"(axe|hammer|mace|warhammer|warmaul|maul|heavymace|heavywarhammer|lochaberaxe|battleaxe|tabar|tomahawk|lucernhammer|greataxe)")),
    ("statmod:staff_focus", re.compile(r"(staff|wand|scepter|sceptre|rod|bow|crossbow|longbow|shortbow)")),
    ("statmod:claymore_pommel", re.compile(r"(claymore|zweihander|bastardsword|longsword|long_sword|great_sword|greatsword|broadsword|messer|falchion|katzbalger|flamebladedsword|sword|sax)")),
]

DEFAULT_COMPONENT_BY_CLASS = {
    "blade": "statmod:claymore_pommel",
    "axe_head": "statmod:warhammer_core",
    "spear_tip": "statmod:halberd_socket",
    "bow_limb": "statmod:staff_focus",
    "staff_core": "statmod:staff_focus",
    "dagger_blade": "statmod:rapier_guard",
}

ACCENT_RULES = [
    ("minecraft:bamboo", re.compile(r"(warglaive|glaive)")),
    ("minecraft:bamboo", re.compile(r"(katana|tachi|odachi|wakizashi|kodachi)")),
    ("minecraft:gold_nugget", re.compile(r"(chivalrylance|rapier|estoc|cutlass|saber|sabre|scimitar|gladius)")),
    ("minecraft:flint", re.compile(r"(ahlspiess|dagger|knife|shuriken|kunai|stylet|stiletto|dirk|tanto|kris|misericorde|cinquedea|axe|battleaxe|tabar|tomahawk|greataxe|lochaberaxe)")),
    ("minecraft:stick", re.compile(r"(ranseur)")),
    ("minecraft:iron_nugget", re.compile(r"(^|_)(sword|short_sword|shortsword|sax|pike)(_|$)")),
    ("minecraft:iron_ingot", re.compile(r"(bastardsword)")),
    ("minecraft:iron_ingot", re.compile(r"(longsword|long_sword|broadsword|katzbalger|messer|falchion|halberd|glaive|warglaive|guisarme|fauchard|bardiche|voulge|concavehalberd|partizan|axespear)")),
    ("minecraft:iron_ingot", re.compile(r"(lucernhammer|heavywarhammer|warhammer)")),
    ("minecraft:copper_ingot", re.compile(r"(claymore|zweihander|great_sword|greatsword|flamebladedsword|hammer|mace|warmaul|maul|heavymace)")),
    ("minecraft:stick", re.compile(r"(spear|naginata|lance)")),
    ("minecraft:prismarine_shard", re.compile(r"(trident)")),
    ("minecraft:amethyst_shard", re.compile(r"(staff|wand|scepter|sceptre|rod|battlestaff)")),
    ("minecraft:string", re.compile(r"(crossbow|longbow|shortbow|bow)")),
]

DEFAULT_ACCENT_BY_CLASS = {
    "blade": "minecraft:iron_ingot",
    "axe_head": "minecraft:flint",
    "spear_tip": "minecraft:stick",
    "bow_limb": "minecraft:string",
    "staff_core": "minecraft:amethyst_shard",
    "dagger_blade": "minecraft:flint",
}

THEME_SIGNATURE_RULES = [
    ("minecraft:redstone", re.compile(r"(blood|crimson|mecha)")),
    ("minecraft:blaze_powder", re.compile(r"(brimstone|cinder|infernal|flame|fire|fiery|charred|ash|ashen)")),
    ("minecraft:snowball", re.compile(r"(frost|ice)")),
    ("minecraft:glowstone_dust", re.compile(r"(storm|lightning|thunder|holy|paladin|priest|justicar|sun)")),
    ("minecraft:ender_pearl", re.compile(r"(soul|wraith|dark|shadow|chaos|watcher|runic|eclipse|moon|night)")),
    ("minecraft:bone", re.compile(r"(dragon|drake|hunter|executioner)")),
    ("minecraft:leather", re.compile(r"(knight|guardian|warrior|viking|jarls|sutton|byzantine|magyar|crucible|slave)")),
    ("minecraft:book", re.compile(r"(arcane|magic|crystal|spell|rune)")),
]

MOD_SIGNATURES = {
    "simplyswords": "minecraft:rabbit_hide",
    "magistuarmory": "minecraft:chain",
    "epicfight": "minecraft:leather",
    "cdmoveset": "minecraft:coal",
    "epicfight_dd": "minecraft:redstone",
    "darkagesarmory": "minecraft:bone",
    "slu": "minecraft:echo_shard",
    "irons_spellbooks": "minecraft:book",
    "block_factorys_bosses": "minecraft:obsidian",
}


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
    for prefix, mat in MATERIAL_PREFIXES:
        if path.startswith(prefix):
            return mat
    # Aussi check si le préfixe apparaît n'importe où séparé par _
    for prefix, mat in MATERIAL_PREFIXES:
        if f"_{prefix}_" in f"_{path}_":
            return mat
    for prefix, mat in MATERIAL_PREFIXES:
        if f"_{prefix}" in f"_{path}":
            return mat
    return None


def infer_material(modid: str, item_path: str, weapon_class: str) -> str:
    path = item_path.lower()
    explicit = detect_material(path)
    if explicit:
        return explicit

    if weapon_class == "staff_core":
        return "arcane"
    if weapon_class == "bow_limb":
        return "wood"
    if modid == "irons_spellbooks":
        if has_any(path, ("blood", "fiery")):
            return "pyrium"
        if has_any(path, ("ice", "graybeard")):
            return "mithril"
        return "arcane"
    if modid == "tensura":
        if has_any(path, ("high_magic_staff",)):
            return "arcane"
        if has_any(path, ("goblin", "beast", "centipede")):
            return "steel"
        return "diamond"
    if modid in {"epicfight", "epicfight_dd", "cdmoveset"}:
        if has_any(path, ("wooden", "bokken")):
            return "wood"
        if "stone" in path:
            return "stone"
        if has_any(path, ("blood", "ritus")):
            return "pyrium"
        if has_any(path, ("phantom", "night", "uchigatana")):
            return "arcane"
        return "steel"
    if modid in {"simplyswords", "slu"}:
        if has_any(path, ("blood", "charred", "fire", "fiery")):
            return "pyrium"
        if has_any(path, ("blessed", "ice", "frost")):
            return "mithril"
        if has_any(path, ("black", "night", "shadow", "phantom")):
            return "arcane"
        if path.startswith("basic_"):
            return "steel"
        return "diamond"
    if modid.startswith("magistuarmory"):
        if has_any(path, ("wood", "longbow", "crossbow")):
            return "wood"
        if "stone" in path:
            return "stone"
        if has_any(path, ("rusted",)):
            return "iron"
        return "steel"
    if has_any(path, ("wood",)):
        return "wood"
    if "stone" in path:
        return "stone"
    return "steel"


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
    if material in ("silver", "netherite", "wood", "stone", "copper"):
        return f"statmod:rough_{weapon_class}_diamond"
    # iron + classes non couvertes par overgeared (spear/bow/staff/dagger)
    if material == "iron" and weapon_class not in OVERGEARED_ROUGH["iron"]:
        return f"statmod:rough_{weapon_class}_diamond"
    if material == "steel" and weapon_class not in OVERGEARED_ROUGH["steel"]:
        return f"statmod:rough_{weapon_class}_diamond"
    return None


def form_component_for(item_path: str, weapon_class: str) -> str:
    for component_id, rx in FORM_COMPONENT_RULES:
        if rx.search(item_path):
            return component_id
    return DEFAULT_COMPONENT_BY_CLASS[weapon_class]


def grip_for(modid: str, item_path: str, component_id: str) -> str:
    """Choisit le grip d'assemblage selon le tier thématique de l'arme."""
    if modid == "simplyswords" and item_path.startswith(("runic_", "brimstone_", "watcher_")):
        return "statmod:runic_grip"
    if component_id in {"statmod:katana_tsuba", "statmod:rapier_guard"}:
        return "statmod:leather_wrap"
    if component_id in {"statmod:claymore_pommel", "statmod:warhammer_core"}:
        return "statmod:wire_wrap"
    return "statmod:wooden_grip"


def accent_for(item_path: str, weapon_class: str) -> str:
    for accent_id, rx in ACCENT_RULES:
        if rx.search(item_path):
            return accent_id
    return DEFAULT_ACCENT_BY_CLASS[weapon_class]


def signature_for(modid: str, item_path: str) -> str:
    for signature_id, rx in THEME_SIGNATURE_RULES:
        if rx.search(item_path):
            return signature_id
    return MOD_SIGNATURES.get(modid, "minecraft:paper")


def stable_hash_int(text: str) -> int:
    return int(hashlib.sha256(text.encode("utf-8")).hexdigest()[:16], 16)


def has_any(path: str, tokens: tuple[str, ...]) -> bool:
    return any(token in path for token in tokens)


def unique_pool(*ingredient_ids: str) -> list[str]:
    seen: set[str] = set()
    ordered: list[str] = []
    for ingredient_id in ingredient_ids:
        if ingredient_id not in seen:
            ordered.append(ingredient_id)
            seen.add(ingredient_id)
    return ordered


def build_profile_ingredients(
    modid: str,
    item_path: str,
    weapon_class: str,
    rough_id: str,
    component_id: str,
    grip_id: str,
    accent_id: str,
    signature_id: str,
) -> list[str]:
    """Construit un profil d'assemblage structurel avant résolution d'unicité."""
    path = item_path.lower()
    ingredients = [rough_id, component_id, grip_id, accent_id, signature_id]

    if weapon_class == "blade":
        if has_any(path, ("katana", "tachi", "odachi", "wakizashi", "kodachi")):
            ingredients.append(grip_id)
        if has_any(path, ("longsword", "long_sword")):
            ingredients.append(rough_id)
        if has_any(path, ("claymore", "zweihander", "great_sword", "greatsword", "bastardsword", "flamebladedsword")):
            ingredients.extend([rough_id, component_id])
        elif has_any(path, ("broadsword", "messer", "falchion", "katzbalger")):
            ingredients.append(component_id)
        elif re.search(r"(^|_)sword($|_)", path):
            ingredients.append(rough_id)
    elif weapon_class == "dagger_blade":
        ingredients.append(accent_id)
        if has_any(path, ("dirk", "kunai", "shuriken", "tanto")):
            ingredients.append(signature_id)
    elif weapon_class == "spear_tip":
        if has_any(path, ("spear", "naginata", "lance", "pike", "ranseur", "ahlspiess", "chivalrylance")):
            ingredients.append(grip_id)
        if has_any(path, ("halberd", "glaive", "warglaive", "concavehalberd", "guisarme", "fauchard", "bardiche", "voulge", "partizan", "axespear")):
            ingredients.extend([rough_id, component_id])
        if "trident" in path:
            ingredients.append(accent_id)
    elif weapon_class == "axe_head":
        ingredients.append(component_id)
        if has_any(path, ("warhammer", "heavywarhammer", "lucernhammer", "hammer", "mace", "heavymace", "warmaul", "maul")):
            ingredients.append(rough_id)
        if has_any(path, ("greataxe", "battleaxe", "lochaberaxe")):
            ingredients.append(grip_id)
    elif weapon_class == "staff_core":
        ingredients.append(component_id)
        if has_any(path, ("battlestaff", "staff")):
            ingredients.append(rough_id)
        if has_any(path, ("wand", "scepter", "sceptre", "rod")):
            ingredients.append(accent_id)
    elif weapon_class == "bow_limb":
        ingredients.append(accent_id)
        if has_any(path, ("crossbow", "longbow")):
            ingredients.append(rough_id)

    # Sous-charge stable déterministe sur les ingrédients déjà cohérents de l'arme.
    pool = unique_pool(rough_id, component_id, grip_id, accent_id, signature_id)
    seed = stable_hash_int(f"{modid}:{item_path}")
    tuning_steps = 1 + (seed % 3)
    for step in range(tuning_steps):
        ingredients.append(pool[(seed >> (step * 3)) % len(pool)])

    return ingredients


def normalized_signature(ingredients: list[str]) -> tuple[tuple[str, int], ...]:
    return tuple(sorted(Counter(ingredients).items()))


def uniquify_ingredients(full_id: str, ingredients: list[str], seen_signatures: set[tuple[tuple[str, int], ...]]) -> list[str]:
    """Ajoute des duplications structurelles jusqu'à obtenir une signature unique."""
    seed = stable_hash_int(full_id)
    pool = unique_pool(*ingredients)
    signature = normalized_signature(ingredients)
    attempt = 0

    while signature in seen_signatures:
        ingredients.append(pool[(seed + attempt) % len(pool)])
        attempt += 1
        if attempt > 128:
            raise RuntimeError(f"Could not build a unique assembly signature for {full_id}")
        signature = normalized_signature(ingredients)

    seen_signatures.add(signature)
    return ingredients


def read_modid(zf: zipfile.ZipFile) -> str | None:
    try:
        toml = zf.read("META-INF/neoforge.mods.toml").decode("utf-8", errors="ignore")
    except KeyError:
        return None
    m = re.search(r'modId\s*=\s*"([^"]+)"', toml)
    return m.group(1) if m else None


def is_weapon_model_candidate(item_path: str) -> bool:
    path = item_path.lower()
    if MODEL_VARIANT_EXCLUDE.search(path):
        return False
    if NON_WEAPON_EXCLUDE.search(path):
        return False
    if not WEAPON_MODEL_HINT.search(path):
        return False
    return True


def parse_jar_items(jar_path: Path) -> JarItems | None:
    """Retourne (modid, item_paths) en scannant les vrais modèles d'item du jar."""
    try:
        zf = zipfile.ZipFile(jar_path)
    except zipfile.BadZipFile:
        return None
    modid = read_modid(zf)
    if not modid:
        return None
    prefix = f"assets/{modid}/models/item/"
    item_paths = sorted({
        name[len(prefix):-5]
        for name in zf.namelist()
        if name.startswith(prefix)
        and name.endswith(".json")
        and "/" not in name[len(prefix):-5]
        and is_weapon_model_candidate(name[len(prefix):-5])
    })
    return JarItems(modid, item_paths)


def write_assembly_recipe(modid: str, item_path: str, ingredients: list[str]) -> Path:
    out_path = OUT / modid / f"{item_path}.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "type": "minecraft:crafting_shapeless",
        "category": "equipment",
        "ingredients": [{"item": ingredient_id} for ingredient_id in ingredients],
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
    seen_signatures: set[tuple[tuple[str, int], ...]] = set()

    for jar_path in sorted(LIBS.glob("*.jar")):
        result = parse_jar_items(jar_path)
        if result is None:
            continue
        modid, item_paths = result.modid, result.item_paths
        if modid in EXCLUDED_NAMESPACES:
            stats["skipped_excluded_mod"] += len(item_paths)
            continue
        mod_count = 0
        for item_path in sorted(item_paths):
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
            material = infer_material(modid, item_path, weapon_class)
            rough_id = rough_for(material, weapon_class)
            if not rough_id:
                stats["skipped_no_rough"] += 1
                continue
            component_id = form_component_for(item_path, weapon_class)
            grip_id = grip_for(modid, item_path, component_id)
            accent_id = accent_for(item_path, weapon_class)
            signature_id = signature_for(modid, item_path)
            ingredients = build_profile_ingredients(
                modid,
                item_path,
                weapon_class,
                rough_id,
                component_id,
                grip_id,
                accent_id,
                signature_id,
            )
            uniquify_ingredients(full_id, ingredients, seen_signatures)
            write_assembly_recipe(modid, item_path, ingredients)
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
