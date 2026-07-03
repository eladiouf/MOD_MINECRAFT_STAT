#!/usr/bin/env python3
"""Mission M6 — Phase ζ.

Génère les textures 16×16 des deux blocs de sortie d'étage :
- {@code return_beacon} : halo violet-arcane, obélisque style beacon
- {@code next_floor_teleporter} : portail cyan-glacé, style end portal frame
"""
from __future__ import annotations

from pathlib import Path
import math

try:
    from PIL import Image
except ImportError:
    raise SystemExit("PIL/Pillow requis : pip install pillow")

ROOT = Path(__file__).parent.parent
TEX = ROOT / "src/main/resources/assets/statmod/textures/block"


def return_beacon() -> Image.Image:
    """Bloc violet lumineux avec cristal central plus clair."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            n = ((x * 43 + y * 91) % 21) - 10
            base_r = 45 + n // 3
            base_g = 15 + n // 5
            base_b = 75 + n // 2
            img.putpixel((x, y), (max(0, base_r), max(0, base_g), max(0, min(255, base_b)), 255))

    # Cristal central lumineux.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d < 4:
                falloff = 1.0 - (d / 4.0)
                r0, g0, b0, _ = img.getpixel((x, y))
                r = min(255, int(r0 + falloff * 160))
                g = min(255, int(g0 + falloff * 80))
                b = min(255, int(b0 + falloff * 200))
                img.putpixel((x, y), (r, g, b, 255))

    # Éclats blancs.
    for px, py in [(4, 7), (7, 3), (11, 8), (8, 12)]:
        img.putpixel((px, py), (255, 240, 255, 255))
    return img


def next_floor_teleporter() -> Image.Image:
    """Portail cyan avec swirl radial, feel glacé."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            angle = math.atan2(y - cy, x - cx)
            swirl = math.sin(angle * 4 + d * 0.8) * 0.5 + 0.5
            base_r = int(15 + swirl * 40)
            base_g = int(80 + swirl * 100)
            base_b = int(150 + swirl * 80)
            img.putpixel((x, y), (base_r, base_g, min(255, base_b), 255))

    # Bordure sombre pour ancrer le bloc.
    for x in range(16):
        for y in (0, 15):
            img.putpixel((x, y), (10, 30, 60, 255))
    for y in range(16):
        for x in (0, 15):
            img.putpixel((x, y), (10, 30, 60, 255))

    # Éclats blancs (particules figées).
    for px, py in [(5, 5), (10, 6), (6, 10), (11, 11)]:
        img.putpixel((px, py), (220, 250, 255, 255))
    return img


def main() -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    return_beacon().save(TEX / "return_beacon.png")
    print(f"Wrote {TEX / 'return_beacon.png'}")
    next_floor_teleporter().save(TEX / "next_floor_teleporter.png")
    print(f"Wrote {TEX / 'next_floor_teleporter.png'}")


if __name__ == "__main__":
    main()
