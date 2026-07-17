import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// Standalone test config: the app's vite.config.js uses vite-plugin-vuetify's
// autoImport, which is unnecessary (and brittle) under vitest. Tests register
// Vuetify explicitly instead.
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./tests/setup.js'],
    server: {
      deps: {
        inline: ['vuetify']
      }
    }
  }
})
