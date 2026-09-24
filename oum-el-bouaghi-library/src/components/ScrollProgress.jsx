import { m, useScroll, useSpring } from 'framer-motion'
import { useLang } from '../i18n/LanguageContext.jsx'

/** شريط رفيع يبيّن تقدّم القراءة في الصفحة */
export default function ScrollProgress() {
  const { dir } = useLang()
  const { scrollYProgress } = useScroll()
  const scaleX = useSpring(scrollYProgress, { stiffness: 120, damping: 30, restDelta: 0.001 })
  return (
    <m.div
      aria-hidden="true"
      className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-l from-olive via-gold to-brown"
      style={{ scaleX, transformOrigin: dir === 'rtl' ? '100% 50%' : '0% 50%' }}
    />
  )
}
