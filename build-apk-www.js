/* ليبنّي ملفات www بتاعة الأندرويد من نسخة الويب (v4) — في واجهة واحدة موحّدة */
const fs = require('fs');
const path = require('path');

const root = __dirname;
const pub = path.join(root, 'public');
const dst = path.join(root, 'android', 'khammeni-android', 'app', 'src', 'main', 'assets', 'www');

// 1) index.html بدون سطر socket.io (الأندرويد بيستخدم HTTP polling مش Socket.IO)
let html = fs.readFileSync(path.join(pub, 'index.html'), 'utf8');
html = html.replace(/\s*<script src="\/socket\.io\/socket\.io\.js"><\/script>/, '');
html = html.replace(/client\.js\?v=\d+/, 'client.js?v=10');
html = html.replace(/styles\.css\?v=\d+/, 'styles.css?v=10');
fs.writeFileSync(path.join(dst, 'index.html'), html, 'utf8');

// 2) styles.css زي ما هي — نفس المعرّفات
fs.copyFileSync(path.join(pub, 'styles.css'), path.join(dst, 'styles.css'));

// 2b) qrcode.js (مكتبة توليد رمز QR للانضمام السريع — Feature 8)
fs.copyFileSync(path.join(pub, 'qrcode.js'), path.join(dst, 'qrcode.js'));

// 3) client.js = شيم نقل (استطلاع 700ms بدل socket.io) + نفس منطق الويب بالظبط
const web = fs.readFileSync(path.join(pub, 'client.js'), 'utf8');

const shim = `/* كرت فكة - طبقة النقل HTTP (استطلاع ~700ms) بديلة عن Socket.IO — خاصة بالأندرويد */
(() => {
  'use strict';

  const TOKEN_KEY = 'khammeni_token';
  const POLL_MS = 700;
  const PROTOCOL_VERSION = 6;
  let token = localStorage.getItem(TOKEN_KEY) || '';
  let pollTimer = null;
  const sockets = [];

  function fetchJson(urlPath, opts, timeout) {
    let ctrl = null;
    if (typeof AbortController !== 'undefined') {
      ctrl = new AbortController();
      opts = Object.assign({}, opts, { signal: ctrl.signal });
    }
    const t = setTimeout(() => { if (ctrl) ctrl.abort(); }, timeout || 6000);
    const p = fetch(urlPath, opts);
    p.then(() => clearTimeout(t), () => clearTimeout(t));
    return p;
  }

  function getState() {
    if (!token) return Promise.resolve({ error: 'no-token' });
    return fetchJson('/api/state?token=' + encodeURIComponent(token))
      .then((r) => r.json().catch(() => null))
      .catch(() => null);
  }

  function post(urlPath, body) {
    return fetchJson(urlPath, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=utf-8' },
      body: JSON.stringify(body || {}),
    }).then((r) => r.json().catch(() => ({}))).catch(() => ({ error: 'مفيش اتصال بالسيرفر' }));
  }

  function showHome(msg) {
    document.querySelectorAll('.screen').forEach((s) => s.classList.remove('active'));
    const home = document.getElementById('screen-home');
    if (home) home.classList.add('active');
    if (msg) {
      const err = document.getElementById('home-error');
      if (err) { err.textContent = msg; err.classList.remove('hidden'); }
    }
  }

  function stopPolling() {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
  }

  function startPolling() {
    stopPolling();
    pollTimer = setInterval(refresh, POLL_MS);
  }

  function refresh() {
    if (!token) return Promise.resolve();
    return getState().then((s) => {
      if (!s) return;
      if (s.error) {
        if (s.error === 'invalid' || s.error === 'no-token') {
          if (token) localStorage.removeItem(TOKEN_KEY);
          token = '';
          stopPolling();
          showHome('الأوضة اتقفلت 🚪');
        }
        return;
      }
      sockets.forEach((sk) => sk._fireState(s));
    });
  }

  // أسماء أحداث socket.io -> مسار API بتاع الأندرويد
  const EVENTS = {
    'set-category': 'set-category',
    'set-difficulty': 'set-difficulty',
    'set-spy-count': 'set-spy-count',
    'set-double-agent': 'set-double-agent',
    'set-squad-size': 'set-squad-size',
    'start-game': 'start',
    'pass-turn': 'pass',
    'make-guess': 'guess',
    'auction-act': 'auction-act',
    'steal-act': 'steal-act',
    'request-vote': 'request-vote',
    'vote-response': 'vote-response',
    'vote-cast': 'vote-cast',
    'next-round': 'next-round',
    'chat': 'chat',
    'chat-media': 'chat-media',
    'liar-answer': 'liar-answer',
    'liar-start-vote': 'liar-start-vote',
    'liar-cast-vote': 'liar-cast-vote',
  };

  window.io = () => {
    const h = {};
    const sk = {
      id: token || 'me',
      on(ev, cb) {
        (h[ev] = h[ev] || []).push(cb);
        return this;
      },
      emit(ev, data, cb) {
        if (ev === 'create-room' || ev === 'join-room') {
          const p = ev === 'create-room' ? '/api/create' : '/api/join';
          return post(p, Object.assign({ v: PROTOCOL_VERSION }, data || {})).then((r) => {
            if (r && r.ok && r.token) {
              token = r.token;
              sk.id = token;
              localStorage.setItem(TOKEN_KEY, token);
              startPolling();
            }
            if (cb) cb(r);
            return refresh();
          });
        }
        if (ev === 'leave-room' || ev === 'leave') {
          return post('/api/leave', { token }).then(() => {
            localStorage.removeItem(TOKEN_KEY);
            token = '';
            stopPolling();
            if (cb) cb({ ok: true });
          });
        }
        const api = EVENTS[ev];
        if (!api) {
          if (cb) cb({ ok: false, error: 'حدث غير معروف: ' + ev });
          return Promise.resolve();
        }
        return post('/api/' + api, Object.assign({}, data || {}, { token })).then((r) => {
          if (cb) cb(r);
          return refresh();
        });
      },
      _fireState(s) {
        (h['state'] || []).forEach((cb) => cb(s));
      },
    };
    sockets.push(sk);
    if (token) {
      startPolling();
      setTimeout(refresh, 0);
    }
    return sk;
  };
})();
/* ===== منطق الواجهة (نسخة الويب كما هي - المعرّفات واحدة) ===== */
`;

fs.writeFileSync(path.join(dst, 'client.js'), shim + web, 'utf8');

// 4) صور الشخصيات (مخزنة داخل التطبيق لتشتغل offline) + خرائط الصور والمعلومات
const imgSrc = path.join(pub, 'img', 'chars');
const imgDst = path.join(dst, 'img', 'chars');
if (fs.existsSync(imgSrc)) {
  fs.mkdirSync(imgDst, { recursive: true });
  let n = 0;
  for (const f of fs.readdirSync(imgSrc)) {
    fs.copyFileSync(path.join(imgSrc, f), path.join(imgDst, f));
    n++;
  }
  console.log(`   - نسخ ${n} صورة شخصية إلى img/chars/`);
}
// 4b) شعار اللعبة (كرت فكة) — يُعرض في الواجهة داخل التطبيق
const logoPath = path.join(pub, 'img', 'logo.svg');
if (fs.existsSync(logoPath)) {
  fs.mkdirSync(path.join(dst, 'img'), { recursive: true });
  fs.copyFileSync(logoPath, path.join(dst, 'img', 'logo.svg'));
  console.log('   - نسخ شعار اللعبة إلى img/logo.svg');
}
for (const j of ['char-img.json', 'char-bio.json', 'players.json', 'liar-questions.json']) {
  const s = j === 'players.json' ? path.join(root, j) : path.join(pub, j);
  if (fs.existsSync(s)) fs.copyFileSync(s, path.join(dst, j));
}

// 5) خطوط الواجهة (Aref Ruqaa + El Messiri) — مخزنة داخل التطبيق لتشتغل offline
const fontsSrc = path.join(pub, 'fonts');
const fontsDst = path.join(dst, 'fonts');
if (fs.existsSync(fontsSrc)) {
  fs.mkdirSync(fontsDst, { recursive: true });
  let n = 0;
  for (const f of fs.readdirSync(fontsSrc)) {
    fs.copyFileSync(path.join(fontsSrc, f), path.join(fontsDst, f));
    n++;
  }
  console.log(`   - نسخ ${n} ملف خط إلى fonts/`);
}

console.log('تم بناء www للأندرويد: index.html + styles.css + client.js + img + fonts ✅');