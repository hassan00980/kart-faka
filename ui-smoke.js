/* فحص الواجهة المعزول: سوكيت وهمي يبث حالات، ونتحقق من الـ DOM و الأحداث المُرسَلة
   (منطق السيرفر اتختبر كفاية في test.js — ده بيختبر client.js بس) */
const { JSDOM } = require('jsdom');
const fs = require('fs');

const html = fs.readFileSync('public/index.html', 'utf8');
const clientSrc = fs.readFileSync('public/client.js', 'utf8');

let ok = 0, fail = 0;
function check(name, cond, extra) {
  if (cond) { ok++; console.log(`  ✅ ${name}`); }
  else { fail++; console.log(`  ❌ ${name} ${extra !== undefined ? '| ' + JSON.stringify(extra) : ''}`); }
}

class MockSocket {
  constructor() {
    this.id = 'me';
    this.handlers = {};
    this.emitted = [];
  }
  on(ev, cb) { (this.handlers[ev] = this.handlers[ev] || []).push(cb); return this; }
  emit(ev, data, ack) { this.emitted.push({ ev, data, ack }); return this; }
  fire(ev, data) { (this.handlers[ev] || []).forEach((cb) => cb(data)); }
  connect() { setTimeout(() => (this.handlers['connect'] || []).forEach((cb) => cb()), 5); }
  last(ev) { return [...this.emitted].reverse().find((e) => e.ev === ev) || null; }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/* ---------- إنشاء تطبيق بسوكيت وهمي ---------- */
function makeApp() {
  const dom = new JSDOM(html, { url: 'http://localhost:3010/', runScripts: 'dangerously', pretendToBeVisual: true });
  const { window } = dom;
  const sock = new MockSocket();
  window.console.log = (...a) => console.log('[wnd]', ...a);
  window.addEventListener('error', (e) => console.log('[wnd-err]', e.message));
  window.io = () => sock;
  window.eval(clientSrc);
  sock.connect();
  const $ = (id) => window.document.getElementById(id);
  const q = (sel) => window.document.querySelector(sel);
  const qa = (sel) => [...window.document.querySelectorAll(sel)];
  return { window, $, q, qa, sock, doc: window.document };
}

/* ---------- بناء حالات (بنفس شكل sanitize في السيرفر) ---------- */
const P = (id, name, isMe) => ({ id, name, connected: true, eliminated: false, isMe, char: null });
const CARD = (n, r, pos, t, c) => ({ n, r, pos, t, c });

function baseState(mode, overrides) {
  const st = {
    code: 'XK2P', host: 'me', myId: 'me',
    state: 'lobby', mode,
    categoryId: 'mix', categories: [{ id: 'mix', name: 'الكل', emoji: '🌍' }, { id: 'sports', name: 'رياضة', emoji: '⚽' }],
    players: [], chat: [], turnId: null, winnerId: null,
    spiesCount: 1, spiesWon: null, word: null,
    hostIP: undefined, points: {}, squadSize: 5,
    votePrompt: false, myVoteResponse: null, voteResponded: 0, voteTotal: 0,
    voteOpen: false, voteTargets: [], myVote: null, voteCast: 0,
    auction: null, auctionResult: null,
  };
  return Object.assign(st, overrides);
}

const fc = (c) => ({ ...c });

const auctionView = {
  phase: 'bidding', cardIndex: 2, deckLength: 22,
  card: CARD('محمد صلاح', 92, 'FW', 'ليفربول', 'مصر'),
  currentBid: 58, startBid: 55,
  remainingBidders: 2, iAmBidder: true, bidTurnId: 'me',
  myBudget: 392,
  myTeam: [fc(CARD('فيرمينو', 88, 'MF', 'ليفربول', 'البرازيل'))],
  teams: { me: [fc(CARD('فيرمينو', 88, 'MF', 'ليفربول', 'البرازيل'))], other: [fc(CARD('رونالدو', 91, 'FW', 'النصر', 'البرتغال'))] },
  stealTurnId: null, stealUsed: {}, canSteal: false,
};
const auctionStealView = {
  phase: 'steal', cardIndex: 22, deckLength: 22, card: null,
  currentBid: 0, startBid: 0, remainingBidders: 0, iAmBidder: false, bidTurnId: null,
  myBudget: 300,
  myTeam: [fc(CARD('كوبا', 90, 'MF', 'برشلونة', 'إسبانيا')), fc(CARD('عبدالعزيز', 85, 'DF', 'الأهلي', 'السعودية')), fc(CARD('صالح', 83, 'GK', 'الهلال', 'السعودية'))],
  teams: {
    me: [fc(CARD('كوبا', 90, 'MF', 'برشلونة', 'إسبانيا')), fc(CARD('عبدالعزيز', 85, 'DF', 'الأهلي', 'السعودية')), fc(CARD('صالح', 83, 'GK', 'الهلال', 'السعودية'))],
    other: [fc(CARD('ميسي', 93, 'FW', 'إنتر ميامي', 'الأرجنتين')), fc(CARD('مودريتش', 88, 'MF', 'ريال مدريد', 'كرواتيا')), fc(CARD('حراس', 84, 'DF', 'باريس', 'فرنسا'))],
  },
  stealTurnId: 'me', stealUsed: { me: false, other: true }, canSteal: true,
};
const auctionResult = [
  { id: 'me', total: 358, team: [fc(CARD('كوبا', 90, 'MF', 'برشلونة', 'إسبانيا')), fc(CARD('محمد صلاح', 92, 'FW', 'ليفربول', 'مصر'))] },
  { id: 'other', total: 340, team: [fc(CARD('ميسي', 93, 'FW', 'إنتر ميامي', 'الأرجنتين')), fc(CARD('رونالدو', 91, 'FW', 'النصر', 'البرتغال'))] },
];

(async () => {
  /* ================= مود التخمين ================= */
  console.log('== مود التخمين 🎭 ==');
  const app = makeApp();
  await sleep(20);
  const { $, q, qa, sock, doc } = app;
  $('inp-name').value = 'أحمد';
  $('mode-guess').click();
  $('btn-create').click();
  const cr = sock.last('create-room');
  check('create-room اتُبعت بمود guess', cr && cr.data.mode === 'guess' && cr.data.name === 'أحمد');

  sock.fire('state', baseState('guess', {
    state: 'lobby', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  check('شاشة اللوبي رست', $('screen-lobby').classList.contains('active'));
  check('الكاتيجوري ظاهر في التخمين', !$('category-row').classList.contains('hidden'));
  check('التشكيلة مخفية في التخمين', $('squad-size-row').classList.contains('hidden'));
  check('اللعيبة اتعرضوا', qa('#lobby-players li').length === 2);

  // 🖱️ منتَقي الكاتيجوري بأزرار chips بدل الـ select (حل الرفرفة على الموبايل)
  check('الكاتيجوري بقى أزرار chips ومفيش select', !$('lobby-category') && qa('#lobby-category-picker .picker-chip').length === 2);
  check('الصف المبوب للصعوبة مخفي في الكاتيجوري المخلوط', $('difficulty-row').classList.contains('hidden'));

  // ⚽ الكورة في التخمين: صف الصعوبة يظهر + نختار سهل/صعب
  sock.fire('state', baseState('guess', {
    state: 'lobby', host: 'me', categoryId: 'sports', difficulty: 'easy',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  check('صف الصعوبة ظهر للكورة (سهل)', !$('difficulty-row').classList.contains('hidden'));
  check('أزرار الصعوبة 4 (كل/سهل/متوسط/صعب)', qa('#difficulty-picker .picker-chip').length === 4);
  check('الاختيار الحالي «سهل» متظلل', q('#difficulty-picker .picker-chip.active').textContent.includes('سهل'));
  const hardChip = qa('#difficulty-picker .picker-chip').find((b) => b.textContent.includes('صعب'));
  hardChip.click();
  const fd = sock.last('set-difficulty');
  check('الضغط على «صعب» بيبعت set-difficulty', fd && fd.data && fd.data.difficulty === 'hard');
  const sportsChip = qa('#lobby-category-picker .picker-chip').find((b) => b.textContent.includes('الكل'));
  sportsChip.click();
  const fsc = sock.last('set-category');
  check('الضغط على «الكل» بيبعت set-category (مخلوط)', fsc && fsc.data && fsc.data.categoryId === 'mix');

  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other', winnerId: null,
    players: [Object.assign(P('me', 'أحمد', true), { char: { name: 'ميسي', emoji: '🐐', isSpy: false } }), P('other', 'سارة', false)],
  }));
  check('شاشة اللعب فتحت للتخمين', $('screen-game').classList.contains('active'));
  check('بطاقة شخصيتي اتعرضت', $('my-char-name').textContent === 'ميسي');

  // 🤳 لاعب كرة قدم: صورة + بروفايل كامل (بطولات/فريق/انتقالات)
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other', winnerId: null,
    players: [Object.assign(P('me', 'أحمد', true), { char: { name: 'ميسي', emoji: '🐐', isSpy: false, img: 'leo.jpg', profile: { r: 90, pos: 'FW', p: 'مهاجم', t: 'إنتر ميامي', c: 'الأرجنتين', num: 10, v: 70, champs: ['كأس العالم 2022'], ach: ['الكرة الذهبية ×8'], prev: [{ t: 'برشلونة', y: '2004–2021' }] } } }), P('other', 'سارة', false)],
  }));
  check('صورة اللاعب ظهرت مكان الإيموجي', !$('my-char-img').classList.contains('hidden') && $('my-char-img').src.includes('leo.jpg') && $('my-char-emoji').classList.contains('hidden'));
  check('بروفايل اللاعب ظهر (بطولات+نادي+انتقالات)', $('my-char-info').textContent.includes('كأس العالم 2022') && $('my-char-info').textContent.includes('الكرة الذهبية') && $('my-char-info').textContent.includes('برشلونة') && $('my-char-info').textContent.includes('إنتر ميامي'));

  // 🦁 شخصية من غير بروفايل: وصف مختصر + إيموجي
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other', winnerId: null,
    players: [Object.assign(P('me', 'أحمد', true), { char: { name: 'أسد', emoji: '🦁', isSpy: false, bio: 'ملك الغابة' } }), P('other', 'سارة', false)],
  }));
  check('الوصف ظهر لغير اللاعبين والإيموجي باقي', !$('my-char-info').classList.contains('hidden') && $('my-char-info').textContent.includes('ملك الغابة') && !$('my-char-emoji').classList.contains('hidden'));

  // 🔁 تكرار نفس الحالة (محاكاة الـ polling؟) لا يكسر العرض — إصلاح الرفهة
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other', winnerId: null,
    players: [Object.assign(P('me', 'أحمد', true), { char: { name: 'أسد', emoji: '🦁', isSpy: false, bio: 'ملك الغابة' } }), P('other', 'سارة', false)],
  }));
  check('إعادة نفس الحالة ما كسرتش البطاقة', $('my-char-name').textContent === 'أسد' && !$('my-char-info').classList.contains('hidden'));
  check('شريط الدور بيقول سارة', $('turn-bar').textContent.includes('سارة'));
  check('زر التصويت مخفي في التخمين', $('vote-request-wrap').classList.contains('hidden'));
  $('btn-guess').click();
  check('مودال التخمين اتفتح', !$('guess-modal').classList.contains('hidden'));
  $('guess-target').value = 'other';
  $('guess-input').value = 'ميسي';
  $('guess-submit').click();
  const mg = sock.last('make-guess');
  check('make-guess اتُبعت', mg && mg.data.targetId === 'other' && mg.data.guess === 'ميسي');
  check('المودال اتقفل بعد الإرسال', $('guess-modal').classList.contains('hidden'));

  /* ================= مود الجاسوس + التصويت ================= */
  console.log('== مود الجاسوس 🕵️ + التصويت ==');
  $('mode-spy').click();
  $('btn-create').click();
  check('create-room اتُبعت بمود spy', sock.last('create-room') && sock.last('create-room').data.mode === 'spy');

  sock.fire('state', baseState('spy', {
    state: 'lobby', host: 'me', categoryId: 'sports',
    players: [P('me', 'أحمد', true), P('sara', 'سارة', false), P('omar', 'عمر', false)],
  }));
  check('عدد الجواسيس ظاهر', !$('spy-count-row').classList.contains('hidden'));
  check('التشكيلة مخفية في الجاسوس', $('squad-size-row').classList.contains('hidden'));
  $('btn-spy-2').click();
  check('set-spy-count 2 اتُبعت', sock.last('set-spy-count') && sock.last('set-spy-count').data.count === 2);
  // العميل المزدوج: تبديلة اللوبي + التفعيل
  check('تبديلة العميل المزدوج ظاهرة في اللوبي', $('btn-da') !== null);
  $('btn-da').checked = true;
  $('btn-da').dispatchEvent(new app.window.Event('change', { bubbles: true }));
  check('set-double-agent اتُبعت بتفعيل', sock.last('set-double-agent') && sock.last('set-double-agent').data.on === true);

  sock.fire('state', baseState('spy', {
    state: 'playing', host: 'me', categoryId: 'sports',
    players: [
      Object.assign(P('me', 'أحمد', true), { char: { name: 'بيتزا', emoji: '🍕', isSpy: false } }),
      Object.assign(P('sara', 'سارة', false), { char: null }),
      Object.assign(P('omar', 'عمر', false), { char: null }),
    ],
  }));
  check('الكلمة سرية ظاهرة (مدني)', $('my-char-name').textContent === 'بيتزا');
  // أنا عميل مزدوج: شايف الكلمة لكن بشارة مميزة
  sock.fire('state', baseState('spy', {
    state: 'playing', host: 'me', categoryId: 'sports',
    players: [
      Object.assign(P('me', 'أحمد', true), { char: { name: 'بيتزا', emoji: '🍕', isSpy: false, isDoubleAgent: true } }),
      Object.assign(P('sara', 'سارة', false), { char: null }),
      Object.assign(P('omar', 'عمر', false), { char: null }),
    ],
  }));
  check('العميل المزدوج شايف الكلمة الطبيعة', $('my-char-name').textContent === 'بيتزا');
  check('العميل المزدوج بيشوف إشعار «عميل مزدوج»', $('secret-label').textContent.includes('العميل المزدوج'));
  check('زر التصويت ظاهر لصاحب الأوضة', !$('vote-request-wrap').classList.contains('hidden'));
  $('btn-request-vote').click();
  check('request-vote اتُبعت', sock.last('request-vote') !== null);

  // طلب التصويت شغال — أنا صاحب الأوضة فمينفعش أصوّت، بس النافذة بتقطع للأجيال
  sock.fire('state', baseState('spy', {
    state: 'playing', host: 'me', categoryId: 'sports',
    votePrompt: true, voteResponded: 2, voteTotal: 3,
    players: [Object.assign(P('me', 'أحمد', true), { char: null }), P('sara', 'سارة', false), P('omar', 'عمر', false)],
  }));
  check('عريضة التصويت ظهرت بصاحب الأوضة (يستنى)', !$('vote-prompt-box').classList.contains('hidden'));
  check('العدد بيقول 2 من 3', $('vote-prompt-count').textContent.includes('2 من 3'));
  check('زر «تصويت الآن» مختفي عن صاحب الأوضة', $('btn-vote-now').classList.contains('hidden'));

  // فتح التصويت بالأغلبية
  sock.fire('state', baseState('spy', {
    state: 'playing', host: 'me', categoryId: 'sports',
    voteOpen: true, voteTargets: ['sara', 'omar'], voteCast: 0, voteTotal: 3, myVote: null,
    players: [P('me', 'أحمد', true), Object.assign(P('sara', 'سارة', false), { char: null }), Object.assign(P('omar', 'عمر', false), { char: null })],
  }));
  check('مودال التصويت اتفتح', !$('vote-modal').classList.contains('hidden'));
  check('الهدفان اتعرضوا', $('vote-modal-targets').children.length === 2);
  qa('.vote-target-btn')[0].click();
  const vc = sock.last('vote-cast');
  check('vote-cast اتُبعت', vc && vc.data.targetId === 'sara');

  // نهاية الجاسوس بفوز الجواسيس (جاسوسي هو المحدد بالدور)
  sock.fire('state', baseState('spy', {
    state: 'over', host: 'me', spiesWon: true, categoryId: 'sports',
    word: { name: 'بيتزا', emoji: '🍕' },
    players: [
      Object.assign(P('me', 'أحمد', true), { char: { name: 'جاسوس', emoji: '🕵️', isSpy: true } }),
      Object.assign(P('sara', 'سارة', false), { char: { name: 'بيتزا', emoji: '🍕', isSpy: false } }),
      Object.assign(P('omar', 'عمر', false), { char: { name: 'بيتزا', emoji: '🍕', isSpy: false } }),
    ],
    points: { me: 3, sara: 1, omar: 1 },
  }));
  check('جاسوس كسب — الرسالة', $('winner-text').textContent.includes('الجواسيس كسبوا'));
  check('صف الجاسوس مميز', q('.reveal-row.spy-row') !== null);
  check('الكلمة انكشفت', $('spy-word-reveal').textContent.includes('بيتزا'));
  check('لوحة النقاط رست', !$('over-score').classList.contains('hidden'));
  check('أنا جاي الأول (3 نقاط)', $('over-scoreboard-list').children[0].textContent.includes('أحمد'));

  /* ================= نهاية التخمين: الكشف بالصور ================= */
  sock.fire('state', baseState('guess', {
    state: 'over', host: 'me', winnerId: 'me', categoryId: 'sports',
    players: [
      Object.assign(P('me', 'أحمد', true), { char: { name: 'ميسي', emoji: '🐐', isSpy: false, img: 'leo.jpg' } }),
      Object.assign(P('sara', 'سارة', false), { char: { name: 'رونالدو', emoji: '👑', isSpy: false, img: 'cristiano.jpg' } }),
    ],
    points: { me: 3, sara: 1 },
  }));
  check('صفوف الكشف في التخمين عرضت صور اللاعبين', qa('#reveal-list .reveal-img').length === 2);

  /* ================= مود المزاد ================= */
  console.log('== مود المزاد 🔨 ==');
  $('mode-auction').click();
  $('btn-create').click();
  check('create-room اتُبعت بمود auction', sock.last('create-room') && sock.last('create-room').data.mode === 'auction');

  sock.fire('state', baseState('auction', {
    state: 'lobby', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'مستثمر', false)],
    squadSize: 11,
  }));
  check('التشكيلة ظاهرة في المزاد', !$('squad-size-row').classList.contains('hidden'));
  check('الكاتيجوري مخفية في المزاد', $('category-row').classList.contains('hidden'));
  check('زر 11 ظاهر كمتاح', !$('btn-squad-11').disabled);
  $('btn-squad-5').click();
  check('set-squad-size 5 اتُبعت', sock.last('set-squad-size') && sock.last('set-squad-size').data.size === 5);

  // بداية المزاد (دوري)
  sock.fire('state', baseState('auction', {
    state: 'playing', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'مستثمر', false)],
    auction: auctionView,
  }));
  check('شاشة المزاد فتحت', !$('auction-wrap').classList.contains('hidden'));
  check('بطاقة فيفا فيها التقييم 92', $('auct-card-rating').textContent === '92');
  check('اسم اللاعب = محمد صلاح', $('auct-card-name').textContent === 'محمد صلاح');
  check('المركز المكتوب فيه مهاجم', $('auct-card-pos').textContent.includes('مهاجم'));
  check('رتبة أسطورة (90+)', $('auct-card').classList.contains('r-legend'));
  check('سعر البداية e.tعرض', $('auct-card-value').textContent.includes('55'));
  check('زر الشراء بيقول 58 مليون', $('auct-buy').textContent.includes('58'));
  check('الميزانية 392', $('auct-budget').textContent.includes('392'));
  check('دوري — شريط الحالة مميز', $('auct-status').classList.contains('mine'));
  check('تشكيلتي فيها فيرمينو', $('auct-my-team').textContent.includes('فيرمينو'));
  check('مجموع الفريق ظاهر', $('auct-my-total').textContent.includes('88'));
  check('المنافس برونالدو ظاهر', $('auct-teams').textContent.includes('رونالدو'));
  $('auct-buy').click();
  check('auction-act buy اتُبعت', sock.last('auction-act') && sock.last('auction-act').data.action === 'buy');

  // مش دوري → الأزرار معطّلة
  sock.fire('state', baseState('auction', {
    state: 'playing', host: 'me',
    players: [Object.assign(P('me', 'أحمد', true), {}), P('other', 'مستثمر', false)],
    auction: Object.assign({}, auctionView, { bidTurnId: 'other', iAmBidder: true }),
  }));
  check('مش دوري — زراير معطّلة', $('auct-buy').disabled && $('auct-r1').disabled);
  check('الحالة بتقول «الدور على»', $('auct-status').textContent.includes('الدور على'));

  // دوري تاني — نزود +5
  sock.fire('state', baseState('auction', {
    state: 'playing', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'مستثمر', false)],
    auction: Object.assign({}, auctionView, { bidTurnId: 'me', currentBid: 63 }),
  }));
  $('auct-r5').click();
  const ra = sock.last('auction-act');
  check('auction-act raise 5 اتُبعت', ra && ra.data.action === 'raise' && ra.data.amount === 5);

  // مود الخطف (دوري)
  sock.fire('state', baseState('auction', {
    state: 'playing', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'مستثمر', false)],
    auction: auctionStealView,
  }));
  check('مرحلة الخطف ظاهرة', !$('auct-steal-wrap').classList.contains('hidden'));
  check('شريط التقدم بيقول مرحلة الخطف', $('auct-progress').textContent.includes('مرحلة الخطف'));
  const targetCard = q('.auct-steal-targets .steal-target-card');
  check('كروت الخصم بتبان', targetCard !== null);
  targetCard.click();
  check('زر تأكيد الخطف اتظهر', !$('auct-steal-confirm').classList.contains('hidden'));
  $('auct-steal-confirm').click();
  const sa = sock.last('steal-act');
  check('steal-act اتُبعت', sa && sa.data.steal === true && sa.data.targetId === 'other');

  // نهاية المزاد
  sock.fire('state', baseState('auction', {
    state: 'over', host: 'me', spiesWon: null, winnerId: 'me',
    players: [P('me', 'أحمد', true), P('other', 'مستثمر', false)],
    auction: null,
    auctionResult,
    points: { me: 3, other: 1 },
  }));
  check('مودال النهاية للمزاد اتفتح', !$('over-modal').classList.contains('hidden'));
  check('الفايز مكتوب (إنت كسبت)', $('winner-text').textContent.includes('كسبت'));
  check('نتيجة المزاد فيها تشكيلتين', qa('#auction-result-list .auction-result-row').length === 2);
  check('كروت النتيجة اتعرضت (4 كروت)', qa('#auction-result-list .mini-card').length === 4);
  check('تفاصيل اللاعبين ظاهرة في النتيجة', $('auction-result-list').textContent.includes('محمد صلاح'));
  check('مجموع الفايز 358 اتكتب', $('auction-result-list').textContent.includes('358'));
  $('btn-next-round').click();
  check('next-round اتُبعت', sock.last('next-round') !== null);

  /* ================= مود مين الكذاب؟ ================= */
  console.log('== مود مين الكذاب؟ 🤥 ==');
  const liarApp = makeApp();
  await sleep(20);
  const { $: L$, q: Lq, qa: Lqa, sock: Lsock } = liarApp;
  L$('mode-liar').click();
  check('زر المود الجديد ظاهر وشغال', L$('mode-liar').classList.contains('active'));
  check('التلميح بتاع مين الكذاب ظهر', L$('mode-hint').textContent.includes('سؤال مختلف'));

  const liarState = (phase, extra) => Object.assign(baseState('liar', {
    state: 'playing', host: 'me',
    players: [P('me', 'أحمد', true), P('p2', 'سارة', false), P('p3', 'مينا', false)],
    liar: Object.assign({
      phase, question: 'إيه أكتر أكلة بتحبها؟',
      myAnswer: null, iAmLiar: false,
      answeredCount: 0, total: 3,
      answers: [], targets: null, myVote: null, votesCast: 0,
      reveal: null,
    }, extra),
  }), { turnId: null });

  // مرحلة الإجابة
  Lsock.fire('state', liarState('answer'));
  check('شاشة اللعب رست للمود', L$('screen-game').classList.contains('active'));
  check('السؤال ظاهر', L$('liar-question').textContent.includes('أكتر أكلة'));
  check('حقل الإجابة ظاهر', !L$('liar-answer-zone').classList.contains('hidden'));
  check('خانة الاعتراف مخفية للمدني', L$('liar-flag').classList.contains('hidden'));
  L$('liar-answer-input').value = 'بحب الكشري';
  L$('liar-answer-send').click();
  const la = Lsock.last('liar-answer');
  check('liar-answer اتُبعت بالنص', la && la.data.text === 'بحب الكشري');

  // إني الكذاب: أرفض الشارة
  Lsock.fire('state', liarState('answer', { iAmLiar: true, question: 'إيه أكتر أكلة بتكرهها؟' }));
  check('شارة أنا الكذاب ظهرت', !L$('liar-flag').classList.contains('hidden'));
  check('الكذاب شايف السؤال البديل', L$('liar-question').textContent.includes('بتكرهها'));

  // مرحلة الكشف
  Lsock.fire('state', liarState('reveal', {
    answers: [
      { id: 'me', name: 'أحمد', text: 'بحب الكشري', isMine: true },
      { id: 'p2', name: 'سارة', text: 'بحب الشاورما', isMine: false },
      { id: 'p3', name: 'مينا', text: 'أنا غريب شوية', isMine: false },
    ],
  }));
  check('الإجابات اتعرضت (3)', Lqa('#liar-answers-list .liar-answer-row').length === 3);
  check('زر ابدأ التصويت ظاهر للهوست', !L$('liar-start-vote').classList.contains('hidden'));
  L$('liar-start-vote').click();
  check('liar-start-vote اتُبعت', Lsock.last('liar-start-vote') !== null);

  // مرحلة التصويت
  Lsock.fire('state', liarState('vote', {
    myVote: null, votesCast: 1,
    targets: [{ id: 'p2', name: 'سارة' }, { id: 'p3', name: 'مينا' }],
  }));
  check('أزرار التصويت ظهرت (2 — بدون نفسي)', Lqa('#liar-vote-targets .vote-target-btn').length === 2);
  Lqa('#liar-vote-targets .vote-target-btn')[0].click();
  const lv = Lsock.last('liar-cast-vote');
  check('liar-cast-vote اتُبعت بالهدف', lv && lv.data.targetId === 'p2');

  // النهاية: اتقبض الكذاب
  Lsock.fire('state', baseState('liar', {
    state: 'over', host: 'me', winnerId: null, points: { me: 3, p2: 3, p3: 1 },
    players: [P('me', 'أحمد', true), P('p2', 'سارة', false), P('p3', 'مينا', false)],
    liar: {
      phase: 'over',
      myAnswer: 'بحب الكشري', iAmLiar: false,
      reveal: { question: 'إيه أكتر أكلة بتحبها؟', liarQuestion: 'إيه أكتر أكلة بتكرهها؟', liarId: 'p3', liarName: 'مينا', liarAnswer: 'أنا غريب شوية', caught: true },
    },
  }));
  check('مودال النهاية اتفتح', !L$('over-modal').classList.contains('hidden'));
  check('اسم الكذاب ظهر في الكشف', L$('liar-reveal-name').textContent.includes('مينا'));
  check('السؤال البديل ظهر في الكشف', L$('liar-reveal-q').textContent.includes('أكتر أكلة بتحبها'));

  /* ================= الشات ================= */
  console.log('== الشات 💬 ==');
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'me',
    players: [Object.assign(P('me', 'أحمد', true), { char: { name: 'ميسي', emoji: '🐐', isSpy: false } }), P('other', 'سارة', false)],
    chat: [
      { from: 'النظام', text: 'بدأت الجولة', system: true },
      { from: 'سارة', text: 'أنا أنا', system: false },
    ],
  }));
  check('الشات النظامي رست', q('.chat-msg.system') !== null && q('.chat-msg.system').textContent.includes('بدأت الجولة'));
  $('chat-input').value = 'سلام عليكم';
  $('chat-send').click();
  check('chat اتُبعت', sock.last('chat') && sock.last('chat').data.text === 'سلام عليكم');

  /* ================= ميديا في الشات (Feature 2) ================= */
  console.log('== ميديا في الشات 📷🎤🎬 ==');
  const win2 = app.window;
  check('أزرار الميديا موجودة', $('btn-chat-img') !== null && $('btn-chat-video') !== null && $('btn-chat-voice') !== null);
  const playingChat = (chat) => baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
    chat,
  });
  // ريندر رسالة صورة
  sock.fire('state', playingChat([{ from: 'سارة', text: '', media: { type: 'image', data: 'data:image/png;base64,AAAA' } }]));
  check('رسالة صورة ظهرت في الشات', q('.chat-img') !== null);
  // معاينة الصورة الكبيرة
  const imgEl = q('.chat-img');
  if (imgEl) imgEl.dispatchEvent(new win2.Event('click', { bubbles: true }));
  check('الضغط على الصورة بيفتح المعاينة', !$('media-viewer').classList.contains('hidden'));
  // ريندر رسالة صوت وفيديو
  sock.fire('state', playingChat([
    { from: 'سارة', text: '', media: { type: 'audio', data: 'data:audio/webm;base64,AAAA' } },
    { from: 'سارة', text: '', media: { type: 'video', data: 'data:video/mp4;base64,AAAA' } },
  ]));
  check('رسالة صوت ظهرت', q('audio') !== null);
  check('رسالة فيديو ظهرت', q('video') !== null);
  // إرسال صورة عبر ملف
  const fakeF = new win2.File(['x'], 'a.png', { type: 'image/png' });
  Object.defineProperty($('chat-file-img'), 'files', { value: [fakeF], configurable: true });
  $('chat-file-img').dispatchEvent(new win2.Event('change', { bubbles: true }));
  await sleep(450);
  const mediaEmit = sock.last('chat-media');
  check('chat-media اتُبعت بملف الصورة', !!(mediaEmit && mediaEmit.data && mediaEmit.data.media && mediaEmit.data.media.type === 'image'));
  // ملف كبير: FileReader حقيقي يعطينا dataURL طويل من غير صورة — جهّز ملف نصي كبير
  const bigFile = new win2.File([new win2.Blob(['A'.repeat(800000)])], 'big.txt', { type: 'text/plain' });
  const beforeLen = sock.emitted.filter((e) => e.ev === 'chat-media').length;
  Object.defineProperty($('chat-file-img'), 'files', { value: [bigFile], configurable: true });
  $('chat-file-img').dispatchEvent(new win2.Event('change', { bubbles: true }));
  await sleep(450);
  check('الملف الكبير مبعوتش (رفض الحجم)', sock.emitted.filter((e) => e.ev === 'chat-media').length === beforeLen);

  /* ================= الإعدادات (Settings) ⚙️ ================= */
  console.log('== الإعدادات ⚙️ ==');
  const { window: win } = app;
  check('زر الإعدادات ظاهر', $('btn-settings') !== null);
  $('btn-settings').click();
  check('مودال الإعدادات اتفتح', !$('settings-modal').classList.contains('hidden'));
  check('افتراضيات: الصوت شغال', $('set-sound').checked);
  check('افتراضيات: داكن مقفول', !$('set-dark').checked);
  // تفعيل الداكن
  $('set-dark').click();
  check('الداكن اتطبق على الـ body', win.document.body.classList.contains('dark'));
  check('الإعداد اتحفظ في localStorage', JSON.parse(win.localStorage.getItem('khammeni_settings')).dark === true);
  // إيقاف الأنيميشن
  $('set-anim').click();
  check('إيقاف الأنيميشن بيضيف no-anim', win.document.body.classList.contains('no-anim'));
  // حجم الخط
  $('set-font').value = '125';
  $('set-font').dispatchEvent(new win.Event('input', { bubbles: true }));
  check('حجم الخط اتغير (125%)', win.document.getElementById('set-font-val').textContent === '125%');
  check('المتغير --font-scale اتظبط', win.document.documentElement.style.getPropertyValue('--font-scale') === '1.25');
  // اللغة الإنجليزية
  $('set-lang').value = 'en';
  $('set-lang').dispatchEvent(new win.Event('change', { bubbles: true }));
  check('اللغة الإنجليزية بتغير العنوان', $('app-title').textContent.includes('Kart Faka'));
  check('اتجاه LTR اتطبق', win.document.documentElement.getAttribute('dir') === 'ltr');
  /* ================= Sound Design 🎧 ================= */
  console.log('== Sound Design 🎧 ==');
  // انتقالات الحالة لازم تكون آمنة من غير AudioContext في jsdom (الأصوات تتجاهل بهدوء)
  let sfxCrash = false;
  try {
    sock.fire('state', baseState('guess', {
      state: 'playing', host: 'me', turnId: 'me',
      players: [Object.assign(P('me', 'أحمد', true), { char: { name: 'ميسي', emoji: '🐐', isSpy: false } }), P('other', 'سارة', false)],
      chat: [{ from: 'سارة', text: 'يا هلا', system: false }],
    }));
    sock.fire('state', baseState('guess', {
      state: 'over', host: 'me', winnerId: 'me', points: { me: 3 },
      players: [P('me', 'أحمد', true), P('other', 'سارة', true)],
    }));
  } catch (e) { sfxCrash = true; }
  check('إطلاق الأصوات مع الحالة ما يرميش خطأ', !sfxCrash);
  check('الحالة اتحدثت بعد الأصوات (over)', $('over-modal') !== null);
  // إيقاف الصوت من الإعدادات — الأزرار لسه بتشتغل عادي
  $('btn-settings').click();
  $('set-sound').checked = false;
  $('set-sound').dispatchEvent(new win.Event('change', { bubbles: true }));
  $('settings-close').click();
  check('إيقاف الصوت اتحفظ', JSON.parse(win.localStorage.getItem('khammeni_settings')).sound === false);
  check('المودال اتقفل بعد إعدادات الصوت', $('settings-modal').classList.contains('hidden'));

  /* ================= الإشعارات داخل التطبيق 🔔 ================= */
  console.log('== الإشعارات داخل التطبيق 🔔 ==');
  check('شريط الإشعارات موجود', $('notif-banner') !== null);
  // تصفير الانتقالات: حالة لوبي هادية الأول
  sock.fire('state', baseState('guess', {
    state: 'lobby', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  // بداية جولة = إشعار
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  check('إشعار بداية اللعبة ظهر', !$('notif-banner').classList.contains('hidden') && $('notif-msg').textContent.includes('بدأت'));
  // دوري = إشعار
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'me',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  check('إشعار «دورك» ظهر', $('notif-msg').textContent.includes('دورك'));
  // صدارة جديدة
  sock.fire('state', baseState('guess', {
    state: 'playing', host: 'me', turnId: 'other', points: { me: 1, other: 3 },
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  check('إشعار الصدارة الجديدة ظهر', $('notif-msg').textContent.includes('الصدارة'));
  // قفل بالإيد
  $('notif-close').click();
  check('قفل الإشعار بالإيد', $('notif-banner').classList.contains('hidden'));

  /* ================= السجل الدائم 🏆 ================= */
  console.log('== السجل الدائم 🏆 ==');
  check('صندوق السجل موجود في الرئيسية', $('rec-points') !== null);
  // تصفير السجل (الفحوصات السابقة أطلقت حالات over كتير)
  win.localStorage.removeItem('khammeni_record');
  sock.fire('state', baseState('lobby', { state: 'lobby', host: 'me', players: [] }));
  // نهاية جولة: نقط + فوز
  sock.fire('state', baseState('guess', {
    state: 'over', host: 'me', winnerId: 'me', points: { me: 3, other: 1 },
    players: [Object.assign(P('me', 'أحمد', true), { eliminated: false }), P('other', 'سارة', false)],
  }));
  check('النقاط اتسجلت في السجل', $('rec-points').textContent === '3');
  check('الفوز اتسجل', $('rec-wins').textContent === '1');
  check('الجولة اتحسبت', $('rec-games').textContent === '1');
  // جولة تانية بنفس الاسم: تراكم
  sock.fire('state', baseState('guess', {
    state: 'over', host: 'me', winnerId: 'other', points: { me: 0, other: 2 },
    players: [Object.assign(P('me', 'أحمد', true), { eliminated: false }), P('other', 'سارة', false)],
  }));
  check('النقاط اتراكمت (3+0)', $('rec-points').textContent === '3');
  check('الجولات اتراكمت (1+1)', $('rec-games').textContent === '2');

  /* ================= رمز QR للانضمام (Feature 8) ================= */
  console.log('== رمز QR 🧾 ==');
  check('زر اعرض QR موجود', $('btn-show-qr') !== null);
  check('مودال QR موجود', $('qr-modal') !== null);
  check('مودال QR مخفي في البداية', $('qr-modal').classList.contains('hidden'));
  // حقن مولّد QR وهمي (المكتبة الحقيقية ملف منفصل مش داخل ui-smoke)
  win.qrcode = () => {
    const n = 21;
    return {
      addData: () => {},
      make: () => {},
      getModuleCount: () => n,
      isDark: (r, c) => (r + c) % 2 === 0,
    };
  };
  // حالة لوبي بمضيف عندي hostIP عشان الرابط يظهر
  sock.fire('state', baseState('guess', {
    state: 'lobby', host: 'me',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
    hostIP: '192.168.1.50',
  }));
  check('صندوق الرابط ظهر للمضيف', !$('host-ip-box').classList.contains('hidden'));
  check('الرابط فيه عنوان المضيف', String($('host-ip').textContent).includes('192.168.1.50'));
  // فتح المودال يرسم QR (جوه المتصفح الحقيقي؛ jsdom من غير دعم canvas)
  $('btn-show-qr').click();
  check('مودال QR اتفتح من غير أخطاء', !$('qr-modal').classList.contains('hidden'));
  check('الرابط اتكتب جوه المودال', String($('qr-link').textContent).includes('192.168.1.50'));
  const canv = $('qr-canvas');
  const ctx2 = canv.getContext ? canv.getContext('2d') : null;
  let pixelsOk = true;
  if (ctx2) {
    try { const px = ctx2.getImageData(0, 0, 1, 1).data; pixelsOk = px && px.length >= 4; }
    catch (e) { pixelsOk = false; }
  }
  check('الكانفس جاهز للرسم (أو canvas غير مدعوم في الـ test env)', !canv || pixelsOk);
  // قفل المودال
  $('qr-close').click();
  check('مودال QR اتقفل', $('qr-modal').classList.contains('hidden'));
  delete win.qrcode;
  // لو مش مضيف: السيرفر مابيبعتش hostIP للكل — الصندوق مخفي
  sock.fire('state', baseState('guess', {
    state: 'lobby', host: 'me', myId: 'other',
    players: [P('me', 'أحمد', true), P('other', 'سارة', false)],
  }));
  check('صندوق الرابط مخفي لغير المضيف', $('host-ip-box').classList.contains('hidden'));

  console.log(`\nالنتيجة: ${ok} نجحت، ${fail} فشلت`);
  process.exit(fail ? 1 : 0);
})().catch((e) => { console.error('CRASH:', e); process.exit(1); });