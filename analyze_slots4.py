from PIL import Image
import numpy as np

def find_bright_structures(img_path, name):
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    
    print(f"\n=== {name} ({w}x{h}) scale=({scale_x:.4f},{scale_y:.4f}) ===")
    
    # Find the brightest 5% of pixels in the image
    threshold = np.percentile(gray, 95)
    bright_mask = gray > threshold
    
    # Find connected bright regions and their bounding boxes
    from scipy import ndimage
    labeled, num_features = ndimage.label(bright_mask)
    
    print(f"\nBright pixel threshold (95th percentile): {threshold:.1f}")
    print(f"Number of bright regions: {num_features}")
    
    # Get bounding boxes of bright regions
    regions = []
    for i in range(1, num_features + 1):
        ys, xs = np.where(labeled == i)
        if len(ys) > 10:  # Filter tiny speckles
            min_y, max_y = ys.min(), ys.max()
            min_x, max_x = xs.min(), xs.max()
            # Convert to GUI coordinates
            gui_min_x = min_x * scale_x
            gui_min_y = min_y * scale_y
            gui_max_x = max_x * scale_x
            gui_max_y = max_y * scale_y
            area = (max_x - min_x) * (max_y - min_y)
            regions.append({
                'src_bbox': (min_x, min_y, max_x, max_y),
                'gui_bbox': (gui_min_x, gui_min_y, gui_max_x, gui_max_y),
                'area': area,
                'pixels': len(ys)
            })
    
    # Sort by area, descending
    regions.sort(key=lambda r: -r['area'])
    
    print(f"\nTop 30 bright regions (by pixel count):")
    for i, r in enumerate(regions[:30]):
        sx1, sy1, sx2, sy2 = r['src_bbox']
        gx1, gy1, gx2, gy2 = r['gui_bbox']
        print(f"  #{i+1}: src=({sx1},{sy1})-({sx2},{sy2}) gui=({gx1:.0f},{gy1:.0f})-({gx2:.0f},{gy2:.0f}) pixels={r['pixels']}")
    
    # Now specifically look for 18x18 gui-sized slot regions
    # by finding 18x18 scaled areas with high brightness
    print(f"\n\n=== Sliding window: 18x18 scaled regions with high brightness ===")
    sw_src = int(18 / scale_x)  # slot width in source pixels
    sh_src = int(18 / scale_y)
    
    slot_candidates = []
    for gy in range(0, 166 - 18, 2):
        for gx in range(0, 176 - 18, 2):
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh_src, sx:sx+sw_src]
            avg = np.mean(region)
            if avg > 80:  # Bright region
                slot_candidates.append((gx, gy, avg))
    
    slot_candidates.sort(key=lambda r: -r[2])
    # Group nearby candidates
    filtered = []
    for gx, gy, avg in slot_candidates:
        if not any(abs(gx - fx) < 10 and abs(gy - fy) < 10 for fx, fy, _ in filtered):
            filtered.append((gx, gy, avg))
    
    print(f"\nTop 30 bright 18x18 regions in GUI space:")
    for gx, gy, avg in filtered[:30]:
        sx = int(gx / scale_x)
        sy = int(gy / scale_y)
        print(f"  ({gx:3d},{gy:3d}) -> src ({sx:4d},{sy:4d}) avg_brightness={avg:.1f}")

try:
    from scipy import ndimage
except ImportError:
    print("scipy not installed, skipping connected components analysis")
    # Fallback: just do the sliding window
    pass

find_bright_structures(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
find_bright_structures(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
