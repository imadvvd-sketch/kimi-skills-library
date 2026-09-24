import Reveal from './Reveal.jsx'

export default function SectionHeading({ id, title, subtitle, align = 'center' }) {
  const alignCls = align === 'center' ? 'text-center mx-auto' : 'text-start'
  return (
    <Reveal className={`mb-12 max-w-2xl ${alignCls}`}>
      <h2 id={id} className="text-3xl font-bold sm:text-4xl md:text-5xl">
        {title}
      </h2>
      <div aria-hidden="true" className={`mt-4 flex items-center gap-2 ${align === 'center' ? 'justify-center' : ''}`}>
        <span className="h-px w-10 bg-brown/40" />
        <span className="size-2.5 rotate-45 bg-olive" />
        <span className="size-1.5 rotate-45 bg-gold" />
        <span className="size-2.5 rotate-45 bg-olive" />
        <span className="h-px w-10 bg-brown/40" />
      </div>
      {subtitle && <p className="mt-4 text-lg text-ink-soft">{subtitle}</p>}
    </Reveal>
  )
}
