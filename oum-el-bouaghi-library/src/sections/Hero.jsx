import { m as motion } from 'framer-motion'
import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import Icon from '../components/Icon.jsx'
import HeroVisual from '../components/HeroVisual.jsx'
import { AmazighPattern } from '../components/AmazighBand.jsx'

const fadeUp = (delay) => ({
  initial: { opacity: 0, y: 28 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.9, delay, ease: [0.22, 1, 0.36, 1] },
})

export default function Hero() {
  const { t, pick } = useLang()
  return (
    <section id="home" aria-labelledby="hero-title" className="relative flex min-h-[100svh] items-center overflow-hidden pt-24 pb-16">
      <AmazighPattern className="opacity-[0.045]" />
      <div className="container-page relative grid items-center gap-10 lg:grid-cols-[1.05fr_1fr]">
        <div className="relative z-10">
          <motion.p {...fadeUp(0.1)} className="mb-4 inline-flex items-center gap-2 rounded-full bg-olive/10 px-4 py-1.5 font-bold text-olive-dark">
            <span aria-hidden="true" className="size-2 rotate-45 bg-olive" />
            {t.hero.kicker}
          </motion.p>
          <motion.h1 {...fadeUp(0.2)} id="hero-title" className="text-4xl font-bold sm:text-5xl lg:text-6xl">
            {t.hero.title}
            <span className="mt-2 block text-2xl text-brown sm:text-3xl lg:text-4xl">{pick(siteInfo.address.region)}</span>
          </motion.h1>
          <motion.p {...fadeUp(0.35)} className="mt-6 max-w-xl text-lg text-ink-soft sm:text-xl">
            {t.hero.welcome}
          </motion.p>
          <motion.div {...fadeUp(0.5)} className="mt-9 flex flex-wrap gap-3">
            <a href="#catalog" className="btn-primary">
              <Icon name="search" className="size-5" />
              {t.hero.ctaCatalog}
            </a>
            <a href="#services" className="btn-ghost">
              {t.hero.ctaServices}
            </a>
          </motion.div>
        </div>

        {/* المشهد ثلاثي الأبعاد (مع رسم ثابت بديل) */}
        <motion.div
          initial={{ opacity: 0, scale: 0.94 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 1.2, delay: 0.2 }}
          className="relative w-full"
        >
          <HeroVisual />
        </motion.div>
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
