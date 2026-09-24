import { useLang } from '../i18n/LanguageContext.jsx'
import { branches } from '../data/branches.js'
import SectionHeading from '../components/SectionHeading.jsx'
import Reveal from '../components/Reveal.jsx'
import Icon from '../components/Icon.jsx'
import Ph from '../components/Ph.jsx'

export default function Branches() {
  const { t, pick } = useLang()
  if (branches.length === 0) return null // القسم يختفي إن كانت القائمة فارغة
  return (
    <section id="branches" aria-labelledby="branches-title" className="py-24">
      <div className="container-page">
        <SectionHeading id="branches-title" title={t.branches.title} subtitle={t.branches.subtitle} />
        <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {branches.map((b, i) => (
            <Reveal as="li" key={b.id} delay={(i % 3) * 0.1} className="card p-6">
              <p className="mb-2 inline-flex items-center gap-1.5 text-sm font-bold text-olive-dark">
                <Icon name="location" className="size-4" />
                <Ph>{pick(b.commune)}</Ph>
              </p>
              <h3 className="text-xl font-bold"><Ph>{pick(b.name)}</Ph></h3>
              <p className="mt-3 flex items-start gap-2 text-ink-soft">
                <Icon name="pin" className="mt-1 size-4 shrink-0" />
                <Ph>{pick(b.address)}</Ph>
              </p>
              <p className="mt-1 flex items-center gap-2 text-ink-soft">
                <Icon name="phone" className="size-4 shrink-0" />
                <span className="sr-only">{t.branches.phone}: </span>
                <span dir="ltr"><Ph>{b.phone}</Ph></span>
              </p>
            </Reveal>
          ))}
        </ul>
      </div>
    </section>
  )
}
