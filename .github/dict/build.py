#!/usr/bin/env python3
"""
Arma los diccionarios de Hilo a partir del Wikcionario (datos extraídos por wiktextract / kaikki.org).
Licencia de los datos: CC BY-SA 4.0 y GFDL (Wikcionario). Uso: build.py es|en salida.db
"""
import gzip, io, json, sqlite3, sys, unicodedata, urllib.request

def fold(s):
    return ''.join(c for c in unicodedata.normalize('NFD', s.lower()) if unicodedata.category(c) != 'Mn')

def opener(urls):
    for u in urls:
        try:
            r = urllib.request.urlopen(urllib.request.Request(u, headers={'User-Agent': 'Hilo-diccionario/1.0'}), timeout=120)
            print('Usando', u, flush=True)
            raw = gzip.GzipFile(fileobj=r) if u.endswith('.gz') else r
            return io.TextIOWrapper(raw, encoding='utf-8', errors='replace')
        except Exception as e:
            print('No disponible', u, e, flush=True)
    return None

def make(path):
    db = sqlite3.connect(path)
    db.executescript('''
      PRAGMA journal_mode=OFF; PRAGMA synchronous=OFF;
      CREATE TABLE w(word TEXT, norm TEXT, pos TEXT, defs TEXT, ipa TEXT, tr TEXT, ety TEXT);
      CREATE TABLE f(form TEXT, lemma TEXT);
      CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT);
    ''')
    return db

POS_ES = {'noun': 'sustantivo', 'verb': 'verbo', 'adj': 'adjetivo', 'adv': 'adverbio', 'name': 'nombre propio', 'pron': 'pronombre',
          'prep': 'preposición', 'conj': 'conjunción', 'intj': 'interjección', 'num': 'numeral', 'det': 'determinante',
          'phrase': 'locución', 'prep_phrase': 'locución', 'article': 'artículo', 'suffix': 'sufijo', 'prefix': 'prefijo', 'abbrev': 'abreviatura'}

def senses_of(e, limit=6):
    out = []
    for s in e.get('senses', []):
        if s.get('form_of') or s.get('alt_of'):
            continue
        g = s.get('glosses') or s.get('raw_glosses') or []
        if not g:
            continue
        text = ' — '.join(x.strip() for x in g if x and x.strip())[:300]
        ex = ''
        for x in s.get('examples', [])[:1]:
            t = (x.get('text') or '').strip()
            if 0 < len(t) <= 220:
                ex = t
        tags = [t for t in s.get('tags', []) if t not in ('form-of',)][:3]
        out.append({'d': text, 'e': ex, 't': tags})
        if len(out) >= limit:
            break
    return out

def forms_of(e):
    for s in e.get('senses', []):
        for f in (s.get('form_of') or []):
            w = f.get('word')
            if w:
                yield w

def ipa_of(e):
    for s in e.get('sounds', []):
        if s.get('ipa'):
            return s['ipa']
    return ''

def build_es(out):
    db = make(out)
    n = 0
    # 1) Definiciones en español, del Wikcionario en español
    src = opener([
        'https://kaikki.org/eswiktionary/raw-wiktextract-data.jsonl.gz',
        'https://kaikki.org/eswiktionary/raw-wiktextract-data.json.gz',
        'https://kaikki.org/dictionary/downloads/es/es-extract.jsonl.gz',
    ])
    if src:
        for line in src:
            try:
                e = json.loads(line)
            except Exception:
                continue
            if e.get('lang_code') != 'es' or not e.get('word'):
                continue
            ss = senses_of(e, 8)
            if not ss:
                continue
            db.execute('INSERT INTO w VALUES(?,?,?,?,?,?,?)', (e['word'], fold(e['word']), POS_ES.get(e.get('pos', ''), e.get('pos', '')),
                       json.dumps(ss, ensure_ascii=False), ipa_of(e), '', (e.get('etymology_text') or '')[:400]))
            n += 1
            if n % 50000 == 0:
                print('es (es):', n, flush=True)
    print('Entradas con definición en español:', n, flush=True)
    # 2) Significado en inglés y formas (conjugaciones, plurales), del Wikcionario en inglés
    src = opener([
        'https://kaikki.org/dictionary/Spanish/kaikki.org-dictionary-Spanish.jsonl',
        'https://kaikki.org/dictionary/Spanish/kaikki.org-dictionary-Spanish.jsonl.gz',
        'https://kaikki.org/dictionary/Spanish/words/kaikki.org-dictionary-Spanish-words.jsonl',
    ])
    db.execute('CREATE TABLE en(word TEXT, pos TEXT, defs TEXT, ipa TEXT)')
    m = 0
    if src:
        for line in src:
            try:
                e = json.loads(line)
            except Exception:
                continue
            w = e.get('word')
            if not w:
                continue
            for lemma in forms_of(e):
                db.execute('INSERT INTO f VALUES(?,?)', (fold(w), lemma))
            ss = senses_of(e, 5)
            if ss:
                db.execute('INSERT INTO en VALUES(?,?,?,?)', (w, POS_ES.get(e.get('pos', ''), e.get('pos', '')), json.dumps(ss, ensure_ascii=False), ipa_of(e)))
                m += 1
    print('Entradas del inglés para el español:', m, flush=True)
    db.execute('CREATE INDEX en_w ON en(word)')
    # Traducción al inglés en cada entrada; si no hay definición en español, la entrada queda con la del inglés
    db.execute('''UPDATE w SET tr = (SELECT group_concat(json_extract(j.value, '$.d'), ' | ') FROM en, json_each(en.defs) j WHERE en.word = w.word)''')
    db.execute('''UPDATE w SET ipa = (SELECT ipa FROM en WHERE en.word = w.word AND en.ipa != '' LIMIT 1) WHERE ipa = '' ''')
    db.execute('''INSERT INTO w SELECT en.word, '', en.pos, '[]', en.ipa, (SELECT group_concat(json_extract(j.value, '$.d'), ' | ') FROM json_each(en.defs) j), ''
                  FROM en WHERE NOT EXISTS (SELECT 1 FROM w WHERE w.word = en.word)''')
    finish(db, 'es')

def build_en(out):
    db = make(out)
    src = opener([
        'https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl',
        'https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl.gz',
    ])
    if not src:
        sys.exit('No se pudo descargar el inglés')
    n = 0
    for line in src:
        try:
            e = json.loads(line)
        except Exception:
            continue
        w = e.get('word')
        if not w or e.get('pos') == 'name':
            continue
        for lemma in forms_of(e):
            db.execute('INSERT INTO f VALUES(?,?)', (fold(w), lemma))
        ss = senses_of(e, 6)
        if not ss:
            continue
        tr = []
        for t in e.get('translations', []):
            if t.get('code') == 'es' and t.get('word') and t['word'] not in tr:
                tr.append(t['word'])
        for s in e.get('senses', []):
            for t in s.get('translations', []) or []:
                if t.get('code') == 'es' and t.get('word') and t['word'] not in tr:
                    tr.append(t['word'])
        db.execute('INSERT INTO w VALUES(?,?,?,?,?,?,?)', (w, fold(w), e.get('pos', ''), json.dumps(ss, ensure_ascii=False), ipa_of(e), ', '.join(tr[:12]), (e.get('etymology_text') or '')[:300]))
        n += 1
        if n % 100000 == 0:
            print('en:', n, flush=True)
    print('Entradas en inglés:', n, flush=True)
    finish(db, 'en')

def finish(db, lang):
    db.execute("UPDATE w SET norm = lower(word) WHERE norm = ''")
    db.execute('DELETE FROM f WHERE rowid NOT IN (SELECT min(rowid) FROM f GROUP BY form, lemma)')
    db.execute('CREATE INDEX w_word ON w(word)')
    db.execute('CREATE INDEX w_norm ON w(norm)')
    db.execute('CREATE INDEX f_form ON f(form)')
    total = db.execute('SELECT count(*) FROM w').fetchone()[0]
    db.execute('INSERT INTO meta VALUES(?,?)', ('lang', lang))
    db.execute('INSERT INTO meta VALUES(?,?)', ('entradas', str(total)))
    db.execute('INSERT INTO meta VALUES(?,?)', ('fuente', 'Wikcionario (CC BY-SA 4.0), datos extraídos con wiktextract (kaikki.org)'))
    try:
        db.execute('DROP TABLE en')
    except Exception:
        pass
    db.commit()
    db.execute('VACUUM')
    db.close()
    print(lang, 'listo:', total, 'entradas', flush=True)

if __name__ == '__main__':
    {'es': build_es, 'en': build_en}[sys.argv[1]](sys.argv[2])
