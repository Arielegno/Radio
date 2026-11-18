# Crea un MOBI pequeño (compresión PalmDOC) para probar el lector de Hilo.
import struct, sys

def palmdoc(data: bytes) -> bytes:
    out = bytearray(); i = 0; n = len(data)
    while i < n:
        # buscar repetición
        best_len = 0; best_dist = 0
        for dist in range(1, min(i, 2047) + 1):
            l = 0
            while l < 10 and i + l < n and data[i + l - dist] == data[i + l]: l += 1
            if l > best_len: best_len, best_dist = l, dist
        if best_len >= 3:
            m = 0x8000 | (best_dist << 3) | (best_len - 3)
            out += struct.pack('>H', m); i += best_len; continue
        c = data[i]
        if c == 0x20 and i + 1 < n and 0x40 <= data[i + 1] < 0x80:
            out.append(data[i + 1] ^ 0x80); i += 2; continue
        if c == 0 or 0x09 <= c < 0x80:
            out.append(c); i += 1; continue
        # literales "raros": hasta 8 seguidos con su cantidad delante
        j = i; chunk = bytearray()
        while j < n and len(chunk) < 8 and not (data[j] == 0 or 0x09 <= data[j] < 0x80):
            chunk.append(data[j]); j += 1
        out.append(len(chunk)); out += chunk; i = j
    return bytes(out)

html = open(sys.argv[1], encoding='utf-8').read().encode('utf-8')
title = "Libro MOBI de prueba".encode('utf-8')
author = "Émile Durkheim".encode('utf-8')
REC = 4096
recs = [palmdoc(html[k:k + REC]) for k in range(0, len(html), REC)]
exth_recs = [(100, author), (503, title)]
exth = b''.join(struct.pack('>II', t, len(d) + 8) + d for t, d in exth_recs)
exth = b'EXTH' + struct.pack('>II', 12 + len(exth), len(exth_recs)) + exth
exth += b'\0' * ((4 - len(exth) % 4) % 4)
mobi_len = 0xE8
mobi = bytearray(mobi_len)
mobi[0:4] = b'MOBI'
struct.pack_into('>I', mobi, 4, mobi_len)
struct.pack_into('>I', mobi, 8, 2)        # tipo libro
struct.pack_into('>I', mobi, 12, 65001)   # UTF-8
name_off = 16 + mobi_len + len(exth)
struct.pack_into('>I', mobi, 0x54 - 16, name_off)
struct.pack_into('>I', mobi, 0x58 - 16, len(title))
struct.pack_into('>I', mobi, 0x6C - 16, 0xFFFFFFFF)
struct.pack_into('>I', mobi, 0x80 - 16, 0x40)  # tiene EXTH
struct.pack_into('>H', mobi, 0xF2 - 16, 0)     # sin datos de relleno
rec0 = struct.pack('>HHIHHHH', 2, 0, len(html), len(recs), REC, 0, 0) + bytes(mobi) + exth + title + b'\0\0'
records = [rec0] + recs
n = len(records)
hdr = bytearray(78)
hdr[0:32] = b'Libro_MOBI_de_prueba'.ljust(32, b'\0')
hdr[60:68] = b'BOOKMOBI'
struct.pack_into('>H', hdr, 76, n)
off = 78 + n * 8 + 2
table = b''
for i, r in enumerate(records):
    table += struct.pack('>II', off, i * 2); off += len(r)
open(sys.argv[2], 'wb').write(bytes(hdr) + table + b'\0\0' + b''.join(records))
print("MOBI:", n - 1, "registros de texto,", len(html), "bytes de HTML")
