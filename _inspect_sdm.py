import struct

def read_nbt_tag(data, offset):
    """Read a single NBT tag and return (value, new_offset)"""
    tag_type = data[offset]
    offset += 1
    if tag_type == 0:  # TAG_End
        return ('TAG_End', offset)
    
    name_len = struct.unpack_from('>H', data, offset)[0]
    offset += 2
    name = data[offset:offset+name_len].decode('utf-8')
    offset += name_len
    
    if tag_type == 1:  # TAG_Byte
        val = data[offset]
        return (('Byte', name, val), offset + 1)
    elif tag_type == 2:  # TAG_Short
        val = struct.unpack_from('>h', data, offset)[0]
        return (('Short', name, val), offset + 2)
    elif tag_type == 3:  # TAG_Int
        val = struct.unpack_from('>i', data, offset)[0]
        return (('Int', name, val), offset + 4)
    elif tag_type == 4:  # TAG_Long
        val = struct.unpack_from('>q', data, offset)[0]
        return (('Long', name, val), offset + 8)
    elif tag_type == 5:  # TAG_Float
        val = struct.unpack_from('>f', data, offset)[0]
        return (('Float', name, val), offset + 4)
    elif tag_type == 6:  # TAG_Double
        val = struct.unpack_from('>d', data, offset)[0]
        return (('Double', name, val), offset + 8)
    elif tag_type == 7:  # TAG_Byte_Array
        length = struct.unpack_from('>i', data, offset)[0]
        offset += 4
        val = data[offset:offset+length]
        return (('ByteArray', name, val), offset + length)
    elif tag_type == 8:  # TAG_String
        strlen = struct.unpack_from('>H', data, offset)[0]
        offset += 2
        val = data[offset:offset+strlen].decode('utf-8')
        return (('String', name, val), offset + strlen)
    elif tag_type == 9:  # TAG_List
        elem_type = data[offset]
        offset += 1
        length = struct.unpack_from('>i', data, offset)[0]
        offset += 4
        items = []
        for _ in range(length):
            item, offset = read_nbt_tag_in_list(data, offset, elem_type)
            items.append(item)
        return (('List', name, elem_type, items), offset)
    elif tag_type == 10:  # TAG_Compound
        entries = {}
        while offset < len(data):
            if data[offset] == 0:  # TAG_End
                offset += 1
                break
            entry, offset = read_nbt_tag(data, offset)
            entries[entry[1]] = entry
        return (('Compound', name, entries), offset)
    elif tag_type == 11:  # TAG_Int_Array
        length = struct.unpack_from('>i', data, offset)[0]
        offset += 4
        vals = list(struct.unpack_from(f'>{length}i', data, offset))
        return (('IntArray', name, vals), offset + length * 4)
    elif tag_type == 12:  # TAG_Long_Array
        length = struct.unpack_from('>i', data, offset)[0]
        offset += 4
        vals = list(struct.unpack_from(f'>{length}q', data, offset))
        return (('LongArray', name, vals), offset + length * 8)
    else:
        raise ValueError(f"Unknown tag type {tag_type} at offset {offset-1}")

def read_nbt_tag_in_list(data, offset, elem_type):
    """Read a tag inside a list (no name)"""
    saved_type = data[offset]
    offset += 1
    if saved_type == 0:
        return ('TAG_End', offset)
    # In a list, skip the type byte and read value directly
    
    if elem_type == 1:  # Byte
        return (data[offset], offset + 1)
    elif elem_type == 2:  # Short
        return (struct.unpack_from('>h', data, offset)[0], offset + 2)
    elif elem_type == 3:  # Int
        return (struct.unpack_from('>i', data, offset)[0], offset + 4)
    elif elem_type == 8:  # String
        strlen = struct.unpack_from('>H', data, offset)[0]
        offset += 2
        return (data[offset:offset+strlen].decode('utf-8'), offset + strlen)
    elif elem_type == 10:  # Compound
        entries = {}
        while True:
            if data[offset] == 0:
                offset += 1
                break
            entry, offset = read_nbt_tag(data, offset)
            entries[entry[1]] = entry
        return (entries, offset)
    else:
        # For list items, we need to handle the type byte but then the tag has no name
        raise ValueError(f"List elem type {elem_type} not implemented")

def print_tag(tag, indent=0):
    prefix = '  ' * indent
    ttype = tag[0]
    name = tag[1]
    if ttype == 'Compound':
        print(f'{prefix}Compound({name}):')
        for key, entry in tag[2].items():
            print_tag(entry, indent + 1)
    elif ttype == 'List':
        print(f'{prefix}List({name}) [{len(tag[3])} items, elem_type={tag[2]}]:')
        for i, item in enumerate(tag[3]):
            if isinstance(item, dict):
                print(f'{prefix}  [{i}]:')
                for key, entry in item.items():
                    print_tag(entry, indent + 2)
            else:
                print(f'{prefix}  [{i}]: {item}')
    elif ttype == 'String':
        val = tag[2]
        if len(str(val)) > 80:
            val = str(val)[:80] + '...'
        print(f'{prefix}{name}: "{val}"')
    elif ttype == 'Int':
        print(f'{prefix}{name}: {tag[2]}')
    elif ttype == 'Byte':
        print(f'{prefix}{name}: {tag[2]}')
    elif ttype == 'Long':
        print(f'{prefix}{name}: {tag[2]}')
    elif ttype == 'Double':
        print(f'{prefix}{name}: {tag[2]}')
    elif ttype == 'ByteArray':
        print(f'{prefix}{name}: [{len(tag[2])} bytes]')
    elif ttype == 'IntArray':
        print(f'{prefix}{name}: [{len(tag[2])} ints]')
    else:
        print(f'{prefix}{name}: ({ttype})')

def inspect(path, label):
    with open(path, 'rb') as f:
        data = f.read()
    print(f'\n=== {label} ({len(data)} bytes) ===')
    root, _ = read_nbt_tag(data, 0)
    print_tag(root)

inspect(r'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test\saves\New World (5)\SDMShopData\SDMTovarTab.sdm', 'TABS')
inspect(r'C:\Users\El Hadji\AppData\Roaming\.minecraft\versions\test\saves\New World (5)\SDMShopData\SDMTovarList.sdm', 'ITEMS')
