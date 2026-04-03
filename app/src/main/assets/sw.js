const APP_SHELL_CACHE = "barcode-audi-shell-v55";
const RUNTIME_CACHE = "barcode-audi-runtime-v55";
const PRECACHE_URLS = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
  "./icons/apple-touch-icon.png",
];

function isCacheableResponse(response) {
  return Boolean(response && response.status === 200 && response.type !== "opaque");
}

function isAppShellRequest(request, url) {
  const scopePath = new URL(self.registration.scope).pathname;
  const scopeRoot =
    scopePath.endsWith("/") && scopePath.length > 1
      ? scopePath.slice(0, -1)
      : scopePath;

  return (
    request.mode === "navigate" ||
    url.pathname === scopePath ||
    url.pathname === scopeRoot ||
    url.pathname.endsWith("/index.html") ||
    url.pathname.endsWith("/manifest.webmanifest") ||
    url.pathname.endsWith("/sw.js")
  );
}

self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(APP_SHELL_CACHE)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter(
              (key) =>
                (key.startsWith("barcode-audi-") || key.startsWith("mathe-guru-")) &&
                key !== APP_SHELL_CACHE &&
                key !== RUNTIME_CACHE,
            )
            .map((key) => caches.delete(key)),
        ),
      )
      .then(() => self.clients.claim()),
  );
});

async function networkFirst(request, cacheName, fallbackUrl) {
  const cache = await caches.open(cacheName);

  try {
    const response = await fetch(request, { cache: "no-store" });
    if (isCacheableResponse(response)) {
      cache.put(request, response.clone()).catch(() => {});
    }
    return response;
  } catch (error) {
    const cached = await cache.match(request);
    if (cached) return cached;

    if (fallbackUrl) {
      const fallback = await cache.match(fallbackUrl);
      if (fallback) return fallback;
    }

    throw error;
  }
}

async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);
  if (cached) return cached;

  const response = await fetch(request);
  if (isCacheableResponse(response)) {
    cache.put(request, response.clone()).catch(() => {});
  }
  return response;
}

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);

  if (url.origin !== self.location.origin) {
    event.respondWith(
      networkFirst(request, RUNTIME_CACHE).catch(() => caches.match(request)),
    );
    return;
  }

  if (isAppShellRequest(request, url)) {
    event.respondWith(
      networkFirst(request, APP_SHELL_CACHE, "./index.html").catch(() =>
        caches.match("./index.html"),
      ),
    );
    return;
  }

  event.respondWith(
    cacheFirst(request, RUNTIME_CACHE).catch(() => caches.match(request)),
  );
});
