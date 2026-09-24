/**
 * رسم ثابت لكتاب مفتوح — يُستعمل:
 *  1) كبديل للمشهد ثلاثي الأبعاد على الأجهزة الضعيفة أو مع reduced-motion
 *  2) كصورة أولية ريثما يُحمَّل المشهد
 */
const letters = [
  { c: 'ا', x: 120, y: 90, s: 34, o: 0.8 },
  { c: 'ⴰ', x: 180, y: 50, s: 26, o: 0.6 },
  { c: 'ب', x: 250, y: 70, s: 30, o: 0.75 },
  { c: 'ⵎ', x: 310, y: 30, s: 22, o: 0.5 },
  { c: 'ق', x: 330, y: 105, s: 28, o: 0.7 },
  { c: 'ⵏ', x: 150, y: 140, s: 20, o: 0.55 },
  { c: 'ع', x: 220, y: 120, s: 24, o: 0.65 },
  { c: 'ⴷ', x: 90, y: 40, s: 18, o: 0.45 },
  { c: 'م', x: 380, y: 60, s: 20, o: 0.5 },
]

export default function HeroIllustration({ label }) {
  return (
    <svg viewBox="0 0 480 400" className="h-full w-full" {...(label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true })}>
      <defs>
        <radialGradient id="hero-glow" cx="50%" cy="55%" r="55%">
          <stop offset="0%" stopColor="#F3D38B" stopOpacity="0.85" />
          <stop offset="60%" stopColor="#E9C57A" stopOpacity="0.18" />
          <stop offset="100%" stopColor="#E9C57A" stopOpacity="0" />
        </radialGradient>
        <linearGradient id="page-l" x1="0" x2="1">
          <stop offset="0%" stopColor="#E6D5B5" />
          <stop offset="100%" stopColor="#FBF5E8" />
        </linearGradient>
        <linearGradient id="page-r" x1="1" x2="0">
          <stop offset="0%" stopColor="#E6D5B5" />
          <stop offset="100%" stopColor="#FBF5E8" />
        </linearGradient>
      </defs>
      <ellipse cx="240" cy="230" rx="230" ry="170" fill="url(#hero-glow)" />
      {letters.map((l, i) => (
        <text
          key={i}
          x={l.x}
          y={l.y + 40}
          fontSize={l.s}
          fill="#B07A2F"
          opacity={l.o}
          fontFamily="Amiri, 'Noto Sans Tifinagh', serif"
          textAnchor="middle"
        >
          {l.c}
        </text>
      ))}
      {/* الغلاف */}
      <path d="M48 262 Q240 300 432 262 L440 300 Q240 340 40 300 Z" fill="#6B4527" />
      {/* الصفحات */}
      <path d="M240 290 Q150 250 60 268 L70 190 Q160 170 240 212 Z" fill="url(#page-l)" stroke="#CBB28A" />
      <path d="M240 290 Q330 250 420 268 L410 190 Q320 170 240 212 Z" fill="url(#page-r)" stroke="#CBB28A" />
      {/* صفحة تنقلب */}
      <path d="M240 212 Q300 150 360 118 L372 196 Q300 214 240 290 Z" fill="#FFFBF2" stroke="#CBB28A" opacity="0.95" />
      {/* أسطر */}
      {[0, 1, 2, 3, 4].map((i) => (
        <g key={i} stroke="#B9A07A" strokeWidth="2" strokeLinecap="round" opacity="0.6">
          <path d={`M95 ${206 + i * 13} Q160 ${198 + i * 13} 220 ${222 + i * 13}`} fill="none" />
          <path d={`M385 ${206 + i * 13} Q320 ${198 + i * 13} 260 ${222 + i * 13}`} fill="none" />
        </g>
      ))}
      <path d="M240 212 L240 290" stroke="#A8895D" strokeWidth="2" />
    </svg>
  )
}
