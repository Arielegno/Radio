#!/usr/bin/env python3
"""Recorre todas las pantallas de la app y saca capturas, para revisar textos cortados o partidos.
Además anota los textos que Android marca como cortados («…»). Uso: pantallas.py APK SEMILLA SALIDA"""
import os, re, sys, time
import telefono as T
from telefono import log, comprobar, sh, tocar, hay, foto, atras, abrir_app, pestana, deslizar
APK, SEM, OUT = sys.argv[1:4]; os.makedirs(OUT, exist_ok=True); T.OUT = OUT
T.preparar_app(APK, SEM, {"aire": False, "playlists": '{"Viaje al mar":{"canciones":[1,2,3,4,5,6]},"Una playlist con un nombre larguísimo para ver cómo se corta":{"canciones":[7,8]}}'})
T.registro_vivo()
abrir_app()
def recorrer(nombre, bajar=3):
    foto(nombre)
    for i in range(bajar):
        deslizar(); foto(f"{nombre}-{i+2}")
    deslizar(bajar + 2, arriba=False)
# Radio
pestana("Radio"); recorrer("radio", 2)
if T.iniciar_radio():
    time.sleep(4); recorrer("radio-sonando", 2)
    if tocar(r"^Modo carro$", 3): time.sleep(2); foto("modo-carro"); atras()
    sh("input keyevent 127")
# Biblioteca: cada vista y un artista, un álbum y una playlist por dentro
pestana("Biblioteca")
for v in ["Artistas", "Álbumes", "Años", "Géneros", "Playlists", "Canciones"]:
    if not tocar("^" + v + "$", 3):
        sh("input swipe 900 700 200 700 300"); time.sleep(1); tocar("^" + v + "$", 3)
    recorrer("biblioteca-" + v.lower(), 1)
    if v in ("Artistas", "Álbumes", "Playlists"):
        b = [n for n in T.nodos() if n[0] and re.search(r"(Lana|Cage|largu|Melophobia|Thank|Viaje|artista con un)", n[0])]
        if b: T.pulsar(b[0][4]); time.sleep(2); recorrer("dentro-" + v.lower(), 2); atras()
# Resumen
pestana("Resumen"); recorrer("resumen", 3)
# Revista
pestana("Revista"); time.sleep(8); recorrer("revista", 2)
# Ajustes: cada sección
cats = ["Tu música", "Luna, tu locutora", "Inteligencia artificial", "Modos y frases", "Programas y dedicatorias", "Voz", "Sonido",
        "Ubicación y clima", "Apariencia", "Revista", "Copia de ajustes y datos", "Guía de uso", "Acerca de"]
pestana("Ajustes"); recorrer("ajustes", 2)
for c in cats:
    pestana("Ajustes"); time.sleep(1)
    if not hay(r"^Tu música$|^Acerca de$|^Voz$", 2): atras()
    deslizar(4, arriba=False)
    b = T.buscar_bajando("^" + re.escape(c) + "$", 6)
    if not b: log("no encontré " + c); continue
    T.pulsar(b); time.sleep(1.5); recorrer("ajustes-" + re.sub(r"[^a-z]", "", c.lower())[:14], 4); atras()
# textos cortados que reporta Android (terminan en «…»)
c = T.cierres()
comprobar("La app no se cerró al recorrer las pantallas", not c, "; ".join(c[:3]))
T.terminar("RECORRIDO DE PANTALLAS")
