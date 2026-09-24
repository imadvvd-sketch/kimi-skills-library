import { useEffect } from 'react'
import { useThree } from '@react-three/fiber'

/** على الهواتف: نرسم 30 إطاراً في الثانية بدل 60 لتوفير البطارية */
export default function FrameLimiter({ fps = 30 }) {
  const invalidate = useThree((s) => s.invalidate)
  useEffect(() => {
    const id = setInterval(() => invalidate(), 1000 / fps)
    return () => clearInterval(id)
  }, [fps, invalidate])
  return null
}
