const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const html = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/index.html'), 'utf8');
const card = html.match(/<a\s[^>]*href="https:\/\/parip69\.github\.io\/EasyLauncher-Download\/info\.html"[^>]*>/);
assert.ok(card, 'Easy Launcher must open the info page');
assert.ok(!html.includes('EasyLauncher-Download/download.html'));
const handler = new vm.Script(`(function () { ${card[0].match(/onclick="([^"]+)"/)[1]} }).call(link)`);
const url = 'https://parip69.github.io/EasyLauncher-Download/info.html';
const opened = [];
assert.equal(handler.runInNewContext({
  link: { href: url },
  window: { AndroidInterface: { openExternalLink: value => { opened.push(value); return true; } } },
}), false, 'Native opening must cancel WebView navigation');
assert.deepEqual(opened, [url]);
for (const window of [{}, { AndroidInterface: {} }]) {
  assert.equal(handler.runInNewContext({ link: { href: url }, window }), undefined,
    'Browser and older bridge must retain the normal info-page link');
}
assert.equal(handler.runInNewContext({ link: { href: url }, window: {
  AndroidInterface: { openExternalLink: () => false },
} }), true, 'Rejected links retain normal browser navigation');
console.log('OK: Easy Launcher opens info.html via native bridge; browser fallbacks preserved.');
