import { createContext, useContext, useLayoutEffect, useMemo, useState } from 'react'
import ar from './ar.js'
import fr from './fr.js'

const dictionaries = { ar, fr }
const LanguageContext = createContext(null)

// نسخة المعاينة (صفحة واحدة): تبديل اللغة داخل الصفحة بدل الانتقال إلى /fr/
const SINGLE_PAGE = import.meta.env.VITE_SINGLE_PAGE === '1'
const useIsoLayoutEffect = typeof window === 'undefined' ? () => {} : useLayoutEffect

/**
 * اللغة تُحدَّد من صفحة HTML نفسها: "/" للعربية و "/fr/" للفرنسية.
 * التبديل ينقل الزائر إلى الرابط الآخر (أفضل لمحركات البحث من تبديل داخلي فقط).
 */
export function LanguageProvider({ lang: initialLang, children }) {
  const [lang, setLang] = useState(initialLang)

  useIsoLayoutEffect(() => {
    if (!SINGLE_PAGE) return
    document.documentElement.lang = lang
    document.documentElement.dir = dictionaries[lang].dir
  }, [lang])

  const value = useMemo(() => {
    const t = dictionaries[lang]
    const base = import.meta.env.BASE_URL
    const otherLang = lang === 'ar' ? 'fr' : 'ar'
    const otherHref = otherLang === 'fr' ? `${base}fr/` : base
    /** يختار النص المناسب من كائن { ar, fr } */
    const pick = (obj) => (obj && typeof obj === 'object' && !Array.isArray(obj) ? obj[lang] ?? obj.ar : obj)
    const locale = lang === 'ar' ? 'ar-DZ-u-nu-latn' : 'fr-DZ'
    const switchInPlace = SINGLE_PAGE ? () => setLang(otherLang) : null
    return { lang, t, pick, otherLang, otherHref, locale, dir: t.dir, switchInPlace }
  }, [lang])
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export const useLang = () => useContext(LanguageContext)
