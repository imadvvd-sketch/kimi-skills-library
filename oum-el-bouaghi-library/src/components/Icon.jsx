/** أيقونات SVG خفيفة مدمجة (بدون مكتبة خارجية) */
const paths = {
  loan: 'M4 6.5A2.5 2.5 0 0 1 6.5 4H20v13H6.5A2.5 2.5 0 0 0 4 19.5zM4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5M14 8l3 3-3 3M17 11H9',
  reading: 'M2 5c3-1.5 7-1.5 10 1 3-2.5 7-2.5 10-1v14c-3-1.5-7-1.5-10 1-3-2.5-7-2.5-10-1zM12 6v14',
  children: 'M12 3l2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 15.3 7.2 17.9l.9-5.4-3.9-3.8 5.4-.8zM5 21h14',
  digital: 'M3 5h18v11H3zM8 20h8M12 16v4M9 9l-2 2 2 2M15 9l2 2-2 2',
  culture: 'M12 3l9 5H3zM5 10v8M9.5 10v8M14.5 10v8M19 10v8M3 21h18',
  card: 'M3 6h18v12H3zM3 10h18M7 15h4',
  phone: 'M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2',
  mail: 'M3 5h18v14H3zM3 6l9 7 9-7',
  pin: 'M12 22s7-6.5 7-12a7 7 0 1 0-14 0c0 5.5 7 12 7 12zM12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5',
  clock: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2',
  calendar: 'M4 5h16v16H4zM4 10h16M9 3v4M15 3v4',
  search: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zM20 20l-4-4',
  menu: 'M4 7h16M4 12h16M4 17h16',
  close: 'M6 6l12 12M18 6L6 18',
  globe: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3c2.5 2.5 3.5 5.5 3.5 9s-1 6.5-3.5 9c-2.5-2.5-3.5-5.5-3.5-9s1-6.5 3.5-9',
  fax: 'M7 9V3h10v6M5 9h14a2 2 0 0 1 2 2v6h-4M3 17v-6a2 2 0 0 1 2-2M7 14h10v7H7z',
  arrowDown: 'M12 4v16M6 14l6 6 6-6',
  facebook: 'M14 8h3V4h-3a4 4 0 0 0-4 4v3H7v4h3v7h4v-7h3l1-4h-4V8a0 0 0 0 1 0 0z',
  instagram: 'M4 4h16v16H4zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM17 7h.01',
  youtube: 'M3 7.5A3.5 3.5 0 0 1 6.5 4h11A3.5 3.5 0 0 1 21 7.5v9a3.5 3.5 0 0 1-3.5 3.5h-11A3.5 3.5 0 0 1 3 16.5zM10 9v6l5-3z',
  location: 'M3 21h18M5 21V8l7-5 7 5v13M9 21v-6h6v6',
}

export default function Icon({ name, className = 'size-6', title }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden={title ? undefined : true}
      role={title ? 'img' : undefined}
    >
      {title && <title>{title}</title>}
      <path d={paths[name]} />
    </svg>
  )
}
