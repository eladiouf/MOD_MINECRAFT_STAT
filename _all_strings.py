with open(r'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test\config\SDMEconomy\currencies\currencies.data', 'rb') as f:
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

print(f'All strings ({data} bytes):')
for s in strings:
    print(f'  "{s}"')
