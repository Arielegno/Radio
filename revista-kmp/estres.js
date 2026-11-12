// Prueba de estrés de la Revista: clics, deslizamientos, texto, teclas, «atrás» y cambios de tamaño al azar, en todas las secciones.
// Uso: node estres.js [url] [acciones por sección]   (por defecto http://localhost:8080/ y 250)
const { chromium, firefox } = require('playwright');
const fs = require('fs');
const BASE = process.argv[2] || 'http://localhost:8080/';
const N = +(process.argv[3] || 250);
const RUTAS = ['', '#buscar', '#mas', '#entrar', '#ejemplo=1', '#ejemplo=2', '#escribir-demo', '#escribir-fondo', '#escribir-collage', '#artista=Soda%20Stereo', '#lista=1'];
const TAMANOS = [[412, 892], [360, 640], [820, 1180], [1440, 900], [1920, 1080], [320, 568]];
const TEXTOS = ['hola', 'Ñandú áéíóú 🎸', '<script>alert(1)</script>', "'; drop table posts; --", 'x'.repeat(400), '   ', '😀😀😀', '12345'];
let semilla = 20261112; const azar = () => { semilla = (semilla * 1103515245 + 12345) % 2147483648; return semilla / 2147483648; };
const elegir = a => a[Math.floor(azar() * a.length)];

(async () => {
  const informe = [];
  const errores = new Map();   // mensaje → cuántas veces
  const anotar = (donde, msg) => { const k = msg.replace(/\s+/g, ' ').slice(0, 300); if (!errores.has(k)) informe.push(`[${donde}] ${k}`); errores.set(k, (errores.get(k) || 0) + 1); };
  for (const [nombre, tipo] of [['chromium', chromium], ['firefox', firefox]]) {
    let b;
    try { b = await tipo.launch(nombre === 'firefox' ? { firefoxUserPrefs: { 'webgl.force-enabled': true, 'webgl.disabled': false } } : {}); }
    catch (e) { informe.push(`(${nombre} no disponible: ${e.message.split('\n')[0]})`); continue; }
    for (const ruta of RUTAS) {
      const [w, h] = elegir(TAMANOS);
      const ctx = await b.newContext({ viewport: { width: w, height: h }, locale: 'es-419' });
      const pg = await ctx.newPage();
      const donde = `${nombre} ${ruta || '#inicio'}`;
      pg.on('pageerror', e => anotar(donde, 'ERROR: ' + e.message));
      pg.on('crash', () => anotar(donde, 'LA PÁGINA SE CERRÓ (crash)'));
      pg.on('console', m => { if (m.type() === 'error' && !/Failed to load resource|net::ERR|NetworkError|supabase|CORS|fetch/i.test(m.text())) anotar(donde, 'consola: ' + m.text()); });
      pg.on('dialog', d => d.dismiss().catch(() => {}));
      try {
        await pg.goto(BASE + ruta, { waitUntil: 'load', timeout: 60000 });
        await pg.waitForTimeout(9000);
        for (let i = 0; i < N; i++) {
          const vw = pg.viewportSize();
          const x = Math.floor(azar() * vw.width), y = Math.floor(azar() * vw.height);
          const r = azar();
          if (r < 0.45) {
            // en la web se tocan botones, enlaces y casillas de verdad; en la nativa (todo dibujado) cualquier punto
            const objetivos = await pg.$$('button:visible, a:visible, [role=button]:visible, input:visible, select:visible, textarea:visible, summary:visible, [onclick]:visible').catch(() => []);
            if (objetivos.length && azar() < 0.85) { const o = elegir(objetivos); await o.click({ timeout: 1500, force: true }).catch(() => {}); if (azar() < 0.3) await pg.keyboard.type(elegir(TEXTOS)).catch(() => {}); }
            else await pg.mouse.click(x, y);
          }
          else if (r < 0.55) await pg.mouse.dblclick(x, y);
          else if (r < 0.70) { await pg.mouse.move(x, y); await pg.mouse.wheel(0, (azar() - 0.3) * 1600); }
          else if (r < 0.78) { await pg.mouse.click(x, y); await pg.keyboard.type(elegir(TEXTOS), { delay: 0 }); }
          else if (r < 0.83) await pg.keyboard.press(elegir(['Escape', 'Enter', 'Tab', 'Backspace', 'ArrowDown', 'PageDown', 'Home']));
          else if (r < 0.87) await pg.goBack({ timeout: 3000 }).catch(() => {});
          else if (r < 0.90) { const [a, c] = elegir(TAMANOS); await pg.setViewportSize({ width: a, height: c }); }
          else if (r < 0.93) await pg.goto(BASE + elegir(RUTAS), { waitUntil: 'load', timeout: 30000 }).catch(() => {});
          else { await pg.mouse.move(x, y); await pg.mouse.down(); await pg.mouse.move(Math.floor(azar() * vw.width), Math.floor(azar() * vw.height), { steps: 5 }); await pg.mouse.up(); }
          if (i % 10 === 0) await pg.waitForTimeout(150);
          if (i % 50 === 0) {   // ¿sigue respondiendo?
            const vivo = await Promise.race([pg.evaluate(() => 1).catch(() => 0), new Promise(res => setTimeout(() => res(-1), 8000))]);
            if (vivo !== 1) { anotar(donde, vivo === -1 ? 'LA PÁGINA DEJÓ DE RESPONDER (8 s)' : 'La página no contesta'); break; }
          }
        }
        // dejar que terminen las tareas pendientes
        await pg.waitForTimeout(3000);
      } catch (e) { anotar(donde, 'La prueba falló: ' + e.message.split('\n')[0]); }
      await ctx.close();
      console.log(`listo: ${donde} · errores distintos hasta ahora: ${errores.size}`);
    }
    await b.close();
  }
  const texto = ['PRUEBA DE ESTRÉS · REVISTA', `Acciones por sección: ${N}`, `Errores distintos: ${errores.size}`, '', ...informe.map(l => { const k = l.replace(/^\[[^\]]*\] /, ''); return l + (errores.get(k) > 1 ? `  (×${errores.get(k)})` : ''); })].join('\n');
  fs.writeFileSync('estres-revista.txt', texto);
  console.log(texto);
})();
