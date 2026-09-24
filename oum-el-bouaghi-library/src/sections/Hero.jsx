import { useRef } from 'react'
import { m as motion, useReducedMotion, useScroll, useTransform } from 'framer-motion'
import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import Icon from '../components/Icon.jsx'
import HeroVisual from '../components/HeroVisual.jsx'
import { AmazighPattern } from '../components/AmazighBand.jsx'

// حركة الدخول بـ CSS (لا تنتظر JavaScript): النص يظهر فور تحميل HTML المولَّد مسبقاً
const enter = (delay, className) => ({ className, style: { animationDelay: `${delay}s` } })

export default function Hero() {
  const { t, pick } = useLang()
  const ref = useRef()
  const reduce = useReducedMotion()
  // النص يصعد ويتلاشى ببطء أثناء النزول (parallax)
  const { scrollYProgress } = useScroll({ target: ref, offset: ['start start', 'end start'] })
  const textY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : -90])
  const textOpacity = useTransform(scrollYProgress, [0, 0.8], [1, reduce ? 1 : 0.2])
  return (
    <section ref={ref} id="home" aria-labelledby="hero-title" className="relative flex min-h-[100svh] items-center overflow-hidden pt-24 pb-16">
      <AmazighPattern className="opacity-[0.045]" />
      <div className="container-page relative grid items-center gap-10 lg:grid-cols-[1.05fr_1fr]">
        <motion.div className="relative z-10" style={{ y: textY, opacity: textOpacity }}>
          <p
            {...enter(0.1, 'hero-in mb-4 inline-flex items-center gap-2 rounded-full bg-olive/10 px-4 py-1.5 font-bold text-olive-dark')}
          >
            <span aria-hidden="true" className="size-2 rotate-45 bg-olive" />
            {t.hero.kicker}
          </p>
          <h1 id="hero-title" {...enter(0, 'hero-rise text-4xl font-bold sm:text-5xl lg:text-6xl')}>
            {t.hero.title}
            <span className="mt-2 block text-2xl text-brown sm:text-3xl lg:text-4xl">{pick(siteInfo.address.region)}</span>
          </h1>
          <p {...enter(0.25, 'hero-in mt-6 max-w-xl text-lg text-ink-soft sm:text-xl')}>{t.hero.welcome}</p>
          <div {...enter(0.4, 'hero-in mt-9 flex flex-wrap gap-3')}>
            <a href="#catalog" className="btn-primary">
              <Icon name="search" className="size-5" />
              {t.hero.ctaCatalog}
            </a>
            <a href="#services" className="btn-ghost">
              {t.hero.ctaServices}
            </a>
          </div>
        </motion.div>

        {/* المشهد ثلاثي الأبعاد (مع رسم ثابت بديل) */}
        <div className="hero-zoom relative w-full">
          <HeroVisual />
        </div>
      </div>

      <a
        href="#about"
        className="absolute bottom-6 left-1/2 hidden -translate-x-1/2 flex-col items-center gap-1 text-sm text-ink-soft hover:text-ink sm:flex"
      >
        {t.hero.scroll}
        <Icon name="arrowDown" className="size-5 animate-bounce" />
      </a>
    </section>
  )
}
