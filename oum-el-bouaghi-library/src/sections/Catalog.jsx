import { useDeferredValue, useId, useMemo, useState } from 'react'
import { AnimatePresence, m as motion } from 'framer-motion'
import { useLang } from '../i18n/LanguageContext.jsx'
import { searchCatalog } from '../lib/search.js'
import SectionHeading from '../components/SectionHeading.jsx'
import Icon from '../components/Icon.jsx'

const TYPES = ['all', 'book', 'children', 'periodical', 'digital']
const LANGS = ['all', 'ar', 'fr']

export default function Catalog() {
  const { t, dir } = useLang()
  const c = t.catalog
  const uid = useId()
  const [query, setQuery] = useState('')
  const [type, setType] = useState('all')
  const [docLang, setDocLang] = useState('all')
  const [availableOnly, setAvailableOnly] = useState(false)
  const deferredQuery = useDeferredValue(query)

  const results = useMemo(
    () => searchCatalog({ query: deferredQuery, type, lang: docLang, availableOnly }),
    [deferredQuery, type, docLang, availableOnly],
  )

  const reset = () => {
    setQuery('')
    setType('all')
    setDocLang('all')
    setAvailableOnly(false)
  }

  const selectCls =
    'w-full rounded-xl border-2 border-sand bg-paper px-3 py-2.5 text-ink focus:border-olive focus:outline-none'

  return (
    <section id="catalog" aria-labelledby="catalog-title" className="bg-parchment/60 py-24">
      <div className="container-page">
        <SectionHeading id="catalog-title" title={c.title} subtitle={c.subtitle} />

        <div className="card mx-auto max-w-4xl p-5 sm:p-8">
          <p className="mb-6 flex items-start gap-2 rounded-xl border border-dashed border-brown/40 bg-gold/10 px-4 py-3 text-sm text-ink-soft">
            <span aria-hidden="true">ⓘ</span>
            {c.demo}
          </p>

          <form role="search" onSubmit={(e) => e.preventDefault()} className="grid gap-4">
            <label htmlFor={`${uid}-q`} className="font-bold">
              {c.label}
            </label>
            <div className="relative">
              <Icon name="search" className="pointer-events-none absolute start-4 top-1/2 size-5 -translate-y-1/2 text-ink-soft" />
              <input
                id={`${uid}-q`}
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={c.placeholder}
                autoComplete="off"
                className="w-full rounded-2xl border-2 border-sand bg-paper py-4 ps-12 pe-4 text-lg placeholder:text-ink-soft/70 focus:border-olive focus:outline-none"
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
              <div>
                <label htmlFor={`${uid}-type`} className="mb-1 block text-sm font-bold">{c.type}</label>
                <select id={`${uid}-type`} value={type} onChange={(e) => setType(e.target.value)} className={selectCls}>
                  {TYPES.map((v) => (
                    <option key={v} value={v}>{v === 'all' ? c.all : c.types[v]}</option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor={`${uid}-lang`} className="mb-1 block text-sm font-bold">{c.language}</label>
                <select id={`${uid}-lang`} value={docLang} onChange={(e) => setDocLang(e.target.value)} className={selectCls}>
                  {LANGS.map((v) => (
                    <option key={v} value={v}>{v === 'all' ? c.all : c.langs[v]}</option>
                  ))}
                </select>
              </div>
              <label className="flex cursor-pointer items-center gap-2 py-2.5">
                <input
                  type="checkbox"
                  checked={availableOnly}
                  onChange={(e) => setAvailableOnly(e.target.checked)}
                  className="size-5 accent-olive"
                />
                {c.availableOnly}
              </label>
            </div>
          </form>

          <div className="mt-8 flex items-center justify-between gap-4 border-b border-sand pb-3">
            <p role="status" aria-live="polite" className="font-bold text-brown">
              {c.results(results.length)}
            </p>
            <button type="button" onClick={reset} className="text-sm text-olive-dark underline underline-offset-4 hover:text-ink">
              {c.reset}
            </button>
          </div>

          {results.length === 0 ? (
            <p className="py-10 text-center text-ink-soft">{c.noResults}</p>
          ) : (
            <ul className="mt-2 max-h-[32rem] divide-y divide-sand/70 overflow-y-auto pe-1">
              <AnimatePresence initial={false}>
                {results.map((item) => (
                  <motion.li
                    key={item.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="flex flex-wrap items-start justify-between gap-3 py-4"
                  >
                    <div className="min-w-0 flex-1">
                      {/* اتجاه النص يتبع لغة الوثيقة، والمحاذاة تتبع لغة الصفحة */}
                      <div dir={item.lang === 'ar' ? 'rtl' : 'ltr'} className={dir === 'rtl' ? 'text-right' : 'text-left'}>
                        <p className="font-display text-xl font-bold">
                          <bdi lang={item.lang}>{item.title}</bdi>
                        </p>
                        <p className="text-ink-soft">
                          <bdi lang={item.lang}>{item.author}</bdi>
                          {item.year ? ` · ${item.year}` : ''} · <bdi lang={item.lang}>{item.subject}</bdi>
                        </p>
                      </div>
                      <p className="mt-1 text-sm text-ink-soft">
                        {c.types[item.type]} · {c.langs[item.lang]} · {c.cote}: <span dir="ltr">{item.cote}</span>
                      </p>
                    </div>
                    <span
                      className={`shrink-0 rounded-full px-3 py-1 text-sm font-bold ${
                        item.available ? 'bg-olive/15 text-olive-dark' : 'bg-brown/10 text-brown'
                      }`}
                    >
                      {item.available ? c.available : c.unavailable}
                    </span>
                  </motion.li>
                ))}
              </AnimatePresence>
            </ul>
          )}
        </div>
      </div>
    </section>
  )
}
