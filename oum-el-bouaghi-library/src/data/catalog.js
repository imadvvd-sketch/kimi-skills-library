/**
 * الرصيد الوثائقي — بيانات تجريبية للعرض فقط.
 * العناوين حقيقية ومعروفة، لكن الرموز (cote) وحالة التوفر وهمية.
 * لاحقاً يمكن استبدال هذا الملف باستدعاء لنظام الفهرسة (مثل PMB أو Koha)
 * عبر دالة searchCatalog في src/lib/search.js
 *
 * type: book | children | periodical | digital
 * lang: ar | fr
 */
export const catalog = [
  { id: 1, title: 'نجمة', author: 'كاتب ياسين', year: 1956, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: true, cote: 'R-KAT-001' },
  { id: 2, title: 'Nedjma', author: 'Kateb Yacine', year: 1956, type: 'book', lang: 'fr', subject: 'Roman algérien', available: false, cote: 'R-KAT-002' },
  { id: 3, title: 'La Grande Maison', author: 'Mohammed Dib', year: 1952, type: 'book', lang: 'fr', subject: 'Roman algérien', available: true, cote: 'R-DIB-001' },
  { id: 4, title: 'الدار الكبيرة', author: 'محمد ديب', year: 1952, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: true, cote: 'R-DIB-002' },
  { id: 5, title: 'Le Fils du pauvre', author: 'Mouloud Feraoun', year: 1950, type: 'book', lang: 'fr', subject: 'Roman algérien', available: true, cote: 'R-FER-001' },
  { id: 6, title: 'La Colline oubliée', author: 'Mouloud Mammeri', year: 1952, type: 'book', lang: 'fr', subject: 'Roman algérien', available: false, cote: 'R-MAM-001' },
  { id: 7, title: 'ريح الجنوب', author: 'عبد الحميد بن هدوقة', year: 1971, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: true, cote: 'R-BEN-001' },
  { id: 8, title: 'اللاز', author: 'الطاهر وطار', year: 1974, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: true, cote: 'R-OUT-001' },
  { id: 9, title: 'ذاكرة الجسد', author: 'أحلام مستغانمي', year: 1993, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: false, cote: 'R-MOS-001' },
  { id: 10, title: 'عابر سرير', author: 'أحلام مستغانمي', year: 2003, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: true, cote: 'R-MOS-002' },
  { id: 11, title: 'كتاب الأمير', author: 'واسيني الأعرج', year: 2005, type: 'book', lang: 'ar', subject: 'رواية تاريخية', available: true, cote: 'R-LAR-001' },
  { id: 12, title: 'غادة أم القرى', author: 'أحمد رضا حوحو', year: 1947, type: 'book', lang: 'ar', subject: 'رواية جزائرية', available: true, cote: 'R-HOU-001' },
  { id: 13, title: 'اللهب المقدس', author: 'مفدي زكريا', year: 1961, type: 'book', lang: 'ar', subject: 'شعر', available: true, cote: 'P-ZAK-001' },
  { id: 14, title: "L'Amour, la fantasia", author: 'Assia Djebar', year: 1985, type: 'book', lang: 'fr', subject: 'Roman algérien', available: true, cote: 'R-DJE-001' },
  { id: 15, title: 'Le Quai aux fleurs ne répond plus', author: 'Malek Haddad', year: 1961, type: 'book', lang: 'fr', subject: 'Roman algérien', available: true, cote: 'R-HAD-001' },
  { id: 16, title: 'Ce que le jour doit à la nuit', author: 'Yasmina Khadra', year: 2008, type: 'book', lang: 'fr', subject: 'Roman algérien', available: false, cote: 'R-KHA-001' },
  { id: 17, title: 'Le Dernier Été de la raison', author: 'Tahar Djaout', year: 1999, type: 'book', lang: 'fr', subject: 'Roman algérien', available: true, cote: 'R-DJA-001' },
  { id: 18, title: 'شروط النهضة', author: 'مالك بن نبي', year: 1949, type: 'book', lang: 'ar', subject: 'فكر وفلسفة', available: true, cote: 'F-BEN-001' },
  { id: 19, title: 'مشكلة الثقافة', author: 'مالك بن نبي', year: 1959, type: 'book', lang: 'ar', subject: 'فكر وفلسفة', available: true, cote: 'F-BEN-002' },
  { id: 20, title: 'المقدمة', author: 'ابن خلدون', year: null, type: 'book', lang: 'ar', subject: 'تاريخ وعلم اجتماع', available: true, cote: 'H-KHA-001' },
  { id: 21, title: "L'Étranger", author: 'Albert Camus', year: 1942, type: 'book', lang: 'fr', subject: 'Roman', available: true, cote: 'R-CAM-001' },
  { id: 22, title: 'كليلة ودمنة', author: 'ابن المقفع', year: null, type: 'children', lang: 'ar', subject: 'حكايات', available: true, cote: 'J-MUQ-001' },
  { id: 23, title: 'Le Petit Prince', author: 'Antoine de Saint-Exupéry', year: 1943, type: 'children', lang: 'fr', subject: 'Conte', available: false, cote: 'J-SAI-001' },
  { id: 24, title: 'الأمير الصغير', author: 'أنطوان دو سانت إكزوبيري', year: 1943, type: 'children', lang: 'ar', subject: 'حكايات', available: true, cote: 'J-SAI-002' },
  { id: 25, title: 'حكايات جحا', author: 'تراث شعبي', year: null, type: 'children', lang: 'ar', subject: 'حكايات', available: true, cote: 'J-TRA-001' },
  { id: 26, title: 'الجريدة الرسمية للجمهورية الجزائرية', author: 'الأمانة العامة للحكومة', year: null, type: 'periodical', lang: 'ar', subject: 'قانون', available: true, cote: 'PER-JO-001' },
  { id: 27, title: 'Journal officiel de la République algérienne', author: 'Secrétariat général du Gouvernement', year: null, type: 'periodical', lang: 'fr', subject: 'Droit', available: true, cote: 'PER-JO-002' },
  { id: 28, title: 'تاريخ الجزائر الثقافي', author: 'أبو القاسم سعد الله', year: 1998, type: 'book', lang: 'ar', subject: 'تاريخ', available: true, cote: 'H-SAA-001' },
  { id: 29, title: 'المقدمة (نسخة رقمية)', author: 'ابن خلدون', year: null, type: 'digital', lang: 'ar', subject: 'تاريخ وعلم اجتماع', available: true, cote: 'NUM-KHA-001' },
  { id: 30, title: 'Nedjma (version numérique)', author: 'Kateb Yacine', year: 1956, type: 'digital', lang: 'fr', subject: 'Roman algérien', available: true, cote: 'NUM-KAT-001' },
]
