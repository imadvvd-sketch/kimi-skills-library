/**
 * ============================================================
 *  ملف البيانات الرئيسي للموقع — عدّل هنا كل المعلومات الحقيقية
 * ============================================================
 *  كل قيمة بين معقوفين [ ... ] هي نص مؤقت يجب استبداله.
 *  هذا الملف يُستعمل أيضاً لتوليد بيانات Schema.org و sitemap.xml
 *  تلقائياً عند البناء، فلا حاجة لتعديلها في مكان آخر.
 */

export const siteInfo = {
  // رابط الموقع النهائي بعد النشر (بدون / في النهاية)
  // مثال: 'https://bibliotheque-oeb.netlify.app'
  siteUrl: 'https://example.netlify.app',

  name: {
    ar: 'المكتبة الرئيسية للمطالعة العمومية لولاية أم البواقي',
    fr: "Bibliothèque principale de lecture publique de la wilaya d'Oum El Bouaghi",
  },
  shortName: {
    ar: 'مكتبة أم البواقي',
    fr: 'Bibliothèque d’Oum El Bouaghi',
  },

  address: {
    street: { ar: '[العنوان]', fr: '[Adresse]' },
    city: { ar: 'أم البواقي', fr: 'Oum El Bouaghi' },
    postalCode: '[الرمز البريدي]',
    region: { ar: 'ولاية أم البواقي', fr: "Wilaya d'Oum El Bouaghi" },
    country: 'DZ',
  },

  // الإحداثيات الجغرافية للمكتبة (أرقام عشرية). اتركها null لإظهار مكان مؤقت بدل الخريطة.
  // مثال: { lat: 35.87, lng: 7.11 }
  geo: { lat: null, lng: null },

  phone: '[رقم الهاتف]',
  fax: '[رقم الفاكس]',
  email: '[البريد الإلكتروني]',

  /**
   * أوقات العمل.
   * days: أسماء الأيام بالإنجليزية (Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)
   * opens / closes: بصيغة 24 ساعة 'HH:MM' مثل '08:30'
   * لن تُضاف إلى بيانات Schema.org إلا الأسطر المكتملة بصيغة صحيحة.
   */
  openingHours: [
    { days: ['[أيام العمل]'], opens: '[ساعة الفتح]', closes: '[ساعة الإغلاق]' },
    { days: ['[أيام أخرى]'], opens: '[ساعة الفتح]', closes: '[ساعة الإغلاق]' },
  ],
  // ملاحظة تظهر تحت الأوقات (مثلاً توقيت رمضان أو العطل)
  hoursNote: { ar: '[ملاحظة حول أوقات العمل، مثل توقيت شهر رمضان]', fr: '[Note sur les horaires, ex. horaires du Ramadan]' },

  // روابط التواصل الاجتماعي — ضع الرابط الكامل يبدأ بـ https://
  // الروابط غير المكتملة تظهر باهتة وغير قابلة للنقر.
  social: {
    facebook: '[رابط صفحة فيسبوك]',
    instagram: '[رابط إنستغرام]',
    youtube: '[رابط يوتيوب]',
  },

  // أرقام تعريفية تظهر في قسم "عن المكتبة"
  stats: [
    { value: '[عدد]', label: { ar: 'عنوان في الرصيد', fr: 'titres au fonds' } },
    { value: '[عدد]', label: { ar: 'منخرط', fr: 'adhérents' } },
    { value: '[عدد]', label: { ar: 'مقعد مطالعة', fr: 'places de lecture' } },
    { value: '[عدد]', label: { ar: 'مكتبة ملحقة', fr: 'bibliothèques annexes' } },
  ],

  // سنة الافتتاح (تظهر في قسم "عن المكتبة" والتذييل)
  foundedYear: '[سنة الافتتاح]',

  // الجهة الوصية
  ministry: {
    ar: 'وزارة الثقافة والفنون',
    fr: 'Ministère de la Culture et des Arts',
  },
}

/** يتحقق مما إذا كانت القيمة ما زالت نصاً مؤقتاً مثل [رقم الهاتف] */
export const isPlaceholder = (value) =>
  value == null || value === '' || /^\[.*\]$/.test(String(value).trim())
