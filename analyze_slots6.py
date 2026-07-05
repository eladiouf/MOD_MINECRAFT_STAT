from PIL import Image
import numpy as np

def analyze_detail(img_path, name):
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    
    print(f"\n=== {name} ({w}x{h}) scale=({scale_x:.4f},{scale_y:.4f}) ===")
    
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    # Lower threshold - look for avg > 30 (slightly brighter than very dark bg ~20)
    print(f"\n18x18 regions with avg brightness > 30 (potential slot areas):")
    candidates = []
    for gy in range(0, 166 - 18, 1):
        for gx in range(0, 176 - 18, 1):
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            avg = np.mean(region)
            if avg > 30:
                candidates.append((gx, gy, avg))
    
    filtered = []
    if candidates:
        candidates.sort(key=lambda r: -r[2])
        for gx, gy, avg in candidates:
            if not any(abs(gx - fx) < 12 and abs(gy - fy) < 12 for fx, fy, _ in filtered):
                filtered.append((gx, gy, avg))
        
        print(f"Top 40 (dedup'd, from {len(candidates)} total):")
        for gx, gy, avg in filtered[:40]:
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            std = np.std(region)
            border = np.concatenate([region[:3,:].flatten(), region[-3:,:].flatten(), region[3:-3,:3].flatten(), region[3:-3,-3:].flatten()])
            center = region[3:-3, 3:-3].flatten()
            delta = np.mean(border) - np.mean(center)
            print(f"  ({gx:3d},{gy:3d}) src=({sx:4d},{sy:4d}) avg={avg:.1f} std={std:.1f} border-center={delta:.1f}")
    else:
        print("  No candidates")
    
    # Special check: examine the area around current slot positions in detail
    print(f"\n\n=== Detailed pixel scan around current slot positions ===")
    for name, gx, gy in [("input0", 30, 36), ("input1", 66, 36), ("output", 124, 36)]:
        sx = int(gx / scale_x)
        sy = int(gy / scale_y)
        print(f"\n  {name} at gui({gx},{gy}) src({sx},{sy}):")
        # Print a 5x5 grid of average brightness in sub-regions
        for row in range(8):
            row_vals = []
            for col in range(8):
                rx = sx + col * (sw // 8)
                ry = sy + row * (sh // 8)
                sub = gray[ry:ry+(sh//8), rx:rx+(sw//8)]
                row_vals.append(f"{np.mean(sub):.0f}")
            print(f"    row {row}: {' '.join(row_vals)}")
    
    return filtered[:20]

analyze_detail(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
analyze_detail(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
