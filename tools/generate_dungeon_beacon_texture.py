#!/usr/bin/env python3
"""Mission M6 — texture 16x16 de l'item dungeon_beacon (balise du Trial Dungeon).

Look : une petite orbe/balise arcane — noyau violet lumineux, anneau doré, éclats blancs.
Cohérent avec le bloc dungeon_portal (tint violet) et les shards.
"""
from __future__ import annotations
from pathlib import Path
import math

try:
    from PIL import Image
except ImportError:
    raise SystemExit("PIL/Pillow requis : pip install pillow")

ROOT = Path(__file__).parent.parent
OUT = ROOT / "src/main/resources/assets/statmod/textures/item/dungeon_beacon.png"


def gen() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 7.2:
                continue  # hors de l'orbe → transparent
            if d > 6.0:
                # anneau doré extérieur
                img.putpixel((x, y), (212, 175, 55, 255))
            elif d > 4.2:
                # coque violette
                t = (d - 4.2) / 1.8
                r = int(70 + 40 * (1 - t)); g = int(20 * (1 - t)); b = int(110 + 40 * (1 - t))
                img.putpixel((x, y), (r, g, b, 255))
            else:
                # noyau lumineux (violet clair → blanc au centre)
                t = d / 4.2
                r = int(200 - 90 * t); g = int(140 - 100 * t); b = int(255 - 40 * t)
                img.putpixel((x, y), (min(255, r), min(255, g), min(255, b), 255))
    # éclats blancs (particules)
    for (sx, sy) in [(6, 4), (10, 6), (5, 9), (9, 10), (7, 7)]:
        img.putpixel((sx, sy), (255, 255, 255, 255))
    return img


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    gen().save(OUT)
    print(f"écrit {OUT}")


if __name__ == "__main__":
    main()
