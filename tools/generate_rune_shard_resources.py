#!/usr/bin/env python3
"""Mission M6 — Phase δ.

Génère toutes les ressources pour les 25 RuneShards :
- Textures 16×16 (tint par famille + éclat par rareté)
- Item models JSON (parent item/generated)
- Lang EN + FR (append)

Pattern :
- Familles : tint de base
- Raretés : intensité + éclats
"""
from __future__ import annotations

from pathlib import Path
import json
import math

try:
    from PIL import Image
except ImportError:
    raise SystemExit("PIL/Pillow requis : pip install pillow")

ROOT = Path(__file__).parent.parent
TEX = ROOT / "src/main/resources/assets/statmod/textures/item"
MODELS = ROOT / "src/main/resources/assets/statmod/models/item"
LANG_EN = ROOT / "src/main/resources/assets/statmod/lang/en_us.json"
LANG_FR = ROOT / "src/main/resources/assets/statmod/lang/fr_fr.json"

RARITIES = ["common", "uncommon", "rare", "epic", "legendary"]
FAMILIES = ["physical", "magical", "vital", "agile", "spiritual"]

FAMILY_TINT = {
    "physical":  (200, 60, 40),    # rouge sang
    "magical":   (80, 100, 220),   # bleu arcane
    "vital":     (60, 180, 90),    # vert vie
    "agile":     (230, 210, 60),   # jaune vif
    "spiritual": (200, 150, 240),  # violet éthéré
}

RARITY_BRIGHTNESS = {
    "common":    0.55,
    "uncommon":  0.75,
    "rare":      0.95,
    "epic":      1.10,
    "legendary": 1.30,
}

RARITY_SPARKLE_COUNT = {
    "common":    0,
    "uncommon":  1,
    "rare":      2,
    "epic":      4,
    "legendary": 6,
}

RARITY_DISPLAY_EN = {
    "common": "Common", "uncommon": "Uncommon", "rare": "Rare",
    "epic": "Epic", "legendary": "Legendary",
}
RARITY_DISPLAY_FR = {
    "common": "Commun", "uncommon": "Peu Commun", "rare": "Rare",
    "epic": "Épique", "legendary": "Légendaire",
}
FAMILY_DISPLAY_EN = {
    "physical": "Physical", "magical": "Magical", "vital": "Vital",
    "agile": "Agile", "spiritual": "Spiritual",
}
FAMILY_DISPLAY_FR = {
    "physical": "Physique", "magical": "Magique", "vital": "Vital",
    "agile": "Agile", "spiritual": "Spirituel",
}


def shard_texture(family: str, rarity: str) -> Image.Image:
    """Génère un cristal diamond-shape 16×16 tinté par famille + intensité par rareté."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    r0, g0, b0 = FAMILY_TINT[family]
    brightness = RARITY_BRIGHTNESS[rarity]
    cx, cy = 7.5, 7.5

    # Corps du cristal (diamond shape).
    for y in range(16):
        for x in range(16):
            dx = abs(x - cx)
            dy = abs(y - cy)
            manhattan = dx + dy
            if manhattan > 6.5:
                continue
            falloff = 1.0 - (manhattan / 7.5)
            r = int(min(255, r0 * brightness * (0.5 + falloff * 0.5)))
            g = int(min(255, g0 * brightness * (0.5 + falloff * 0.5)))
            b = int(min(255, b0 * brightness * (0.5 + falloff * 0.5)))
            img.putpixel((x, y), (r, g, b, 255))

    # Highlight central diagonal (facette lumineuse).
    for i in range(-3, 4):
        px = int(cx + i)
        py = int(cy - i)
        if 0 <= px < 16 and 0 <= py < 16:
            base = img.getpixel((px, py))
            if base[3] > 0:
                img.putpixel((px, py), (
                    min(255, base[0] + 60),
                    min(255, base[1] + 60),
                    min(255, base[2] + 60),
                    255
                ))

    # Contour sombre pour ancrer.
    for y in range(16):
        for x in range(16):
            pixel = img.getpixel((x, y))
            if pixel[3] == 0:
                continue
            # Check neighbors — si voisin transparent → assombrir bord.
            edge = False
            for nx, ny in [(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)]:
                if 0 <= nx < 16 and 0 <= ny < 16:
                    if img.getpixel((nx, ny))[3] == 0:
                        edge = True
                        break
            if edge:
                img.putpixel((x, y), (
                    pixel[0] // 3,
                    pixel[1] // 3,
                    pixel[2] // 3,
                    255
                ))

    # Éclats blancs (sparkles) selon rareté.
    sparkles = RARITY_SPARKLE_COUNT[rarity]
    positions = [(3, 4), (11, 5), (4, 11), (12, 12), (7, 2), (2, 8)]
    for i in range(min(sparkles, len(positions))):
        px, py = positions[i]
        img.putpixel((px, py), (255, 245, 240, 255))
    return img


def item_model_json(shard_id: str) -> dict:
    return {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"statmod:item/{shard_id}"
        }
    }


def main() -> None:
    TEX.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)

    en_lang = json.loads(LANG_EN.read_text(encoding="utf-8"))
    fr_lang = json.loads(LANG_FR.read_text(encoding="utf-8"))

    for rarity in RARITIES:
        for family in FAMILIES:
            shard_id = f"{rarity}_{family}_shard"
            # Texture
            tex = shard_texture(family, rarity)
            tex.save(TEX / f"{shard_id}.png")
            # Model
            (MODELS / f"{shard_id}.json").write_text(
                json.dumps(item_model_json(shard_id), indent=2), encoding="utf-8"
            )
            # Lang
            en_lang[f"item.statmod.{shard_id}"] = (
                f"{RARITY_DISPLAY_EN[rarity]} {FAMILY_DISPLAY_EN[family]} Shard"
            )
            fr_lang[f"item.statmod.{shard_id}"] = (
                f"Éclat {FAMILY_DISPLAY_FR[family]} {RARITY_DISPLAY_FR[rarity]}"
            )

    LANG_EN.write_text(json.dumps(en_lang, indent=2, ensure_ascii=False), encoding="utf-8")
    LANG_FR.write_text(json.dumps(fr_lang, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"Generated 25 shards under {TEX}")
    print(f"Generated 25 models under {MODELS}")
    print(f"Added 25 lang entries to en_us.json and fr_fr.json")


if __name__ == "__main__":
    main()
