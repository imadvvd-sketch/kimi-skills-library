import { useLang } from '../i18n/LanguageContext.jsx'
import { services } from '../data/services.js'
import SectionHeading from '../components/SectionHeading.jsx'
import Reveal from '../components/Reveal.jsx'
import Icon from '../components/Icon.jsx'
import Ph from '../components/Ph.jsx'

export default function Services() {
  const { t, pick } = useLang()
  return (
    <section id="services" aria-labelledby="services-title" className="relative bg-parchment/60 py-24">
      <div className="container-page">
        <SectionHeading id="services-title" title={t.services.title} subtitle={t.services.subtitle} />

        {/* مكان رف الكتب ثلاثي الأبعاد (المرحلة 3) */}

        <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {services.map((s, i) => (
            <Reveal as="li" key={s.id} delay={(i % 3) * 0.1} className="card group relative overflow-hidden p-7" >
              <span
                aria-hidden="true"
                className="absolute inset-y-0 start-0 w-2"
                style={{ background: s.color }}
              />
              <span
                aria-hidden="true"
                className="mb-5 inline-flex size-14 items-center justify-center rounded-2xl text-paper transition-transform duration-300 group-hover:-rotate-6"
                style={{ background: s.color }}
              >
                <Icon name={s.icon} className="size-7" />
              </span>
              <h3 id={`service-${s.id}`} className="text-2xl font-bold">{pick(s.title)}</h3>
              <p className="mt-2 text-ink-soft"><Ph>{pick(s.description)}</Ph></p>
            </Reveal>
          ))}
        </ul>
      </div>
    </section>
  )
}
