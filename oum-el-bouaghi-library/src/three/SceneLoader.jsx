/** شاشة تحميل أنيقة: كتاب صغير تتقلب صفحاته */
export default function SceneLoader({ label }) {
  return (
    <div role="status" className="pointer-events-none absolute inset-x-0 bottom-2 flex flex-col items-center gap-2 text-sm text-ink-soft">
      <svg viewBox="0 0 64 40" className="h-8 w-14" aria-hidden="true">
        <path d="M32 36 Q18 30 4 33 V9 Q18 6 32 12 Z" fill="var(--color-parchment)" stroke="var(--color-brown)" strokeWidth="1.5" />
        <path d="M32 36 Q46 30 60 33 V9 Q46 6 32 12 Z" fill="var(--color-parchment)" stroke="var(--color-brown)" strokeWidth="1.5" />
        {[0, 1, 2].map((i) => (
          <path
            key={i}
            className="loader-page"
            style={{ animationDelay: `${i * 0.35}s` }}
            d="M32 36 Q46 30 60 33 V9 Q46 6 32 12 Z"
            fill="var(--color-paper)"
            stroke="var(--color-gold)"
            strokeWidth="1.2"
          />
        ))}
      </svg>
      <span>{label}</span>
    </div>
  )
}
