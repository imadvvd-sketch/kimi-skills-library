import { CanvasTexture, SRGBColorSpace } from 'three'
import { waitForFonts } from './textures.js'

/** كعب كتاب: لون الخدمة + أشرطة ذهبية + اسم الخدمة مكتوباً عمودياً */
export function createSpineTexture({ color, title, lang }) {
  const w = 112
  const h = 560
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = color
  ctx.fillRect(0, 0, w, h)
  // استدارة الكعب: إضاءة في الوسط وظل على الحافتين
  const shade = ctx.createLinearGradient(0, 0, w, 0)
  shade.addColorStop(0, 'rgba(0,0,0,0.35)')
  shade.addColorStop(0.35, 'rgba(255,255,255,0.12)')
  shade.addColorStop(0.65, 'rgba(255,255,255,0.05)')
  shade.addColorStop(1, 'rgba(0,0,0,0.4)')
  ctx.fillStyle = shade
  ctx.fillRect(0, 0, w, h)

  // أشرطة ذهبية وزخرفة معيّن
  ctx.fillStyle = '#D9AE5F'
  for (const y of [34, 44, h - 48, h - 38]) ctx.fillRect(10, y, w - 20, 3)
  const diamond = (cy) => {
    ctx.beginPath()
    ctx.moveTo(w / 2, cy - 9); ctx.lineTo(w / 2 + 9, cy); ctx.lineTo(w / 2, cy + 9); ctx.lineTo(w / 2 - 9, cy)
    ctx.fill()
  }
  diamond(78)
  diamond(h - 82)

  // العنوان عمودياً (من الأعلى إلى الأسفل)
  ctx.save()
  ctx.translate(w / 2, h / 2)
  ctx.rotate(Math.PI / 2)
  ctx.fillStyle = '#F8F1E2'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.direction = lang === 'ar' ? 'rtl' : 'ltr'
  const maxLen = h - 200
  let size = lang === 'ar' ? 52 : 40
  const family = lang === 'ar' ? 'Amiri, serif' : 'Amiri, Georgia, serif'
  do {
    ctx.font = `bold ${size}px ${family}`
    size -= 2
  } while (ctx.measureText(title).width > maxLen && size > 16)
  ctx.fillText(title, 0, lang === 'ar' ? 6 : 2)
  ctx.restore()

  const tex = new CanvasTexture(canvas)
  tex.colorSpace = SRGBColorSpace
  tex.anisotropy = 4
  return tex
}

/** ألياف الخشب للرف */
export function createWoodTexture() {
  const w = 1024
  const h = 128
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#8A5E36'
  ctx.fillRect(0, 0, w, h)
  for (let i = 0; i < 70; i++) {
    const y = Math.random() * h
    ctx.strokeStyle = `rgba(${Math.random() < 0.5 ? '60,35,18' : '170,120,72'},${0.12 + Math.random() * 0.2})`
    ctx.lineWidth = 0.6 + Math.random() * 2
    ctx.beginPath()
    ctx.moveTo(0, y)
    for (let x = 0; x <= w; x += 32) ctx.lineTo(x, y + Math.sin(x / (60 + i * 3) + i) * 3)
    ctx.stroke()
  }
  const tex = new CanvasTexture(canvas)
  tex.colorSpace = SRGBColorSpace
  return tex
}

export async function createShelfAssets(items, lang) {
  await waitForFonts()
  return {
    spines: items.map((s) => createSpineTexture({ color: s.color, title: s.title, lang })),
    wood: createWoodTexture(),
  }
}
