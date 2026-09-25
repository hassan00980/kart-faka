/* تنزيل صور حقيقية للشخصيات (من ويكيبيديا — صور حرة) وتخزينها داخل التطبيق
   الهدف: صور موجودة offline في الويب والأندرويد معاً.
   المخرجات:
     - public/img/chars/<slug>.<ext>  +  android assets/www/img/chars/ (نفس النسخ)
     - public/char-img.json  { الاسم العربي: "slug.ext" } — للفتحات الناجحة فقط
*/
'use strict';
const fs = require('fs');
const path = require('path');
const os = require('os');

const ROOT = __dirname + '/..';
const WEB_IMG = path.join(ROOT, 'public', 'img', 'chars');
const AND_IMG = path.join(ROOT, 'android', 'khammeni-android', 'app', 'src', 'main', 'assets', 'www', 'img', 'chars');
const TMP = fs.mkdtempSync(path.join(os.tmpdir(), 'wkr-'));

/* 1) لاعبي الكورة من players.json (الاسم العربي + الاسم الإنجليزي) */
const players = require(path.join(ROOT, 'players.json')).players;
const candidates = players
  .filter((p) => p && p.n && p.e)
  .map((p) => ({ arabic: p.n, title: p.e }));
/* 2) أشخاص حقيقيون آخرون (مشهورين/فنانين/رياضيين غير موجودين) */
for (const p of require(path.join(__dirname, 'people.js'))) {
  if (!candidates.some((c) => c.arabic === p.arabic)) candidates.push({ arabic: p.arabic, title: p.title });
}
const LIMIT = parseInt(process.env.LIMIT || '0', 10);
if (LIMIT > 0) candidates.length = Math.min(LIMIT, candidates.length);
console.log(`[download-images] إجمالي المرشحين: ${candidates.length}`);

const UA = { 'User-Agent': 'KhammeniGame/1.0 (local dev; contact: emSquid) node-fetch' };

function slugOf(title) {
  return title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');
}
function extOf(url) {
  const m = /\.(jpe?g|png|webp|gif)(?:$|\?)/i.exec(String(url));
  return m ? m[1].toLowerCase().replace('jpeg', 'jpg') : 'jpg';
}

async function fetchJson(url, retries = 5) {
  for (let attempt = 0; attempt < retries; attempt++) {
    const ctl = new AbortController();
    const t = setTimeout(() => ctl.abort(), 20000);
    try {
      const r = await fetch(url, { signal: ctl.signal, headers: UA });
      if (r.status === 429 || r.status >= 500) {
        const ra = parseInt(r.headers.get('retry-after') || '', 10);
        await sleep(Math.min(12000, (ra || 3 + attempt * 3) * 1000) + Math.random() * 500);
        continue;
      }
      if (!r.ok) return null;
      const ct = r.headers.get('content-type') || '';
      if (!ct.includes('json')) return null;
      return await r.json();
    } catch { await sleep((attempt + 1) * 800); } finally { clearTimeout(t); }
  }
  return null;
}
function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }

/* صورة سطر المرجع من الصفحة (lead image حرة) أو البحث الاحتياطي */
async function imageOf(title) {
  let j = await fetchJson('https://en.wikipedia.org/api/rest_v1/page/summary/' + encodeURIComponent(title));
  if (j && (j.thumbnail || j.originalimage)) {
    return (j.thumbnail || j.originalimage).source;
  }
  /* البحث الاحتياطي */
  const q = 'https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch='
    + encodeURIComponent(title) + '&gsrlimit=3&prop=pageimages&piprop=original&pilicense=free&format=json&origin=*';
  const s = await fetchJson(q);
  const pages = s && s.query && s.query.pages;
  if (pages) {
    for (const pg of Object.values(pages)) {
      if (pg && pg.original && pg.original.source) return pg.original.source;
    }
  }
  return null;
}

async function download(url, dest) {
  if (fs.existsSync(dest) && fs.statSync(dest).size > 1000) return true; // موجود أصلًا (تشغيل متكرر)
  for (let attempt = 0; attempt < 4; attempt++) {
    const ctl = new AbortController();
    const t = setTimeout(() => ctl.abort(), 30000);
    try {
      const r = await fetch(url, { signal: ctl.signal, headers: UA });
      if (r.status === 429 || r.status >= 500) { await sleep((3 + attempt * 3) * 1000 + Math.random() * 500); continue; }
      if (!r.ok || !r.body) return false;
      const ct = r.headers.get('content-type') || '';
      if (!ct.startsWith('image/')) return false;
      const buf = Buffer.from(await r.arrayBuffer());
      if (buf.length < 1000) return false; // صورة صغيرة جداً = غالباً مش صحيحة
      fs.writeFileSync(dest, buf);
      return true;
    } catch { await sleep((attempt + 1) * 1200); } finally { clearTimeout(t); }
  }
  return false;
}

async function one(c) {
  /* لو الصورة موجودة أصلًا على القرص (من تشغيل سابق) — نتخطى الشبكة خالص */
  const slug = slugOf(c.title);
  const existing = fs.readdirSync(WEB_IMG).find((f) => f.startsWith(slug + '.'));
  if (existing) {
    try { fs.copyFileSync(path.join(WEB_IMG, existing), path.join(AND_IMG, existing)); } catch { }
    return { arabic: c.arabic, name: existing };
  }
  const url = await imageOf(c.title);
  if (!url) return null;
  const ext = extOf(url);
  const name = `${slug}.${ext}`;
  const wDest = path.join(WEB_IMG, name);
  const aDest = path.join(AND_IMG, name);
  const okW = await download(url, wDest);
  if (!okW) return null;
  try { fs.copyFileSync(wDest, aDest); } catch { /* الأندرويد غير موجود */ }
  return { arabic: c.arabic, name };
}

(async () => {
  fs.mkdirSync(WEB_IMG, { recursive: true });
  fs.mkdirSync(AND_IMG, { recursive: true });
  const map = {};
  const saveMap = () => fs.writeFileSync(path.join(ROOT, 'public', 'char-img.json'), JSON.stringify(map, null, 1));
  /* تسلسلي وبمعدل هادي — ويكيبيديا بتحظر الطلبات المتزامنة (429) */
  for (let i = 0; i < candidates.length; i++) {
    const c = candidates[i];
    try {
      const r = await one(c);
      if (r) map[r.arabic] = r.name;
    } catch { /* تجاهل */ }
    process.stdout.write(`\r${i + 1}/${candidates.length} (${Object.keys(map).length} ناجحة)`);
    if (i % 5 === 0) saveMap(); // حفظ تدريجي — لو اتقطع التشغيل نحتفظ باللي راح
    await sleep(2000);
  }
  console.log('');
  /* إضافة أسماء فئات مختصرة تشير لنفس صورة اللاعب الكامل — مثال: «تريزيجيه» ≡ «محمود حسن تريزيجيه» */
  const gameChars = require(path.join(__dirname, 'game-chars.js'));
  let aliasCount = 0;
  for (const cname of gameChars) {
    if (map[cname]) continue;
    const hit = players.find((pl) => pl.n && pl.n.includes(cname) && cname.length >= 6);
    if (hit && map[hit.n]) { map[cname] = map[hit.n]; aliasCount++; }
  }
  console.log(`[download-images] أسماء مستعارة مضافة: ${aliasCount}`);
  saveMap();
  console.log(`[download-images] نجح: ${Object.keys(map).length}/${candidates.length}`);
  console.log('[download-images] انتهى — public/char-img.json (حجم: ' + (fs.statSync(path.join(ROOT, 'public', 'char-img.json')).size) + ' بايت)');
})().catch((e) => { console.error('[download-images] خطأ:', e); process.exitCode = 1; });