import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import { branches } from '../data/branches.js'
import { navItems } from './navItems.js'
import Logo from './Logo.jsx'
import Ph from './Ph.jsx'
import AmazighBand, { AmazighPattern } from './AmazighBand.jsx'
import SocialLinks from './SocialLinks.jsx'

export default function Footer() {
  const { t, pick, lang } = useLang()
  const year = new Date().getFullYear()
  const items = navItems.filter((item) => item.id !== 'branches' || branches.length > 0)

  return (
    <footer className="relative mt-24 overflow-hidden bg-ink text-paper">
      <AmazighBand className="bg-parchment" opacity={0.9} />
      <AmazighPattern className="opacity-[0.05] invert" />
      <div className="container-page relative grid gap-10 py-14 md:grid-cols-3">
        <div>
          <div className="flex items-center gap-3">
            <Logo className="size-12" />
            <p className="font-display text-xl font-bold leading-snug">{pick(siteInfo.name)}</p>
          </div>
          <p className="mt-4 text-paper/80">{t.footer.tagline}</p>
        </div>

        <nav aria-labelledby="footer-links">
          <h2 id="footer-links" className="mb-4 font-display text-lg font-bold text-paper">
            {t.footer.links}
          </h2>
          <ul className="grid grid-cols-2 gap-2">
            {items.map((item) => (
              <li key={item.id}>
                <a href={item.href} className="text-paper/80 underline-offset-4 hover:text-paper hover:underline">
                  {t.nav[item.id]}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div>
          <h2 className="mb-4 font-display text-lg font-bold text-paper">{t.info.contact}</h2>
          <ul className="space-y-1 text-paper/80">
            <li><Ph>{pick(siteInfo.address.street)}</Ph>{lang === 'ar' ? '، ' : ', '}{pick(siteInfo.address.city)}</li>
            <li><span dir="ltr"><Ph>{siteInfo.phone}</Ph></span></li>
            <li><Ph>{siteInfo.email}</Ph></li>
          </ul>
          <SocialLinks className="mt-5" dark />
        </div>
      </div>
      <div className="relative border-t border-paper/15">
        <div className="container-page flex flex-col gap-2 py-5 text-sm text-paper/70 sm:flex-row sm:justify-between">
          <p>
            © {year} {pick(siteInfo.shortName)} — {t.footer.rights}
          </p>
          <p>
            {t.footer.under} {pick(siteInfo.ministry)}
          </p>
        </div>
      </div>
    </footer>
  )
}
