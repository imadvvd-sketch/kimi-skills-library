/**
 * يُستعمل عند البناء فقط: يولّد HTML الصفحة مسبقاً (prerender)
 * حتى يظهر المحتوى فوراً ولمحركات البحث قبل تحميل JavaScript.
 */
import { renderToString } from 'react-dom/server'
import { LazyMotion, MotionConfig, domAnimation } from 'framer-motion'
import { LanguageProvider } from './i18n/LanguageContext.jsx'
import App from './App.jsx'

export function render(lang) {
  return renderToString(
    <LazyMotion features={domAnimation} strict>
      <MotionConfig reducedMotion="user">
        <LanguageProvider lang={lang}>
          <App />
        </LanguageProvider>
      </MotionConfig>
    </LazyMotion>,
  )
}
