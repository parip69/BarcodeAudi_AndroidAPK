const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'app/src/main/assets/index.html'), 'utf8');
const canonical = 'https://parip69.github.io/BarcodeAudi_AndroidAPK/';
const shareFunction = html.slice(html.indexOf('        async function shareAppNow()'), html.indexOf('        async function importSharedDataFromUrl()'));
const baseFunction = html.slice(html.indexOf('        function getShareBaseUrl()'), html.indexOf('        async function buildAppShareUrl('));

// Parse every inline script to catch syntax errors in the complete web app.
for (const match of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)) {
  new vm.Script(match[1]);
}

async function check(mode, target) {
  const url = canonical + (mode === 'data' ? '#shared=test-data' : '');
  const sent = [];
  let closed = 0;
  const button = {};
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
    showAppUpdateMessage(message, type) { assert.notEqual(type, 'error', message); },
    console,
    window: { isSecureContext: true, AndroidInterface: {} },
    navigator: {},
  };
  if (target === 'native') {
    context.window.AndroidInterface.shareAppLink = (text, link) => sent.push({ text, url: link });
  } else if (target === 'clipboard') {
    context.navigator.clipboard = { writeText: async link => sent.push({ url: link }) };
  } else {
    context.navigator.share = async payload => sent.push(payload);
  }
  // An old bridge must never be used to send a QR image.
  context.window.AndroidInterface.shareQrCode = () => { throw new Error('QR-Dateiversand aufgerufen'); };
  vm.createContext(context);
  vm.runInContext(baseFunction + shareFunction, context);
  assert.equal(vm.runInContext('getShareBaseUrl()', context), canonical);
  await vm.runInContext('shareAppNow()', context);
  assert.equal(sent.length, 1);
  assert.equal(sent[0].url, url);
  assert.equal('files' in sent[0], false);
  assert.equal(button.disabled, false);
  assert.equal(closed, target === 'clipboard' ? 0 : 1);
}

(async () => {
  for (const mode of ['app', 'data']) {
    for (const target of ['browser', 'native', 'clipboard']) await check(mode, target);
  }
  assert.match(html, /og:image" content="https:\/\/parip69.github.io\/BarcodeAudi_AndroidAPK\/icons\/share-card.png/);
  assert.match(html, /id="appShareQrCanvas"/);
  console.log('OK: 6 Teilen-Fälle, QR-Vorschau, Kärtchen-Metadaten und JavaScript-Syntax.');
})().catch(error => { console.error(error); process.exitCode = 1; });
