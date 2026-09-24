import { useEffect, useMemo, useRef } from 'react'
import { useFrame } from '@react-three/fiber'
import { DoubleSide, ExtrudeGeometry, PlaneGeometry, Shape } from 'three'

/**
 * كتاب مفتوح مبني بالكامل بالكود (بدون نماذج خارجية).
 * المحور Z يمثّل كعب الكتاب، والصفحة اليمنى تمتد على +X.
 */
export const W = 1.55 // عرض الصفحة
export const D = 2.1 // طول الصفحة
const SEG = 40

/** شكل سطح رزمة الصفحات: ينخفض عند الكعب ويرتفع ثم ينحدر قليلاً عند الحافة */
const curve = (t) => 0.035 + 0.2 * (1 - (1 - t) ** 3) - 0.075 * t * t
const slope = (t) => Math.atan((0.6 * (1 - t) ** 2 - 0.15 * t) / W)

function useStackGeometries() {
  return useMemo(() => {
    const shape = new Shape()
    shape.moveTo(0, 0)
    shape.lineTo(W, 0)
    for (let i = SEG; i >= 0; i--) shape.lineTo((i / SEG) * W, curve(i / SEG))
    shape.closePath()
    const block = new ExtrudeGeometry(shape, { depth: D, bevelEnabled: false })
    block.translate(0, 0, -D / 2)

    const top = new PlaneGeometry(W, D, SEG, 1)
    top.rotateX(-Math.PI / 2)
    top.translate(W / 2, 0, 0)
    const pos = top.attributes.position
    for (let i = 0; i < pos.count; i++) pos.setY(i, curve(pos.getX(i) / W) + 0.003)
    top.computeVertexNormals()
    return { block, top }
  }, [])
}

function Half({ geos, pageTexture, mirror }) {
  return (
    <group scale={[mirror ? -1 : 1, 1, 1]}>
      {/* الغلاف الجلدي */}
      <mesh position={[(W + 0.1) / 2, -0.03, 0]} castShadow>
        <boxGeometry args={[W + 0.1, 0.06, D + 0.16]} />
        <meshStandardMaterial color="#6B4527" roughness={0.75} />
      </mesh>
      {/* خط ذهبي على حافة الغلاف */}
      <mesh position={[W + 0.085, 0.001, 0]}>
        <boxGeometry args={[0.012, 0.004, D + 0.1]} />
        <meshStandardMaterial color="#C8963E" metalness={0.6} roughness={0.35} />
      </mesh>
      {/* رزمة الصفحات */}
      <mesh geometry={geos.block}>
        <meshStandardMaterial color="#EFE3C9" roughness={0.95} />
      </mesh>
      <mesh geometry={geos.top}>
        <meshStandardMaterial map={pageTexture} roughness={0.9} />
      </mesh>
    </group>
  )
}

/** صفحة تتقلب: نعيد حساب شكلها في كل إطار كسلسلة منحنية تنثني حافتها وتتأخر */
function FlippingPage({ texture, tiltRef, offset = 0, period = 5.5, duration = 2, layer = 1 }) {
  const geo = useMemo(() => {
    const g = new PlaneGeometry(W, D, SEG, 1)
    g.rotateX(-Math.PI / 2)
    g.translate(W / 2, 0, 0)
    return g
  }, [])
  useEffect(() => () => geo.dispose(), [geo])

  const cols = useMemo(() => {
    const pos = geo.attributes.position
    return Array.from({ length: pos.count }, (_, i) => Math.round((pos.getX(i) / W) * SEG))
  }, [geo])
  const xs = useMemo(() => new Float32Array(SEG + 1), [])
  const ys = useMemo(() => new Float32Array(SEG + 1), [])

  useFrame(({ clock }) => {
    const tilt = tiltRef.current
    const time = (clock.elapsedTime + offset) % period
    const p = Math.min(Math.max(time / duration, 0), 1)
    const lag = 0.45 // تأخر الحافة الحرة → انحناء طبيعي
    const ds = W / SEG
    xs[0] = 0
    // الصفحة التي تتقلب أولاً تكون فوق الأخرى قبل التقليب وتحتها بعده
    const lift = 0.006 + 0.004 * (p < 0.5 ? layer : 1 - layer)
    ys[0] = curve(0) + lift
    for (let i = 1; i <= SEG; i++) {
      const t = i / SEG
      const local = Math.min(Math.max(p * (1 + lag) - lag * t, 0), 1)
      const e = local * local * (3 - 2 * local) // smoothstep
      const rest = slope(t - 0.5 / SEG) + tilt
      const phi = rest * (1 - e) + (Math.PI - rest) * e
      xs[i] = xs[i - 1] + ds * Math.cos(phi)
      ys[i] = ys[i - 1] + ds * Math.sin(phi)
    }
    const pos = geo.attributes.position
    for (let v = 0; v < pos.count; v++) {
      pos.setX(v, xs[cols[v]])
      pos.setY(v, ys[cols[v]] + 0.004 * Math.sin(Math.PI * p))
    }
    pos.needsUpdate = true
    geo.computeVertexNormals()
  })

  return (
    <mesh geometry={geo}>
      <meshStandardMaterial map={texture} side={DoubleSide} roughness={0.9} />
    </mesh>
  )
}

export default function Book({ pages, tiltRef }) {
  const geos = useStackGeometries()
  const right = useRef()
  const left = useRef()
  useEffect(() => () => { geos.block.dispose(); geos.top.dispose() }, [geos])

  useFrame(() => {
    right.current.rotation.z = tiltRef.current
    left.current.rotation.z = -tiltRef.current
  })

  return (
    <group>
      <group ref={right}>
        <Half geos={geos} pageTexture={pages[0]} />
      </group>
      <group ref={left}>
        <Half geos={geos} pageTexture={pages[1]} mirror />
      </group>
      {/* الكعب */}
      <mesh position={[0, -0.05, 0]} rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.09, 0.09, D + 0.16, 16, 1, false, Math.PI / 2, Math.PI]} />
        <meshStandardMaterial color="#5A3A20" roughness={0.8} side={DoubleSide} />
      </mesh>
      {/* شريط علامة القراءة */}
      <mesh position={[0.06, -0.12, D / 2 + 0.12]} rotation={[0.25, 0, 0.05]}>
        <boxGeometry args={[0.07, 0.34, 0.008]} />
        <meshStandardMaterial color="#566B2E" roughness={0.6} />
      </mesh>
      {/* الصفحات تتقلب من اليسار إلى اليمين كما في الكتاب العربي */}
      <group scale={[-1, 1, 1]}>
        <FlippingPage texture={pages[1]} tiltRef={tiltRef} />
        <FlippingPage texture={pages[0]} tiltRef={tiltRef} offset={-0.45} period={5.5} duration={2.2} layer={0} />
      </group>
    </group>
  )
}
