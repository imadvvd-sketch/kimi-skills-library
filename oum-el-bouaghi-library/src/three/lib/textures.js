import { CanvasTexture, SRGBColorSpace } from 'three'

/** حروف عربية وتيفيناغ تطفو من الكتاب */
export const GLYPHS = [
  'ا', 'ب', 'ت', 'ج', 'د', 'ر', 'س', 'ع', 'ق', 'ك', 'ل', 'م', 'ن', 'ه', 'و', 'ي',
  'ⴰ', 'ⴱ', 'ⴷ', 'ⵎ', 'ⵏ', 'ⵜ', 'ⵣ', 'ⵍ', 'ⵔ',
]
export const ATLAS_COLS = 8
export const ATLAS_ROWS = 4

/** ننتظر تحميل الخطوط قبل رسمها على canvas (بحد أقصى 2.5 ثانية) */
export async function waitForFonts() {
  if (!document.fonts?.load) return
  const timeout = new Promise((r) => setTimeout(r, 2500))
  await Promise.race([
    Promise.all([
      document.fonts.load('96px Amiri', 'ابت'),
      document.fonts.load('96px "Noto Sans Tifinagh"', 'ⴰⴱ'),
    ]).catch(() => {}),
    timeout,
  ])
}

/** أطلس الحروف: شبكة 8×4 خلايا، كل حرف أبيض بتوهج خفيف (يُلوَّن في الـ shader) */
export function createGlyphAtlas() {
  const cell = 128
  const canvas = document.createElement('canvas')
  canvas.width = ATLAS_COLS * cell
  canvas.height = ATLAS_ROWS * cell
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#fff'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.shadowColor = 'rgba(255,255,255,0.9)'
  ctx.shadowBlur = 10
  GLYPHS.forEach((g, i) => {
    const x = (i % ATLAS_COLS) * cell + cell / 2
    const y = Math.floor(i / ATLAS_COLS) * cell + cell / 2
    const tifinagh = g.charCodeAt(0) >= 0x2d30
    ctx.font = tifinagh ? '72px "Noto Sans Tifinagh", sans-serif' : 'bold 88px Amiri, serif'
    ctx.fillText(g, x, y + (tifinagh ? 0 : 8))
  })
  const tex = new CanvasTexture(canvas)
  tex.colorSpace = SRGBColorSpace
  return tex
}

/** صفحة كتاب: ورق كريمي مع أسطر توحي بالنص وزخرفة معيّنات صغيرة */
export function createPageTexture(seed = 1) {
  const w = 512
  const h = 704
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  const grad = ctx.createLinearGradient(0, 0, w, 0)
  grad.addColorStop(0, '#E9DCC0') // ظل قرب الكعب
  grad.addColorStop(0.12, '#F8F1E2')
  grad.addColorStop(1, '#FBF6EA')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, w, h)

  let s = seed * 9301
  const rand = () => ((s = (s * 9301 + 49297) % 233280) / 233280)

  // عنوان
  ctx.fillStyle = 'rgba(122, 82, 48, 0.55)'
  ctx.beginPath()
  ctx.roundRect(w * 0.3, 70, w * 0.45, 16, 8)
  ctx.fill()
  // زخرفة معيّنات
  ctx.fillStyle = 'rgba(86, 107, 46, 0.6)'
  for (let i = -1; i <= 1; i++) {
    const cx = w * 0.53 + i * 22
    ctx.beginPath()
    ctx.moveTo(cx, 108); ctx.lineTo(cx + 6, 114); ctx.lineTo(cx, 120); ctx.lineTo(cx - 6, 114)
    ctx.fill()
  }
  // أسطر النص (محاذاة يمينية كالكتاب العربي)
  ctx.fillStyle = 'rgba(58, 42, 28, 0.28)'
  for (let y = 150; y < h - 70; y += 26) {
    if (rand() < 0.08) continue // فقرة جديدة
    const len = (0.55 + rand() * 0.28) * w
    const right = w - 56
    let x = right
    while (x > right - len) {
      const word = 18 + rand() * 46
      ctx.beginPath()
      ctx.roundRect(Math.max(x - word, right - len), y, Math.min(word, x - (right - len)), 7, 3.5)
      ctx.fill()
      x -= word + 9
    }
  }
  const tex = new CanvasTexture(canvas)
  tex.colorSpace = SRGBColorSpace
  tex.anisotropy = 4
  return tex
}

/** توهج دافئ دائري خلف الكتاب */
export function createGlowTexture() {
  const size = 256
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = size
  const ctx = canvas.getContext('2d')
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  g.addColorStop(0, 'rgba(243, 211, 139, 0.95)')
  g.addColorStop(0.45, 'rgba(236, 196, 120, 0.35)')
  g.addColorStop(1, 'rgba(236, 196, 120, 0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  const tex = new CanvasTexture(canvas)
  tex.colorSpace = SRGBColorSpace
  return tex
}

/** ظل ناعم تحت الكتاب */
export function createShadowTexture() {
  const size = 256
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = size
  const ctx = canvas.getContext('2d')
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  g.addColorStop(0, 'rgba(46, 33, 22, 0.45)')
  g.addColorStop(1, 'rgba(46, 33, 22, 0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  return new CanvasTexture(canvas)
}

export async function createHeroAssets() {
  await waitForFonts()
  return {
    atlas: createGlyphAtlas(),
    pages: [createPageTexture(1), createPageTexture(7)],
    glow: createGlowTexture(),
    shadow: createShadowTexture(),
  }
}
