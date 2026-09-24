import { useLang } from '../i18n/LanguageContext.jsx'
import { events } from '../data/events.js'
import SectionHeading from '../components/SectionHeading.jsx'
import Reveal from '../components/Reveal.jsx'
import Icon from '../components/Icon.jsx'
import Ph from '../components/Ph.jsx'

export default function Events() {
  const { t, pick, locale } = useLang()
  const upcoming = [...events].sort((a, b) => a.date.localeCompare(b.date))
  const day = new Intl.DateTimeFormat(locale, { day: 'numeric' })
  const month = new Intl.DateTimeFormat(locale, { month: 'long' })
  const full = new Intl.DateTimeFormat(locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <section id="events" aria-labelledby="events-title" className="py-24">
      <div className="container-page">
        <SectionHeading id="events-title" title={t.events.title} subtitle={t.events.subtitle} />
        {upcoming.length === 0 ? (
          <p className="text-center text-ink-soft">{t.events.none}</p>
        ) : (
          <ul className="grid gap-6 md:grid-cols-2">
            {upcoming.map((e, i) => {
              const date = new Date(`${e.date}T12:00:00`)
              return (
                <Reveal as="li" key={e.id} delay={(i % 2) * 0.12}>
                  <article className="card flex h-full gap-5 p-6 transition hover:-translate-y-1 hover:shadow-xl">
                    <time
                      dateTime={e.date}
                      className="flex w-20 shrink-0 flex-col items-center justify-center rounded-xl bg-olive py-3 text-paper"
                    >
                      <span className="font-display text-4xl leading-none font-bold">{day.format(date)}</span>
                      <span className="mt-1 text-sm">{month.format(date)}</span>
                    </time>
                    <div className="min-w-0">
                      <div className="mb-2 flex flex-wrap items-center gap-2 text-sm">
                        <span className="rounded-full bg-brown/10 px-3 py-0.5 font-bold text-brown">
                          {t.events.categories[e.category]}
                        </span>
                        {e.isSample && (
                          <span className="rounded-full border border-dashed border-brown/50 px-3 py-0.5 text-ink-soft">
                            {t.events.sample}
                          </span>
                        )}
                      </div>
                      <h3 className="text-xl font-bold sm:text-2xl">{pick(e.title)}</h3>
                      <p className="mt-1 text-ink-soft">{pick(e.description)}</p>
                      <p className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm text-ink-soft">
                        <span className="inline-flex items-center gap-1">
                          <Icon name="calendar" className="size-4" /> {full.format(date)} — <Ph>{e.time}</Ph>
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <Icon name="pin" className="size-4" /> {pick(e.place)}
                        </span>
                      </p>
                    </div>
                  </article>
                </Reveal>
              )
            })}
          </ul>
        )}
      </div>
    </section>
  )
}
