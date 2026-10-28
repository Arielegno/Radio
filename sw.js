// Red primero: siempre intenta la versión nueva y solo usa la copia guardada si no hay internet.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', e => e.waitUntil(self.clients.claim()));
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== location.origin) return;
  e.respondWith(fetch(e.request).then(r => { const c = r.clone(); caches.open('rr1').then(k => k.put(e.request, c)); return r; }).catch(() => caches.match(e.request)));
});
