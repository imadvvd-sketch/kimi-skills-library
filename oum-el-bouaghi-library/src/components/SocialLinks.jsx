import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import Icon from './Icon.jsx'

const labels = { facebook: 'Facebook', instagram: 'Instagram', youtube: 'YouTube' }

export default function SocialLinks({ className = '', dark = false }) {
  const { t } = useLang()
  const base = dark
    ? 'border-paper/30 text-paper hover:bg-paper hover:text-ink'
    : 'border-brown/30 text-brown hover:bg-brown hover:text-paper'
  return (
    <ul className={`flex gap-3 ${className}`} aria-label={t.info.social}>
      {Object.entries(siteInfo.social).map(([key, url]) => {
        const ready = /^https?:\/\//.test(url)
        const cls = `inline-flex size-11 items-center justify-center rounded-full border transition ${base}`
        return (
          <li key={key}>
            {ready ? (
              <a href={url} target="_blank" rel="noopener noreferrer" className={cls} aria-label={labels[key]}>
                <Icon name={key} className="size-5" />
              </a>
            ) : (
              // رابط غير مكتمل في siteInfo.js: يظهر باهتاً ولا يُنقر
              <span className={`${cls} cursor-not-allowed opacity-40`} title={`${labels[key]} — ${url}`}>
                <Icon name={key} className="size-5" title={`${labels[key]} ${url}`} />
              </span>
            )}
          </li>
        )
      })}
    </ul>
  )
}
