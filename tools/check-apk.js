/* يتحقق من محتويات الـ APK: الملفات الجديدة جوه assets/www */
const { execSync } = require('child_process');
const fs = require('fs');
const apk = process.argv[2] || 'كرت-فكة.apk';
let list;
try {
  list = execSync(`jar tf "${apk}"`, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
} catch (e) {
  try {
    list = execSync(`tar tf "${apk}"`, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  } catch (e2) {
    console.log('ERROR listing:', e.message);
    process.exit(1);
  }
}
const has = (p) => list.includes(p);
const entry = (p) => list.split(/\r?\n/).find((l) => l.endsWith(p));
console.log('client.js:', !!entry('assets/www/client.js'));
console.log('index.html:', !!entry('assets/www/index.html'));
console.log('char-img.json:', !!entry('assets/www/char-img.json'));
console.log('char-bio.json:', !!entry('assets/www/char-bio.json'));
console.log('players.json:', !!entry('assets/www/players.json'));
const imgs = list.split(/\r?\n/).filter((l) => l.includes('assets/www/img/chars/'));
console.log('images count:', imgs.length, imgs.length ? 'sample=' + imgs[0] : '');
console.log('APK path:', apk);