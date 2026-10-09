const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'app/src/main/assets/index.html'), 'utf8');
const canonical = 'https://parip69.github.io/BarcodeAudi_AndroidAPK/';
const sharePage = canonical + 'teilen.html';
const shareFunction = html.slice(html.indexOf('        async function shareAppNow()'), html.indexOf('        async function importSharedDataFromUrl()'));
const baseFunction = html.slice(html.indexOf('        function getShareBaseUrl()'), html.indexOf('        async function buildAppShareUrl('));

// Parse every inline script to catch syntax errors in the complete web app.
for (const match of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)) {
  new vm.Script(match[1]);
}

async function check(mode, target) {
  const url = sharePage + (mode === 'data' ? '?share=test-data' : '');
  const sent = [];
  let closed = 0;
  const button = {};
  const messages = [];
  const context = {
    DEFAULT_APP_UPDATE_URL: canonical,
    document: {
      getElementById: () => button,
      querySelector: () => ({ value: mode }),
    },
    currentAppShareQr: { includeData: mode === 'data', url, canvas: {
      toDataURL() { throw new Error('QR-Code darf nicht gesendet werden'); },
    } },
    buildAppShareUrl: async () => url,
    closeAppShareMenu() { closed++; },
    showAppUpdateMessage(message, type) { messages.push({ message, type }); },
    console,
    window: { isSecureContext: true, AndroidInterface: {} },
    navigator: {},
  };
  if (target === 'native' || target === 'old-native') {
    context.window.AndroidInterface.shareAppLink = (text, link) => sent.push({ text, url: link });
  } else if (target === 'clipboard') {
    context.navigator.clipboard = { writeText: async link => sent.push({ url: link }) };
  } else {
    context.navigator.share = async payload => sent.push(payload);
    context.navigator.canShare = () => target !== 'no-files';
  }
  // An old bridge must never be used to send a QR image.
  context.window.AndroidInterface.shareQrCode = () => { throw new Error('QR-Dateiversand aufgerufen'); };
  context.window.AndroidInterface.shareAppCard = () => { throw new Error('Bildanhang statt Link-Vorschau aufgerufen'); };
  context.URL = URL;
  vm.createContext(context);
  vm.runInContext(baseFunction + shareFunction, context);
  assert.equal(vm.runInContext('getShareBaseUrl()', context), sharePage);
  await vm.runInContext('shareAppNow()', context);
  assert.equal(sent.length, 1);
  if (target === 'browser' || target === 'no-files') {
    assert.equal(sent[0].text.split('\n')[0], url);
  } else assert.equal(sent[0].url, url);
  assert.equal(sent[0].files, undefined);
  assert.equal(messages.some(message => message.type === 'error'), false);
  assert.equal(button.disabled, false);
  assert.equal(closed, target === 'clipboard' ? 0 : 1);
}

(async () => {
  for (const mode of ['app', 'data']) {
    for (const target of ['browser', 'native', 'clipboard', 'no-files', 'old-native']) await check(mode, target);
  }
  assert.match(html, /og:image" content="https:\/\/parip69.github.io\/BarcodeAudi_AndroidAPK\/icons\/share-card-preview.jpg/);
  assert.match(html, /id="appShareQrCanvas"/);
  const landing = fs.readFileSync(path.join(root, 'app/src/main/assets/teilen.html'), 'utf8');
  assert.ok(Buffer.byteLength(landing) < 10000, 'Teilen-Seite muss klein bleiben');
  assert.match(landing, /og:url" content="https:\/\/parip69.github.io\/BarcodeAudi_AndroidAPK\/teilen.html/);
  assert.match(landing, /og:image:type" content="image\/jpeg/);
  assert.ok(fs.statSync(path.join(root, 'app/src/main/assets/icons/share-card-preview.jpg')).size < 100000);
  const redirectScript = landing.match(/<script>([\s\S]*?)<\/script>/)[1];
  for (const suffix of ['', '?share=g.test-data#bookmark', '?share=b.test-data']) {
    let redirected;
    const links = { cardLink: {}, openApp: {} };
    const location = new URL(sharePage + suffix);
    location.replace = value => { redirected = value; };
    vm.runInNewContext(redirectScript, { URL, URLSearchParams, navigator: {userAgent:'Desktop'}, window: { location }, document: { getElementById: id => links[id] || (links[id]={}) } });
    assert.equal(redirected, undefined);
    assert.equal(links.openApp.href, canonical + suffix);
    assert.equal(links.cardLink.href, canonical + suffix);
  }
  const importFunction = html.slice(html.indexOf('        async function importSharedDataFromUrl()'), html.indexOf('        function updateFooterVersion()'));
  const stored = new Map([['userConsent', 'accepted']]);
  let address = canonical + '#share=b.example';
  let failDecode = true;
  const importContext = {
    URL, URLSearchParams, console: {warn() {}},
    window: { location: {href: address}, isBarcodeAndroid: () => false },
    history: { replaceState(_, __, value) { address = value; } },
    appStorage: {
      getItem: key => stored.get(key) || null,
      setItem: (key,value) => stored.set(key,value),
      removeItem: key => stored.delete(key),
      clear: () => stored.clear(),
    },
    decodeSharedPayload: async () => { if (failDecode) throw new Error('Test failure'); return {storage:{barcodes:'["TEST"]'}}; },
    parseLocalStorageImportPayload: text => Object.entries(JSON.parse(text).storage),
    confirmSharedDataImport: async () => true,
    refreshUiAfterFullLocalImport() {}, applyStoredConsentState() {},
    showAppUpdateMessage() {}, showToast() {},
  };
  vm.createContext(importContext);
  vm.runInContext(importFunction, importContext);
  await vm.runInContext('importSharedDataFromUrl()', importContext);
  assert.equal(stored.get('__pendingSharedData'),'b.example');
  failDecode = false;
  importContext.window.location.href = canonical;
  await vm.runInContext('importSharedDataFromUrl()', importContext);
  assert.equal(stored.get('barcodes'),'["TEST"]');
  assert.equal(stored.get('userConsent'),'accepted');
  assert.equal(stored.has('__pendingSharedData'),false);
  console.log('OK: 10 Link-Teilen-Fälle ohne Bildanhang, kompakte Vorschau und Karten-Links mit erhaltenen Daten/Fragment.');
})().catch(error => { console.error(error); process.exitCode = 1; });
