/** شعار نصي مؤقت — استبدله بشعار المكتبة الرسمي (ضع الملف في public/ وعدّل هنا) */
export default function Logo({ className = 'size-11' }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <rect width="64" height="64" rx="16" fill="var(--color-olive)" />
      <path d="M12 21c7-3 14-3 20 1 6-4 13-4 20-1v25c-7-3-14-3-20 1-6-4-13-4-20-1z" fill="var(--color-paper)" />
      <path d="M32 22v24" stroke="var(--color-olive)" strokeWidth="2" />
      <path d="M17 27h10M17 32h10M37 27h10M37 32h10" stroke="var(--color-sand)" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M32 7l4.5 5.5L32 18l-4.5-5.5z" fill="var(--color-gold)" />
    </svg>
  )
}
