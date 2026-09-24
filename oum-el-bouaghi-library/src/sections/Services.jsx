import { lazy, Suspense, useMemo, useRef, useState } from 'react'
import { useLang } from '../i18n/LanguageContext.jsx'
import { services } from '../data/services.js'
import useDeviceTier from '../hooks/useDeviceTier.js'
import useInView from '../hooks/useInView.js'
import SectionHeading from '../components/SectionHeading.jsx'
import SceneBoundary from '../components/SceneBoundary.jsx'
import SceneLoader from '../three/SceneLoader.jsx'
import Reveal from '../components/Reveal.jsx'
import Icon from '../components/Icon.jsx'
import Ph from '../components/Ph.jsx'

const ShelfScene = lazy(() => import('../three/ShelfScene.jsx'))

export default function Services() {
  const { t, pick, lang, dir } = useLang()
  const [tier, setTier] = useDeviceTier()
  const [active, setActive] = useState(null)
  const [ready, setReady] = useState(false)
  const shelfRef = useRef()
  // نبدأ تحميل الرف عندما يقترب القسم من الشاشة
  const near = useInView(shelfRef, '400px')
  const [load, setLoad] = useState(false)
  if (near && !load) setLoad(true)
  const cardRefs = useRef({})

  const items = useMemo(() => services.map((s) => ({ id: s.id, color: s.color, title: pick(s.title) })), [pick])
  const use3d = tier !== 'static'

  const select = (id) => {
    setActive(id)
    cardRefs.current[id]?.focus({ preventScroll: true })
    cardRefs.current[id]?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }

  return (
    <section id="services" aria-labelledby="services-title" className="relative bg-parchment/60 py-24">
      <div className="container-page">
        <SectionHeading id="services-title" title={t.services.title} subtitle={use3d ? t.services.subtitle : null} />

        {use3d && (
          <div ref={shelfRef} className="relative -mx-4 mb-10 h-[260px] sm:mx-0 sm:h-[380px] lg:h-[420px]">
            {load && (
              <div className={`h-full transition-opacity duration-700 ${ready ? 'opacity-100' : 'opacity-0'}`}>
                <SceneBoundary onFail={() => setTier('static')}>
                  <Suspense fallback={null}>
                    <ShelfScene
                      tier={tier}
                      items={items}
                      lang={lang}
                      dir={dir}
                      active={active}
                      onHover={setActive}
                      onSelect={select}
                      onReady={() => setReady(true)}
                    />
                  </Suspense>
                </SceneBoundary>
              </div>
            )}
            {!ready && <SceneLoader label={t.loader.label} />}
          </div>
        )}

        <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {services.map((s, i) => {
            const isActive = active === s.id
            return (
              <Reveal as="li" key={s.id} delay={(i % 3) * 0.1} className="h-full">
                <article
                  ref={(el) => (cardRefs.current[s.id] = el)}
                  tabIndex={0}
                  aria-labelledby={`service-${s.id}`}
                  onMouseEnter={() => setActive(s.id)}
                  onMouseLeave={() => setActive(null)}
                  onFocus={() => setActive(s.id)}
                  onBlur={() => setActive(null)}
                  className={`card group relative h-full overflow-hidden p-7 transition duration-300 ${
                    isActive ? '-translate-y-1 shadow-xl ring-2 ring-olive/60' : ''
                  }`}
                >
                  <span aria-hidden="true" className="absolute inset-y-0 start-0 w-2" style={{ background: s.color }} />
                  <span
                    aria-hidden="true"
                    className={`mb-5 inline-flex size-14 items-center justify-center rounded-2xl text-paper transition-transform duration-300 ${
                      isActive ? '-rotate-6' : ''
                    }`}
                    style={{ background: s.color }}
                  >
                    <Icon name={s.icon} className="size-7" />
                  </span>
                  <h3 id={`service-${s.id}`} className="text-2xl font-bold">{pick(s.title)}</h3>
                  <p className="mt-2 text-ink-soft"><Ph>{pick(s.description)}</Ph></p>
                </article>
              </Reveal>
            )
          })}
        </ul>
      </div>
    </section>
  )
}
