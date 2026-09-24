/**
 * الخدمات — كل خدمة تظهر ككتاب على الرف ثلاثي الأبعاد وكبطاقة نصية.
 * color: لون كعب الكتاب في المشهد ثلاثي الأبعاد.
 * icon: اسم الأيقونة (انظر src/components/Icon.jsx)
 */
export const services = [
  {
    id: 'loans',
    icon: 'loan',
    color: '#7A5230',
    title: { ar: 'الإعارة الخارجية', fr: 'Prêt à domicile' },
    description: {
      ar: 'استعر الكتب إلى منزلك لمدة محددة، وجدّد إعارتك بسهولة لدى مكتب الإعارة.',
      fr: 'Empruntez des livres à domicile pour une durée définie et renouvelez facilement vos prêts.',
    },
  },
  {
    id: 'reading-rooms',
    icon: 'reading',
    color: '#566B2E',
    title: { ar: 'قاعات المطالعة', fr: 'Salles de lecture' },
    description: {
      ar: 'فضاءات هادئة ومضاءة جيداً للمطالعة والمراجعة والبحث، للطلبة والباحثين والقرّاء.',
      fr: 'Des espaces calmes et lumineux pour lire, réviser et faire des recherches.',
    },
  },
  {
    id: 'children',
    icon: 'children',
    color: '#B5712F',
    title: { ar: 'فضاء الأطفال', fr: 'Espace jeunesse' },
    description: {
      ar: 'ركن مخصص للصغار بكتب مصوّرة وقصص وأنشطة تنمّي حب القراءة منذ الطفولة.',
      fr: 'Un coin dédié aux petits : albums, contes et activités pour cultiver le goût de la lecture.',
    },
  },
  {
    id: 'digital',
    icon: 'digital',
    color: '#2F4A5C',
    title: { ar: 'المكتبة الرقمية', fr: 'Bibliothèque numérique' },
    description: {
      ar: 'حواسيب وإنترنت ومصادر رقمية لدعم البحث والتعلّم الذاتي.',
      fr: 'Ordinateurs, accès Internet et ressources numériques pour la recherche et l’autoformation.',
    },
  },
  {
    id: 'cultural',
    icon: 'culture',
    color: '#8C3B2E',
    title: { ar: 'الأنشطة الثقافية', fr: 'Activités culturelles' },
    description: {
      ar: 'ندوات وأمسيات أدبية ومعارض وورشات تحيي المشهد الثقافي في الولاية.',
      fr: 'Conférences, soirées littéraires, expositions et ateliers qui animent la vie culturelle.',
    },
  },
  {
    id: 'membership',
    icon: 'card',
    color: '#4A5D26',
    title: { ar: 'التسجيل والانخراط', fr: 'Inscription et adhésion' },
    description: {
      ar: 'احصل على بطاقة المنخرط للاستفادة من كل الخدمات. [الوثائق المطلوبة وشروط الانخراط]',
      fr: 'Obtenez votre carte d’adhérent pour profiter de tous les services. [Pièces requises et conditions]',
    },
  },
]
