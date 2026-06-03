"""
STAT Mod UI Asset Generator — Dark RPG Minecraft-style
HUD icons + UI chrome in dark stone/metallic palette with pixel-art NEAREST.
"""
import json, os, re, math, random, xml.etree.ElementTree as ET
from svg.path import parse_path
from PIL import Image, ImageDraw

ICON_SIZE = 128
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
GAME_ICONS_DIR = os.path.join(SCRIPT_DIR, "..", "..", "SIHRIYA", "tools", "game-icons", "icons-master")
OUT_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "assets", "statmod", "textures", "gui")


def render_svg_pixel(svg_path, target_size, color):
    tree = ET.parse(svg_path); root = tree.getroot()
    vb = root.get('viewBox')
    if vb: sw, sh = [float(x) for x in vb.split()][2:]
    else: sw, sh = float(root.get('width',512)), float(root.get('height',512))
    ns = {'svg':'http://www.w3.org/2000/svg'}
    path_els = root.findall('.//svg:path',ns) or root.findall('.//{http://www.w3.org/2000/svg}path')
    pd = []
    for p in path_els:
        d = p.get('d',''); f = (p.get('fill') or '#fff').lower()
        if d and 'm0 0' not in d[:6].lower() and 'm0,0' not in d[:6].lower():
            if f in ('#fff','#ffffff','white','') or f == 'none': pd.append(d)
    if not pd:
        for p in path_els:
            d = p.get('d','')
            if d and 'm0 0' not in d[:6].lower(): pd.append(d)
    if not pd: return None
    scale = 4; rs = target_size*scale
    img = Image.new("RGBA", (rs,rs), (0,0,0,0)); draw = ImageDraw.Draw(img)
    for d in pd:
        try:
            parsed = parse_path(d)
            pts = [(parsed.point(i/128).real, parsed.point(i/128).imag) for i in range(129)]
            xs, ys = [p[0] for p in pts], [p[1] for p in pts]
            if not xs: continue
            pw, ph = max(xs)-min(xs), max(ys)-min(ys)
            if pw == 0 or ph == 0: continue
            pad = 0.05*max(pw,ph)
            s = min((rs-2)/(pw+2*pad), (rs-2)/(ph+2*pad))
            cx, cy = (max(xs)+min(xs))/2, (max(ys)+min(ys))/2
            scaled = [(int((x-cx)*s+rs/2), int((y-cy)*s+rs/2)) for x,y in pts]
            draw.polygon(scaled, fill=color+(255,))
        except: continue
    return img.resize((target_size, target_size), Image.NEAREST)


def build_svg_index():
    index = {}
    if not os.path.isdir(GAME_ICONS_DIR): return index
    for root, dirs, files in os.walk(GAME_ICONS_DIR):
        for f in files:
            if not f.endswith('.svg'): continue
            index[f[:-4]] = os.path.join(root, f)
    return index


def make_icon(svg_name, icon_color, bg_tint, size=ICON_SIZE, border=2):
    svg_index = build_svg_index()
    cx, cy = size//2, size//2
    icon = Image.new("RGBA", (size,size), (0,0,0,0))
    draw = ImageDraw.Draw(icon)
    bg_r = size//2 - 2
    for i in range(bg_r, 0, -1):
        t = i/bg_r
        r = int(bg_tint[0]*t*0.4); g = int(bg_tint[1]*t*0.4); b = int(bg_tint[2]*t*0.4)
        draw.ellipse([cx-i,cy-i,cx+i,cy+i], fill=(r,g,b,255))
    if svg_name in svg_index:
        svg_img = render_svg_pixel(svg_index[svg_name], size, icon_color)
        if svg_img: icon = Image.alpha_composite(icon, svg_img)
    bc = tuple(min(255, ic+30) for ic in icon_color)
    draw.ellipse([cx-bg_r,cy-bg_r,cx+bg_r,cy+bg_r], outline=bc+(180,), width=border)
    return icon


# ─── Generate ───

def gen_hud():
    print("── HUD ──")
    items = [
        ("hud_heart.png",    "heart",          (200,90,70),   (50,18,12)),
        ("hud_food.png",     "drumstick",      (190,150,100), (48,35,20)),
        ("hud_fatigue.png",  "sleepy",         (130,120,175), (30,28,45)),
        ("hud_thirst.png",   "water-drop",     (100,150,210), (22,38,55)),
    ]
    for fname, svg, ic, bt in items:
        make_icon(svg, ic, bt, 128).save(os.path.join(OUT_DIR, fname))
        print(f"  {fname}")


def gen_xp_bar():
    print("── XP Bar ──")
    make_icon("upgrade", (150,185,130), (35,48,28), 64).save(os.path.join(OUT_DIR, "xp_bar_fill.png"))
    print(f"  xp_bar_fill.png")


def gen_perks():
    print("── Perks ──")
    make_icon("starburst", (130,120,180), (30,28,45), 64, 2).save(os.path.join(OUT_DIR, "perk_node_bg.png"))
    make_icon("checked-shield", (120,185,120), (28,48,28), 64, 2).save(os.path.join(OUT_DIR, "perk_node_unlocked.png"))
    print(f"  perk_node_bg/unlocked")

    # connector: thin dark line
    img = Image.new("RGBA", (64,7), (0,0,0,0))
    draw = ImageDraw.Draw(img)
    for x in range(64):
        a = int(120*(1-abs(x-32)/32))
        draw.point((x,3), fill=(120,100,70,a))
        draw.point((x,2), fill=(120,100,70,a))
        draw.point((x,4), fill=(120,100,70,max(0,a-20)))
    img.save(os.path.join(OUT_DIR, "perk_connector.png"))
    print(f"  perk_connector.png")


def gen_panel():
    """Dark stone-style 9-patch panel."""
    img = Image.new("RGBA", (32,32), (0,0,0,0))
    draw = ImageDraw.Draw(img)
    fill = (35,30,25)
    draw.rectangle([(1,1),(30,30)], fill=fill+(255,))
    draw.rectangle([(0,0),(31,31)], outline=(60,55,45,255), width=1)
    draw.rectangle([(1,1),(30,30)], outline=(50,45,35,180), width=1)
    for cx,cy in [(4,4),(27,4),(4,27),(27,27)]:
        draw.ellipse([cx-2,cy-2,cx+2,cy+2], fill=(60,55,45,120))
    img.save(os.path.join(OUT_DIR, "panel_border.png"))
    print(f"  panel_border.png")


def gen_bg():
    """Dark stone parchment with noise."""
    w,h = 256,256
    img = Image.new("RGBA", (w,h), (0,0,0,0))
    draw = ImageDraw.Draw(img)
    base = (28,25,22)
    draw.rectangle([(0,0),(w-1,h-1)], fill=base+(255,))
    rng = random.Random(42)
    for _ in range(4000):
        x=rng.randint(0,w-1); y=rng.randint(0,h-1)
        v=rng.randint(-8,8)
        img.putpixel((x,y), tuple(max(0,min(255,base[i]+v)) for i in range(3))+(200,))
    for i in range(15):
        a = int(25*(15-i)/15)
        draw.rectangle([(i,i),(w-1-i,h-1-i)], outline=(0,0,0,a), width=1)
    draw.rectangle([(0,0),(w-1,h-1)], outline=(60,55,45,200), width=2)
    img.save(os.path.join(OUT_DIR, "bg_parchment.png"))
    print(f"  bg_parchment.png")


def gen_tabs():
    """Dark stone tabs with subtle active/inactive distinction."""
    print("── Tabs ──")
    tw, th = 48, 24
    for fname, (light, dark) in [
        ("tab_active.png",   ((60,55,45), (40,35,30))),
        ("tab_inactive.png", ((45,40,35), (30,28,24))),
    ]:
        img = Image.new("RGBA", (tw,th), (0,0,0,0))
        draw = ImageDraw.Draw(img)
        r = 4
        for y in range(th):
            for x in range(tw):
                if y<r and x<r and (x-r)**2+(y-r)**2 > r*r: continue
                if y<r and x>tw-1-r and (x-(tw-1-r))**2+(y-r)**2 > r*r: continue
                t = y/th
                c = tuple(int(light[i]+(dark[i]-light[i])*t) for i in range(3))
                img.putpixel((x,y), c+(255,))
        draw.line([(0,th-1),(tw-1,th-1)], fill=(80,70,55,255), width=1)
        draw.rounded_rectangle([(0,0),(tw-1,th-1)], radius=r, outline=(50,45,35,200), width=1)
        img.save(os.path.join(OUT_DIR, fname))
        print(f"  {fname}")


def gen_class_arts():
    print("── Class Arts ──")
    icon = make_icon("spell-book", (185,175,140), (45,42,30), 64)
    icon.save(os.path.join(OUT_DIR, "skill", "class_arts.png"))
    # Legacy
    old_dir = os.path.join(OUT_DIR, "skills", "class_arts")
    os.makedirs(old_dir, exist_ok=True)
    icon.copy().resize((32,32), Image.NEAREST).save(os.path.join(old_dir, "class_arts.png"))
    print(f"  skill/class_arts.png + legacy copy")


def clean_old():
    for f in ["hud_heart.png","hud_food.png","hud_fatigue.png","hud_thirst.png",
              "xp_bar_fill.png","perk_node_bg.png","perk_node_unlocked.png",
              "perk_connector.png","panel_border.png","bg_parchment.png",
              "tab_active.png","tab_inactive.png"]:
        p = os.path.join(OUT_DIR, f)
        if os.path.exists(p): os.remove(p)
    for i in range(23):
        p = os.path.join(OUT_DIR, f"stat_icon_{i}.png")
        if os.path.exists(p): os.remove(p)


def main():
    print("=== STAT Mod UI Asset Generator (Dark RPG) ===")
    svg_index = build_svg_index()
    print(f"SVGs: {len(svg_index)}\n")
    clean_old()
    gen_hud(); gen_xp_bar(); gen_perks(); gen_panel(); gen_bg(); gen_tabs(); gen_class_arts()
    print(f"\nDone! All UI assets -> {OUT_DIR}")

if __name__ == "__main__":
    main()
