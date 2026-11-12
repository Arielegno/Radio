#!/usr/bin/env python3
"""Arma canciones de prueba (tonos cortos) y la lista de la biblioteca, para la prueba de estrés."""
import json, os, subprocess, sys, random, time
salida = sys.argv[1]; os.makedirs(os.path.join(salida, "music"), exist_ok=True); os.makedirs(os.path.join(salida, "covers"), exist_ok=True)
random.seed(7)
artistas = ["Cage the Elephant", "Lana Del Rey", "Soda Stereo", "Calle 13", "Lana Del Rey feat. The Weeknd", "Ñandú & Los Acentos Ágiles",
            "Un artista con un nombre larguísimo que no cabe en ninguna pantalla del teléfono", "Greta Van Fleet", "Bad Bunny", "Ýrsa Ölafsdóttir"]
albumes = ["Melophobia", "Melophobia (Deluxe Edition)", "Thank You, Happy Birthday", "Thank You Happy Birthday", "Promo Only: Modern Rock Radio",
           "", "Born to Die", "Canción Animal", "Álbum con nombre muy largo para ver si se corta bien en las tarjetas y en las listas", "Spotify Singles"]
generos = ["Rock", "Alternative Rock", "Pop", "Reggaeton", "", "Indie", "Rock en español"]
pistas = []
for i in range(60):
    n = i + 1
    nombre = random.choice(["Cigarette Daydreams", "Aberdeen", "Video Games", "De Música Ligera", "Atrévete-te-te", "Tema %d" % n,
                            "Canción número %d con un título que sigue y sigue sin terminar nunca jamás" % n, "¿Qué se siente?", "Rubber Ball", "Highway Tune"])
    dur = random.choice([20, 25, 30, 40])
    f = "%d.mp3" % n
    ruta = os.path.join(salida, "music", f)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=%d:duration=%d" % (220 + i * 7, dur), "-ac", "2", "-b:a", "64k", ruta], check=True)
    cover = ""
    if i % 3:
        cover = "%d.jpg" % n
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=0x%06x:s=600x600" % random.randint(0, 0xFFFFFF), "-frames:v", "1", os.path.join(salida, "covers", cover)], check=True)
    pistas.append({"id": n, "name": nombre, "artist": random.choice(artistas), "album": random.choice(albumes), "year": random.choice([0, 2008, 2013, 2017, 2021]),
                   "genre": random.choice(generos), "durMs": dur * 1000, "file": f, "mime": "audio/mpeg", "size": os.path.getsize(ruta), "cover": cover,
                   "addedAt": int(time.time() * 1000) - i * 1000, "manual": [], "revisada": i % 2 == 0})
json.dump(pistas, open(os.path.join(salida, "biblioteca.json"), "w"), ensure_ascii=False)
print("Canciones de prueba:", len(pistas))
