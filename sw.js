// Red primero, pero con paciencia limitada: si la red tarda más de 2,5 s y hay copia guardada, se usa la copia
// (la red sigue en segundo plano y la actualiza). Así la app no se queda en negro con mala señal.
// Sube el número de CACHE (rr22, rr23...) cada vez que cambies el index.html.
// 'rr-artistas' guarda las fotos de los artistas y NO se borra al actualizar.
const CACHE = 'rr32';
const PRECACHE = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './logo.png'
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE)
      // Cada archivo por separado: si alguno no existe, los demás igual se guardan.
      .then(c => Promise.allSettled(PRECACHE.map(u => c.add(u))))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE && k !== 'rr-artistas').map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== self.location.origin) return;
  const net = fetch(e.request).then(r => {
    if (r.ok) { const c = r.clone(); caches.open(CACHE).then(k => k.put(e.request, c)).catch(() => {}); }
    return r;
  });
  e.waitUntil(net.catch(() => {}));
  e.respondWith((async () => {
    // ignoreSearch: sirve aunque la URL traiga ?code=... del redirect de Spotify.
    const cached = await caches.match(e.request, { ignoreSearch: true });
    if (!cached) {
      try { return await net; } catch {
        if (e.request.mode === 'navigate') { const h = await caches.match('./index.html'); if (h) return h; }
        return Response.error();
      }
    }
    return Promise.race([net.catch(() => cached), new Promise(ok => setTimeout(() => ok(cached), 2500))]);
  })());
});
