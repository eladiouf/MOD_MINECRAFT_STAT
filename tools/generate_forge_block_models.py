#!/usr/bin/env python
"""
Mission M5 — Polish visuel blocs Phase δ + ε.

Prend le modèle du tier_b_smithing_anvil d'Overgeared (l'enclume la plus élevée) et
le duplique pour nos 2 blocs statmod:infusion_forge et statmod:enchantment_anvil, en
remplaçant :
- la texture par une version tintée (comme pour heated_* items)
- les texture refs dans le modèle JSON par nos statmod:block/<name>

Comme le modèle Overgeared utilise "facing" via blockstate, on écrit aussi un blockstate
qui gère les 4 rotations horizontales (mais pour l'instant, nos blocks Java ne sont pas
directionnels — on garde juste la variante "" par défaut qui rend le modèle sans rotation).

Usage : python tools/generate_forge_block_models.py
"""

import json
import re
from pathlib import Path
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
BASE_MODEL = REPO / "tools" / ".tier_b_anvil_model.json"
BASE_TEX = REPO / "tools" / ".smithing_anvil_base.png"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "statmod"

# (block_name, tint_rgb)
BLOCKS = [
    ("infusion_forge",     (88, 28, 135)),    # obsidian-amethyst purple
    ("enchantment_anvil",  (200, 40, 200)),   # magenta enchant
]


def tint(base: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    """Tint multiplicatif préservant l'alpha. Convertit en RGBA d'abord."""
    base = base.convert("RGBA")
    r, g, b, a = base.split()
    tr = r.point(lambda v: max(0, min(255, int(v * color[0] / 255))))
    tg = g.point(lambda v: max(0, min(255, int(v * color[1] / 255))))
    tb = b.point(lambda v: max(0, min(255, int(v * color[2] / 255))))
    return Image.merge("RGBA", (tr, tg, tb, a))


def load_base_model() -> dict:
    """Charge le modèle Overgeared tier_b et remplace les refs de texture."""
    return json.loads(BASE_MODEL.read_text(encoding="utf-8"))


def build_block_model(name: str) -> dict:
    """Renvoie une copie du modèle base avec texture ref remplacée."""
    model = load_base_model()
    # Remplacer les texture refs "overgeared:block/smithing_anvil" par le nôtre
    if "textures" in model:
        for key, value in model["textures"].items():
            if isinstance(value, str) and "smithing_anvil" in value:
                model["textures"][key] = f"statmod:block/{name}"
    return model


def write_texture(name: str, tint_rgb: tuple[int, int, int], base: Image.Image) -> None:
    out = ASSETS / "textures" / "block" / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    tint(base, tint_rgb).save(out)


def write_block_model(name: str) -> None:
    out = ASSETS / "models" / "block" / f"{name}.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(build_block_model(name), indent=2) + "\n", encoding="utf-8")


def write_item_model(name: str) -> None:
    """Le modèle item pointe vers le modèle block."""
    out = ASSETS / "models" / "item" / f"{name}.json"
    out.write_text(json.dumps({
        "parent": f"statmod:block/{name}",
    }, indent=2) + "\n", encoding="utf-8")


def write_blockstate(name: str) -> None:
    """Blockstate simple (pas de facing pour l'instant, block non-directionnel)."""
    out = ASSETS / "blockstates" / f"{name}.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({
        "variants": {"": {"model": f"statmod:block/{name}"}},
    }, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    base_tex = Image.open(BASE_TEX)
    for (name, tint_rgb) in BLOCKS:
        write_texture(name, tint_rgb, base_tex)
        write_block_model(name)
        write_item_model(name)
        write_blockstate(name)
        print(f"  {name}: texture + block model + item model + blockstate")
    print(f"\nDone — {len(BLOCKS)} blocks polished with tier_b_smithing_anvil geometry.")


if __name__ == "__main__":
    main()
