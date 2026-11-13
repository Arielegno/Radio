// Prueba de nombres largos: abre la Revista web con datos de mentira donde todo nombre mide 40 letras sin espacios
// (el peor caso) y anota cada texto que se sale de su casilla. Uso: node nombres-largos.js http://localhost:8090/
const { chromium } = require('playwright');
const fs = require('fs');
const BASE = process.argv[2] || 'http://localhost:8090/';
const LARGO = 'LARGO' + 'Bartolomeoaurelianoconstantinoxyzwq'.slice(0, 35);          // 40 letras seguidas
const LARGO2 = 'LARGO Maximiliano Bartolomé de la Santísima Trinid';               // 50 con espacios (versiones viejas)
const uid = i => `0000000${i}-aaaa-4bbb-8ccc-${String(100000000000 + i)}`;
const fecha = new Date(Date.now() - 3600e3).toISOString();
const persona = i => ({ id: uid(i), name: i % 2 ? LARGO : LARGO2, avatar: null, bio: LARGO + ' ' + LARGO, role: i === 1 ? 'editor' : 'user', created_at: fecha, banned: false, wall_open: true, style: {} });
const fila = i => ({
  ...persona(i), id: uid(i), author_id: uid(i + 1), owner_id: uid(i + 1), user_id: uid(i + 1), follower_id: uid(i + 2), followee_id: uid(i + 3), uploader_id: uid(i + 1),
  title: 'Título ' + LARGO, body: ('Texto de prueba con ' + LARGO + '. ').repeat(8), excerpt: 'Resumen ' + LARGO, cover: null, tags: ['rock', LARGO],
  status: 'published', kind: 'article', genre: 'Rock', country: 'Honduras', image: null, url: 'https://example.com/' + i, source_name: LARGO, note: LARGO,
  artist: LARGO, album: LARGO, rating: 4, likes: 3, comments: 2, count: 3, created_at: fecha, updated_at: fecha, starts_at: fecha, city: LARGO, place: LARGO,
  profiles: persona(i + 1), author: persona(i + 1), from_profile: persona(i + 2), to_profile: persona(i + 3), review: { album: LARGO, artist: LARGO, rating: 4 },
  public: true, open: true, items: [], options: ['Sí', 'No'], question: LARGO + '?', text: LARGO, reason: LARGO, category: 'spam', type: 'post',
});
(async () => {
  const b = await chromium.launch();
  const malos = new Map();
  for (const [w, h, logueado] of [[1366, 800, true], [390, 844, true], [1366, 800, false]]) {
    const ctx = await b.newContext({ viewport: { width: w, height: h }, locale: 'es-419' });
    await ctx.route(/supabase\.co/, r => {
      const u = r.request().url();
      if (/\/rpc\//.test(u)) return r.fulfill({ status: 200, contentType: 'application/json', body: /invite_only|can_dm|is_/.test(u) ? 'false' : '[]' });
      if (/\/auth\//.test(u)) return r.fulfill({ status: 400, contentType: 'application/json', body: '{"error":"x"}' });
      if (/\/storage\//.test(u)) return r.fulfill({ status: 404, body: '' });
      const una = /[?&]id=eq\./.test(u) || /limit=1\b/.test(u);
      const datos = una ? [fila(1)] : [1, 2, 3, 4, 5, 6].map(fila);
      return r.fulfill({ status: 200, contentType: 'application/json', headers: { 'content-range': '0-5/6', 'access-control-expose-headers': 'content-range' }, body: JSON.stringify(datos) });
    });
    const pg = await ctx.newPage();
    pg.on('dialog', d => d.dismiss().catch(() => {}));
    if (logueado) await pg.addInitScript(([u, n]) => { localStorage.setItem('rr_rev_ses', JSON.stringify({ access: 'x', refresh: 'x', exp: Date.now() + 9e8, uid: u, name: n })); }, [uid(99), LARGO]);
    await pg.goto(BASE + '#revista', { waitUntil: 'load' });
    await pg.waitForTimeout(5000);
    const medir = async (donde) => {
      const r = await pg.evaluate((marca) => {
        const out = [];
        for (const el of document.querySelectorAll('body *')) {
          if (!['B', 'STRONG', 'SPAN', 'A', 'SMALL', 'P', 'H1', 'H2', 'H3', 'H4', 'DIV', 'LABEL', 'BUTTON', 'FIGCAPTION', 'LI', 'TD'].includes(el.tagName)) continue;
          if (![...el.childNodes].some(n => n.nodeType === 3 && n.textContent.includes(marca))) continue;
          const cs = getComputedStyle(el); if (cs.display === 'none' || cs.visibility === 'hidden') continue;
          const re = el.getBoundingClientRect(); if (!re.width || !re.height) continue;
          // la casilla que lo contiene: el primer padre con borde redondeado, fondo, recorte o ancho fijo
          let a = el.parentElement;
          while (a && a !== document.body) { const s = getComputedStyle(a); if (s.borderRadius !== '0px' || s.overflow !== 'visible' || s.backgroundColor !== 'rgba(0, 0, 0, 0)') break; a = a.parentElement; }
          if (!a || a === document.body) a = document.documentElement;
          const ra = a.getBoundingClientRect();
          const sale = re.right > ra.right + 2 || re.left < ra.left - 2;
          const recortado = el.scrollWidth > el.clientWidth + 2 && cs.overflow === 'visible' && cs.textOverflow !== 'ellipsis';
          if (sale || recortado) out.push(`${el.tagName.toLowerCase()}.${(el.className || '').toString().split(' ')[0]} dentro de ${a.tagName.toLowerCase()}.${(a.className || '').toString().split(' ')[0]} · se sale ${Math.round(Math.max(re.right - ra.right, ra.left - re.left, el.scrollWidth - el.clientWidth))}px`);
        }
        return out;
      }, 'LARGO');
      for (const x of r) { const k = `[${w}px${logueado ? ', con cuenta' : ''}] ${x.replace(/ · se sale.*/, '')}`; if (!malos.has(k)) malos.set(k, `${x.match(/se sale.*/)[0]} · en ${donde}`); }
    };
    await medir('inicio');
    // recorrer: secciones, perfiles, artículos, artistas… (se cierran los diálogos con Esc)
    const acciones = ['sec', 'profile', 'open', 'artist', 'tag', 'lstab', 'read', 'poster', 'wall', 'st', 'acct'];
    for (let i = 0; i < 70; i++) {
      const act = acciones[i % acciones.length];
      const els = await pg.$$(`[data-act="${act}"]:visible`);
      if (els.length) { await els[Math.floor(Math.random() * els.length)].click({ timeout: 2000, force: true }).catch(() => {}); await pg.waitForTimeout(700); await medir(act); }
      if (i % 6 === 5) { await pg.keyboard.press('Escape').catch(() => {}); await pg.waitForTimeout(200); }
      if (i % 20 === 19) { await pg.goto(BASE + '#revista', { waitUntil: 'load' }); await pg.waitForTimeout(3000); }
    }
    await ctx.close();
  }
  await b.close();
  const texto = ['NOMBRES LARGOS · REVISTA WEB', `Textos que se salen de su casilla: ${malos.size}`, '', ...[...malos].map(([k, v]) => `${k} — ${v}`)].join('\n');
  fs.writeFileSync('nombres-largos.txt', texto); console.log(texto);
})();
