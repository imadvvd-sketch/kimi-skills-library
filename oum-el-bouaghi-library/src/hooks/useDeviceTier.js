import { useEffect, useState } from 'react'

/**
 * يحدد مستوى العرض ثلاثي الأبعاد المناسب للجهاز:
 *  - 'full'   : حاسوب قوي → المشهد كاملاً
 *  - 'lite'   : هاتف أو جهاز متوسط → جزيئات أقل ودقة أقل
 *  - 'static' : جهاز ضعيف / بدون WebGL / توفير البيانات / reduced-motion → صورة ثابتة
 *
 * للتجربة يمكن فرض المستوى من الرابط: ?3d=full  أو  ?3d=lite  أو  ?3d=off
 */
function hasWebGL() {
  try {
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl')
    gl?.getExtension('WEBGL_lose_context')?.loseContext()
    return !!gl
  } catch {
    return false
  }
}

export function detectTier() {
  if (typeof window === 'undefined') return 'static'
  const forced = new URLSearchParams(window.location.search).get('3d')
  if (forced === 'full' || forced === 'lite') return forced
  if (forced === 'off') return 'static'

  const mq = (q) => window.matchMedia(q).matches
  if (mq('(prefers-reduced-motion: reduce)')) return 'static'
  const conn = navigator.connection
  if (conn?.saveData || /2g$/.test(conn?.effectiveType ?? '')) return 'static'
  if (!hasWebGL()) return 'static'

  const memory = navigator.deviceMemory ?? 8
  const cores = navigator.hardwareConcurrency ?? 4
  if (memory <= 2 || cores <= 2) return 'static'
  const mobile = mq('(pointer: coarse)') || window.innerWidth < 768
  if (mobile || memory <= 4 || cores <= 4) return 'lite'
  return 'full'
}

export default function useDeviceTier() {
  // نبدأ بـ 'static' (مطابق لـ HTML المولَّد مسبقاً) ثم نحدد المستوى الفعلي في المتصفح
  const [tier, setTier] = useState('static')
  useEffect(() => {
    setTier(detectTier())
    // إن غيّر الزائر إعداد تقليل الحركة أثناء التصفح
    const media = window.matchMedia('(prefers-reduced-motion: reduce)')
    const onChange = () => setTier(detectTier())
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])
  return [tier, setTier]
}
