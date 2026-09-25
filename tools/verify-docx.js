/* فحص سريع: يفتح الوورد كـ ZIP ويتأكد أن النص العربي موجود في الوثيقة */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const file = path.join(__dirname, '..', 'docs', 'توثيق-التطبيق-كرت-فكة.docx');
const buf = fs.readFileSync(file);
if (buf[0] !== 0x50 || buf[1] !== 0x4B) { console.error('خطأ: ليس ملف ZIP'); process.exit(1); }

// central directory: scan for word/document.xml
const s = buf.toString('latin1');
if (!s.includes('word/document.xml')) { console.error('خطأ: لا يوجد word/document.xml'); process.exit(1); }

// find local file entry for document.xml
const marker = Buffer.from('word/document.xml', 'latin1');
let localOff = -1;
for (let i = 0; i < buf.length - marker.length; i++) {
  if (buf[i] === marker[0] && buf.subarray(i, i + marker.length).equals(marker)) { localOff = i; break; }
}
if (localOff < 0) { console.error('خطأ: لم يُعثر على الإدخال'); process.exit(1); }

// go back to locate 'PK\x03\x04' before marker
let entryOff = -1;
for (let i = localOff; i >= 0 && i > localOff - 1000; i--) {
  if (buf[i] === 0x50 && buf[i+1] === 0x4B && buf[i+2] === 0x03 && buf[i+3] === 0x04) { entryOff = i; break; }
}
if (entryOff < 0) { console.error('خطأ: لم يُعثر على بداية الإدخال'); process.exit(1); }

const nameLen = buf.readUInt16LE(entryOff + 26);
const extraLen = buf.readUInt16LE(entryOff + 28);
const dataStart = entryOff + 30 + nameLen + extraLen;
// local header: 30 + name + extra, then data (compressed with deflate typically)
const compLen = buf.readUInt32LE(entryOff + 18);
const raw = buf.subarray(dataStart, dataStart + compLen);
let xml;
try { xml = zlib.inflateRawSync(raw).toString('utf8'); }
catch (e) { try { xml = zlib.inflateSync(raw).toString('utf8'); } catch (e2) { console.error('فك الضغط فشل'); process.exit(1); } }

const checks = [
  'كرت فكة', 'مستويات صعوبة', 'سهل', 'متوسط', 'صعب',
  'الرفرفة', 'الكورة والرياضة', 'كرت-فكة.apk', '247 صورة', 'e143c01',
  'تخمين شخصيات', 'لعبة الجاسوس', 'مزاد النجوم', 'chips',
];
const missing = checks.filter((c) => !xml.includes(c));
console.log('حجم الوورد:', (buf.length / 1024).toFixed(1), 'KB');
console.log('حجم document.xml (مفكوك):', (xml.length / 1024).toFixed(1), 'KB');
if (missing.length) { console.error('نصوص ناقصة:', missing.join(' | ')); process.exit(1); }
console.log('✔ كل النصوص الأساسية موجودة في الوثيقة —', checks.length, 'فحص نجح');