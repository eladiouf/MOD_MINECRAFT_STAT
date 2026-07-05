from PIL import Image
import numpy as np

def analyze_position(img_path, name, positions):
    """Analyze specific positions to see if they look like slots"""
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    print(f"\n=== {name} ===")
    print(f"Image: {w}x{h}, scale: {scale_x:.4f}x{scale_y:.4f}")
    print(f"Slot src size: {sw}x{sh}")
    
    for sx, sy in positions:
        src_x = int(sx / scale_x)
        src_y = int(sy / scale_y)
        if src_x + sw > w or src_y + sh > h:
            print(f"  ({sx},{sy}) -> src ({src_x},{src_y}) OUT OF BOUNDS")
            continue
        
        region = gray[src_y:src_y+sh, src_x:src_x+sw]
        mean = np.mean(region)
        
        # Sample small areas to find bright spots within the region
        # Check 3x3 pixel blocks at various offsets
        print(f"\n  ({sx:3d},{sy:3d}) -> src ({src_x:4d},{src_y:4d}) overall_avg={mean:.1f}")
        for row_pct in [10, 25, 50, 75, 90]:
            for col_pct in [10, 25, 50, 75, 90]:
                local_y = int(row_pct / 100 * sh)
                local_x = int(col_pct / 100 * sw)
                sub = region[local_y:local_y+5, local_x:local_x+5]
                print(f"    sub({col_pct}%,{row_pct}%) at src({src_x+local_x},{src_y+local_y}) avg={np.mean(sub):.1f}")

def deep_scan_slots(img_path, name):
    """Dense scan looking for any brighter-than-average 18x18 scaled regions"""
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    print(f"\n=== DEEP SCAN: {name} ===")
    
    # Scan for regions with high variance (slot cages have high contrast)
    results = []
    for y_16 in range(0, 166, 1):
        for x_16 in range(0, 176, 1):
            sx = int(x_16 / scale_x)
            sy = int(y_16 / scale_y)
            if sx + sw > w or sy + sh > h:
                continue
            region = gray[sy:sy+sh, sx:sx+sw]
            std = np.std(region)
            mean = np.mean(region)
            # High std means lots of contrast (like a bright cage on dark bg)
            if std > 25:
                results.append((x_16, y_16, std, mean))
    
    results.sort(key=lambda r: -r[2])
    print(f"\nTop 50 high-contrast positions (std > 25):")
    seen = set()
    for x, y, std, mean in results:
        # Deduplicate nearby (within 5px)
        key = (round(x/5), round(y/5))
        if key not in seen:
            seen.add(key)
            print(f"  ({x:3d},{y:3d}) std={std:.1f} mean={mean:.1f}")

    # Also look for the standard 9-slot player inventory grid pattern
    print(f"\n\n=== Looking for player inventory grid pattern ===")
    # Scan for a 3x3 grid of dark rectangles
    for grid_y in range(70, 110, 1):
        for grid_x in range(0, 170, 1):
            # Check if there's a pattern of 9 slots (3x3) at this origin
            slot_gap = 18
            found_slots = 0
            for row in range(3):
                for col in range(9):
                    sx = int((grid_x + col * slot_gap) / scale_x)
                    sy = int((grid_y + row * slot_gap) / scale_y)
                    if sx + sw > w or sy + sh > h:
                        continue
                    r = gray[sy:sy+sh, sx:sx+sw]
                    if np.mean(r) < 40:
                        found_slots += 1
            if found_slots >= 20:  # At least 20 of 27 possible inventory slots
                print(f"  Inventory grid pattern at ({grid_x},{grid_y}) - {found_slots}/27 dark regions")

deep_scan_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
deep_scan_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
