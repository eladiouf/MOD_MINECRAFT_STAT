from PIL import Image
import numpy as np

def probe_position(img_path, name, gui_x, gui_y):
    """Examine brightness at a specific gui position and its surroundings"""
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    sw = int(18 / scale_x)  # ~131
    sh = int(18 / scale_y)  # ~132
    
    sx = int(gui_x / scale_x)
    sy = int(gui_y / scale_y)
    
    region = gray[sy:sy+sh, sx:sx+sw]
    
    # 3x3 grid of sub-regions within the 18x18 slot
    print(f"\n{name} ({gui_x},{gui_y}) -> src ({sx},{sy})")
    for row in range(3):
        vals = []
        for col in range(3):
            r = gray[sy + row*sh//3 : sy + (row+1)*sh//3, 
                     sx + col*sw//3 : sx + (col+1)*sw//3]
            vals.append(f"{np.mean(r):.1f}")
        print(f"  row{row}: {' '.join(vals)}")

def slide_and_find_slots(img_path, name, gui_y_range=(30, 50)):
    """Slide horizontally and find the best slot positions at a given y range"""
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    print(f"\n=== {name}: sliding window at y≈38 to find slot centers ===")
    
    # For each x position, compute: does this look like a slot center?
    # A slot center has: dark center, bright-ish border, and the dark center should be ~8px region
    results = []
    for gx in range(0, 176 - 18, 1):
        for gy in range(gui_y_range[0], gui_y_range[1], 1):
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            
            # Quarter the region
            rows = np.array_split(region, 4, axis=0)
            cols = [np.array_split(r, 4, axis=1) for r in rows]
            
            # Center 2x2 quarters should be dark
            center_vals = [np.mean(cols[r][c]) for r in [1,2] for c in [1,2]]
            center_avg = np.mean(center_vals)
            
            # Border quarters (top, bottom, left, right) should be brighter
            border_vals = [
                np.mean(cols[0][1]), np.mean(cols[0][2]),  # top
                np.mean(cols[3][1]), np.mean(cols[3][2]),  # bottom
                np.mean(cols[1][0]), np.mean(cols[2][0]),  # left
                np.mean(cols[1][3]), np.mean(cols[2][3]),  # right
            ]
            border_avg = np.mean(border_vals)
            
            delta = border_avg - center_avg
            if delta > 10 and border_avg > 40:
                results.append((gx, gy, delta, border_avg, center_avg))
    
    results.sort(key=lambda r: -r[2])
    
    # Group into clusters
    clusters = []
    for gx, gy, delta, ba, ca in results:
        if not any(abs(gx - cx) < 10 and abs(gy - cy) < 10 for cx, cy, _, _, _ in clusters):
            clusters.append((gx, gy, delta, ba, ca))
    
    print(f"Found {len(clusters)} slot clusters in y={gui_y_range}")
    for gx, gy, delta, ba, ca in clusters[:15]:
        print(f"  ({gx:3d},{gy:3d}) border={ba:.1f} center={ca:.1f} delta={delta:.1f}")

slide_and_find_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
slide_and_find_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")

# Also look at broader y range
slide_and_find_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE", (20, 80))
slide_and_find_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL", (20, 80))
