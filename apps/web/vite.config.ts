import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': `http://127.0.0.1:${process.env.API_PORT || 8180}`,
      '/actuator': `http://127.0.0.1:${process.env.API_PORT || 8180}`,
    },
  },
  test: { environment: 'jsdom', setupFiles: ['./src/test-setup.ts'], include: ['src/**/*.test.tsx'], css: false },
})
