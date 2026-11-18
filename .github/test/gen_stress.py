# Genera documentos enormes y miles de tarjetas para la prueba de estrés de Hilo.
import json, os, random, sys
out = sys.argv[1]
os.makedirs(out, exist_ok=True)
r = random.Random(7)
core = "sociedad capitalismo ideología clase estado poder norma institución cultura trabajo anomia habitus burocracia acción racionalidad dominación".split()
fill = "la de el que en y los se del las un por con una su para es al lo como más".split()
vocab = ["concepto%x" % (i * 7919 % 99991) for i in range(5000)]
authors = "Marx Weber Durkheim Bourdieu Foucault Giddens Habermas Simmel".split()

def sentence():
    n = 8 + r.randrange(25)
    w = [r.choice(core) if r.random() < .25 else r.choice(vocab) if r.random() < .3 else r.choice(fill) for _ in range(n)]
    if r.random() < .15: w.insert(0, "Según %s," % r.choice(authors))
    if r.random() < .1: w.append("en %d" % (1700 + r.randrange(320)))
    s = " ".join(w)
    return s[0].upper() + s[1:] + "."

# Libro de ~300.000 palabras
with open(f"{out}/libro_grande.txt", "w") as f:
    for ch in range(1, 31):
        f.write(f"Capítulo {ch}. Tema {ch}\n\n")
        for _ in range(180):
            f.write(" ".join(sentence() for _ in range(3)) + "\n\n")

# Código de 3000 artículos
art = 1
with open(f"{out}/ley_grande.txt", "w") as f:
    for lib, name in enumerate(["PRIMERO", "SEGUNDO", "TERCERO", "CUARTO"], 1):
        f.write(f"LIBRO {name}\n\nDISPOSICIONES DEL LIBRO {lib}\n\n")
        for tit in range(1, 6):
            f.write(f"TÍTULO {tit}\n\nDE LA MATERIA {tit}\n\n")
            for cap in range(1, 4):
                f.write(f"CAPÍTULO {cap}\n\nNormas del capítulo {cap}\n\n")
                for _ in range(50):
                    refs = [str(1 + r.randrange(3000)) for _ in range(r.randrange(4))]
                    dl = " El plazo será dentro de %s días hábiles." % r.choice(["tres", "cinco", "diez", "treinta (30)"]) if r.random() < .2 else ""
                    rf = (" Conforme a los artículos " + ", ".join(refs) + ".") if refs else ""
                    f.write(f"ARTÍCULO {art}.- Título {art}. {sentence()}{rf}{dl}\n\n")
                    art += 1

# PDF de 400 páginas
try:
    from fpdf import FPDF
    pdf = FPDF()
    pdf.set_auto_page_break(True, 15)
    pdf.set_font("Helvetica", size=11)
    for p in range(400):
        pdf.add_page()
        pdf.multi_cell(0, 6, f"Página {p + 1}. " + " ".join(sentence() for _ in range(22)))
    pdf.output(f"{out}/pdf_grande.pdf")
except Exception as e:
    print("Sin PDF grande:", e)

# 5000 tarjetas (1500 para hoy) y 800 resaltados
import datetime
today = (datetime.date.today() - datetime.date(1970, 1, 1)).days
cards = []
for i in range(5000):
    due = today - r.randrange(10) if i < 1500 else today + 1 + r.randrange(200)
    cards.append({"id": 1000 + i, "book": r.choice(["s1", "s2"]), "kind": r.choice(["qa", "cloze", "vocab"]),
                  "front": f"Pregunta {i}: ¿qué es el concepto {r.choice(core)}?", "back": sentence(), "ctx": sentence(),
                  "lang": "", "src": f"párrafo {i % 5000}", "srcPos": i % 5000, "srcFlow": True, "ease": 2.5,
                  "iv": r.randrange(30), "reps": r.randrange(5), "lapses": 0, "due": due, "created": i})
marks = [{"id": 90000 + i, "book": "s1", "para": i * 6, "start": 0, "end": 20, "text": sentence(), "color": -256, "time": i} for i in range(800)]
with open(f"{out}/estudio.json", "w") as f:
    json.dump({"cards": cards, "marks": marks, "notes": {"s1": "Apuntes " * 2000}, "exams": {"s1": today + 9}}, f, ensure_ascii=False)
print("listo")
