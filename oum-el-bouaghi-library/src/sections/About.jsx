import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import SectionHeading from '../components/SectionHeading.jsx'
import Reveal from '../components/Reveal.jsx'
import Ph from '../components/Ph.jsx'
import AmazighBand from '../components/AmazighBand.jsx'

const missionIcons = ['M12 4l2 4 4 .6-3 3 .7 4.4L12 14l-3.7 2 .7-4.4-3-3L10 8z', 'M5 4h14v16l-7-4-7 4z', 'M4 12a8 8 0 1 1 16 0M8 12h8M12 8v8']

export default function About() {
  const { t, pick } = useLang()
  return (
    <section id="about" aria-labelledby="about-title" className="relative py-24">
      <AmazighBand className="absolute inset-x-0 top-0" />
      <div className="container-page">
        <SectionHeading id="about-title" title={t.about.title} subtitle={t.about.lead} />

        <div className="grid gap-12 lg:grid-cols-2">
          <Reveal className="space-y-5 text-lg">
            {t.about.body.map((p, i) => (
              <p key={i}>
                <Ph>{p}</Ph>
              </p>
            ))}
            <p className="text-ink-soft">
              <span className="font-bold text-ink">{t.about.founded}:</span> <Ph>{String(siteInfo.foundedYear)}</Ph>
            </p>
          </Reveal>

          <Reveal delay={0.15}>
            <dl className="grid grid-cols-2 gap-4">
              {siteInfo.stats.map((s, i) => (
                <div key={i} className="card flex flex-col-reverse p-6 text-center">
                  <dt className="mt-1 text-ink-soft">{pick(s.label)}</dt>
                  <dd className="font-display text-3xl font-bold text-brown">
                    <Ph>{s.value}</Ph>
                  </dd>
                </div>
              ))}
            </dl>
          </Reveal>
        </div>

        <h3 className="mt-20 mb-8 text-center text-3xl font-bold">{t.about.missionTitle}</h3>
        <ul className="grid gap-6 md:grid-cols-3">
          {t.about.missions.map((m, i) => (
            <Reveal as="li" key={i} delay={i * 0.12} className="card relative overflow-hidden p-7">
              <span aria-hidden="true" className="mb-4 inline-flex size-12 items-center justify-center rounded-xl bg-olive text-paper">
                <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round">
                  <path d={missionIcons[i]} />
                </svg>
              </span>
              <h4 className="font-display text-2xl font-bold">{m.title}</h4>
              <p className="mt-2 text-ink-soft">{m.text}</p>
              <span aria-hidden="true" className="absolute -end-6 -bottom-6 size-20 rotate-45 border-4 border-sand/50" />
            </Reveal>
          ))}
        </ul>
      </div>
    </section>
  )
}
