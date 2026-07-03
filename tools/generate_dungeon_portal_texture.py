#!/usr/bin/env python3
"""Mission M6 — Phase β.

Génère la texture 16x16 du bloc {@code dungeon_portal}.

Look : base noire obsidienne + halo violet-arcane pulsant au centre, quelques éclats blancs
pour évoquer les particules ENCHANT. Cohérent visuellement avec les blocs magic forge de
Mission M5 (tint violet).
"""
from __future__ import annotations

from pathlib import Path
import math

try:
    from PIL import Image
except ImportError:
    raise SystemExit("PIL/Pillow requis : pip install pillow")

ROOT = Path(__file__).parent.parent
OUT = ROOT / "src/main/resources/assets/statmod/textures/block/dungeon_portal.png"


def gen_texture() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5

    # Base : obsidienne bruitée.
    for y in range(16):
        for x in range(16):
            # Bruit déterministe (hash simple x/y).
            n = ((x * 41 + y * 97) % 23) - 11
            r = max(0, min(35, 20 + n // 4))
            g = 0
            b = max(0, min(50, 30 + n // 3))
            img.putpixel((x, y), (r, g, b, 255))

    # Halo violet radial.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d < 6:
                falloff = 1.0 - (d / 6.0)
                r0, g0, b0, _ = img.getpixel((x, y))
                r = int(r0 + falloff * 130)
                g = int(g0 + falloff * 20)
                b = int(b0 + falloff * 180)
                img.putpixel((x, y), (min(255, r), min(255, g), min(255, b), 255))

    # Éclats blancs (4 pixels bien placés).
    for px, py in [(4, 4), (11, 5), (5, 11), (12, 11), (8, 2), (2, 8)]:
        img.putpixel((px, py), (240, 220, 255, 255))

    return img


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    gen_texture().save(OUT)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
