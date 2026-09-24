import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import Icon from './Icon.jsx'
import { AmazighPattern } from './AmazighBand.jsx'

/**
 * خريطة OpenStreetMap (مجانية وبدون مفتاح API) تُحمَّل كسولاً.
 * تظهر فقط بعد إضافة الإحداثيات في siteInfo.geo
 */
export default function MapEmbed() {
  const { t, pick } = useLang()
  const { lat, lng } = siteInfo.geo
  const hasGeo = typeof lat === 'number' && typeof lng === 'number'

  if (!hasGeo) {
    return (
      <div className="card relative flex min-h-64 w-full items-center justify-center overflow-hidden p-6 text-center sm:aspect-[16/7]">
        <AmazighPattern className="opacity-[0.07]" />
        <p className="relative flex flex-col items-center gap-3 text-ink-soft">
          <Icon name="pin" className="size-10 text-olive" />
          <span className="placeholder-text">{t.info.mapPlaceholder}</span>
        </p>
      </div>
    )
  }

  const d = 0.006
  const bbox = [lng - d, lat - d / 2, lng + d, lat + d / 2].join(',')
  const src = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat},${lng}`
  return (
    <div className="card overflow-hidden">
      <iframe
        title={`${t.info.map} — ${pick(siteInfo.name)}`}
        src={src}
        loading="lazy"
        className="aspect-[16/7] min-h-72 w-full border-0"
        referrerPolicy="no-referrer-when-downgrade"
      />
      <a
        href={`https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`}
        target="_blank"
        rel="noopener noreferrer"
        className="flex items-center gap-2 p-4 font-bold text-olive-dark hover:underline"
      >
        <Icon name="pin" className="size-5" /> {t.info.openMap}
      </a>
    </div>
  )
}
