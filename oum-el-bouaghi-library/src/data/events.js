/**
 * الأنشطة والفعاليات القادمة.
 * ⚠️ الفعاليات الحالية أمثلة توضيحية فقط (isSample: true تُظهر شارة "مثال").
 * احذفها وأضف الفعاليات الحقيقية بنفس الصيغة، مع حذف isSample أو جعلها false.
 *
 * date: بصيغة 'YYYY-MM-DD'   |   time: نص حر مثل '10:00'
 * category: reading | workshop | exhibition | conference | children | cinema
 */
export const events = [
  {
    id: 'e1',
    isSample: true,
    date: '2026-10-15',
    time: '[الساعة]',
    category: 'children',
    title: { ar: 'ساعة الحكاية للأطفال', fr: "L'heure du conte pour enfants" },
    description: {
      ar: 'جلسة سرد قصص تفاعلية لأطفال من 5 إلى 10 سنوات في فضاء الطفل.',
      fr: 'Séance de contes interactive pour les enfants de 5 à 10 ans, à l’espace jeunesse.',
    },
    place: { ar: 'فضاء الأطفال', fr: 'Espace jeunesse' },
  },
  {
    id: 'e2',
    isSample: true,
    date: '2026-10-22',
    time: '[الساعة]',
    category: 'reading',
    title: { ar: 'نادي القراءة: رواية جزائرية', fr: 'Club de lecture : un roman algérien' },
    description: {
      ar: 'لقاء شهري لمناقشة رواية من الأدب الجزائري، مفتوح لكل المنخرطين.',
      fr: 'Rencontre mensuelle autour d’un roman de la littérature algérienne, ouverte à tous les adhérents.',
    },
    place: { ar: 'قاعة المطالعة الكبرى', fr: 'Grande salle de lecture' },
  },
  {
    id: 'e3',
    isSample: true,
    date: '2026-11-01',
    time: '[الساعة]',
    category: 'exhibition',
    title: { ar: 'معرض: ذاكرة نوفمبر في الكتب', fr: 'Exposition : la mémoire de Novembre dans les livres' },
    description: {
      ar: 'معرض وثائقي بمناسبة ذكرى اندلاع الثورة التحريرية المجيدة.',
      fr: 'Exposition documentaire à l’occasion de l’anniversaire du déclenchement de la Révolution.',
    },
    place: { ar: 'بهو المكتبة', fr: 'Hall de la bibliothèque' },
  },
  {
    id: 'e4',
    isSample: true,
    date: '2026-11-12',
    time: '[الساعة]',
    category: 'workshop',
    title: { ar: 'ورشة الكتابة الإبداعية', fr: "Atelier d'écriture créative" },
    description: {
      ar: 'ورشة للشباب لاكتشاف تقنيات كتابة القصة القصيرة.',
      fr: 'Atelier pour les jeunes pour découvrir les techniques de la nouvelle.',
    },
    place: { ar: 'قاعة الأنشطة', fr: 'Salle d’animation' },
  },
]
