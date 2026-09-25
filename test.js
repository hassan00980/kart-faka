/* اختبار سريع: يحاكي اللاعبين على نفس السيرفر
   يغطي: مود التخمين (عادي + كاتيجوري) + مود الجاسوس (صح وغلط) */
const { io } = require('socket.io-client');
const { spawn } = require('child_process');
const http = require('http');

const PORT = 3999;
const URL = `http://localhost:${PORT}`;

function client() {
  const c = {
    socket: io(URL, { transports: ['websocket'], reconnection: false }),
    latest: null,
    waiters: [],
  };
  c.socket.on('state', (s) => {
    c.latest = s;
    c.waiters = c.waiters.filter((w) => {
      if (w.pred(s)) { w.resolve(s); return false; }
      return true;
    });
  });
  c.state = () => c.latest;
  c.waitFor = (pred, ms = 5000) => {
    if (c.latest && pred(c.latest)) return Promise.resolve(c.latest);
    return new Promise((resolve, reject) => {
      const w = { pred, resolve };
      c.waiters.push(w);
      setTimeout(() => {
        c.waiters = c.waiters.filter((x) => x !== w);
        reject(new Error('مستنيش الحالة (timeout)'));
      }, ms);
    });
  };
  c.emit = (ev, data) => new Promise((res) => {
    const d = data || {};
    if ((ev === 'create-room' || ev === 'join-room') && d.v === undefined) d.v = 6;
    c.socket.emit(ev, d, (r) => res(r));
  });
  c.close = () => c.socket.close();
  return c;
}

let ok = 0, fail = 0;
function check(name, cond) {
  if (cond) { ok++; console.log(`  ✅ ${name}`); }
  else { fail++; console.log(`  ❌ ${name}`); }
}

(async () => {
  const server = spawn(process.execPath, ['server.js'], {
    env: { ...process.env, PORT: String(PORT) },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  const started = await new Promise((resolve) => {
    let tries = 0;
    const tryOnce = () => {
      const req = http.get(`${URL}/`, (r) => { r.resume(); resolve(true); });
      req.on('error', () => {
        tries++;
        if (tries > 30) resolve(false);
        else setTimeout(tryOnce, 300);
      });
    };
    tryOnce();
  });
  check('السيرفر اشتغل', started);
  if (!started) { server.kill(); process.exit(1); }

  try {
    /* ===================== مود التخمين: الأساسيات ===================== */
    const host = client();
    const res1 = await host.emit('create-room', { name: 'أحمد' });
    const roomCode = res1.code;
    check('إنشاء أوضة تخمين', !!roomCode && res1.mode === 'guess');

    // ميزة الأونلاين: لينك المشاركة العام + IP الشبكة المحلية + كود أمان أطول
    await host.waitFor((s) => s && !!s.hostURL && !!s.hostIP, 3000);
    check('كود الأوضة 5 خانات (أمان الأونلاين)', roomCode.length === 5);
    check('صاحب الأوضة شايف لينك اللعب (hostURL)', host.state().hostURL === URL);
    check('صاحب الأوضة شايف IP الشبكة المحلية (hostIP)', /^\d{1,3}(\.\d{1,3}){3}$/.test(host.state().hostIP || ''));

    const p2 = client();
    const p3 = client();
    const j2 = await p2.emit('join-room', { name: 'محمد', code: roomCode });
    const j3 = await p3.emit('join-room', { name: 'سارة', code: roomCode });
    check('انضمام محمد', j2.ok);
    check('انضمام سارة', j3.ok);

    // اختيار كاتيجوري موحّدة (أكلات)
    const setCat = await host.emit('set-category', { categoryId: 'foods' });
    check('تحديد كاتيجوري الأكلات', setCat.ok);
    await host.waitFor((s) => s.categoryId === 'foods');
    check('الكتاجوري اتبعت للكل', host.state().categories.some((c) => c.id === 'foods'));

    const startRes = await host.emit('start-game');
    check('بدء اللعب', startRes.ok);
    await host.waitFor((s) => s.state === 'playing');

    // كل اللاعبين ليهم شخصيات من نفس الكاتيجوري (أكلات)
    // نستنى كل واحد توصله شخصيته الأول
    await Promise.all([host, p2, p3].map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const foods = ['بطيخ', 'مانجو', 'كشري', 'شاورما', 'ذرة مشوي', 'فراولة', 'فول وفلافل', 'بيتزا', 'كنافة', 'بقلاوة',
      'أم علي', 'عصير قصب', 'ملوخية', 'محشي', 'كفتة', 'تمر', 'جبنة', 'عيش بلدي', 'فسيخ', 'سلطة'];
    const allChars = [host, p2, p3].map((c) => {
      const me = c.state().players.find((p) => p.isMe);
      return me && me.char ? me.char.name : null;
    });
    check('كل الأسماء من كاتيجوري الأكلات', allChars.every((n) => foods.includes(n)));
    check('الأسماء متكررة مش مكررة', new Set(allChars).size === allChars.length);

    // الدور والشات والتخمين الصح
    const hostState = host.state();
    check('لقطة الحالة فيها تيرن', !!hostState.turnId);

    host.emit('chat', { text: 'هل أنت لاعب كرة قدم؟' });
    await host.waitFor((s) => s.chat.some((m) => m.text.includes('كرة قدم')));
    check('الشات وصل', true);

    // ميديا في الشات (Feature 2): صورة/صوت/فيديو DataURL
    const imgData = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
    await host.emit('chat-media', { media: { type: 'image', data: imgData } });
    await host.waitFor((s) => s.chat.some((m) => m.media && m.media.type === 'image'));
    check('صورة في الشات وصلت', true);
    const imgMsg = host.state().chat.find((m) => m.media && m.media.type === 'image');
    check('رسالة الصورة فيها الـ DataURL', !!imgMsg.media.data && String(imgMsg.media.data).startsWith('data:image'));
    // ميديا كبيرة/غير صالحة تُرفض (لكن ack موجود دائمًا)
    const capMsg = await host.emit('chat-media', { media: { type: 'image', data: 'data:image/png;base64,' + 'A'.repeat(800000) } });
    check('ملف كبير بيترفض بالـ ack', capMsg && capMsg.ok === false);
    const badMsg = await host.emit('chat-media', { media: { type: 'doc', data: 'data:x' } });
    check('نوع غير مدعوم بيترفض', badMsg && badMsg.ok === false);

    const turnInfo = host.state();
    const turner = [host, p2, p3].find((c) => c.socket.id === turnInfo.turnId);
    check('في لاعب صحيح ليه الدور', !!turner);
    const victims = [host, p2, p3].filter((c) => c.socket.id !== turnInfo.turnId);
    const target = victims[0];
    await target.waitFor((s) => s.players.some((p) => p.isMe && p.char));
    const targetChar = target.state().players.find((p) => p.isMe).char.name;
    await turner.emit('make-guess', { targetId: target.socket.id, guess: targetChar });
    await turner.waitFor((s) => s.players.some((p) => p.id === target.socket.id && p.eliminated));
    check('تخمين صحيح خلّى الهدف يتخرج', true);

    // كاتيجوري صغيرة على عدد كبير — لازم ترفض
    // (هنا 2 ناشطين بس فمش هتتعمق، التفتيش في مود التخمين جوه)
    host.close(); p2.close(); p3.close();

    /* ===================== مود الجاسوس: صح وغلط ===================== */
    const spyHost = client();
    const spy1 = client();
    const sRes = await spyHost.emit('create-room', { name: 'محمود', mode: 'spy' });
    check('إنشاء أوضة جاسوس', !!sRes.code && sRes.mode === 'spy');
    const spyCode = sRes.code;

    await spy1.emit('join-room', { name: 'سلمى', code: spyCode });

    const setSpyCat = await spyHost.emit('set-category', { categoryId: 'foods' });
    check('تحديد كاتيجوري الجاسوس', setSpyCat.ok);
    const setCount = await spyHost.emit('set-spy-count', { count: 1 });
    check('تحديد جاسوس واحد', setCount.ok);
    await spyHost.waitFor((s) => s.spiesCount === 1 && s.state === 'lobby');

    // 2 لاعبين بس — لازم يرفض البدء
    const badStart = await spyHost.emit('start-game');
    check('الجاسوس يرفض البدء بلاعبين بس', badStart && badStart.ok === false);

    // 3 لاعبين — يبدأ
    const spy2 = client();
    await spy2.emit('join-room', { name: 'كريم', code: spyCode });
    const spy3 = client();
    await spy3.emit('join-room', { name: 'نور', code: spyCode });
    const sStart = await spyHost.emit('start-game');
    check('بدء لعبة الجاسوس بثلاثة', sStart.ok);
    await spy3.waitFor((s) => s.state === 'playing');

    // حدد مين الجاسوس (اللي char.name == جاسوس)
    const all4 = [spyHost, spy1, spy2, spy3];
    // نستنى كل واحد تشوف حالتها الأول
    await Promise.all(all4.map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const roles = [];
    for (const c of all4) {
      const st = c.state();
      const me = st.players.find((p) => p.isMe);
      roles.push({ c, name: me.name, isSpy: !!(me.char && me.char.isSpy), char: me.char ? me.char.name : null });
    }
    const spies = roles.filter((r) => r.isSpy);
    const civs = roles.filter((r) => !r.isSpy);
    check('في جاسوس واحد', spies.length === 1);
    check('الجاسوس شايف «جاسوس»', spies[0].char === 'جاسوس');
    check('المدنيين كلهم شايفين نفس الكلمة', civs.length > 0 && civs.every((r) => r.char === civs[0].char && r.char !== 'جاسوس'));

    // اتهام صح → المدنيين يكسبوا
    const accuser = civs[0].c;
    const spyTarget = spies[0].c;
    await accuser.emit('make-guess', { targetId: spyTarget.socket.id, guess: 'جاسوس' });
    await accuser.waitFor((s) => s.state === 'over');
    check('الاتهام الصح خلّى المدنيين يكسبوا', accuser.state().spiesWon === false);
    check('الكلمة انكشفت في النهاية', !!accuser.state().word && accuser.state().word.name !== 'جاسوس');
    check('شكلة الجاسوس باينة في الكشف', accuser.state().players.some((p) => p.char && p.char.isSpy));

    // جولة تانية في نفس الأوضة (4 لاعبين)
    const nr = await spyHost.emit('next-round');
    check('جولة جاسوس تانية بدأت', nr.ok);
    // نستنى رسالة "لعبة الجاسوس بدأت" الثانية (يعني الجولة الجديدة وصلت)
    await Promise.all(all4.map((c) => c.waitFor((s) =>
      s.state === 'playing' && s.chat.filter((m) => m.system).length >= 2)));

    for (const c of all4) c.close();

    /* ===================== مود الجاسوس: اتهام غلط =====================
       أوضة جديدة بـ 3 لاعبين — اتهام غلط واحد بيخلي المدنيين اتنين ضد جاسوس
       وحيد → الجواسيس يكسبوا على طول */
    const gHost = client();
    const g1 = client();
    const g2 = client();
    const gr = await gHost.emit('create-room', { name: 'عادل', mode: 'spy' });
    await g1.emit('join-room', { name: 'منى', code: gr.code });
    await g2.emit('join-room', { name: 'طارق', code: gr.code });
    await gHost.emit('set-category', { categoryId: 'foods' });
    await gHost.emit('start-game');
    const g3 = [gHost, g1, g2];
    await Promise.all(g3.map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const grow = {};
    for (const c of g3) {
      const me = c.state().players.find((p) => p.isMe);
      if (me.char.isSpy) grow.spy = c; else (grow.civs = grow.civs || []).push(c);
    }
    // مدني ياتهّم مدني تاني غلط
    await grow.civs[0].emit('make-guess', { targetId: grow.civs[1].socket.id });
    await grow.spy.waitFor((s) => s.state === 'over');
    check('الاتهام الغلط خلّى الجواسيس يكسبوا', grow.spy.state().spiesWon === true);
    check('المدني اللي اتهم غلط اتخرج', grow.civs[0].state().players.find((p) => p.isMe).eliminated === true);

    for (const c of g3) c.close();

    /* ===================== العميل المزدوج في الجاسوس 🎭 ===================== */
    const xHost = client();
    const x1 = client();
    const x2 = client();
    const xr = await xHost.emit('create-room', { name: 'فطين', mode: 'spy' });
    await x1.emit('join-room', { name: 'ريم', code: xr.code });
    await x2.emit('join-room', { name: 'يحيى', code: xr.code });
    await xHost.emit('set-category', { categoryId: 'foods' });
    const setDa = await xHost.emit('set-double-agent', { on: true });
    check('تفعيل العميل المزدوج', setDa.ok);
    await xHost.waitFor((s) => s.doubleAgent === true);
    check('الحالة بتبعت doubleAgent=true', true);
    await xHost.emit('start-game');
    const x3 = [xHost, x1, x2];
    await Promise.all(x3.map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const dw = {};
    for (const c of x3) {
      const me = c.state().players.find((p) => p.isMe);
      if (me.char.isSpy) dw.spy = c;
      else if (me.char.isDoubleAgent) dw.da = c;
      else (dw.civs = dw.civs || []).push(c);
    }
    check('في جاسوس واحد وعميل مزدوج واحد', !!dw.spy && !!dw.da && dw.civs.length === 1);
    check('العميل المزدوج شايف الكلمة (مش جاسوس)', dw.da.state().players.find((p) => p.isMe).char.name !== 'جاسوس');
    check('المدني شايف نفس الكلمة', dw.civs && dw.civs[0].state().players.find((p) => p.isMe).char.name === dw.da.state().players.find((p) => p.isMe).char.name);
    // الاتهام يقع على العميل المزدوج بالغلط → المتهم يخرج وفريق الجواسيس يكسب
    await dw.civs[0].emit('make-guess', { targetId: dw.da.socket.id });
    await dw.da.waitFor((s) => s.state === 'over');
    check('اتهام العميل المزدوج بيخلص الجولة لصالح الجواسيس', dw.da.state().spiesWon === true);
    check('المتهم اتقبض بدل العميل المزدوج', dw.civs[0].state().players.find((p) => p.isMe).eliminated === true);
    check('العميل المزدوج أخذ نقاط الفوز (+3 مع الجواسيس)', (dw.da.state().points[dw.da.socket.id] || 0) >= 3);

    for (const c of x3) c.close();

    /* ===================== المزحة: الكود الغلط ===================== */
    const joker = client();
    const badJoin = await joker.emit('join-room', { name: 'حزين', code: 'ZZZZ' });
    check('الكود الغلط مرفوض', badJoin && badJoin.ok === false);
    check('الرسالة: الكود غلط يا حيوان', badJoin && badJoin.error.includes('حيوان'));
    joker.close();

    /* ===================== Protocol Version ⚠️ ===================== */
    const oldV = client();
    const oldCreate = await oldV.emit('create-room', { name: 'قديم', v: 5 });
    check('نسخة قديمة ما تعملش أوضة', oldCreate && oldCreate.ok === false && oldCreate.error.includes('حدّث'));
    const noVer = await oldV.emit('create-room', { name: 'من غير نسخة', v: null });
    check('عميل من غير نسخة يتنرفض', noVer && noVer.ok === false && noVer.error.includes('حدّث'));
    oldV.close();
    const verOk = client();
    const okRoom = await verOk.emit('create-room', { name: 'حديث' });
    check('نسخة حديثة تدخل عادي', okRoom && okRoom.ok === true && !!okRoom.code);
    const verState = await verOk.waitFor((s) => s.version === 6);
    check('الحالة بتبعت version=6', !!verState);
    verOk.close();

    /* ===================== مود المزاد: مزايدة كاملة ===================== */
    const aHost = client();
    const a1 = client();
    const ar = await aHost.emit('create-room', { name: 'مدير المزاد', mode: 'auction' });
    check('إنشاء أوضة مزاد', !!ar.code && ar.mode === 'auction');
    const aCode = ar.code;
    await a1.emit('join-room', { name: 'مستثمر', code: aCode });

    // اختيار تشكيلة 11
    const setSize = await aHost.emit('set-squad-size', { size: 11 });
    check('تحديد تشكيلة 11', setSize.ok);
    await aHost.waitFor((s) => s.squadSize === 11 && s.state === 'lobby');

    // بدء المزاد
    const aStart = await aHost.emit('start-game');
    check('بدء المزاد بلاعبين', aStart.ok);
    await Promise.all([aHost, a1].map((c) => c.waitFor((s) => s.state === 'playing' && s.auction && s.auction.phase === 'bidding')));
    check('المزاد اتفتح والبيدنج شغال', aHost.state().auction.phase === 'bidding');
    check('الميزانية 450 لتشكيلة 11', aHost.state().auction.myBudget === 450);

    // نكمل المزايدة: اللي دوره يشتري الكارت (للتسريع)، ولو الفلوس مش كفاية يخرج
    let guard = 0;
    while (aHost.state().state === 'playing' && aHost.state().auction && aHost.state().auction.phase === 'bidding' && guard++ < 300) {
      const turnId = aHost.state().auction.bidTurnId;
      const bidder = turnId === aHost.socket.id ? aHost : a1;
      await bidder.waitFor((s) => s.auction && s.auction.bidTurnId === turnId, 3000);
      const bv = bidder.state().auction;
      const base = bidder.state().chat.length; // نقرأ قبل الإرسال (عشان سباق البث)
      const action = bv.myBudget >= bv.currentBid ? 'buy' : 'pass';
      const r = await bidder.emit('auction-act', { action });
      if (r && r.ok === false) break; // مش دور فعلي — نوقف
      try { await bidder.waitFor((s) => s.chat.length > base, 3000); } catch (e) { break; }
    }
    await aHost.waitFor((s) => s.state === 'playing' && s.auction && s.auction.phase === 'steal');

    // مرحلة الخطف
    const st1 = aHost.state();
    check('المزاد وصل لمرحلة الخطف', st1.state === 'playing' && st1.auction && st1.auction.phase === 'steal');
    check('التشكيلات اتكملت (11 كارت لكل واحد)', Object.values(st1.auction.teams).every((t) => t.length === 11));

    // أول واحد في ترتيب الخطف يخطف كارت من التاني
    const stealer = [aHost, a1].find((cc) => cc.state().auction.canSteal);
    const victim = stealer === aHost ? a1 : aHost;
    const before = JSON.parse(JSON.stringify(st1.auction.teams));
    await stealer.emit('steal-act', { steal: true, targetId: victim.socket.id, cardIndex: 0 });
    await aHost.waitFor((s) => s.auction && s.auction.stealUsed[stealer.socket.id] === true);
    // التاني يعدي
    await victim.emit('steal-act', { steal: false });
    await aHost.waitFor((s) => s.state === 'over');
    check('المزاد خلص والنتيجة ظهرت', !!aHost.state().auctionResult && aHost.state().auctionResult.length === 2);
    check('في فايز اتسجل', !!aHost.state().winnerId);
    check('الفايز خد 3 نقاط', (aHost.state().points[aHost.state().winnerId] || 0) >= 3);
    check('النتيجة مرتبة تنازليًا', aHost.state().auctionResult[0].total >= aHost.state().auctionResult[1].total);
    check('الخطف فعلاً غيّر التيم', JSON.stringify(aHost.state().auctionResult.map((r) => r.team)) !== JSON.stringify(before));

    // جولة مزاد تانية
    const a2 = await aHost.emit('next-round', { squadSize: 5 });
    check('جولة مزاد جديدة (تشكيلة 5)', a2.ok);
    await aHost.waitFor((s) => s.state === 'playing' && s.auction && s.auction.phase === 'bidding');
    check('المزاد الجديد فعلاً شغال', aHost.state().auction.phase === 'bidding' && aHost.state().auction.deckLength === 10);
    check('الميزانية بقت 200 لخماسية', aHost.state().auction.myBudget === 200);

    for (const c of [aHost, a1]) c.close();

    /* ===================== التصويت في الجاسوس: أغلبية «تصويت الآن» ===================== */
    const vHost = client();
    const v1 = client();
    const v2 = client();
    const vr = await vHost.emit('create-room', { name: 'قائد', mode: 'spy' });
    await v1.emit('join-room', { name: 'لينة', code: vr.code });
    await v2.emit('join-room', { name: 'يوسف', code: vr.code });
    await vHost.emit('set-category', { categoryId: 'foods' });
    await vHost.emit('start-game');
    const v3 = [vHost, v1, v2];
    await Promise.all(v3.map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const vroles = {};
    for (const c of v3) {
      const me = c.state().players.find((p) => p.isMe);
      if (me.char.isSpy) vroles.spy = c; else (vroles.civs = vroles.civs || []).push(c);
    }

    // صاحب الأوضة يطلب تصويت
    await vHost.emit('request-vote');
    await v1.waitFor((s) => s.votePrompt === true);
    check('طلب التصويت وصل للكل', vHost.state().votePrompt === true && v1.state().votePrompt === true);

    // مؤسس الرد: الاتنين غير صاحب الأوضة بيقولوا «تصويت الآن» → أغلبية
    const responders = v3.filter((c) => c !== vHost);
    await responders[0].emit('vote-response', { now: true });
    await responders[1].emit('vote-response', { now: true });
    await v1.waitFor((s) => s.voteOpen === true);
    check('الأغلبية فتحت نافذة التصويت', vHost.state().voteOpen === true);

    // الكل يصوت: المدنيين على الجاسوس، والجاسوس على أول مدني
    for (const cc of v3) {
      const me = cc.state().players.find((p) => p.isMe);
      if (me.char.isSpy) await cc.emit('vote-cast', { targetId: vroles.civs[0].socket.id });
      else await cc.emit('vote-cast', { targetId: vroles.spy.socket.id });
    }
    await vHost.waitFor((s) => s.state === 'over');
    check('التصويت قبض على الجاسوس', vHost.state().spiesWon === false);
    check('الكلمة انكشفت', !!vHost.state().word && vHost.state().word.name !== 'جاسوس');
    check('اللي صوتوا على الجاسوس خدوا 3 نقاط', (vHost.state().points[vroles.civs[0].socket.id] || 0) >= 3);
    check('الجاسوس ماخدش نقطة من التصويت', (vHost.state().points[vroles.spy.socket.id] || 0) === 0);

    for (const c of v3) c.close();

    /* ===================== التصويت: أغلبية «استمرار» ===================== */
    const kHost = client();
    const k1 = client();
    const k2 = client();
    const kr = await kHost.emit('create-room', { name: 'كابتن', mode: 'spy' });
    await k1.emit('join-room', { name: 'زينة', code: kr.code });
    await k2.emit('join-room', { name: 'فارس', code: kr.code });
    await kHost.emit('start-game');
    const k3 = [kHost, k1, k2];
    await Promise.all(k3.map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    await kHost.emit('request-vote');
    await k1.emit('vote-response', { now: false });
    await k2.emit('vote-response', { now: false });
    await k1.waitFor((s) => s.voteOpen === false && s.votePrompt === false);
    check('أغلبية الاستمرار سيّبت الجولة شغالة', k1.state().state === 'playing' && k1.state().voteOpen === false);
    check('رسالة الاستمرار اتكتبت في الشات', k1.state().chat.some((m) => m.text.includes('الأغلبية اختارت الاستمرار')));

    for (const c of k3) c.close();

    /* ===================== صعوبات لعيبة الكورة (التخمين) ===================== */
    const db = require('./players.json');
    const dbNames = new Set(db.players.map((p) => p.n));
    const rOf = Object.fromEntries(db.players.map((p) => [p.n, p.r]));
    const isEasy = (nm) => !dbNames.has(nm) || rOf[nm] >= 89; // الأساطير الثابتة من غير بروفايل => سهل
    const isHard = (nm) => dbNames.has(nm) && rOf[nm] <= 83;

    // — أوضة "سهل" —
    const dHost = client();
    const d1 = client();
    const dr = await dHost.emit('create-room', { name: 'كابتن', mode: 'guess' });
    await d1.emit('join-room', { name: 'ظهير', code: dr.code });
    await dHost.emit('set-category', { categoryId: 'sports' });
    await dHost.waitFor((s) => s.categoryId === 'sports');
    check('الصعوبة الافتراضية: الكل', dHost.state().difficulty === 'all');
    const badD = await dHost.emit('set-difficulty', { difficulty: 'مستحيل' });
    check('صعوبة غير معروفة متتسجلش', badD.ok === false || dHost.state().difficulty === 'all');
    await dHost.emit('set-difficulty', { difficulty: 'easy' });
    await dHost.waitFor((s) => s.difficulty === 'easy');
    check('صاحب الأوضة اختار سهل', dHost.state().difficulty === 'easy');
    const nonD = await d1.emit('set-difficulty', { difficulty: 'hard' });
    check('اللاعب العادي مش بيقدر يغير الصعوبة', !nonD.ok || dHost.state().difficulty === 'easy');
    await dHost.emit('start-game');
    await Promise.all([dHost, d1].map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const easyChars = [dHost, d1].map((c) => c.state().players.find((p) => p.isMe).char.name);
    check('سهل: كل الشخصيات من السهل فعلاً', easyChars.every(isEasy));
    check('سهل: رسالة الصعوبة في الشات', dHost.state().chat.some((m) => m.text.includes('سهل')));
    for (const c of [dHost, d1]) c.close();

    // — أوضة "صعب" —
    const hHost = client();
    const h1 = client();
    const hr = await hHost.emit('create-room', { name: 'مدرب', mode: 'guess' });
    await h1.emit('join-room', { name: 'حارس', code: hr.code });
    await hHost.emit('set-category', { categoryId: 'sports' });
    await hHost.waitFor((s) => s.categoryId === 'sports');
    await hHost.emit('set-difficulty', { difficulty: 'hard' });
    await hHost.waitFor((s) => s.difficulty === 'hard');
    check('صاحب الأوضة اختار صعب', hHost.state().difficulty === 'hard');
    await hHost.emit('start-game');
    await Promise.all([hHost, h1].map((c) => c.waitFor((s) => {
      const me = s.players.find((p) => p.isMe);
      return s.state === 'playing' && me && !!me.char;
    })));
    const hardChars = [hHost, h1].map((c) => c.state().players.find((p) => p.isMe).char.name);
    check('صعب: كل الشخصيات من الصعب فعلاً', hardChars.every(isHard));
    check('صعب: رسالة الصعوبة في الشات', hHost.state().chat.some((m) => m.text.includes('صعب')));
    for (const c of [hHost, h1]) c.close();

    // — لعبة «مين الكذاب؟» —
    const lHost = client();
    const l2 = client();
    const l3 = client();
    const lr = await lHost.emit('create-room', { name: 'كذاب1', mode: 'liar' });
    check('إنشاء أوضة مين الكذاب', lr.ok && lr.mode === 'liar');
    await l2.emit('join-room', { name: 'كذاب2', code: lr.code });
    await l3.emit('join-room', { name: 'كذاب3', code: lr.code });
    await lHost.waitFor((s) => s.players.length === 3);
    check('اللوبي شايف 3 لاعبين', lHost.state().players.length === 3);
    check('اللوبي معندوش كاتيجوري', lHost.state().mode === 'liar');

    const lStart = await lHost.emit('start-game');
    check('بدء مين الكذاب', lStart.ok);
    await Promise.all([lHost, l2, l3].map((c) => c.waitFor((s) => s.liar && s.liar.phase === 'answer' && s.state === 'playing')));

    const liarCount = [lHost, l2, l3].filter((c) => c.state().liar.iAmLiar).length;
    check('لاعب واحد بس هو الكذاب', liarCount === 1);
    const qs = new Set([lHost, l2, l3].map((c) => c.state().liar.question));
    check('الكذاب شايف سؤال مختلف', qs.size === 2);
    check('الكل جاوب 0 من 3', lHost.state().liar.answeredCount === 0);

    await lHost.emit('liar-answer', { text: 'أنا بحب الكشري' });
    await l2.emit('liar-answer', { text: 'أنا بحب الشاورما' });
    await l3.waitFor((s) => s.liar.answeredCount === 2);
    await l3.emit('liar-answer', { text: 'أنا بحب البيتزا' });
    await Promise.all([lHost, l2, l3].map((c) => c.waitFor((s) => s.liar && s.liar.phase === 'reveal')));
    check('بعد ما الكل جاوب اتكشفت الإجابات تلقائيًا', lHost.state().liar.phase === 'reveal');
    check('الإجابات ظهرت (3)', lHost.state().liar.answers.length === 3);

    // التصويت: الاتنين الصح يختاروا الكذاب
    const liarId = [lHost, l2, l3].find((c) => c.state().liar.iAmLiar).socket.id;
    const targets = [lHost, l2, l3].filter((c) => c.socket.id !== liarId);
    const liarClient = [lHost, l2, l3].find((c) => c.socket.id === liarId);
    const sv = await lHost.emit('liar-start-vote');
    check('صاحب الأوضة بدأ التصويت', sv.ok);
    await Promise.all([lHost, l2, l3].map((c) => c.waitFor((s) => s.liar && s.liar.phase === 'vote')));
    for (const c of targets) await c.emit('liar-cast-vote', { targetId: liarId });
    // الكذاب لازم يصوت كمان (على أي حد) عشان الجولة تتحل
    const fakeTarget = targets.find((c) => c.socket.id !== liarId);
    await liarClient.emit('liar-cast-vote', { targetId: fakeTarget.socket.id });
    await lHost.waitFor((s) => s.state === 'over' && s.liar && s.liar.reveal);
    check('الكذاب اتقبض بالأغلبية', lHost.state().liar.reveal.caught === true);
    check('المصوتين الصح كسبوا نقاط', lHost.state().points[targets[0].socket.id] >= 3);
    check('كشف النتيجة فيه اسم الكذاب وسؤاله', !!lHost.state().liar.reveal.liarName && !!lHost.state().liar.reveal.liarQuestion);
    for (const c of [lHost, l2, l3]) c.close();

    // — جولة كذاب تانية: الكذاب ينجو —
    const lh2 = client();
    const l22 = client();
    const l32 = client();
    const lr2 = await lh2.emit('create-room', { name: 'ناجي', mode: 'liar' });
    await l22.emit('join-room', { name: 'ناجي2', code: lr2.code });
    await l32.emit('join-room', { name: 'ناجي3', code: lr2.code });
    await lh2.emit('start-game');
    await Promise.all([lh2, l22, l32].map((c) => c.waitFor((s) => s.liar && s.liar.phase === 'answer')));
    const liar2Id = [lh2, l22, l32].find((c) => c.state().liar.iAmLiar).socket.id;
    for (const c of [lh2, l22, l32]) await c.emit('liar-answer', { text: 'إجابة عادية جدًا' });
    await Promise.all([lh2, l22, l32].map((c) => c.waitFor((s) => s.liar && s.liar.phase === 'reveal')));
    await lh2.emit('liar-start-vote');
    await Promise.all([lh2, l22, l32].map((c) => c.waitFor((s) => s.liar && s.liar.phase === 'vote')));
    // الاتنين الصح يختاروا حد غلط (مش الكذاب) — والكذاب نفسه يصوت عشان الجولة تتحل
    const innocents2 = [lh2, l22, l32].filter((c) => c.socket.id !== liar2Id);
    const victim2 = innocents2[0];
    const other2 = innocents2[1];
    const liarClient2 = [lh2, l22, l32].find((c) => c.socket.id === liar2Id);
    await victim2.emit('liar-cast-vote', { targetId: other2.socket.id });
    await other2.emit('liar-cast-vote', { targetId: victim2.socket.id });
    await liarClient2.emit('liar-cast-vote', { targetId: victim2.socket.id });
    await lh2.waitFor((s) => s.state === 'over' && s.liar && s.liar.reveal);
    check('الكذاب نجا لما اتهموا بريء', lh2.state().liar.reveal.caught === false);
    check('الكذاب كسب نقاط', lh2.state().points[liar2Id] >= 3);
    for (const c of [lh2, l22, l32]) c.close();

    console.log(`\nالنتيجة: ${ok} نجحت، ${fail} فشلت`);
    process.exit(fail ? 1 : 0);
  } catch (e) {
    console.error('خطأ في الاختبار:', e.message);
    process.exit(1);
  } finally {
    server.kill();
  }
})();