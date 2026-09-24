import { useEffect, useState } from 'react'

/** هل العنصر ظاهر في الشاشة؟ (لإيقاف رسم المشاهد عند الخروج منها) */
export default function useInView(ref, rootMargin = '0px') {
  const [inView, setInView] = useState(false)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const io = new IntersectionObserver(([entry]) => setInView(entry.isIntersecting), { rootMargin })
    io.observe(el)
    return () => io.disconnect()
  }, [ref, rootMargin])
  return inView
}
