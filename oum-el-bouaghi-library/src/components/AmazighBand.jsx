/**
 * شريط زخرفي مستوحى من زخارف الزرابي والوشم الشاوي في الأوراس
 * (معيّنات ومثلثات وخطوط متعرجة). عنصر تزييني فقط.
 */
export default function AmazighBand({ className = '', opacity = 0.55 }) {
  return (
    <div aria-hidden="true" className={`h-5 w-full ${className}`} style={{ opacity }}>
      <svg width="100%" height="100%" preserveAspectRatio="none">
        <defs>
          <pattern id="amazigh-band" width="48" height="20" patternUnits="userSpaceOnUse">
            <path d="M0 10 L6 4 L12 10 L6 16 Z" fill="none" stroke="var(--color-brown)" strokeWidth="1.3" />
            <path d="M6 8 L8 10 L6 12 L4 10 Z" fill="var(--color-olive)" />
            <path d="M12 10 L18 4 L24 10 L30 4 L36 10" fill="none" stroke="var(--color-gold)" strokeWidth="1.3" />
            <path d="M36 10 L42 16 L48 10" fill="none" stroke="var(--color-gold)" strokeWidth="1.3" />
            <circle cx="24" cy="15" r="1.2" fill="var(--color-brown)" />
            <circle cx="42" cy="5" r="1.2" fill="var(--color-brown)" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#amazigh-band)" />
      </svg>
    </div>
  )
}

/** خلفية معيّنات خفيفة جداً للأقسام */
export function AmazighPattern({ className = '' }) {
  return (
    <svg aria-hidden="true" className={`pointer-events-none absolute inset-0 h-full w-full ${className}`}>
      <defs>
        <pattern id="amazigh-bg" width="64" height="64" patternUnits="userSpaceOnUse">
          <path d="M32 6 L58 32 L32 58 L6 32 Z" fill="none" stroke="var(--color-brown)" strokeWidth="1" />
          <path d="M32 22 L42 32 L32 42 L22 32 Z" fill="none" stroke="var(--color-olive)" strokeWidth="1" />
          <path d="M0 0 L6 6 M64 0 L58 6 M0 64 L6 58 M64 64 L58 58" stroke="var(--color-brown)" strokeWidth="1" />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill="url(#amazigh-bg)" />
    </svg>
  )
}
