import { useLang } from './i18n/LanguageContext.jsx'
import Header from './components/Header.jsx'
import Footer from './components/Footer.jsx'
import Hero from './sections/Hero.jsx'
import About from './sections/About.jsx'
import Services from './sections/Services.jsx'
import Events from './sections/Events.jsx'
import Catalog from './sections/Catalog.jsx'
import Branches from './sections/Branches.jsx'
import Info from './sections/Info.jsx'

export default function App() {
  const { t } = useLang()
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:start-4 focus:top-4 focus:z-[100] focus:rounded-lg focus:bg-olive focus:px-4 focus:py-2 focus:text-paper"
      >
        {t.nav.skip}
      </a>
      <Header />
      <main id="main" tabIndex={-1}>
        <Hero />
        <About />
        <Services />
        <Events />
        <Catalog />
        <Branches />
        <Info />
      </main>
      <Footer />
    </>
  )
}
