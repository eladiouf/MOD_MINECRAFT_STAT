#!/usr/bin/env python
"""
Mission M5 — Phase α.2 (placeholder texture generation).

Génère 14 textures placeholder pour les heated_<material>_ingot à partir de la texture
heated_iron_ingot d'Overgeared. Recoloriser via tint multiplicative pour donner à chaque
métal une teinte reconnaissable.

Usage: python tools/generate_heated_textures.py
"""

from pathlib import Path
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
BASE_TEX = REPO / "tools" / ".heated_iron_base.png"
OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "statmod" / "textures" / "item"

# Tint (R, G, B) — multiplicatif. Voir spec §"Métaux chauffés (heated_*)".
TINTS = {
    "heated_gold_ingot":           (255, 215, 0),
    "heated_tin_ingot":            (208, 208, 208),
    "heated_bronze_ingot":         (205, 127, 50),
    "heated_diamond":              (185, 242, 255),
    "heated_pyrium_ingot":         (255, 107, 53),
    "heated_arcane_ingot":         (157, 78, 221),
    "heated_mithril_ingot":        (192, 192, 224),
    "heated_low_magisteel_ingot":  (112, 128, 144),
    "heated_magisteel_ingot":      (70, 130, 180),
    "heated_pure_magisteel_ingot": (30, 144, 255),
    "heated_high_magisteel_ingot": (0, 191, 255),
    "heated_orichalcum_ingot":     (255, 193, 7),
    "heated_adamantite_ingot":     (106, 13, 173),
    "heated_hihiirokane_ingot":    (220, 20, 60),
}


def tint(base: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    """Tint multiplicative en conservant l'alpha source."""
    base = base.convert("RGBA")
    r, g, b, a = base.split()
    tr = r.point(lambda v: int(v * color[0] / 255))
    tg = g.point(lambda v: int(v * color[1] / 255))
    tb = b.point(lambda v: int(v * color[2] / 255))
    return Image.merge("RGBA", (tr, tg, tb, a))


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    base = Image.open(BASE_TEX)
    for name, color in TINTS.items():
        out_path = OUT_DIR / f"{name}.png"
        tinted = tint(base, color)
        tinted.save(out_path)
        print(f"  wrote {out_path.name}")
    print(f"Done — {len(TINTS)} textures into {OUT_DIR}")


if __name__ == "__main__":
    main()
