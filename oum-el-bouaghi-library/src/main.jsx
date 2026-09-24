import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { LazyMotion, MotionConfig, domAnimation } from 'framer-motion'
import { LanguageProvider } from './i18n/LanguageContext.jsx'
import App from './App.jsx'
import './styles/index.css'

const lang = document.documentElement.lang === 'fr' ? 'fr' : 'ar'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* reducedMotion="user": تُلغى الحركات تلقائياً إن فعّل الزائر prefers-reduced-motion */}
    <LazyMotion features={domAnimation} strict>
      <MotionConfig reducedMotion="user">
        <LanguageProvider lang={lang}>
          <App />
        </LanguageProvider>
      </MotionConfig>
    </LazyMotion>
  </StrictMode>,
)
