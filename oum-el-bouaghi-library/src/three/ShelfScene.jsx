import { useEffect, useMemo, useRef, useState } from 'react'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
import { Color, MeshStandardMaterial, RepeatWrapping, Vector3 } from 'three'
import { createShelfAssets } from './lib/shelfTextures.js'
import { createShadowTexture } from './lib/textures.js'
import useInView from '../hooks/useInView.js'
import FrameLimiter from './FrameLimiter.jsx'

/**
 * رف كتب ثلاثي الأبعاد: كل كتاب يمثّل خدمة.
 * المرور بالفأرة (أو التركيز على بطاقة الخدمة بلوحة المفاتيح) يُبرز الكتاب ويديره.
 */
const SPACING = 0.78
const SHELF_Y = 0
const DIMS = [
  // [السُمك، الارتفاع، العمق]
  [0.46, 2.2, 1.45],
  [0.52, 2.45, 1.5],
  [0.44, 2.05, 1.4],
  [0.5, 2.35, 1.5],
  [0.47, 2.15, 1.45],
  [0.54, 2.4, 1.52],
]
const LEAN = [0, 0.012, -0.01, 0.006, -0.014, 0.008]

function darker(hex, amount = 0.35) {
  return new Color(hex).lerp(new Color('#1c120a'), amount)
}

function ShelfBook({ index, item, spine, x, active, onHover, onSelect, startRef }) {
  const ref = useRef()
  const [thick, height, depth] = DIMS[index % DIMS.length]
  const materials = useMemo(() => {
    const cover = new MeshStandardMaterial({ color: darker(item.color, 0.15), roughness: 0.7 })
    const pages = new MeshStandardMaterial({ color: '#EFE3C9', roughness: 0.95 })
    const front = new MeshStandardMaterial({ map: spine, roughness: 0.6 })
    // ترتيب أوجه BoxGeometry: +x, -x, +y, -y, +z (الكعب نحو الكاميرا), -z
    return [cover, cover, pages, pages, front, pages]
  }, [item.color, spine])
  useEffect(() => () => materials.forEach((m, i) => i !== 1 && i !== 3 && m.dispose()), [materials])

  useFrame((state, delta) => {
    const g = ref.current
    const k = 1 - Math.exp(-delta * 7)
    // دخول متتابع: الكتب تنزل إلى الرف واحداً تلو الآخر
    const start = startRef.current
    const t = start == null ? 0 : Math.min(Math.max((state.clock.elapsedTime - start - index * 0.12) / 0.7, 0), 1)
    const drop = (1 - t) ** 3 * 2.2
    const isActive = active === item.id
    const targetZ = isActive ? 0.55 : 0
    const targetY = SHELF_Y + height / 2 + drop + (isActive ? 0.12 : 0)
    const targetRotY = isActive ? -0.42 : Math.sin(state.clock.elapsedTime * 0.6 + index) * 0.015
    g.position.z += (targetZ - g.position.z) * k
    g.position.y = start == null ? targetY : g.position.y + (targetY - g.position.y) * (drop > 0.001 ? 1 : k)
    g.rotation.y += (targetRotY - g.rotation.y) * k
    g.rotation.z += ((isActive ? 0 : LEAN[index]) - g.rotation.z) * k
    g.scale.setScalar(g.scale.x + ((isActive ? 1.04 : 1) - g.scale.x) * k)
  })

  return (
    <group
      ref={ref}
      position={[x, SHELF_Y + height / 2 + 2.2, 0]}
      onPointerOver={(e) => {
        e.stopPropagation()
        onHover(item.id)
        document.body.style.cursor = 'pointer'
      }}
      onPointerOut={() => {
        onHover(null)
        document.body.style.cursor = ''
      }}
      onClick={(e) => {
        e.stopPropagation()
        onSelect(item.id)
      }}
    >
      <mesh material={materials}>
        <boxGeometry args={[thick, height, depth]} />
      </mesh>
      {/* نقطة مرجعية فوق الكتاب لوضع اسم الخدمة */}
      <object3D name={`label-${item.id}`} position={[0, height / 2 + 0.15, 0]} />
    </group>
  )
}

function Bookend({ x, mirror }) {
  return (
    <group position={[x, 0, -0.1]} scale={[mirror ? -1 : 1, 1, 1]}>
      <mesh position={[0, 0.02, 0]}>
        <boxGeometry args={[0.5, 0.05, 1.2]} />
        <meshStandardMaterial color="#3F4F20" roughness={0.5} metalness={0.2} />
      </mesh>
      <mesh position={[-0.22, 0.65, 0]}>
        <boxGeometry args={[0.06, 1.3, 1.2]} />
        <meshStandardMaterial color="#566B2E" roughness={0.5} metalness={0.2} />
      </mesh>
      {/* معيّن ذهبي — زخرفة أمازيغية */}
      <mesh position={[-0.18, 0.75, 0.3]} rotation={[0, Math.PI / 2, Math.PI / 4]}>
        <boxGeometry args={[0.02, 0.22, 0.22]} />
        <meshStandardMaterial color="#C8963E" metalness={0.6} roughness={0.3} />
      </mesh>
    </group>
  )
}

function Rig({ wrapRef, rtl, startRef, active, labelRef, groupRef }) {
  const { camera, size } = useThree()
  const tmp = useMemo(() => new Vector3(), [])
  useFrame((state, delta) => {
    if (startRef.current == null) startRef.current = state.clock.elapsedTime
    // حركة الكاميرا مرتبطة بموضع القسم في الشاشة أثناء التمرير
    const rect = wrapRef.current.getBoundingClientRect()
    const p = Math.min(Math.max(1 - (rect.top + rect.height) / (window.innerHeight + rect.height), 0), 1)
    const dir = rtl ? -1 : 1
    const k = 1 - Math.exp(-delta * 4)
    const narrow = size.width < 640
    const goalX = (p - 0.5) * 1.6 * dir
    const goalY = 1.9 + (0.5 - p) * 1.1
    const goalZ = narrow ? 9.6 : 7.6
    camera.position.x += (goalX - camera.position.x) * k
    camera.position.y += (goalY - camera.position.y) * k
    camera.position.z += (goalZ - camera.position.z) * k
    camera.lookAt(0, 1.05, 0)

    // وضع اسم الخدمة فوق الكتاب النشط (عنصر HTML حقيقي)
    const label = labelRef.current
    if (!label) return
    const anchor = active && groupRef.current.getObjectByName(`label-${active}`)
    if (anchor) {
      anchor.getWorldPosition(tmp).project(camera)
      const x = (tmp.x * 0.5 + 0.5) * size.width
      const y = (-tmp.y * 0.5 + 0.5) * size.height
      label.style.transform = `translate(-50%, -100%) translate(${x}px, ${y}px)`
      label.style.opacity = '1'
    } else {
      label.style.opacity = '0'
    }
  })
  return null
}

export default function ShelfScene({ tier = 'full', items, lang, dir, active, onHover, onSelect, onReady }) {
  const wrapRef = useRef()
  const labelRef = useRef()
  const groupRef = useRef()
  const startRef = useRef(null)
  const inView = useInView(wrapRef, '100px')
  const [assets, setAssets] = useState(null)
  const rtl = dir === 'rtl'

  useEffect(() => {
    let alive = true
    let created
    createShelfAssets(items, lang).then((a) => {
      a.wood.wrapS = a.wood.wrapT = RepeatWrapping
      a.shadow = createShadowTexture()
      created = a
      if (alive) {
        setAssets(a)
        onReady?.()
      }
    })
    return () => {
      alive = false
      if (created) [...created.spines, created.wood, created.shadow].forEach((t) => t.dispose())
    }
  }, [items, lang]) // eslint-disable-line react-hooks/exhaustive-deps

  const n = items.length
  const activeItem = items.find((s) => s.id === active)
  // في العربية الخدمة الأولى على اليمين
  const xFor = (i) => (rtl ? (n - 1) / 2 - i : i - (n - 1) / 2) * SPACING

  return (
    <div ref={wrapRef} className="relative h-full w-full">
      {assets && (
        <Canvas
          flat
          frameloop={!inView ? 'never' : tier === 'lite' ? 'demand' : 'always'}
          dpr={tier === 'full' ? [1, 1.75] : [1, 1.25]}
          gl={{ antialias: tier === 'full', alpha: true }}
          camera={{ fov: 30, position: [0, 1.9, 7.6], near: 0.1, far: 40 }}
          onPointerMissed={() => onHover(null)}
          aria-hidden="true"
        >
          {tier === 'lite' && inView && <FrameLimiter fps={30} />}
          <ambientLight intensity={0.85} color="#FFF4E0" />
          <hemisphereLight args={['#FFF7EA', '#7A5230', 0.5]} />
          <directionalLight position={[-3, 5, 5]} intensity={1.7} color="#FFF1D6" />
          <pointLight position={[0, 2.8, 2.2]} intensity={3} distance={7} color="#FFD08A" />

          <group ref={groupRef}>
            {/* الرف الخشبي */}
            <mesh position={[0, -0.09, 0]}>
              <boxGeometry args={[n * SPACING + 1.6, 0.18, 1.9]} />
              <meshStandardMaterial map={assets.wood} roughness={0.8} />
            </mesh>
            <mesh position={[0, -0.2, 0.9]}>
              <boxGeometry args={[n * SPACING + 1.6, 0.06, 0.12]} />
              <meshStandardMaterial color="#5A3A20" roughness={0.8} />
            </mesh>
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.003, 0.2]}>
              <planeGeometry args={[n * SPACING + 1.2, 1.6]} />
              <meshBasicMaterial map={assets.shadow} transparent opacity={0.5} depthWrite={false} />
            </mesh>
            <Bookend x={-(n / 2) * SPACING - 0.25} />
            <Bookend x={(n / 2) * SPACING + 0.25} mirror />
            {items.map((item, i) => (
              <ShelfBook
                key={item.id}
                index={i}
                item={item}
                spine={assets.spines[i]}
                x={xFor(i)}
                active={active}
                onHover={onHover}
                onSelect={onSelect}
                startRef={startRef}
              />
            ))}
          </group>
          <Rig wrapRef={wrapRef} rtl={rtl} startRef={startRef} active={active} labelRef={labelRef} groupRef={groupRef} />
        </Canvas>
      )}
      {/* اسم الخدمة فوق الكتاب النشط (زخرفي: المعلومة نفسها موجودة في البطاقات) */}
      <div
        ref={labelRef}
        aria-hidden="true"
        className="pointer-events-none absolute top-0 left-0 rounded-full bg-ink px-4 py-1.5 font-display text-lg font-bold whitespace-nowrap text-paper opacity-0 shadow-lg transition-opacity duration-200"
      >
        {activeItem?.title}
      </div>
    </div>
  )
}
