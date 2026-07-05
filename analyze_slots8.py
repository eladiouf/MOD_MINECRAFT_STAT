from PIL import Image
import numpy as np

def scan_slots(img_path, name, y_range=(0, 166)):
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    print(f"\n=== {name} ===")
    print(f"Slot src: {sw}x{sh}, scale: {scale_x:.4f}x{scale_y:.4f}")
    
    # For each 18x18 window, compute:
    # 1. Overall brightness
    # 2. Border-to-center ratio (measure of cage-like appearance)
    # 3. Standard deviation
    scores = []
    for gy in range(y_range[0], min(y_range[1], 166 - 18)):
        for gx in range(0, 176 - 18):
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            
            avg = float(np.mean(region))
            
            # Border (3px thick around edges)
            top = region[:3, :]
            bottom = region[-3:, :]
            left = region[3:-3, :3]
            right = region[3:-3, -3:]
            border_avg = float(np.mean(np.concatenate([top.flatten(), bottom.flatten(), left.flatten(), right.flatten()])))
            
            # Center (inner area excluding 3px border)
            center = region[3:-3, 3:-3]
            center_avg = float(np.mean(center))
            
            # Score: how much brighter is border than center, weighted by overall
            cage_score = border_avg - center_avg
            
            # A real slot cage: bright border, dark center, moderate overall
            if cage_score > 5 and border_avg > 25:
                scores.append((gx, gy, cage_score, border_avg, center_avg, avg))
    
    # Sort by cage_score descending
    scores.sort(key=lambda r: -r[2])
    
    # Print unique clusters
    clusters = []
    for gx, gy, cs, ba, ca, avg in scores:
        key = (round(gx / 15), round(gy / 15))
        if key not in [c[0] for c in clusters]:
            clusters.append((key, gx, gy, cs, ba, ca, avg))
    
    print(f"\nTop slot-like positions (cage_score = border - center):")
    for key, gx, gy, cs, ba, ca, avg in clusters[:25]:
        sx = int(gx / scale_x)
        sy = int(gy / scale_y)
        print(f"  ({gx:3d},{gy:3d}) src=({sx:4d},{sy:4d}) cage_score={cs:.1f} border={ba:.1f} center={ca:.1f} overall={avg:.1f}")

scan_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
scan_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
