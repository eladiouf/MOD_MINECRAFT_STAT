#!/usr/bin/env python3
"""Mission M6 — Boss Altar texture 16×16."""
from PIL import Image
import math
from pathlib import Path

ROOT = Path(__file__).parent.parent
TEX = ROOT / "src/main/resources/assets/statmod/textures/block"

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
cx, cy = 7.5, 7.5

for y in range(16):
    for x in range(16):
        d = math.hypot(x - cx, y - cy)
        if d < 7:
            intensity = 1.0 - (d / 7.0)
            r = int(20 + intensity * 180)
            g = int(5 + intensity * 20)
            b = int(20 + intensity * 180)
            img.putpixel((x, y), (r, g, b, 255))

for px, py in [(7,3),(4,7),(11,7),(7,11)]:
    img.putpixel((px, py), (255, 50, 255, 255))
for px, py in [(3,4),(9,2),(12,6),(5,10),(10,12)]:
    img.putpixel((px, py), (255, 180, 255, 255))

img.save(TEX / "boss_altar.png")
print(f"Wrote {TEX / 'boss_altar.png'}")
