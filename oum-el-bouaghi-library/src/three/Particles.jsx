import { useEffect, useMemo } from 'react'
import { useFrame, useThree } from '@react-three/fiber'
import { BufferAttribute, BufferGeometry, Color, NormalBlending, ShaderMaterial } from 'three'
import { ATLAS_COLS, ATLAS_ROWS, GLYPHS } from './lib/textures.js'

/**
 * جزيئات ضوئية وحروف تخرج من الكتاب وتطفو للأعلى.
 * كل الحركة محسوبة في الـ GPU (vertex shader) → لا كلفة على المعالج.
 */
const vertex = /* glsl */ `
  attribute vec4 aSeed;   // x: السرعة، y: الطور، z: نصف قطر الدوران، w: الحجم
  attribute vec2 aExtra;  // x: رقم الحرف في الأطلس، y: اللون
  uniform float uTime;
  uniform float uScale;
  uniform float uHeight;
  varying float vAlpha;
  varying vec2 vExtra;
  varying float vAngle;
  void main() {
    float t = fract(uTime * aSeed.x + aSeed.y);
    vec3 p = position;
    float ang = aSeed.y * 6.2831 + t * 2.4;
    p.x += sin(ang) * aSeed.z * t + position.x * t * 0.5;
    p.z += cos(ang) * aSeed.z * t * 0.5;
    p.y += t * uHeight;
    vAlpha = smoothstep(0.0, 0.12, t) * (1.0 - smoothstep(0.55, 1.0, t));
    vExtra = aExtra;
    vAngle = sin(uTime * 0.7 + aSeed.y * 6.2831) * 0.35;
    vec4 mv = modelViewMatrix * vec4(p, 1.0);
    gl_Position = projectionMatrix * mv;
    gl_PointSize = aSeed.w * uScale * smoothstep(0.0, 0.2, t) / -mv.z;
  }
`

const sparkFragment = /* glsl */ `
  uniform vec3 uColor;
  varying float vAlpha;
  void main() {
    float d = length(gl_PointCoord - 0.5);
    float a = smoothstep(0.5, 0.0, d);
    gl_FragColor = vec4(uColor, a * a * vAlpha * 0.9);
  }
`

const glyphFragment = /* glsl */ `
  uniform sampler2D uAtlas;
  uniform vec3 uColorA;
  uniform vec3 uColorB;
  varying float vAlpha;
  varying vec2 vExtra;
  varying float vAngle;
  void main() {
    vec2 pc = gl_PointCoord - 0.5;
    float c = cos(vAngle), s = sin(vAngle);
    pc = mat2(c, -s, s, c) * pc + 0.5;
    if (pc.x < 0.0 || pc.x > 1.0 || pc.y < 0.0 || pc.y > 1.0) discard;
    float col = mod(vExtra.x, ${ATLAS_COLS}.0);
    float row = floor(vExtra.x / ${ATLAS_COLS}.0);
    vec2 uv = vec2((col + pc.x) / ${ATLAS_COLS}.0, 1.0 - (row + pc.y) / ${ATLAS_ROWS}.0);
    float a = texture2D(uAtlas, uv).a;
    vec3 color = mix(uColorA, uColorB, vExtra.y);
    gl_FragColor = vec4(color, a * vAlpha * 0.85);
    if (gl_FragColor.a < 0.01) discard;
  }
`

function makeGeometry(count, sizeRange, glyphs) {
  const positions = new Float32Array(count * 3)
  const seeds = new Float32Array(count * 4)
  const extra = new Float32Array(count * 2)
  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 2.6
    positions[i * 3 + 1] = 0.25
    positions[i * 3 + 2] = (Math.random() - 0.5) * 1.4
    seeds[i * 4] = glyphs ? 0.05 + Math.random() * 0.04 : 0.08 + Math.random() * 0.1
    seeds[i * 4 + 1] = Math.random()
    seeds[i * 4 + 2] = 0.2 + Math.random() * 0.7
    seeds[i * 4 + 3] = sizeRange[0] + Math.random() * (sizeRange[1] - sizeRange[0])
    const glyph = Math.floor(Math.random() * GLYPHS.length)
    extra[i * 2] = glyph
    // حروف تيفيناغ بالأخضر الزيتوني، والعربية بالعنبري
    extra[i * 2 + 1] = GLYPHS[glyph].charCodeAt(0) >= 0x2d30 ? 1 : Math.random() < 0.2 ? 0.5 : 0
  }
  const g = new BufferGeometry()
  g.setAttribute('position', new BufferAttribute(positions, 3))
  g.setAttribute('aSeed', new BufferAttribute(seeds, 4))
  g.setAttribute('aExtra', new BufferAttribute(extra, 2))
  return g
}

export default function Particles({ atlas, sparkCount, glyphCount }) {
  const { size, viewport, camera } = useThree()
  const sparks = useMemo(() => makeGeometry(sparkCount, [0.03, 0.075], false), [sparkCount])
  const glyphs = useMemo(() => makeGeometry(glyphCount, [0.26, 0.46], true), [glyphCount])

  const materials = useMemo(() => {
    const common = { transparent: true, depthWrite: false, blending: NormalBlending }
    return {
      spark: new ShaderMaterial({
        ...common,
        vertexShader: vertex,
        fragmentShader: sparkFragment,
        uniforms: { uTime: { value: 0 }, uScale: { value: 1 }, uHeight: { value: 2.6 }, uColor: { value: new Color('#C98F2E') } },
      }),
      glyph: new ShaderMaterial({
        ...common,
        vertexShader: vertex,
        fragmentShader: glyphFragment,
        uniforms: {
          uTime: { value: 0 },
          uScale: { value: 1 },
          uHeight: { value: 3.1 },
          uAtlas: { value: atlas },
          uColorA: { value: new Color('#A8692A') },
          uColorB: { value: new Color('#4A5D26') },
        },
      }),
    }
  }, [atlas])

  useEffect(() => () => {
    sparks.dispose(); glyphs.dispose(); materials.spark.dispose(); materials.glyph.dispose()
  }, [sparks, glyphs, materials])

  // حجم النقطة بالبكسل = الحجم × (ارتفاع الشاشة / (2·tan(fov/2)))
  const scale = (size.height * viewport.dpr) / (2 * Math.tan(((camera.fov ?? 35) * Math.PI) / 360))

  useFrame(({ clock }) => {
    const t = clock.elapsedTime
    for (const m of [materials.spark, materials.glyph]) {
      m.uniforms.uTime.value = t
      m.uniforms.uScale.value = scale
    }
  })

  return (
    <>
      <points geometry={sparks} material={materials.spark} frustumCulled={false} />
      <points geometry={glyphs} material={materials.glyph} frustumCulled={false} renderOrder={2} />
    </>
  )
}
