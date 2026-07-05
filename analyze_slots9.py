from PIL import Image
import numpy as np

def find_inventory_y(img_path, name):
    """Find where the player inventory grid starts in the scaled image"""
    img = Image.open(img_path)
    w, h = img.size
    scale_x = 176.0 / w
    scale_y = 166.0 / h
    arr = np.array(img)
    gray = np.mean(arr, axis=2)
    sw = int(18 / scale_x)
    sh = int(18 / scale_y)
    
    print(f"\n=== {name}: Finding inventory grid Y ===")
    
    # For each row position, check if there's a 9-column pattern of dark rectangles
    # The inventory has 3 rows of 9 slots + 1 hotbar row of 9 slots
    for gy in range(60, 150):
        count = 0
        for gx in range(0, 176 - 18, 18):
            sx = int(gx / scale_x)
            sy = int(gy / scale_y)
            region = gray[sy:sy+sh, sx:sx+sw]
            avg = np.mean(region)
            if avg < 35:  # Dark = slot background
                count += 1
        if count >= 7:  # At least 7 of 9 slots detected
            print(f"  y={gy}: {count}/9 dark rectangles")

find_inventory_y(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\infusion_forge.png", "INFUSION FORGE")
find_inventory_y(r"C:\Users\El Hadji\Downloads\STAT_MOD\test\MOD_MINECRAFT_STAT\src\main\resources\assets\statmod\textures\gui\enchantment_anvil.png", "ENCHANTMENT ANVIL")
