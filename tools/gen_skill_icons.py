"""
STAT Mod Skill Icon Generator — Dark RPG Minecraft-style with varied shapes
Matches stat icon style: Shield (passives), Diamond (actives), Octagon (weapon innates), Hexagon (identities), RoundedSquare (non-combat)
"""
import json, os, re, math
import xml.etree.ElementTree as ET
from svg.path import parse_path
from PIL import Image, ImageDraw

ICON_SIZE = 128
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
GAME_ICONS_DIR = os.path.join(SCRIPT_DIR, "..", "..", "SIHRIYA", "tools", "game-icons", "icons-master")
OUT_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "statmod", "textures", "gui", "skill")

# ─── Shape helpers (same as gen_stat_icons) ───

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
    'shield': shield_poly, 'diamond': diamond_poly,
    'octagon': octagon_poly, 'hexagon': hexagon_poly,
}

# ─── Skill definitions with data-driven shapes ───

SKILLS_DATA = [
    # Passives (shield) - 50 skills
    {"name":"fortitude","svg":"muscle","color":(160,140,120),"tint":(42,36,30),"shape":"shield","cat":"passive"},
    {"name":"endurance","svg":"heart","color":(175,100,100),"tint":(45,25,22),"shape":"shield","cat":"passive"},
    {"name":"battle_ready","svg":"sword","color":(170,165,160),"tint":(42,40,38),"shape":"shield","cat":"passive"},
    {"name":"firm_grip","svg":"hand","color":(165,150,130),"tint":(40,38,34),"shape":"shield","cat":"passive"},
    {"name":"vitality_boost","svg":"health","color":(180,130,110),"tint":(48,32,25),"shape":"shield","cat":"passive"},
    {"name":"iron_belly","svg":"armor","color":(155,150,145),"tint":(38,36,34),"shape":"shield","cat":"passive"},
    {"name":"steady_foot","svg":"boots","color":(150,140,120),"tint":(38,36,30),"shape":"shield","cat":"passive"},
    {"name":"brawler","svg":"fist","color":(170,140,110),"tint":(44,36,28),"shape":"shield","cat":"passive"},
    {"name":"tank","svg":"shield","color":(160,155,160),"tint":(40,38,40),"shape":"shield","cat":"passive"},
    {"name":"bulwark","svg":"wall","color":(140,135,130),"tint":(35,33,32),"shape":"shield","cat":"passive"},
    {"name":"quick_hands","svg":"fast","color":(170,170,165),"tint":(42,42,40),"shape":"shield","cat":"passive"},
    {"name":"dexterous","svg":"agile","color":(165,168,175),"tint":(40,42,44),"shape":"shield","cat":"passive"},
    {"name":"adrenaline","svg":"blood","color":(185,90,80),"tint":(48,20,18),"shape":"shield","cat":"passive"},
    {"name":"last_stand","svg":"standing","color":(175,150,130),"tint":(45,38,32),"shape":"shield","cat":"passive"},
    {"name":"pathfinder","svg":"compass","color":(160,165,150),"tint":(40,42,38),"shape":"shield","cat":"passive"},
    {"name":"scavenger","svg":"loot","color":(170,160,130),"tint":(44,40,32),"shape":"shield","cat":"passive"},
    {"name":"evasion","svg":"dodge","color":(165,170,170),"tint":(42,44,44),"shape":"shield","cat":"passive"},
    {"name":"reflexes","svg":"eye","color":(175,175,180),"tint":(44,44,46),"shape":"shield","cat":"passive"},
    {"name":"mighty_swing","svg":"heavy-slash","color":(180,110,90),"tint":(48,28,22),"shape":"shield","cat":"passive"},
    {"name":"cleave","svg":"slice","color":(175,120,95),"tint":(46,30,22),"shape":"shield","cat":"passive"},
    {"name":"piercing","svg":"spear","color":(170,165,170),"tint":(42,40,42),"shape":"shield","cat":"passive"},
    {"name":"maiming","svg":"wound","color":(185,90,85),"tint":(48,20,18),"shape":"shield","cat":"passive"},
    {"name":"crippling","svg":"broken-bone","color":(175,130,100),"tint":(46,34,24),"shape":"shield","cat":"passive"},
    {"name":"power_stance","svg":"stance","color":(160,155,150),"tint":(40,38,36),"shape":"shield","cat":"passive"},
    {"name":"warden","svg":"guard","color":(155,160,155),"tint":(38,40,38),"shape":"shield","cat":"passive"},
    {"name":"barbaric","svg":"axe","color":(175,120,90),"tint":(46,30,22),"shape":"shield","cat":"passive"},
    {"name":"savage","svg":"claw","color":(180,100,80),"tint":(48,25,18),"shape":"shield","cat":"passive"},
    {"name":"titan_strength","svg":"strong","color":(160,140,120),"tint":(42,36,30),"shape":"shield","cat":"passive"},
    {"name":"lightweight","svg":"feather","color":(175,175,180),"tint":(44,44,46),"shape":"shield","cat":"passive"},
    {"name":"wind_walker","svg":"wind","color":(175,178,190),"tint":(44,44,48),"shape":"shield","cat":"passive"},
    {"name":"stone_skin","svg":"stone","color":(140,135,130),"tint":(35,33,32),"shape":"shield","cat":"passive"},
    {"name":"iron_skin","svg":"metal","color":(150,148,145),"tint":(36,36,34),"shape":"shield","cat":"passive"},
    {"name":"diamond_skin","svg":"diamond","color":(155,160,170),"tint":(38,40,42),"shape":"shield","cat":"passive"},
    {"name":"sharp_edges","svg":"blade","color":(170,168,172),"tint":(42,42,44),"shape":"shield","cat":"passive"},
    {"name":"heavy_hitter","svg":"hammer","color":(165,140,110),"tint":(42,36,28),"shape":"shield","cat":"passive"},
    {"name":"impact","svg":"crash","color":(170,150,120),"tint":(44,38,30),"shape":"shield","cat":"passive"},
    {"name":"shockwave","svg":"wave","color":(165,160,175),"tint":(42,40,44),"shape":"shield","cat":"passive"},
    {"name":"berserker","svg":"rage","color":(190,90,75),"tint":(50,20,16),"shape":"shield","cat":"passive"},
    {"name":"vampiric","svg":"vampire","color":(180,100,110),"tint":(48,25,28),"shape":"shield","cat":"passive"},
    {"name":"leech","svg":"leech","color":(170,130,110),"tint":(44,34,28),"shape":"shield","cat":"passive"},
    {"name":"sanctuary","svg":"holy","color":(175,170,160),"tint":(44,42,40),"shape":"shield","cat":"passive"},
    {"name":"determination","svg":"will","color":(170,165,150),"tint":(44,42,38),"shape":"shield","cat":"passive"},
    {"name":"fortress","svg":"castle","color":(150,145,140),"tint":(38,36,34),"shape":"shield","cat":"passive"},
    {"name":"bastion","svg":"tower","color":(155,148,140),"tint":(38,36,34),"shape":"shield","cat":"passive"},
    {"name":"precision_strikes","svg":"target","color":(185,185,190),"tint":(46,46,48),"shape":"shield","cat":"passive"},
    {"name":"deadly_aim","svg":"crosshair","color":(190,180,170),"tint":(48,46,42),"shape":"shield","cat":"passive"},
    {"name":"flurry","svg":"storm","color":(170,170,175),"tint":(42,42,44),"shape":"shield","cat":"passive"},
    {"name":"rapid_strikes","svg":"lightning","color":(180,175,100),"tint":(46,44,24),"shape":"shield","cat":"passive"},
    {"name":"second_wind","svg":"regen","color":(150,180,150),"tint":(38,46,38),"shape":"shield","cat":"passive"},
    {"name":"unyielding","svg":"fist","color":(170,150,130),"tint":(44,38,32),"shape":"shield","cat":"passive"},

    # Weapon Innates (octagon) - 8
    {"name":"sword_innate","svg":"cutlass","color":(170,170,185),"tint":(42,42,46),"shape":"octagon","cat":"weapon_innate"},
    {"name":"greatsword_innate","svg":"heavy-slash","color":(175,140,110),"tint":(44,36,28),"shape":"octagon","cat":"weapon_innate"},
    {"name":"axe_innate","svg":"axe","color":(175,125,90),"tint":(46,32,22),"shape":"octagon","cat":"weapon_innate"},
    {"name":"dagger_innate","svg":"dagger","color":(175,170,175),"tint":(44,42,44),"shape":"octagon","cat":"weapon_innate"},
    {"name":"bow_innate","svg":"bow","color":(170,165,150),"tint":(42,42,38),"shape":"octagon","cat":"weapon_innate"},
    {"name":"fist_innate","svg":"fist","color":(170,145,110),"tint":(44,36,28),"shape":"octagon","cat":"weapon_innate"},
    {"name":"spear_innate","svg":"spear","color":(165,160,165),"tint":(42,40,42),"shape":"octagon","cat":"weapon_innate"},
    {"name":"shield_innate","svg":"shield","color":(160,155,160),"tint":(40,38,40),"shape":"octagon","cat":"weapon_innate"},

    # Movers (hexagon) - 3
    {"name":"quick_step","svg":"step","color":(170,175,180),"tint":(42,44,46),"shape":"hexagon","cat":"mover"},
    {"name":"shadow_leap","svg":"shadow","color":(130,120,145),"tint":(32,30,38),"shape":"hexagon","cat":"mover"},
    {"name":"wind_dash","svg":"wind","color":(175,178,190),"tint":(44,44,48),"shape":"hexagon","cat":"mover"},

    # Identity (diamond) - 3
    {"name":"warrior_identity","svg":"warrior","color":(175,160,140),"tint":(44,40,36),"shape":"diamond","cat":"identity"},
    {"name":"rogue_identity","svg":"rogue","color":(165,165,175),"tint":(42,42,44),"shape":"diamond","cat":"identity"},
    {"name":"guardian_identity","svg":"guardian","color":(155,160,165),"tint":(38,40,42),"shape":"diamond","cat":"identity"},

    # Guard (rounded_square) - 2
    {"name":"guarding","svg":"shield","color":(160,155,150),"tint":(40,38,36),"shape":"rounded_square","cat":"guard"},
    {"name":"parry","svg":"block","color":(170,165,160),"tint":(42,40,38),"shape":"rounded_square","cat":"guard"},
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
        bg_r = int(r * 0.82); rr = int(r * 0.22)
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

    if svg_name in svg_index:
        svg_img = render_svg_pixel(svg_index[svg_name], size, icon_color)
        if svg_img:
            blank = Image.new("RGBA", (size,size), (0,0,0,0))
            clipped = Image.composite(svg_img, blank, mask)
            img = Image.alpha_composite(img, clipped)
    return img

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    svg_index = build_svg_index()
    print(f"SVGs: {len(svg_index)}")
    count = 0
    by_cat = {}
    for s in SKILLS_DATA:
        by_cat.setdefault(s['cat'], []).append(s)
    for cat, items in by_cat.items():
        print(f"\n── {cat.upper()} ({len(items)}) ──")
        for s in items:
            icon = make_icon(s['svg'], s['color'], s['tint'], s['shape'], ICON_SIZE)
            if icon:
                icon.save(os.path.join(OUT_DIR, f"{s['name']}.png"))
                count += 1
                print(f"  {s['name']:25s} {s['shape']:15s} OK")
            else:
                print(f"  {s['name']:25s} FAIL")
    print(f"\nDone! {count} skill icons -> {OUT_DIR}")

if __name__ == "__main__":
    main()
