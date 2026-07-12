import struct

def extract_strings(path, label):
    with open(path, 'rb') as f:
        data = f.read()
    
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
    for s in strings:
        if any(kw in s.lower() for kw in ['fdp', 'cfa', 'sdmcoin', 'name', 'symbol', 'default']):
            print(f'  "{s}"')

extract_strings(r'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test\config\SDMEconomy\currencies\currencies.data', 'CONFIG CURRENCIES')
