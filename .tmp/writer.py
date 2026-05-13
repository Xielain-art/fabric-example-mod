import base64, os

BASE = r'C:\\Users\\user\\test\\gerbarium-regions-bridge'

def write(rel, b64):
    p = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    c = base64.b64decode(b64).decode('utf-8')
    c = c.replace(chr(13) + chr(10), chr(10))
    with open(p, 'w', encoding='utf-8', newline='') as f:
        f.write(c)
    print(f'Wrote: {rel} ({len(c)} bytes)')

if __name__ == '__main__':
    import sys
    rel = sys.argv[1]
    b64 = sys.stdin.read().strip()
    write(rel, b64)
