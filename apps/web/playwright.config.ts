import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  workers: 1,
  use: {
    baseURL: process.env.PAYREASON_WEB_URL || 'http://127.0.0.1:5180',
    viewport: { width: 1440, height: 960 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
