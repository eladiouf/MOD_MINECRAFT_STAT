import re

def extract_strings(path, label):
    with open(path, 'rb') as f:
        data = f.read()
    
    # Extract readable strings (>= 3 chars)
    strings = []
    current = []
    for byte in data:
        if 32 <= byte < 127:
            current.append(chr(byte))
        else:
            if len(current) >= 3:
                strings.append(''.join(current))
            current = []
    if len(current) >= 3:
        strings.append(''.join(current))
    
    print(f'\n=== {label} ({len(data)} bytes) ===')
    print(f'Strings found:')
    for s in strings:
        print(f'  "{s}"')

extract_strings(r'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test\saves\New World (5)\SDMShopData\SDMTovarTab.sdm', 'TABS')
extract_strings(r'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test\saves\New World (5)\SDMShopData\SDMTovarList.sdm', 'ITEMS')
