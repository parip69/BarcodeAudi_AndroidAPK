const APP_SHELL_CACHE = "barcode-audi-shell-installed-v104";
const RUNTIME_CACHE = "barcode-audi-runtime-v104";
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

function versionFromLegacyCacheName(name) {
  const match = String(name || "").match(/^barcode-audi-shell-(?:installed-)?v(\d+)$/i);
  return match ? Number(match[1]) : -1;
}

async function findBestLegacyShellCache() {
  const keys = await caches.keys();
  return keys
    .filter((key) =>
      key !== APP_SHELL_CACHE &&
      /^barcode-audi-shell-(?:installed-)?v\d+$/i.test(key)
    )
    .sort((a, b) => versionFromLegacyCacheName(b) - versionFromLegacyCacheName(a))[0] || "";
}

async function copyLegacyInstalledVersion(targetCache) {
  const legacyName = await findBestLegacyShellCache();
  if (!legacyName) return false;
  const legacy = await caches.open(legacyName);
  const indexResponse =
    (await legacy.match("./index.html", { ignoreSearch: true })) ||
    (await legacy.match("./", { ignoreSearch: true }));
  if (!indexResponse) return false;

  await targetCache.put("./index.html", indexResponse.clone());
  await targetCache.put("./", indexResponse.clone());

  for (const url of PRECACHE_URLS.slice(2)) {
    const response = await legacy.match(url, { ignoreSearch: true });
    if (response) await targetCache.put(url, response.clone());
  }
  return true;
}

async function fetchFresh(url) {
  const requestUrl = new URL(url, self.registration.scope);
  requestUrl.searchParams.set("_barcodeInstall", String(Date.now()));
  const response = await fetch(requestUrl.toString(), { cache: "no-store" });
  if (!isCacheableResponse(response)) {
    throw new Error(`HTTP ${response ? response.status : "?"} bei ${url}`);
  }
  return response;
}

async function refreshInstalledShell() {
  const cache = await caches.open(APP_SHELL_CACHE);
  for (const url of PRECACHE_URLS) {
    const response = await fetchFresh(url);
    await cache.put(url, response.clone());
  }
}

async function ensureInstalledShell() {
  const cache = await caches.open(APP_SHELL_CACHE);
  const existing = await cache.match("./index.html", { ignoreSearch: true });
  if (existing) return;

  // Beim Wechsel von älteren Versionen die zuletzt wirklich installierte
  // Version übernehmen. Eine nur online gefundene neue Version wird dadurch
  // NICHT automatisch installiert.
  if (await copyLegacyInstalledVersion(cache)) return;

  // Nur bei einer echten Erstinstallation gibt es noch keine installierte
  // Version, deshalb wird dann einmalig der aktuelle Stand übernommen.
  await refreshInstalledShell();
}

function replyToMessage(event, payload) {
  try {
    if (event.ports && event.ports[0]) event.ports[0].postMessage(payload);
  } catch (_) {}
}

self.addEventListener("message", (event) => {
  const data = event.data || {};

  if (data.type === "SET_UPDATE_MODE") {
    event.waitUntil(
      setUpdateMode(data.mode)
        .then((mode) => replyToMessage(event, { ok: true, mode }))
        .catch((error) => replyToMessage(event, { ok: false, error: error?.message || String(error) })),
    );
    return;
  }

  if (data.type === "APPLY_UPDATE") {
    event.waitUntil(
      refreshInstalledShell()
        .then(() => replyToMessage(event, { ok: true }))
        .catch((error) => replyToMessage(event, { ok: false, error: error?.message || String(error) })),
    );
    return;
  }

  if (data.type === "APPLY_UPDATE_AND_ACTIVATE" || data.type === "SKIP_WAITING") {
    // SKIP_WAITING bleibt absichtlich kompatibel zu V81. Wichtig: Auch dieser
    // alte Befehl aktualisiert zuerst den installierten Cache. So kann V81 nur
    // nach einem bewussten „Aktualisieren“ auf V82 wechseln.
    event.waitUntil(
      refreshInstalledShell()
        .then(async () => {
          replyToMessage(event, { ok: true });
          await self.skipWaiting();
        })
        .catch((error) => replyToMessage(event, { ok: false, error: error?.message || String(error) })),
    );
  }
});

self.addEventListener("install", (event) => {
  // KEIN skipWaiting und KEIN Überschreiben mit der Serverversion.
  // Dadurch kann ein Browser-Check allein keine neue App-Version aktivieren.
  event.waitUntil(ensureInstalledShell());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      await ensureInstalledShell();
      const keys = await caches.keys();
      await Promise.all(
        keys
          .filter(
            (key) =>
              (/^barcode-audi-shell-v\d+$/i.test(key) || /^barcode-audi-shell-installed-v\d+$/i.test(key) || /^barcode-audi-runtime-v\d+$/i.test(key) || key.startsWith("mathe-guru-")) &&
              key !== APP_SHELL_CACHE &&
              key !== RUNTIME_CACHE &&
              key !== SETTINGS_CACHE,
          )
          .map((key) => caches.delete(key)),
      );
      await self.clients.claim();
    })(),
  );
});

function isAppShellRequest(request, url) {
  const scopePath = new URL(self.registration.scope).pathname;
  const scopeRoot = scopePath.endsWith("/") && scopePath.length > 1 ? scopePath.slice(0, -1) : scopePath;
  return (
    request.mode === "navigate" ||
    url.pathname === scopePath ||
    url.pathname === scopeRoot ||
    url.pathname.endsWith("/index.html") ||
    url.pathname.endsWith("/manifest.webmanifest")
  );
}

async function installedShellFirst(request, url) {
  const cache = await caches.open(APP_SHELL_CACHE);
  const scopePath = new URL(self.registration.scope).pathname;
  const isNavigation = request.mode === "navigate";
  const isRoot = url.pathname === scopePath || url.pathname === scopePath.replace(/\/$/, "");

  if (isNavigation || isRoot || url.pathname.endsWith("/index.html")) {
    const installed =
      (await cache.match("./index.html", { ignoreSearch: true })) ||
      (await cache.match("./", { ignoreSearch: true }));
    if (installed) return installed;
  }

  const cached = await cache.match(request, { ignoreSearch: true });
  if (cached) return cached;

  const response = await fetch(request);
  if (isCacheableResponse(response)) await cache.put(request, response.clone());
  return response;
}

async function runtimeCacheFirst(request) {
  const cache = await caches.open(RUNTIME_CACHE);
  const cached = await cache.match(request, { ignoreSearch: true });
  if (cached) return cached;
  const response = await fetch(request);
  if (isCacheableResponse(response)) await cache.put(request, response.clone());
  return response;
}

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;
  const url = new URL(request.url);

  if (url.origin !== self.location.origin) {
    event.respondWith(runtimeCacheFirst(request).catch(() => caches.match(request)));
    return;
  }

  // Reine Versionsprüfung: immer direkt vom Server lesen, aber NICHT in den
  // installierten App-Cache schreiben. Genau dadurch kann ein kurzer Druck auf
  // „Update“ eine neue Version erkennen, ohne sie schon zu installieren.
  if (url.searchParams.has("_barcodeUpdateCheck")) {
    event.respondWith(fetch(request, { cache: "no-store" }));
    return;
  }

  if (isAppShellRequest(request, url)) {
    // WICHTIG: Ein normaler Start/Reload darf niemals selbst eine neue
    // index.html installieren. Immer die zuletzt ausdrücklich installierte
    // Version ausliefern. Ob MANUELL nur geprüft oder AUTOMATISCH wirklich
    // aktualisiert wird, entscheidet erst die bereits geladene Seite.
    event.respondWith(
      (async () => {
        await ensureInstalledShell();
        return installedShellFirst(request, url);
      })(),
    );
    return;
  }

  event.respondWith(runtimeCacheFirst(request).catch(() => caches.match(request)));
});
