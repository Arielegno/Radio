#!/bin/bash
# Descarga las letras de la Revista (Google Fonts, licencia SIL Open Font License) a la carpeta indicada.
set -u
d=${1:-composeApp/src/wasmJsMain/resources/fonts}; mkdir -p "$d"
B=https://github.com/google/fonts/raw/main/ofl
bajar() { curl -fsSL --retry 3 "$B/$1" -o "$d/$2" && echo "ok $2" || echo "FALTA $2"; }
bajar barlow/Barlow-Regular.ttf Barlow-Regular.ttf
bajar barlow/Barlow-SemiBold.ttf Barlow-SemiBold.ttf
bajar barlow/Barlow-Bold.ttf Barlow-Bold.ttf
bajar barlowcondensed/BarlowCondensed-SemiBold.ttf BarlowCondensed-SemiBold.ttf
bajar barlowcondensed/BarlowCondensed-Bold.ttf BarlowCondensed-Bold.ttf
bajar barlowcondensed/BarlowCondensed-ExtraBold.ttf BarlowCondensed-ExtraBold.ttf
bajar 'playfairdisplay/PlayfairDisplay%5Bwght%5D.ttf' PlayfairDisplay.ttf
bajar 'lora/Lora%5Bwght%5D.ttf' Lora.ttf
bajar spacemono/SpaceMono-Regular.ttf SpaceMono-Regular.ttf
bajar spacemono/SpaceMono-Bold.ttf SpaceMono-Bold.ttf
bajar 'caveat/Caveat%5Bwght%5D.ttf' Caveat.ttf
bajar abrilfatface/AbrilFatface-Regular.ttf AbrilFatface-Regular.ttf
bajar anton/Anton-Regular.ttf Anton-Regular.ttf
bajar shrikhand/Shrikhand-Regular.ttf Shrikhand-Regular.ttf
bajar rampartone/RampartOne-Regular.ttf RampartOne-Regular.ttf
bajar rubikglitch/RubikGlitch-Regular.ttf RubikGlitch-Regular.ttf
bajar archivoblack/ArchivoBlack-Regular.ttf ArchivoBlack-Regular.ttf
bajar notosanssymbols2/NotoSansSymbols2-Regular.ttf NotoSansSymbols2-Regular.ttf
ls "$d" | wc -l
