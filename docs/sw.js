const APP_SHELL_CACHE = "barcode-audi-shell-v85";
const RUNTIME_CACHE = "barcode-audi-runtime-v85";
const SETTINGS_CACHE = "barcode-audi-settings-v1";
const UPDATE_MODE_URL = new URL("__update_mode__", self.registration.scope).toString();

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

function normalizeUpdateMode(value) {
  return String(value || "").toLowerCase() === "auto" ? "auto" : "manual";
}

async function getUpdateMode() {
  try {
    const cache = await caches.open(SETTINGS_CACHE);
    const response = await cache.match(UPDATE_MODE_URL);
    if (!response) return "manual";
    return normalizeUpdateMode(await response.text());
  } catch (_) {
    return "manual";
  }
}

async function setUpdateMode(mode) {
  const normalized = normalizeUpdateMode(mode);
  const cache = await caches.open(SETTINGS_CACHE);
  await cache.put(
    UPDATE_MODE_URL,
    new Response(normalized, {
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    }),
  );
  return normalized;
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
  const data = event.data || {};

  if (data.type === "SET_UPDATE_MODE") {
    event.waitUntil(
      setUpdateMode(data.mode).then((mode) => {
        // Nur im ausdrücklich gewählten Automatik-Modus darf ein wartender
        // Service Worker selbständig aktiv werden.
        if (mode === "auto") return self.skipWaiting();
        return undefined;
      }),
    );
    return;
  }

  if (data.type === "SKIP_WAITING") {
    // Dieser Befehl kommt ausschließlich vom bewusst gestarteten Update.
    self.skipWaiting();
  }
});

self.addEventListener("install", (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(APP_SHELL_CACHE);
      await cache.addAll(PRECACHE_URLS);

      // Standard ist MANUELL. Ein neuer Worker bleibt dann wartend und ersetzt
      // die installierte Version nicht von selbst.
      if ((await getUpdateMode()) === "auto") {
        await self.skipWaiting();
      }
    })(),
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
                key !== RUNTIME_CACHE &&
                key !== SETTINGS_CACHE,
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
    const cached = await cache.match(request, { ignoreSearch: true });
    if (cached) return cached;

    if (fallbackUrl) {
      const fallback = await cache.match(fallbackUrl, { ignoreSearch: true });
      if (fallback) return fallback;
    }

    throw error;
  }
}

async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request, { ignoreSearch: true });
  if (cached) return cached;

  const response = await fetch(request);
  if (isCacheableResponse(response)) {
    cache.put(request, response.clone()).catch(() => {});
  }
  return response;
}

async function manualAppShell(request, url) {
  const cache = await caches.open(APP_SHELL_CACHE);
  const scopePath = new URL(self.registration.scope).pathname;
  const isNavigation = request.mode === "navigate";
  const isRoot = url.pathname === scopePath || url.pathname === scopePath.replace(/\/$/, "");

  // Im manuellen Modus wird bei Öffnen/Neu laden/Pull-to-refresh bewusst die
  // installierte index.html aus dem Cache genommen. Es findet kein Netz-Update statt.
  if (isNavigation || isRoot || url.pathname.endsWith("/index.html")) {
    const installed =
      (await cache.match("./index.html", { ignoreSearch: true })) ||
      (await cache.match("./", { ignoreSearch: true }));
    if (installed) return installed;
  }

  const cached = await cache.match(request, { ignoreSearch: true });
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
      (async () => {
        const mode = await getUpdateMode();
        if (mode === "auto") {
          return networkFirst(request, APP_SHELL_CACHE, "./index.html").catch(() =>
            caches.match("./index.html", { ignoreSearch: true }),
          );
        }
        return manualAppShell(request, url);
      })(),
    );
    return;
  }

  event.respondWith(
    cacheFirst(request, RUNTIME_CACHE).catch(() => caches.match(request)),
  );
});
