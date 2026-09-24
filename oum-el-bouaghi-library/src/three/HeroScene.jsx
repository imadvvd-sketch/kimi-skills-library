import { useEffect, useRef, useState } from 'react'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
import { Vector3 } from 'three'
import Book from './Book.jsx'
import Particles from './Particles.jsx'
import { createHeroAssets } from './lib/textures.js'
import useInView from '../hooks/useInView.js'

const TIERS = {
  full: { dpr: [1, 1.75], sparks: 380, glyphs: 26, antialias: true },
  lite: { dpr: [1, 1.25], sparks: 110, glyphs: 12, antialias: false },
}

const BASE_CAM = new Vector3(0, 3.3, 6.6)
const TARGET = new Vector3(0, 0.75, 0)

/** مؤشر الفأرة (أو ميلان الهاتف) محفوظ خارج React لتفادي إعادة الرسم */
function usePointer() {
  const pointer = useRef({ x: 0, y: 0 })
  useEffect(() => {
    const onMove = (e) => {
      pointer.current.x = (e.clientX / window.innerWidth) * 2 - 1
      pointer.current.y = -((e.clientY / window.innerHeight) * 2 - 1)
    }
    const onTilt = (e) => {
      if (e.gamma == null) return
      pointer.current.x = Math.max(-1, Math.min(1, e.gamma / 30))
      pointer.current.y = Math.max(-1, Math.min(1, (45 - e.beta) / 30))
    }
    window.addEventListener('pointermove', onMove, { passive: true })
    window.addEventListener('deviceorientation', onTilt, { passive: true })
    return () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('deviceorientation', onTilt)
    }
  }, [])
  return pointer
}

function Rig({ pointer, tiltRef, bookRef, onReady }) {
  const { camera } = useThree()
  const frames = useRef(0)
  const current = useRef(new Vector3().copy(BASE_CAM))
  const goal = new Vector3()

  useFrame((state, delta) => {
    // تقدّم التمرير داخل القسم الرئيسي (0 → 1)
    const scroll = Math.min(Math.max(window.scrollY / (window.innerHeight * 0.9), 0), 1)
    const k = 1 - Math.exp(-delta * 3) // تنعيم مستقل عن سرعة الإطارات

    goal.set(
      BASE_CAM.x + pointer.current.x * 0.9,
      BASE_CAM.y + pointer.current.y * 0.45 + scroll * 1.2,
      BASE_CAM.z + scroll * 1.6,
    )
    current.current.lerp(goal, k)
    camera.position.copy(current.current)
    camera.lookAt(TARGET)

    // الكتاب يطفو بلطف، وينغلق قليلاً مع النزول
    tiltRef.current += (0.07 + scroll * 0.55 - tiltRef.current) * k
    const book = bookRef.current
    book.position.y = Math.sin(state.clock.elapsedTime * 0.8) * 0.05 - scroll * 0.3
    book.rotation.y = -0.18 + Math.sin(state.clock.elapsedTime * 0.35) * 0.05

    if (frames.current < 3 && ++frames.current === 3) onReady?.()
  })
  return null
}

function Glow({ texture }) {
  const ref = useRef()
  useFrame(({ clock }) => {
    ref.current.material.opacity = 0.75 + Math.sin(clock.elapsedTime * 1.2) * 0.12
  })
  return (
    <sprite ref={ref} position={[0, 0.9, -0.9]} scale={[5.4, 3.9, 1]} renderOrder={-1}>
      <spriteMaterial map={texture} transparent depthWrite={false} />
    </sprite>
  )
}

export default function HeroScene({ tier = 'full', onReady }) {
  const wrap = useRef()
  const inView = useInView(wrap)
  const pointer = usePointer()
  const tiltRef = useRef(0.07)
  const bookRef = useRef()
  const [assets, setAssets] = useState(null)
  const settings = TIERS[tier] ?? TIERS.lite

  useEffect(() => {
    let alive = true
    let created
    createHeroAssets().then((a) => {
      created = a
      if (alive) setAssets(a)
    })
    return () => {
      alive = false
      if (created) [created.atlas, created.glow, created.shadow, ...created.pages].forEach((t) => t.dispose())
    }
  }, [])

  return (
    <div ref={wrap} className="absolute inset-0" aria-hidden="true">
      {assets && (
        <Canvas
          flat
          frameloop={inView ? 'always' : 'never'}
          dpr={settings.dpr}
          gl={{ antialias: settings.antialias, alpha: true, powerPreference: 'high-performance' }}
          camera={{ fov: 32, position: BASE_CAM.toArray(), near: 0.1, far: 50 }}
          style={{ pointerEvents: 'none' }}
        >
          <ambientLight intensity={0.9} color="#FFF4E0" />
          <hemisphereLight args={['#FFF7EA', '#8C6A4A', 0.6]} />
          <directionalLight position={[3, 6, 4]} intensity={1.6} color="#FFF1D6" />
          <pointLight position={[0, 1.3, 0.4]} intensity={4} distance={5} color="#FFC46B" />

          <Glow texture={assets.glow} />
          <group ref={bookRef}>
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.08, 0]} renderOrder={-1}>
              <planeGeometry args={[5.6, 3.8]} />
              <meshBasicMaterial map={assets.shadow} transparent depthWrite={false} />
            </mesh>
            <Book pages={assets.pages} tiltRef={tiltRef} />
          </group>
          <Particles atlas={assets.atlas} sparkCount={settings.sparks} glyphCount={settings.glyphs} />
          <Rig pointer={pointer} tiltRef={tiltRef} bookRef={bookRef} onReady={onReady} />
        </Canvas>
      )}
    </div>
  )
}
