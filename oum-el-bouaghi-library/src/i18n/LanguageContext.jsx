import { createContext, useContext, useMemo } from 'react'
import ar from './ar.js'
import fr from './fr.js'

const dictionaries = { ar, fr }
const LanguageContext = createContext(null)

/**
 * اللغة تُحدَّد من صفحة HTML نفسها: "/" للعربية و "/fr/" للفرنسية.
 * التبديل ينقل الزائر إلى الرابط الآخر (أفضل لمحركات البحث من تبديل داخلي فقط).
 */
export function LanguageProvider({ lang, children }) {
  const value = useMemo(() => {
    const t = dictionaries[lang]
    const base = import.meta.env.BASE_URL
    const otherLang = lang === 'ar' ? 'fr' : 'ar'
    const otherHref = otherLang === 'fr' ? `${base}fr/` : base
    /** يختار النص المناسب من كائن { ar, fr } */
    const pick = (obj) => (obj && typeof obj === 'object' && !Array.isArray(obj) ? obj[lang] ?? obj.ar : obj)
    const locale = lang === 'ar' ? 'ar-DZ-u-nu-latn' : 'fr-DZ'
    return { lang, t, pick, otherLang, otherHref, locale, dir: t.dir }
  }, [lang])
  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export const useLang = () => useContext(LanguageContext)
