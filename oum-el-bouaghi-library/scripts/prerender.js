/**
 * يحقن HTML المولَّد مسبقاً في dist/index.html و dist/fr/index.html
 * يُشغَّل تلقائياً ضمن "npm run build"
 */
import { readFileSync, writeFileSync, rmSync } from 'node:fs'
import { resolve } from 'node:path'
import { pathToFileURL } from 'node:url'

const root = resolve(import.meta.dirname, '..')
const { render } = await import(pathToFileURL(resolve(root, 'dist-ssr/entry-server.js')).href)

for (const [lang, file] of [['ar', 'dist/index.html'], ['fr', 'dist/fr/index.html']]) {
  const path = resolve(root, file)
  const html = readFileSync(path, 'utf8')
  const appHtml = render(lang)
  if (!html.includes('<div id="root"></div>')) throw new Error(`#root not found in ${file}`)
  writeFileSync(path, html.replace('<div id="root"></div>', `<div id="root">${appHtml}</div>`))
  console.log(`✓ prerendered ${file} (${(appHtml.length / 1024).toFixed(1)} kB)`)
}
rmSync(resolve(root, 'dist-ssr'), { recursive: true, force: true })
