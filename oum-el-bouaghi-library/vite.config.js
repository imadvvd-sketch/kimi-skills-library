import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'node:path'
import seoPlugin from './build/seoPlugin.js'

export default defineConfig({
  plugins: [react(), tailwindcss(), seoPlugin()],
  build: {
    target: 'es2020',
    rollupOptions: {
      // صفحتان: العربية (/) والفرنسية (/fr/)
      input: {
        main: resolve(import.meta.dirname, 'index.html'),
        fr: resolve(import.meta.dirname, 'fr/index.html'),
      },
    },
  },
})
