// Red primero: siempre intenta la versión nueva y solo usa la copia guardada si no hay internet.
// Sube el número de CACHE (rr3, rr4...) cada vez que cambies el index.html.
const CACHE = 'rr18';
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
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== self.location.origin) return;
  e.respondWith(
    fetch(e.request).then(r => {
      if (r.ok) { const c = r.clone(); caches.open(CACHE).then(k => k.put(e.request, c)).catch(() => {}); }
      return r;
    }).catch(async () => {
      // ignoreSearch: sirve aunque la URL traiga ?code=... del redirect de Spotify.
      const cached = await caches.match(e.request, { ignoreSearch: true });
      if (cached) return cached;
      if (e.request.mode === 'navigate') return (await caches.match('./index.html')) || Response.error();
      return Response.error();
    })
  );
});
