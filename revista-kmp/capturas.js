// Saca capturas de la versión nativa (ya compilada y servida en http://localhost:8080) con datos reales de la Revista.
const { chromium } = require('playwright');
const fs = require('fs');
const SB = 'https://qfgncdzpcssiirxijdgj.supabase.co', KEY = 'sb_publishable_VWLMFbw4lhkV6lW4-DQ2FA_x6E9lVLT';
const q = async p => { try { const r = await fetch(SB + p, { headers: { apikey: KEY } }); const t = await r.text(); if (!r.ok) { console.log('Supabase', r.status, t.slice(0, 300)); return []; } return JSON.parse(t); } catch (e) { console.log('Supabase falló:', e.message); return []; } };
(async () => {
  fs.mkdirSync('capturas', { recursive: true });
  const ahora = encodeURIComponent(new Date().toISOString());
  let posts = await q(`/rest/v1/posts?select=id,title,style&status=eq.published&publish_at=lte.${ahora}&order=created_at.desc&limit=60`);
  if (!posts.length) posts = await q('/rest/v1/posts?select=id,title,style&order=created_at.desc&limit=60');
  const dz = (await q('/rest/v1/posts?select=id,design&status=eq.published&order=created_at.desc&limit=60')).find(x => x.design && Object.keys(x.design).length);
  const mag = posts.find(p => p.style && p.style.cv === 'm');
  const tema = posts.find(p => p !== mag && p.style && p.style.th && p.style.th !== 'asfalto');
  const elegidos = [mag, tema, dz, posts[0]].filter(Boolean).filter((p, i, a) => a.findIndex(x => x.id === p.id) === i).slice(0, 3);
  console.log('Artículos elegidos:', elegidos.map(p => p.id).join(', ') || '(ninguno)');
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 412, height: 892 }, deviceScaleFactor: 2, locale: 'es-419', timezoneId: 'America/Tegucigalpa' });
  const log = [];
  const shot = async (hash, nombre, bajadas = [0]) => {
    const pg = await ctx.newPage();
    pg.on('console', m => log.push(`[${nombre}] ${m.type()}: ${m.text()}`)); pg.on('pageerror', e => log.push(`[${nombre}] ERROR: ${e.message}`));
    await pg.goto('http://localhost:8080/' + hash, { waitUntil: 'load' });
    await pg.waitForTimeout(14000);
    let i = 0;
    for (const px of bajadas) {
      if (px) { await pg.mouse.move(206, 520); await pg.mouse.wheel(0, px); await pg.waitForTimeout(2500); }
      await pg.screenshot({ path: `capturas/${nombre}${bajadas.length > 1 ? '-' + (++i) : ''}.png` });
    }
    await pg.close();
  };
  await shot('', '1-inicio', [0, 1400]);
  let n = 2;
  for (const p of elegidos) await shot('#post=' + p.id, `${n++}-articulo`, [0, 800, 900]);
  if (!elegidos.length) for (const k of [1, 2, 3]) await shot('#ejemplo=' + k, `${n++}-ejemplo${k}`, [0, 800, 900]);
  const perfiles = await q('/rest/v1/profiles?select=id&order=created_at.asc&limit=1');
  if (perfiles[0]) await shot('#perfil=' + perfiles[0].id, `${n++}-perfil`);
  await shot('#entrar', `${n++}-entrar`);
  await shot('#escribir-demo', `${n++}-editor`, [0, 900, 900]);
  await shot('#escribir-fondo', `${n++}-editor-fondo`, [0, 700]);
  await shot('#escribir-collage', `${n++}-collage`);
  await shot('#posters', `${n++}-posters`);
  await shot('#boveda', `${n++}-boveda`);
  fs.writeFileSync('capturas/consola.txt', log.join('\n'));
  await b.close();
})();
