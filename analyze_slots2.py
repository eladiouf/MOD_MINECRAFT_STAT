from PIL import Image
import numpy as np

def find_slots(img_path, name):
    img = Image.open(img_path)
    w, h = img.size
    print(f"\n=== {name} ({w}x{h}) ===")

    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)

    # Slot in source pixels (roughly 18x18 at GUI scale)
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    print(f"Slot src size: ~{sw}x{sh}")

    # Look for slot-cage pattern: bright border ring around a darker center
    # Check a 18x18 scaled region and see if border pixels are brighter than center
    results = []
    for y_16 in range(8, 152, 2):
        for x_16 in range(8, 168, 2):
            sx = int(x_16 / scale_x)
            sy = int(y_16 / scale_y)
            if sx + sw >= w or sy + sh >= h:
                continue

            region = gray[sy:sy+sh, sx:sx+sw]

            # Border: top row, bottom row, left col, right col (2px thick)
            border = np.concatenate([
                region[:2, :].flatten(),      # top
                region[-2:, :].flatten(),     # bottom
                region[2:-2, :2].flatten(),   # left edge (middle)
                region[2:-2, -2:].flatten(),  # right edge (middle)
            ])
            center = region[2:-2, 2:-2].flatten()

            border_avg = np.mean(border)
            center_avg = np.mean(center)
            diff = border_avg - center_avg

            # A slot cage has border significantly brighter than center
            # And border should be reasonably bright (>80)
            if diff > 15 and border_avg > 60:
                results.append((x_16, y_16, diff, border_avg, center_avg))

    # Sort by border-center difference, descending
    results.sort(key=lambda r: -r[2])

    print(f"\nTop 30 slot-cage candidates (ranked by border vs center brightness):")
    for x, y, diff, ba, ca in results[:30]:
        print(f"  ({x:3d},{y:3d}) -> src ({int(x/scale_x):4d},{int(y/scale_y):4d})  border={ba:.1f} center={ca:.1f} delta={diff:.1f}")

    # Also check current positions
    print(f"\nCurrent slot positions analysis:")
    for sx, sy in [(30,36), (66,36), (124,36)]:
        src_x = int(sx / scale_x)
        src_y = int(sy / scale_y)
        if src_x + sw < w and src_y + sh < h:
            region = gray[src_y:src_y+sh, src_x:src_x+sw]
            border = np.concatenate([
                region[:2, :].flatten(),
                region[-2:, :].flatten(),
                region[2:-2, :2].flatten(),
                region[2:-2, -2:].flatten(),
            ])
            center = region[2:-2, 2:-2].flatten()
            print(f"  Current slot ({sx},{sy}) -> src ({src_x},{src_y}): border={np.mean(border):.1f} center={np.mean(center):.1f} delta={np.mean(border)-np.mean(center):.1f}")

    return results[:20]

find_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
find_slots(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
