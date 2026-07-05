from PIL import Image
import numpy as np

img = Image.open(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png")
w, h = img.size
print(f"Infusion Forge: {w}x{h}")

scale_x = 176.0 / w
scale_y = 166.0 / h

arr = np.array(img)
gray = np.mean(arr, axis=2)
slot_w_src = int(18 / scale_x)
slot_h_src = int(18 / scale_y)
print(f"Slot in source: ~{slot_w_src}x{slot_h_src}")

print("\nScale key positions:")
for y in [6, 36, 84, 142]:
    print(f"  scaled y={y} -> src y={int(y/scale_y)}")
for x in [8, 30, 66, 124]:
    print(f"  scaled x={x} -> src x={int(x/scale_x)}")

print("\n--- Slot detection: sampling grid ---")
found = []
for y_16 in range(0, 166, 18):
    src_y = int(y_16 / scale_y)
    for x_16 in range(0, 176, 18):
        src_x = int(x_16 / scale_x)
        if src_x + slot_w_src <= w and src_y + slot_h_src <= h:
            region = gray[src_y:src_y+slot_h_src, src_x:src_x+slot_w_src]
            avg = np.mean(region)
            if avg < 90:
                found.append((x_16, y_16, avg))
                print(f"  Slot at ({x_16},{y_16}) avg={avg:.1f}")

print(f"\nFound {len(found)} potential slot positions")
for x, y, a in found:
    print(f"  addSlot(container, N, {x}, {y})")

# Now do the same for enchantment anvil
img2 = Image.open(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png")
w2, h2 = img2.size
print(f"\n\nEnchantment Anvil: {w2}x{h2}")

scale_x2 = 176.0 / w2
scale_y2 = 166.0 / h2

arr2 = np.array(img2)
gray2 = np.mean(arr2, axis=2)
slot_w_src2 = int(18 / scale_x2)
slot_h_src2 = int(18 / scale_y2)
print(f"Slot in source: ~{slot_w_src2}x{slot_h_src2}")

found2 = []
for y_16 in range(0, 166, 18):
    src_y = int(y_16 / scale_y2)
    for x_16 in range(0, 176, 18):
        src_x = int(x_16 / scale_x2)
        if src_x + slot_w_src2 <= w2 and src_y + slot_h_src2 <= h2:
            region = gray2[src_y:src_y+slot_h_src2, src_x:src_x+slot_w_src2]
            avg = np.mean(region)
            if avg < 90:
                found2.append((x_16, y_16, avg))
                print(f"  Slot at ({x_16},{y_16}) avg={avg:.1f}")

print(f"\nFound {len(found2)} potential slot positions")
for x, y, a in found2:
    print(f"  addSlot(container, N, {x}, {y})")

# Let's also look at the pixel colors at the default positions to understand what's there
print("\n\n--- Analysis of CURRENT slot positions ---")
positions = [(30,36), (66,36), (124,36)]
for sx, sy in positions:
    src_x = int(sx / scale_x)
    src_y = int(sy / scale_y)
    region = gray[src_y:src_y+slot_h_src, src_x:src_x+slot_w_src]
    avg = np.mean(region)
    print(f"  Current slot ({sx},{sy}) -> src ({src_x},{src_y}), avg_brightness={avg:.1f}")
