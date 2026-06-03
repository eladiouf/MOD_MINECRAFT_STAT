"""
STAT Mod Icon Generator — Dark RPG Minecraft-style with varied shapes
Shapes: Shield (Combat), Diamond (Magic), Rounded Square (Survival), Octagon (Crafting), Hexagon (Mental)
All NEAREST pixel-art, dark stone palettes.
"""
import json, os, re, math
import xml.etree.ElementTree as ET
from svg.path import parse_path
from PIL import Image, ImageDraw

ICON_SIZE = 128
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
GAME_ICONS_DIR = os.path.join(SCRIPT_DIR, "..", "..", "SIHRIYA", "tools", "game-icons", "icons-master")
OUT_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "statmod", "textures", "gui")

# ─── Shape helpers ───

def shield_poly(cx, cy, r):
    w = r * 0.88
    return [
        (cx - w, cy - r), (cx + w, cy - r),
        (cx + w, cy - r * 0.3), (cx + w * 0.7, cy + r * 0.55),
        (cx + w * 0.3, cy + r * 0.85), (cx, cy + r),
        (cx - w * 0.3, cy + r * 0.85), (cx - w * 0.7, cy + r * 0.55),
        (cx - w, cy - r * 0.3),
    ]

def diamond_poly(cx, cy, r):
    return [
        (cx, cy - r),
        (cx + r * 0.82, cy - r * 0.35),
        (cx + r * 0.65, cy + r * 0.5),
        (cx, cy + r),
        (cx - r * 0.65, cy + r * 0.5),
        (cx - r * 0.82, cy - r * 0.35),
    ]

def octagon_poly(cx, cy, r):
    s = r * 0.414
    return [
        (cx - s, cy - r), (cx + s, cy - r),
        (cx + r, cy - s), (cx + r, cy + s),
        (cx + s, cy + r), (cx - s, cy + r),
        (cx - r, cy + s), (cx - r, cy - s),
    ]

def hexagon_poly(cx, cy, r):
    pts = []
    for i in range(6):
        a = math.pi / 3 * i - math.pi / 6
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts

SHAPES = {
    'shield': shield_poly,
    'diamond': diamond_poly,
    'octagon': octagon_poly,
    'hexagon': hexagon_poly,
}

STAT_SHAPES = [
    'shield','shield','shield','shield','shield','shield','shield',
    'diamond','diamond','diamond','diamond','diamond','diamond','diamond','diamond','diamond',
    'rounded_square','rounded_square',
    'octagon','octagon','octagon',
    'hexagon','hexagon',
]

STATS = [
    ("BRUTE_FORCE", "heavy-slash", (180,100,80), (50,25,20)),
    ("BLADE_TECHNIQUE", "cutlass", (170,170,185), (40,40,45)),
    ("RAPIDITE", "sprint", (180,150,100), (45,38,25)),
    ("AGILITY", "acrobatic", (165,170,175), (38,40,42)),
    ("PHYSICAL_RESISTANCE", "shield", (160,165,170), (38,40,42)),
    ("PHYSICAL_ENDURANCE", "heart", (180,120,90), (48,30,20)),
    ("PRECISION", "eye-target", (190,190,195), (45,45,47)),
    ("ARCANE_POWER", "magic-swirl", (180,140,200), (42,30,50)),
    ("WATER_AFFINITY", "water-drop", (120,150,200), (25,35,55)),
    ("EARTH_AFFINITY", "stone-pile", (140,120,90), (35,30,22)),
    ("FIRE_AFFINITY", "fire-flower", (200,100,70), (55,22,15)),
    ("AIR_AFFINITY", "wind", (185,185,200), (42,42,48)),
    ("MAGIC_RESISTANCE", "diamond-armor", (130,120,145), (30,28,38)),
    ("CASTING_SPEED", "lightning", (200,170,80), (50,42,18)),
    ("MANA_POOL", "mana", (150,190,220), (30,48,60)),
    ("ERUDITION", "book", (190,185,170), (48,45,40)),
    ("TRACKING", "footprint", (165,130,90), (42,32,22)),
    ("KEEN_SENSES", "eagle-eye", (175,175,190), (42,42,48)),
    ("FORGING", "anvil", (150,145,140), (38,36,34)),
    ("COOKING", "cooking-pot", (170,165,145), (42,40,35)),
    ("ALCHEMY", "potion-ball", (200,170,80), (50,42,20)),
    ("INTIMIDATION", "skull", (190,180,165), (48,42,38)),
    ("WILLPOWER", "crown", (200,185,100), (50,45,25)),
]

def build_svg_index():
    index = {}
    if not os.path.isdir(GAME_ICONS_DIR): return index
    for root, dirs, files in os.walk(GAME_ICONS_DIR):
        for f in files:
            if not f.endswith('.svg'): continue
            index[f[:-4]] = os.path.join(root, f)
    return index

def render_svg_pixel(svg_path, target_size, color):
    tree = ET.parse(svg_path); root = tree.getroot()
    vb = root.get('viewBox')
    if vb: sw, sh = [float(x) for x in vb.split()][2:]
    else: sw, sh = float(root.get('width',512)), float(root.get('height',512))
    ns = {'svg':'http://www.w3.org/2000/svg'}
    path_els = root.findall('.//svg:path',ns) or root.findall('.//{http://www.w3.org/2000/svg}path')
    pd = []
    for p in path_els:
        d = p.get('d','')
        if not d: continue
        f = (p.get('fill') or '#fff').lower()
        if 'm0 0' in d[:8].lower() or 'm0,0' in d[:8].lower(): continue
        if f in ('#fff','#ffffff','white','','#fff','none'): pd.append(d)
    if not pd:
        for p in path_els:
            d = p.get('d','')
            if d and 'm0 0' not in d[:8].lower(): pd.append(d)
    if not pd: return None
    scale = 4; rs = target_size * scale
    img = Image.new("RGBA", (rs, rs), (0,0,0,0)); draw = ImageDraw.Draw(img)
    for d in pd:
        try:
            parsed = parse_path(d)
            pts = [(parsed.point(i/128).real, parsed.point(i/128).imag) for i in range(129)]
            xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
            pw = max(xs)-min(xs); ph = max(ys)-min(ys)
            if pw == 0 or ph == 0: continue
            pad = 0.05 * max(pw, ph)
            s = min((rs-2)/(pw+2*pad), (rs-2)/(ph+2*pad))
            cx2 = (max(xs)+min(xs))/2; cy2 = (max(ys)+min(ys))/2
            scaled = [(int((x-cx2)*s+rs/2), int((y-cy2)*s+rs/2)) for x,y in pts]
            draw.polygon(scaled, fill=color+(255,))
        except: continue
    return img.resize((target_size, target_size), Image.NEAREST)

def make_icon(svg_name, icon_color, bg_tint, shape, size=ICON_SIZE):
    svg_index = build_svg_index()
    cx, cy = size // 2, size // 2
    r = size // 2 - 3
    img = Image.new("RGBA", (size, size), (0,0,0,0))
    draw = ImageDraw.Draw(img)
    bc = tuple(min(255, icon_color[j]+30) for j in range(3))

    if shape == 'rounded_square':
        bg_r = int(r * 0.82)
        rr = int(r * 0.22)
        bbox = [cx-bg_r, cy-bg_r, cx+bg_r, cy+bg_r]
        draw.rounded_rectangle(bbox, radius=rr, outline=bc+(180,), width=2)
        draw.rounded_rectangle([bbox[0]+2,bbox[1]+2,bbox[2]-2,bbox[3]-2], radius=max(0,rr-2), outline=bc+(60,), width=1)
        mask = Image.new("L", (size,size), 0)
        mdraw = ImageDraw.Draw(mask)
        mdraw.rounded_rectangle(bbox, radius=rr, fill=255)
    else:
        poly_fn = SHAPES.get(shape)
        if not poly_fn: return None
        poly = poly_fn(cx, cy, r)
        draw.polygon(poly, outline=bc+(200,), width=2)
        draw.polygon(poly_fn(cx-1, cy, r-2), outline=bc+(80,), width=1)
        # Shape-specific decorations
        if shape == 'shield':
            band_y = cy - r * 0.55; band_l = r * 0.88
            draw.line([(cx-band_l, band_y), (cx+band_l, band_y)], fill=bc+(100,), width=2)
        elif shape == 'diamond':
            draw.line([(cx, cy-r), (cx+r*0.65, cy+r*0.5)], fill=bc+(60,), width=1)
            draw.line([(cx, cy-r), (cx-r*0.65, cy+r*0.5)], fill=bc+(60,), width=1)
        elif shape == 'octagon':
            cr = int(r * 0.3)
            draw.ellipse([cx-cr, cy-cr, cx+cr, cy+cr], outline=bc+(80,), width=1)
        elif shape == 'hexagon':
            for vx, vy in poly:
                draw.ellipse([vx-2, vy-2, vx+2, vy+2], fill=bc+(180,))
        mask = Image.new("L", (size,size), 0)
        mdraw = ImageDraw.Draw(mask)
        mdraw.polygon(poly, fill=255)

    # Apply SVG icon through mask
    if svg_name in svg_index:
        svg_img = render_svg_pixel(svg_index[svg_name], size, icon_color)
        if svg_img:
            blank = Image.new("RGBA", (size,size), (0,0,0,0))
            clipped = Image.composite(svg_img, blank, mask)
            img = Image.alpha_composite(img, clipped)

    return img

def main():
    print(f"SVGs index: ", end="")
    svg_index = build_svg_index()
    print(f"{len(svg_index)} found")
    stat_icons = []
    for idx, (name, svg_name, icon_color, bg_tint) in enumerate(STATS):
        shape = STAT_SHAPES[idx]
        icon = make_icon(svg_name, icon_color, bg_tint, shape, ICON_SIZE)
        if icon:
            stat_icons.append(icon)
            print(f"  {idx:2d} {name:25s} {shape:15s} OK")
        else:
            stat_icons.append(Image.new("RGBA", (ICON_SIZE,ICON_SIZE), (0,0,0,0)))
            print(f"  {idx:2d} {name:25s} FAIL")
    COLS, ROWS = 8, 3
    atlas = Image.new("RGBA", (COLS * ICON_SIZE, ROWS * ICON_SIZE), (0,0,0,0))
    grid = [(r, c) for r in range(ROWS) for c in range(COLS)]
    for idx, icon in enumerate(stat_icons):
        row, col = grid[idx]
        atlas.paste(icon, (col * ICON_SIZE, row * ICON_SIZE), icon)
    atlas.save(os.path.join(OUT_DIR, "stat_icons.png"))
    print(f"\nAtlas: {COLS*ICON_SIZE}x{ROWS*ICON_SIZE} (grid {COLS}x{ROWS}) -> stat_icons.png")

if __name__ == "__main__":
    main()
