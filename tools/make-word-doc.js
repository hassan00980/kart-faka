/* يولّد ملف وورد (docx) بتوثيق كامل للتطبيق «كرت فكة» — عربي RTL */
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType,
  Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle,
} = require('docx');

const DATE = '22 سبتمبر 2026';
const OUT = path.join(__dirname, '..', 'docs', 'توثيق-التطبيق-كرت-فكة.docx');

/* ألوان التطبيق */
const C_TITLE = '1B1240';   // بنفسجي غامق
const C_GOLD = 'E4B04A';    // ذهبي
const C_TEAL = '13BFA8';    // أخضر-أزرق
const C_TEXT = '2A2350';
const C_MUTED = '6B6687';
const C_HEADBG = 'EFECF9';

/* ---------- أدوات ---------- */
const FONT = { ascii: 'Calibri', hAnsi: 'Calibri', cs: 'Calibri' };
function t(text, opts = {}) {
  return new TextRun({ text, font: FONT, size: opts.size || 21, bold: !!opts.bold, italics: !!opts.italics, color: opts.color || C_TEXT, rtl: true });
}
function p(children, opts = {}) {
  return new Paragraph({
    children: Array.isArray(children) ? children : [t(children)],
    bidirectional: true,
    alignment: opts.align || AlignmentType.RIGHT,
    spacing: { after: opts.after !== undefined ? opts.after : 140, line: 300 },
    indent: opts.indent ? { right: 240 } : undefined,
  });
}
function h1(text) {
  return new Paragraph({
    children: [t(text, { bold: true, size: 30, color: C_TITLE })],
    bidirectional: true,
    alignment: AlignmentType.RIGHT,
    spacing: { before: 360, after: 160 },
    shading: { type: ShadingType.CLEAR, fill: C_HEADBG, color: 'auto' },
  });
}
function h2(text) {
  return new Paragraph({
    children: [t(text, { bold: true, size: 24, color: C_TEAL })],
    bidirectional: true,
    alignment: AlignmentType.RIGHT,
    spacing: { before: 240, after: 120 },
  });
}
function bullet(text, depth = 0) {
  return new Paragraph({
    children: [t('• ', { color: C_TEAL, bold: true }), t(text)],
    bidirectional: true,
    alignment: AlignmentType.RIGHT,
    indent: { right: 360 + depth * 260 },
    spacing: { after: 100, line: 290 },
  });
}
function cell(text, opts = {}) {
  return new TableCell({
    width: opts.width ? { size: opts.width, type: WidthType.PERCENTAGE } : undefined,
    shading: opts.head ? { type: ShadingType.CLEAR, fill: C_TITLE, color: 'auto' } : undefined,
    children: [new Paragraph({
      children: [t(text, { bold: !!opts.head, color: opts.head ? 'FFFFFF' : C_TEXT, size: opts.head ? 20 : 20 })],
      bidirectional: true, alignment: AlignmentType.RIGHT,
      spacing: { after: 40, line: 280 },
    })],
  });
}
function table(headers, rows, widths) {
  const headRow = new TableRow({ tableHeader: true, children: headers.map((h, i) => cell(h, { head: true, width: widths[i] })) });
  const bodyRows = rows.map((r) => new TableRow({ children: r.map((c, i) => cell(c, { width: widths[i] })) }));
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [headRow, ...bodyRows],
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: 'D8D2ED' },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: 'D8D2ED' },
      left: { style: BorderStyle.SINGLE, size: 4, color: 'D8D2ED' },
      right: { style: BorderStyle.SINGLE, size: 4, color: 'D8D2ED' },
      insideHorizontal: { style: BorderStyle.SINGLE, size: 4, color: 'D8D2ED' },
      insideVertical: { style: BorderStyle.SINGLE, size: 4, color: 'D8D2ED' },
    },
  });
}
function spacer(after = 200) {
  return new Paragraph({ children: [t('')], bidirectional: true, spacing: { after }, size: 8 });
}

/* ---------- بناء الوثيقة ---------- */
const children = [];

/* === غلاف === */
children.push(new Paragraph({ children: [t('🃏🎭🕵️🔨', { size: 30 })], alignment: AlignmentType.CENTER, bidirectional: true, spacing: { after: 80 } }));
children.push(new Paragraph({
  children: [t('كرت فكة', { bold: true, size: 56, color: C_TITLE })],
  alignment: AlignmentType.CENTER, bidirectional: true, spacing: { after: 60 },
}));
children.push(new Paragraph({
  children: [t('لعبة الصالونات متعددة اللاعبين — تخمين شخصيات + جاسوس + مزاد كروت الفيفا', { size: 26, color: C_TEAL, bold: true })],
  alignment: AlignmentType.CENTER, bidirectional: true, spacing: { after: 120 },
}));
children.push(new Paragraph({
  children: [t('توثيق كامل للتطبيق وكل ما تم تنفيذه • الإصدار v5', { size: 22, color: C_MUTED })],
  alignment: AlignmentType.CENTER, bidirectional: true, spacing: { after: 40 },
}));
children.push(new Paragraph({
  children: [t(`تاريخ الإصدار: ${DATE}`, { size: 20, color: C_MUTED })],
  alignment: AlignmentType.CENTER, bidirectional: true, spacing: { after: 320 },
}));

/* === 1. نظرة عامة === */
children.push(h1('١. نظرة عامة'));
children.push(p('«كرت فكة» لعبة مجموعة أصحاب بتتلعب على الموبايلات المتصلة بنفس الشبكة (واي فاي البيت أو هوت سبوت)، من غير ما تحتاج إنترنت ولا أكونتات ولا سيرفرات خارجية. واحدة من اللاعيبة بتعمل «الأوضة»، والباقيين بينضموا من موبايلاتهم على طول، واللعبة كلها بتشتغل بينهم مباشرة.'));
children.push(bullet('منصة ويب: خادم Node.js (Express + Socket.IO) على اللابتوب، والموبايلات تفتح اللعبة من المتصفح برابط الـ IP.'));
children.push(bullet('منصة أندرويد: تطبيق APK بخادم «مدمج» جوه التطبيق نفسه (NanoHTTPD) — أول موبايل يعمل أوضة بيبقى هو السيرفر، وأي ملسوء؟ لا — أول موبايل يعمل أوضة بيبقى هو السيرفر والبقية يتصلوا بيه.'));
children.push(bullet('واجهة موحّدة: نفس الواجهة والمعرّفات على المنصتين؛ الأندرويد بيستخدم طبقة نقل HTTP (استطلاع كل 700ms) بدل Socket.IO.'));
spacer();

/* === 2. المودات === */
children.push(h1('٢. المودات الثلاثة (صاحب الأوضة يختار من الشاشة الأولى)'));
children.push(h2('🎭 تخمين شخصيات (Guess)'));
children.push(p('كل لاعب ياخد شخصية سرية من نفس الكاتيجوري اللي صاحب الأوضة اختارها، والدور بيتناوب بالأسئلة (نعم/لا) — شفهيًا أو في شات مدمج. أول ما حد يعرف شخصية خصمه يضغط «🔍 خمّن شخصية»: التخمين الصح يطرد الهدف، والغلط يطرد المخمّن. آخر واحد فاضل هو الفايز 🏆، وتبدأ جولة جديدة فورًا بشخصيات جديدة.'));
children.push(h2('🕵️ لعبة الجاسوس (Spy)'));
children.push(p('الكل بيشوف نفس الكلمة السرية، وواحد أو اتنين (حسب ما يحدد صاحب الأوضة) بيشوفوا «جاسوس» بدلها. الجواسيس بيحاولوا يندمجوا ويستخبّوا، والمدنيين بيحاولوا يكشفوهم بالأسئلة. في خاصية «🗳️ تصويت الآن»: صاحب الأوضة يطلب تصويت عاجل، والكل يقرر «الآن» أو «بعدين»، وبالأغلبية بيُفتح تصويت عام على المشبوه — اللي الأغلبية تشير عليه يتقبض: لو طلع جاسوس المدنيين يكسبوا، ولو طلع مدني اللي صوتوا عليه يتخرجوا بداله. محتاجة 3 لاعبين على الأقل.'));
children.push(h2('🔨 مزاد النجوم (Auction)'));
children.push(p('مزاد كروت كرة قدم فيفا: كل لاعب له ميزانية (200 مليون لتشكيلة 5 / 450 مليون لتشكيلة 11)، والكروت بتتعرض كارت كارت بسعر بداية، والدور بيتناوب في المزايدة أو الشراء الفوري أو العدّي. بعد المزاد بيبدأ «مرحلة الخطف»: كل واحد ليه حركة خطف واحدة ياخد بيها كارت من تشكيلة خصمه ويسيب له واحد. الفايز = أعلى مجموع تقييم للتشكيلة، وبياخد 3 نقاط.'));
children.push(h2('النقاط والجولات'));
children.push(p('النقاط مستمرة عبر الجولات وبتظهر في لوحة نقاط اللوبي وفي الشاشة، وكل مود من الثلاثة بيقدر يفتح جولة تانية فورًا بنفس اللاعبين بتكوين جديد.'));
spacer();

/* === 3. الكاتيجوريات === */
children.push(h1('٣. الكاتيجوريات'));
children.push(p('في اللوبي بيختار صاحب الأوضة الكاتيجوري من أزرار مباشرة (عشوائي مخلوط + 8 كاتيجوريات). في مود التخمين كل اللاعبين بياخدوا شخصيات من نفس الكاتيجوري — على بعضهم.'));
children.push(table(
  ['الكاتيجوري', 'المعرّف', 'المحتوى'],
  [
    ['🎲 عشوائي (مخلوط)', 'mix', 'كل الشخصيات من كل الكاتيجوريات'],
    ['⚽ الكورة والرياضة', 'sports', 'لاعبو كرة قدم (200 لاعب) + نجوم ورياضيون (ميسي، أبو تريكة، الشربيني...)'],
    ['🎤 فنانين ومطربين', 'artists', '24 فنان ومطرب (عمرو دياب، تامر حسني، محمد رمضان، إليسا...)'],
    ['🌟 مشاهير عالم', 'celebrities', '18 من مشاهير العالم والعرب'],
    ['📺 كرتون وأنمي', 'cartoons', '21 شخصية كرتون (كابتن ماجد، ون بيس...)'],
    ['🐾 حيوانات', 'animals', '18 حيوان بالأوصاف'],
    ['💼 وظائف ومهن', 'jobs', 'وظائف ومهن (طبيب، محامي، طيار...)'],
    ['🍔 أكلات', 'foods', 'أكلات مصرية وعالمية'],
    ['🏠 أشياء', 'things', 'أشياء ومفاهيم عامة'],
  ],
  [22, 14, 64],
));
spacer();

/* === 4. الصور والمعلومات (v4) === */
children.push(h1('٤. صور حقيقية حديثة + معلومات تفصيلية للشخصيات (الإصدار v4)'));
children.push(h2('الصور الحقيقية'));
children.push(bullet('247 صورة حقيقية حديثة للاعبين والمشاهير والفنانين، مخزّنة داخل التطبيق نفسه (Offline) — بتشتغل من غير إنترنت خالص.'));
children.push(bullet('197 من أصل 200 لاعب في قاعدة كرة القدم + الـ 10 رياضيين الإضافيين (أبو تريكة، تريزيجيه، أحمد رفعت، الخطيب، أبو جبل، بالوتيلي، نور الشربيني، رامي عاشور، علي فرج، إيهاب أمير) + المشاهير الـ 18 + 23/24 فنان.'));
children.push(bullet('المصدر: ويكيبيديا (صور برخصة حرة) عبر واجهة REST + بحث احتياطي بصفحات الشخصيات.'));
children.push(bullet('الوظائف والأكلات والأشياء (مفاهيم مش أشخاص): أفاتار إيموجي — مش صور حقيقية.'));
children.push(h2('بطاقة المعلومات للاعبين'));
children.push(p('بطاقة «شخصيتك السرية» بتعرض الصورة الحقيقية + بروفايل كامل للاعب: الفريق الحالي، الجنسية (مع علم)، التقييم ⭐، المركز، رقم القميص، القيمة السوقية، البطولات 🏆، الإنجازات 🏅، ورحلة الانتقالات 🔄. للمشاهير والفنانين والحيوانات: وصف مختصر عند توفرها.'));
children.push(h2('قاعدة البيانات'));
children.push(table(
  ['الملف', 'المحتوى'],
  [
    ['public/img/chars/', '247 صورة بأسماء موحّدة (slug.extension)'],
    ['public/char-img.json', 'خريطة الاسم العربي → ملف الصورة (الناجح فقط — يمنع الروابط المكسورة)'],
    ['public/char-bio.json', '91 وصف مختصر (رياضة 10 + فنانين 24 + مشاهير 18 + كرتون 21 + حيوانات 18)'],
    ['players.json', 'قاعدة 200 لاعب: الاسم، الفريق، الجنسية، التقييم، المركز، الرقم، البطولات، الإنجازات، الانتقالات + ألوان الأندية'],
  ],
  [32, 68],
));
spacer();

/* === 5. مستويات الصعوبة (v5) === */
children.push(h1('٥. مستويات صعوبة لعيبة الكورة (الإصدار v5)'));
children.push(p('في مود «التخمين» مع كاتيجوري «⚽ الكورة والرياضة»، بيظهر في اللوبي صف «🎚️ صعوبة لعيبة الكورة» يختار منه صاحب الأوضة، وكل اللاعيبة بتاخد شخصيات من نفس المستوى. التقسيم مبني على تقييم اللاعبين من قاعدة الـ 200 لاعب، مع وضع النجوم الثابتة (الأساطير) في المستوى السهل:'));
children.push(table(
  ['المستوى', 'المعيار', 'عدد اللاعبين', 'الوصف'],
  [
    ['🎲 الكل (افتراضي)', '—', 'الكل', 'من غير فلترة — نفس سلوك اللعبة الأصلي'],
    ['🟢 سهل', 'تقييم ≥ 89 + الأساطير الثابتة', '33+', 'النجوم الأشهر: ميسي، رونالدو، صلاح، أبو تريكة... إلخ'],
    ['🟡 متوسط', 'تقييم 84 – 88', '118', 'نجوم كبار معروفين كويس'],
    ['🔴 صعب', 'تقييم ≤ 83', '49', 'لاعبين أقل شهرة — تحدي للعارفين بالكورة'],
  ],
  [20, 30, 14, 36],
));
p('الفكرة: كل ما ارتفع التقييم زادت الشهرة، فالمستوى «سهل» جوه أشهر النجوم، و«صعب» جوه اللاعبين اللي قليل اللي يعرفهم. صاحب الأوضة يقدر يبدّل المستوى في أي وقت وهو في اللوبي، ورسالة بداية الجولة بتوضح المستوى المختار في الشات. تطبق الفلترة على الـ server (الويب والأندرويد) أول ما تنقسم الشخصيات السرية.', { after: 200 });
spacer();

/* === 6. حل مشكلة الرفرفة (v5) === */
children.push(h1('٦. حل مشكلة «الاختفاء والظهور» لنافذة اختيار الكاتيجوري'));
children.push(h2('المشكلة'));
children.push(p('على الموبايل والأندرويد، نافذة اختيار الكاتيجوري (القائمة المنسدلة الأصلية `<select>`) كانت بتظهر وبتختفي بسرعة — الريفرة او «الرفرفة» — كل ما تضغط عليها، وده كان بيفسّد تجربة اختيار الكاتيجوري.'));
children.push(h2('السبب الجذري (بعد تشخيص فعلي)'));
children.push(bullet('على أندرويد اللعبة بتجيب حالة السيرفر باستطلاع كل 700ms، وكل ما الحالة توصل كانت الواجهة تعيد رسم اللوبي بالكامل.'));
children.push(bullet('القائمة المنسدلة الأصلية على أندرويد بتفتح نافذة نظام (native picker)، وأي لمسة على عناصر الصفحة أثناء فتحها — حتى إعادة بناء قائمة اللاعبين الداخلية `innerHTML` — بيقفلها فورًا، فتبان «بتظهر وتختفي».'));
children.push(h2('الحل المطبّق نهائيًا'));
children.push(bullet('استبدال القائمة المنسدلة بأزرار chips مباشرة قابلة للضغط: كاتيجوري في كاتيجوري زر — مفيش نافذة نظام أصلًا تفتح عشان تقفل.'));
children.push(bullet('تقليل إعادة رسم اللوبي: قائمة اللاعبين ولوحة النقاط ما بيتعاد رسمهم غير لما محتواهم يتغير فعليًا (توقيع مقارنة) بدل كل استطلاع.'));
children.push(bullet('حماية `show()` من إعادة تشغيل أنيميشن الظهور لنفس الشاشة — بتسيّب الشاشة النشطة زي ما هي.'));
children.push(p('النتيجة: اختيار الكاتيجوري بقى أزرار ثابتة ما فيها أي رفرفة، والواجهة كلها أهدى على الأندرويد.', { after: 200 }));
spacer();

/* === 7. بنية النظام === */
children.push(h1('٧. بنية النظام والملفات'));
children.push(h2('منصة الويب'));
children.push(table(
  ['الملف', 'الوظيفة'],
  [
    ['server.js', 'السيرفر الرئيسي: Express + Socket.IO، إدارة الأوضاع والغرف والجولات والنقاط، معالجات set-category / set-difficulty / start-game / pass / guess / auction-act / steal-act / vote، تقديم الحالة (sanitize) مع الصور والبروفايلات'],
    ['public/index.html', 'الواجهة: شاشة البداية (3 مودات)، اللوبي (picker كاتيجوري + صعوبة + جواسيس + تشكيلة)، شاشة اللعب، شاشة الكشف، الشات'],
    ['public/client.js', 'منطق الواجهة: render / renderLobby / renderGame / renderAuction / renderOver، منتَقيا الأزرار، رسم الصور والبروفايلات، فحوصات show()'],
    ['public/styles.css', 'تصميم داكن عصري RTL + ستايلات البطاقات والصور ومستويات الصعوبة'],
    ['public/char-img.json + char-bio.json + players.json', 'مصادر حالة الصور والوصف وقاعدة اللاعبين'],
  ],
  [30, 70],
));
children.push(h2('منصة الأندرويد'));
children.push(table(
  ['الملف', 'الوظيفة'],
  [
    ['GameServer.java', 'نفس منطق السيرفر بالكامل مكتوب في Java: إدارة الغرف، الأوضاع، الصعوبة، serialization تبعث img/bio/profile، يخدم الصور عبر mimeOf'],
    ['assets/www/', 'نسخة www المبنية: index.html + client.js (طبقة HTTP بدل Socket.IO) + styles.css + 247 صورة + 3 ملفات JSON'],
    ['MainActivity (ناقل التطبيق)', 'يعرض الـ WebView، ويعمل هوت سبوت للـ IP، ويشغّل خادم NanoHTTPD الداخلي'],
    ['TestServer.java', 'فحص أندرويد شامل (71 فحص) ضد الخادم الداخلي'],
    ['كرت-فكة.apk', 'ناتج البناء النهائي (≈68MB)'],
  ],
  [30, 70],
));
children.push(h2('أدوات التطوير'));
children.push(table(
  ['الأداة', 'الوظيفة'],
  [
    ['tools/download-images.js', 'تنزيل صور ويكيبيديا: تسلسلي بتباعد 2ث، تخطي الموجود، حفظ تدريجي، retry بانتظار تصاعدي (لتفادي حظر 429)'],
    ['tools/people.js', '60 عنوان عربي → إنجليزي للمجموعات اللي مش في القاعدة'],
    ['tools/game-chars.js', '164 اسم فئة للـ aliases المطابقة'],
    ['build-apk-www.js', 'يبني assets/www من public + يضيف شيم الاستطلاع + يثبّت الـ EVENTS'],
    ['test.js', 'فحص السيرفر (61 فحص) — محاكاة لاعبين حقيقيين'],
    ['ui-smoke.js', 'فحص الواجهة (77 فحص) — JSDOM محاكاة'],
    ['check-www.js / check-apk.js', 'تأكيد سلامة نسخة www والأبنية'],
  ],
  [30, 70],
));
spacer();

/* === 8. جودة وفحوصات === */
children.push(h1('٨. الفحوصات والجودة'));
children.push(table(
  ['الفحص', 'ماذا يغطي', 'النتيجة'],
  [
    ['test.js (سيرفر ويب)', 'إنشاء وانضمام، الكاتيجوريات، التخمين الصح/الغلط، جواسيس والتصويت، المزاد الكامل والخطف، النقاط، مزحة الكود الغلط 🐒, مستويات الصعوبة', '61/61 ✅'],
    ['ui-smoke.js (واجهة ويب)', 'رندر الشاشات الثلاث، بطاقات الشخصية والصورة والبروفايل، منتقيا الكاتيجوري والصعوبة، الشات، النتيجة، إرسال الأحداث', '77/77 ✅'],
    ['TestServer.java (أندرويد)', 'نفس نطاق test.js على الخادم الداخلي + البروفايلات والصور + الصعوبة + المزاد + الجاسوس', '71/71 ✅'],
  ],
  [26, 54, 20],
));
spacer();

/* === 9. ما تم تنفيذه عبر الإصدارات === */
children.push(h1('٩. سجل الإصدارات — ما تم تنفيذه خطوة بخطوة'));
children.push(table(
  ['الإصدار', 'ما تم تنفيذه'],
  [
    ['RELEASE v2.0 (841ac7d)', 'نقطة البداية المستقرة: تخمين + جاسوس، كاتيجوري موحد، ويب + أندرويد'],
    ['v1 → v2 في dev', 'صور حقيقية للاعبي الكورة + معلومات تفصيلية + حل رفرفة النافذة (أول محاولة)'],
    ['v3 (82e9b67)', 'بورت كامل للمزاد والتصويت والنقاط إلى الأندرويد (61 فحص TestServer) + بناء khammeni-v3.apk'],
    ['v4 (9afcbed)', 'صور حقيقية حديثة (247) + بروفايلات اللاعبين في بطاقة الشخصية + إصلاح رفرفة show() + بناء khammeni-v4.apk'],
    ['v5 (e143c01)', 'حل الرفرفة نهائيًا (أزرار chips بدل القائمة المنسدلة + تقليل إعادة الرسم) + مستويات صعوبة لعيبة الكورة (سهل/متوسط/صعب/الكل يختارها صاحب الأوضة في التخمين) + فحوصات 61/77/71 + بناء كرت-فكة.apk'],
  ],
  [18, 82],
));
spacer();

/* === 10. التشغيل === */
children.push(h1('١٠. طريقة التشغيل'));
children.push(h2('منصة الويب'));
children.push(bullet('`npm install` (أول مرة فقط) ثم `npm start`.'));
children.push(bullet('صاحب الأوضة يفتح http://localhost:3000 ويعمل أوضة، والصحاب يفتحوا http://IP-اللابتوب:3000 ويكتبوا الكود.'));
children.push(bullet('لو الرابط مش مفتوح: يعملوا هوت سبوت، والسماح لجدار الحماية (Allow) للشبكات الخاصة.'));
children.push(h2('منصة الأندرويد'));
children.push(bullet('انقل كرت-فكة.apk لكل موبايل وثبّته (اسمح بتثبيت من مصادر غير معروفة).'));
children.push(bullet('اتصلوا بنفس الواي فاي أو الهوت سبوت — أول واحد يعمل أوضة بيبقى السيرفر والبقية ينضموا أوتوماتيك (أو بكود الأوضة).'));
children.push(bullet('إعادة البناء: `node build-apk-www.js` ثم `gradle :app:assembleDebug` — الناتج انسخه باسم كرت-فكة.apk.'));
spacer();

/* === 11. التسليم === */
children.push(h1('١١. ملفات التسليم'));
children.push(bullet('android/كرت-فكة.apk — تطبيق الأندرويد الجاهز (68MB بالصور).'));
children.push(bullet('المستودع «لعبة-dev» — شجرة التطوير الحالية (آخر commit: e143c01).'));
children.push(bullet('المستودع «لعبة» — النسخة المستقرة (841ac7d) لم تُمَس.'));
children.push(bullet('الويب: نفس الـ server.js والواجهة يقدروا يتشغّلوا على أي لابتوب.'));
spacer();

/* === خاتمة === */
children.push(h1('١٢. خاتمة'));
children.push(p('ابتدأ التطبيق بلعبة تخمين شخصيات بسيطة، وتطوّر على مراحل ليشمل لعبة الجاسوس مع التصويت، ومزاد كروت الفيفا المتكامل، وصور حقيقية حديثة مع بطاقات معلومات كاملة للاعبين، ومستويات صعوبة حسب اختيار صاحب الأوضة — على منصتين (ويب وأندرويد) بواجهة موحّدة وتجربة لعب سلسة خالية من الرفرفة، وكل ده محمي بشبكة فحوصات 209 فحصًا (61 + 77 + 71) تعمل كلها بالنجاح.'));

const doc = new Document({
  creator: 'Hassan Abd EL-zaher',
  title: 'توثيق التطبيق — كرت فكة',
  description: 'توثيق كامل للتطبيق وكل ما تم تنفيذه',
  sections: [{
    properties: {
      page: { margin: { top: 1000, bottom: 1000, left: 1000, right: 1000 } },
    },
    children,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, buf);
  console.log('تم إنشاء الملف:', OUT, '(' + (buf.length / 1024).toFixed(1) + ' KB)');
}).catch((e) => { console.error('خطأ:', e.message); process.exit(1); });