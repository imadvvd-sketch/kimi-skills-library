import { m as motion } from 'framer-motion'

/** ظهور تدريجي عند الوصول للعنصر أثناء التمرير (يُلغى تلقائياً مع reduced-motion) */
export default function Reveal({ children, delay = 0, y = 24, className = '', as = 'div' }) {
  const Tag = motion[as]
  return (
    <Tag
      className={className}
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-60px' }}
      transition={{ duration: 0.7, delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </Tag>
  )
}
