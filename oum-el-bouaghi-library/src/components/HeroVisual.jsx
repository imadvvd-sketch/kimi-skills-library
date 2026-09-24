import { lazy, Suspense, useEffect, useState } from 'react'
import { useLang } from '../i18n/LanguageContext.jsx'
import useDeviceTier from '../hooks/useDeviceTier.js'
import HeroIllustration from './HeroIllustration.jsx'
import SceneLoader from '../three/SceneLoader.jsx'
import SceneBoundary from './SceneBoundary.jsx'

// المشهد ثلاثي الأبعاد في ملف منفصل يُحمَّل لاحقاً (three.js لا يُثقل التحميل الأول)
const HeroScene = lazy(() => import('../three/HeroScene.jsx'))

const whenIdle = (cb) =>
  'requestIdleCallback' in window ? requestIdleCallback(cb, { timeout: 1500 }) : setTimeout(cb, 600)
const cancelIdle = (id) => ('cancelIdleCallback' in window ? cancelIdleCallback(id) : clearTimeout(id))

export default function HeroVisual() {
  const { t } = useLang()
  const [tier, setTier] = useDeviceTier()
  const [load, setLoad] = useState(false)
  const [ready, setReady] = useState(false)
  const use3d = tier !== 'static'

  useEffect(() => {
    if (!use3d) return
    // نبدأ التحميل بعد اكتمال الصفحة ووقت فراغ المتصفح
    let id
    const start = () => (id = whenIdle(() => setLoad(true)))
    if (document.readyState === 'complete') start()
    else window.addEventListener('load', start, { once: true })
    return () => {
      window.removeEventListener('load', start)
      if (id) cancelIdle(id)
    }
  }, [use3d])

  return (
    <div role="img" aria-label={t.hero.sceneAlt} className="relative mx-auto aspect-[6/5] w-full max-w-xl">
      <div className={`absolute inset-0 transition-opacity duration-1000 ${ready && use3d ? 'opacity-0' : 'opacity-100'}`}>
        <HeroIllustration />
      </div>
      {use3d && load && (
        <div
          className={`scene-mask absolute -inset-x-[12%] -inset-y-[14%] transition-opacity duration-1000 ${ready ? 'opacity-100' : 'opacity-0'}`}
        >
          <SceneBoundary onFail={() => setTier('static')}>
            <Suspense fallback={null}>
              <HeroScene tier={tier} onReady={() => setReady(true)} />
            </Suspense>
          </SceneBoundary>
        </div>
      )}
      {use3d && !ready && load && <SceneLoader label={t.loader.label} />}
    </div>
  )
}
