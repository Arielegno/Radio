package com.estudio.musical.music

/** Diccionario local de palabras frecuentes en canciones (para el buscador de rimas). Se amplía con tus propias letras. */
object Palabras {
    val LISTA: List<String> = """
amor dolor calor color temor valor flor señor mejor peor favor rumor clamor sabor honor error interior
corazón canción razón pasión ilusión perdón emoción oración traición canción rincón balcón montón
vida herida querida perdida salida medida escondida partida dormida bendecida
amar besar soñar volar llorar cantar mirar pensar llegar quedar olvidar esperar caminar bailar
sentir vivir morir partir decir seguir pedir mentir subir abrir dormir reír sufrir existir
mar sol luna estrella cielo suelo vuelo hielo consuelo anhelo pañuelo abuelo desvelo
noche coche reproche derroche broche
día alegría poesía melodía fantasía todavía mía tuya compañía agonía energía armonía
tiempo momento viento lamento tormento sentimiento pensamiento juramento cuento aliento intento
camino destino divino vino molino sino peregrino cristalino
fuego juego luego ruego
alma calma palma
beso regreso preso peso exceso proceso verso universo inverso diverso
mirada nada almohada madrugada callada enamorada llamada jugada pasada parada mañana ventana semana hermana campana lejana cercana temprana
gente siempre frente mente presente ausente corriente suerte muerte fuerte verte tenerte quererte perderte
ojos rojos antojos enojos
labios sabios agravios
boca loca toca provoca
piel miel fiel cruel papel cartel
voz veloz atroz feroz
paz capaz audaz
luz cruz
dios adiós
tierra guerra sierra
cama llama drama fama dama
casa pasa brasa
mundo profundo segundo vagabundo
lluvia rubia
sueño dueño pequeño empeño
dulce
lágrima
recuerdo cuerdo pierdo
olvido latido sentido perdido querido vivido dolido herido nido ruido sonido
quiero espero muero primero sincero verdadero entero sendero lucero
nunca jamás atrás demás detrás
contigo testigo amigo abrigo castigo enemigo digo sigo trigo
conmigo
ti mí aquí allí así sí
yo voy soy hoy estoy doy
tú
pueblo cielo río frío
agua
madre padre
hijo
ángel
fe
gloria historia victoria memoria
gracia
esperanza alabanza confianza danza balanza mudanza
santo canto llanto manto encanto tanto cuanto espanto
señal final igual mal sal cristal
bien también quien cien
calle valle
loco poco
nadie
sola ola
solo
triste
feliz raíz nariz
amigos
deseo veo creo paseo
locura ternura dulzura amargura figura segura aventura hermosura altura cintura
tristeza belleza cabeza naturaleza grandeza promesa princesa
cariño niño
rosa cosa hermosa preciosa mariposa
pena buena llena
""".trim().split(Regex("\\s+")).distinct()
}
