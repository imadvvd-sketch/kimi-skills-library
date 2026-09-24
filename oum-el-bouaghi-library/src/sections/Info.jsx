import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo, isPlaceholder } from '../data/siteInfo.js'
import SectionHeading from '../components/SectionHeading.jsx'
import Reveal from '../components/Reveal.jsx'
import Icon from '../components/Icon.jsx'
import Ph from '../components/Ph.jsx'
import SocialLinks from '../components/SocialLinks.jsx'
import MapEmbed from '../components/MapEmbed.jsx'

const WEEK = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

function formatDays(days, t, lang) {
  const known = days.every((d) => WEEK.includes(d))
  if (!known) return days.join(lang === 'ar' ? '، ' : ', ')
  const idx = days.map((d) => WEEK.indexOf(d))
  const consecutive = idx.every((v, i) => i === 0 || v === idx[i - 1] + 1)
  if (days.length > 2 && consecutive) return `${t.info.days[days[0]]} ${t.info.to} ${t.info.days[days.at(-1)]}`
  return days.map((d) => t.info.days[d]).join(lang === 'ar' ? '، ' : ', ')
}

function ContactRow({ icon, label, children, href }) {
  const content = href ? (
    <a href={href} className="font-bold text-ink underline-offset-4 hover:text-olive-dark hover:underline">{children}</a>
  ) : (
    <span className="font-bold text-ink">{children}</span>
  )
  return (
    <li className="flex items-start gap-4">
      <span aria-hidden="true" className="inline-flex size-11 shrink-0 items-center justify-center rounded-xl bg-olive/10 text-olive-dark">
        <Icon name={icon} className="size-5" />
      </span>
      <span className="flex flex-col">
        <span className="text-sm text-ink-soft">{label}</span>
        {content}
      </span>
    </li>
  )
}

export default function Info() {
  const { t, pick, lang } = useLang()
  const { phone, fax, email, address } = siteInfo
  return (
    <section id="info" aria-labelledby="info-title" className="bg-parchment/60 py-24">
      <div className="container-page">
        <SectionHeading id="info-title" title={t.info.title} />

        <div className="grid gap-6 lg:grid-cols-2">
          <Reveal className="card p-7">
            <h3 className="mb-5 flex items-center gap-2 text-2xl font-bold">
              <Icon name="clock" className="size-6 text-olive" /> {t.info.hours}
            </h3>
            <table className="w-full">
              <caption className="sr-only">{t.info.hours}</caption>
              <tbody>
                {siteInfo.openingHours.map((h, i) => (
                  <tr key={i} className="border-b border-sand/70 last:border-0">
                    <th scope="row" className="py-3 text-start font-medium">
                      <Ph>{formatDays(h.days, t, lang)}</Ph>
                    </th>
                    <td className="py-3 text-end font-bold" dir="ltr">
                      <Ph>{h.opens}</Ph> – <Ph>{h.closes}</Ph>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <p className="mt-4 text-sm text-ink-soft"><Ph>{pick(siteInfo.hoursNote)}</Ph></p>
          </Reveal>

          <Reveal delay={0.1} className="card p-7">
            <h3 className="mb-5 flex items-center gap-2 text-2xl font-bold">
              <Icon name="mail" className="size-6 text-olive" /> {t.info.contact}
            </h3>
            <ul className="space-y-4">
              <ContactRow icon="pin" label={t.info.address}>
                <Ph>{pick(address.street)}</Ph>{lang === 'ar' ? '، ' : ', '}{pick(address.city)} <Ph>{address.postalCode}</Ph>
              </ContactRow>
              <ContactRow icon="phone" label={t.info.phone} href={isPlaceholder(phone) ? undefined : `tel:${phone.replace(/\s/g, '')}`}>
                <span dir="ltr"><Ph>{phone}</Ph></span>
              </ContactRow>
              <ContactRow icon="fax" label={t.info.fax}>
                <span dir="ltr"><Ph>{fax}</Ph></span>
              </ContactRow>
              <ContactRow icon="mail" label={t.info.email} href={isPlaceholder(email) ? undefined : `mailto:${email}`}>
                <Ph>{email}</Ph>
              </ContactRow>
            </ul>
            <p className="mt-6 mb-3 font-bold">{t.info.social}</p>
            <SocialLinks />
          </Reveal>
        </div>

        <Reveal delay={0.1} className="mt-6">
          <MapEmbed />
        </Reveal>
      </div>
    </section>
  )
}
