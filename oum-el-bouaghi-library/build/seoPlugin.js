/**
 * إضافة Vite تحقن وسوم SEO و Open Graph وبيانات Schema.org في صفحتي HTML،
 * وتولّد sitemap.xml و robots.txt عند البناء — كلها من src/data/siteInfo.js
 */
import { siteInfo, isPlaceholder } from '../src/data/siteInfo.js'
import ar from '../src/i18n/ar.js'
import fr from '../src/i18n/fr.js'

const dict = { ar, fr }
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
const pageUrl = (lang) => `${siteInfo.siteUrl}${lang === 'fr' ? '/fr/' : '/'}`
const valid = (v) => !isPlaceholder(v)
const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
const TIME = /^\d{2}:\d{2}$/

export function buildJsonLd(lang) {
  const other = lang === 'ar' ? 'fr' : 'ar'
  const a = siteInfo.address
  const data = {
    '@context': 'https://schema.org',
    '@type': 'Library',
    '@id': `${siteInfo.siteUrl}/#library`,
    name: siteInfo.name[lang],
    alternateName: siteInfo.name[other],
    url: pageUrl(lang),
    image: `${siteInfo.siteUrl}/og-image.jpg`,
    logo: `${siteInfo.siteUrl}/favicon.svg`,
    inLanguage: lang,
    address: {
      '@type': 'PostalAddress',
      ...(valid(a.street[lang]) && { streetAddress: a.street[lang] }),
      addressLocality: a.city[lang],
      addressRegion: a.region[lang],
      ...(valid(a.postalCode) && { postalCode: a.postalCode }),
      addressCountry: a.country,
    },
  }
  if (valid(siteInfo.phone)) data.telephone = siteInfo.phone
  if (valid(siteInfo.email)) data.email = siteInfo.email
  if (typeof siteInfo.geo.lat === 'number' && typeof siteInfo.geo.lng === 'number') {
    data.geo = { '@type': 'GeoCoordinates', latitude: siteInfo.geo.lat, longitude: siteInfo.geo.lng }
  }
  const hours = siteInfo.openingHours
    .filter((h) => TIME.test(h.opens) && TIME.test(h.closes) && h.days.every((d) => DAYS.includes(d)))
    .map((h) => ({ '@type': 'OpeningHoursSpecification', dayOfWeek: h.days, opens: h.opens, closes: h.closes }))
  if (hours.length) data.openingHoursSpecification = hours
  const sameAs = Object.values(siteInfo.social).filter((u) => /^https?:\/\//.test(u))
  if (sameAs.length) data.sameAs = sameAs
  if (valid(siteInfo.foundedYear)) data.foundingDate = String(siteInfo.foundedYear)
  return data
}

function headTags(lang) {
  const { seo } = dict[lang]
  const other = lang === 'ar' ? 'fr' : 'ar'
  const url = pageUrl(lang)
  const img = `${siteInfo.siteUrl}/og-image.jpg`
  return `
    <title>${esc(seo.title)}</title>
    <meta name="description" content="${esc(seo.description)}" />
    <meta name="keywords" content="${esc(seo.keywords)}" />
    <meta name="theme-color" content="#F5EDDC" />
    <link rel="canonical" href="${url}" />
    <link rel="alternate" hreflang="ar" href="${pageUrl('ar')}" />
    <link rel="alternate" hreflang="fr" href="${pageUrl('fr')}" />
    <link rel="alternate" hreflang="x-default" href="${pageUrl('ar')}" />
    <meta property="og:type" content="website" />
    <meta property="og:site_name" content="${esc(siteInfo.name[lang])}" />
    <meta property="og:title" content="${esc(seo.title)}" />
    <meta property="og:description" content="${esc(seo.description)}" />
    <meta property="og:url" content="${url}" />
    <meta property="og:image" content="${img}" />
    <meta property="og:image:width" content="1200" />
    <meta property="og:image:height" content="630" />
    <meta property="og:locale" content="${seo.ogLocale}" />
    <meta property="og:locale:alternate" content="${dict[other].seo.ogLocale}" />
    <meta name="twitter:card" content="summary_large_image" />
    <meta name="twitter:title" content="${esc(seo.title)}" />
    <meta name="twitter:description" content="${esc(seo.description)}" />
    <meta name="twitter:image" content="${img}" />
    <script type="application/ld+json">${JSON.stringify(buildJsonLd(lang)).replace(/</g, '\\u003c')}</script>`
}

export default function seoPlugin() {
  return {
    name: 'library-seo',
    transformIndexHtml(html, ctx) {
      const lang = /[\\/]fr[\\/]index\.html$/.test(ctx.filename) ? 'fr' : 'ar'
      return html.replace('<!-- seo-head -->', headTags(lang))
    },
    generateBundle() {
      const today = new Date().toISOString().slice(0, 10)
      const entry = (lang) => `  <url>
    <loc>${pageUrl(lang)}</loc>
    <lastmod>${today}</lastmod>
    <xhtml:link rel="alternate" hreflang="ar" href="${pageUrl('ar')}" />
    <xhtml:link rel="alternate" hreflang="fr" href="${pageUrl('fr')}" />
  </url>`
      this.emitFile({
        type: 'asset',
        fileName: 'sitemap.xml',
        source: `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${entry('ar')}
${entry('fr')}
</urlset>
`,
      })
      this.emitFile({
        type: 'asset',
        fileName: 'robots.txt',
        source: `User-agent: *\nAllow: /\n\nSitemap: ${siteInfo.siteUrl}/sitemap.xml\n`,
      })
    },
  }
}
