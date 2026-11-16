# Busca un elemento en el volcado de uiautomator por texto o descripción y muestra su centro.
import re, sys, xml.etree.ElementTree as ET
path, q = sys.argv[1], sys.argv[2]
partial = len(sys.argv) > 3
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(0)
for n in root.iter('node'):
    for k in ('content-desc', 'text'):
        v = n.get(k) or ''
        if v == q or (partial and q in v):
            m = re.findall(r'\d+', n.get('bounds', ''))
            if len(m) == 4:
                x1, y1, x2, y2 = map(int, m)
                print((x1 + x2) // 2, (y1 + y2) // 2)
                sys.exit(0)
