import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, m as motion } from 'framer-motion'
import { useLang } from '../i18n/LanguageContext.jsx'
import { siteInfo } from '../data/siteInfo.js'
import { branches } from '../data/branches.js'
import { navItems } from './navItems.js'
import Logo from './Logo.jsx'
import Icon from './Icon.jsx'
import ScrollProgress from './ScrollProgress.jsx'

export default function Header() {
  const { t, lang, pick, otherHref, otherLang } = useLang()
  const [open, setOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const menuButton = useRef(null)
  const items = navItems.filter((item) => item.id !== 'branches' || branches.length > 0)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // إغلاق القائمة بزر Escape وإرجاع التركيز إلى زر القائمة
  useEffect(() => {
    if (!open) return
    const onKey = (e) => {
      if (e.key === 'Escape') {
        setOpen(false)
        menuButton.current?.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  const switchLang = (e) => {
    // نحافظ على القسم الحالي عند تبديل اللغة
    e.currentTarget.href = otherHref + window.location.hash
  }

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 transition-all duration-300 ${
        scrolled || open ? 'bg-paper/90 shadow-[0_6px_20px_-12px_rgb(46_33_22/0.35)] backdrop-blur-md' : 'bg-transparent'
      }`}
    >
      <ScrollProgress />
      <div className="container-page flex h-18 items-center justify-between gap-4">
        <a href="#home" className="flex min-w-0 items-center gap-3">
          <Logo className="size-11 shrink-0" />
          <span className="min-w-0 leading-tight">
            <span className="block truncate font-display text-lg font-bold text-ink">{pick(siteInfo.shortName)}</span>
            <span className="hidden truncate text-xs text-ink-soft sm:block">{t.hero.title}</span>
          </span>
        </a>

        <nav aria-label={t.nav.menu} className="hidden xl:block">
          <ul className="flex items-center gap-1">
            {items.map((item) => (
              <li key={item.id}>
                <a
                  href={item.href}
                  className="whitespace-nowrap rounded-full px-3 py-2 text-[0.95rem] font-medium text-ink-soft transition hover:bg-parchment hover:text-ink"
                >
                  {t.nav[item.id]}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="flex items-center gap-2">
          <a
            href={otherHref}
            onClick={switchLang}
            hrefLang={otherLang}
            lang={otherLang}
            aria-label={`${t.nav.switchTo} — ${t.nav.switchLabel}`}
            className="inline-flex items-center gap-1.5 rounded-full border-2 border-olive/40 px-3 py-1.5 text-sm font-bold text-olive-dark transition hover:border-olive hover:bg-olive hover:text-paper"
          >
            <Icon name="globe" className="size-4" />
            {t.nav.switchTo}
          </a>
          <button
            ref={menuButton}
            type="button"
            className="inline-flex size-11 items-center justify-center rounded-full text-ink hover:bg-parchment xl:hidden"
            aria-expanded={open}
            aria-controls="mobile-menu"
            aria-label={open ? t.nav.close : t.nav.menu}
            onClick={() => setOpen((v) => !v)}
          >
            <Icon name={open ? 'close' : 'menu'} />
          </button>
        </div>
      </div>

      <AnimatePresence>
        {open && (
          <motion.nav
            id="mobile-menu"
            aria-label={t.nav.menu}
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden border-t border-sand/60 xl:hidden"
            lang={lang}
          >
            <ul className="container-page grid gap-1 py-4">
              {items.map((item) => (
                <li key={item.id}>
                  <a
                    href={item.href}
                    onClick={() => setOpen(false)}
                    className="block rounded-xl px-4 py-3 text-lg font-medium hover:bg-parchment"
                  >
                    {t.nav[item.id]}
                  </a>
                </li>
              ))}
            </ul>
          </motion.nav>
        )}
      </AnimatePresence>
    </header>
  )
}
