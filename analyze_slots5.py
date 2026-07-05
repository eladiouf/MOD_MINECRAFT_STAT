from PIL import Image
import numpy as np

def analyze(img_path, name):
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    
    print(f"\n=== {name} ({w}x{h}) scale=({scale_x:.4f},{scale_y:.4f}) ===")
    
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    # Scan for 18x18 scaled regions with high avg brightness
    print(f"\n18x18 scaled regions with avg brightness > 60:")
    candidates = []
    for gy in range(0, 166 - 18, 1):
        for gx in range(0, 176 - 18, 1):
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            avg = np.mean(region)
            if avg > 60:
                candidates.append((gx, gy, avg))
    
    # Deduplicate: keep only positions that are local maxima
    filtered = []
    if candidates:
        candidates.sort(key=lambda r: -r[2])
        for gx, gy, avg in candidates:
            if not any(abs(gx - fx) < 12 and abs(gy - fy) < 12 for fx, fy, _ in filtered):
                filtered.append((gx, gy, avg))
        
        print(f"Top 30 (dedup'd, from {len(candidates)} total):")
        for gx, gy, avg in filtered[:30]:
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            std = np.std(region)
            # Also compute border vs center
            border = np.concatenate([region[:3,:].flatten(), region[-3:,:].flatten(), region[3:-3,:3].flatten(), region[3:-3,-3:].flatten()])
            center = region[3:-3, 3:-3].flatten()
            delta = np.mean(border) - np.mean(center)
            print(f"  ({gx:3d},{gy:3d}) src=({sx:4d},{sy:4d}) avg={avg:.1f} std={std:.1f} border-center={delta:.1f}")
    else:
        print("  No candidates found with avg > 60")
    
    return filtered[:10]

infusion_candidates = analyze(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
enchant_candidates = analyze(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
