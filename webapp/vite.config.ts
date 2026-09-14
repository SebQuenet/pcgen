import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

const PCGEN_SERVER = 'http://127.0.0.1:8420'

export default defineConfig({
  plugins: [react()],
  // The built files land where HttpApiServer looks for a front end.
  build: { outDir: '../web', emptyOutDir: true },
  // In dev the page is served by Vite, so calls are proxied to PCGen rather
  // than sent cross-origin, which the server's Host check would refuse.
  server: { proxy: { '/api': PCGEN_SERVER, '/health': PCGEN_SERVER } },
  test: { environment: 'node', globals: true },
})
