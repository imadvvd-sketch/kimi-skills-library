/**
 * يعرض نصاً ويُبرز الأجزاء المؤقتة [ ... ] بخلفية ذهبية خفيفة
 * حتى تُلاحَظ بسهولة قبل النشر.
 */
export default function Ph({ children }) {
  if (typeof children !== 'string') return children
  const parts = children.split(/(\[[^\]]+\])/g)
  return parts.map((part, i) =>
    /^\[.*\]$/.test(part) ? (
      <span key={i} className="placeholder-text">{part}</span>
    ) : (
      part
    ),
  )
}
