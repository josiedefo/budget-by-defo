import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vuetify from 'vite-plugin-vuetify'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import { fileURLToPath, URL } from 'node:url'

const API_PROXY = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    vuetify({ autoImport: true }),
    VitePWA({
      // 'prompt', not 'autoUpdate': never swap the running app out from under a
      // half-finished budget edit. App.vue shows a "Reload" snackbar instead.
      registerType: 'prompt',
      includeAssets: ['favicon.ico', 'favicon.svg', 'apple-touch-icon-180x180.png'],
      manifest: {
        name: 'Budget App',
        short_name: 'Budget',
        description: 'Personal budget, transaction and savings tracking',
        theme_color: '#1976D2',
        background_color: '#FFFFFF',
        display: 'standalone',
        // 'any', not 'portrait': the transactions and budget tables are wide and
        // are genuinely more usable in landscape on a phone.
        orientation: 'any',
        scope: '/',
        start_url: '/',
        icons: [
          { src: 'pwa-64x64.png', sizes: '64x64', type: 'image/png' },
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          { src: 'maskable-icon-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ],
        shortcuts: [
          { name: 'Transactions', short_name: 'Transactions', url: '/transactions' },
          { name: 'Savings', short_name: 'Savings', url: '/savings' }
        ]
      },
      workbox: {
        // woff2 only. @mdi/font emits the same icon font as .eot/.ttf/.woff/.woff2;
        // Android Chrome only ever picks woff2, so precaching the rest would waste
        // ~3 MB of the user's storage on every install.
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        // @mdi/font's CSS requests its fonts as `...woff2?v=7.4.47`, which does not
        // match the query-less precache entry. Without this the icon font falls
        // through to the network and renders as empty boxes when offline.
        ignoreURLParametersMatching: [/^utm_/, /^fbclid$/, /^v$/],
        navigateFallback: '/index.html',
        // API and health checks must always hit the network, never the SPA shell.
        navigateFallbackDenylist: [/^\/api\//, /^\/actuator\//],
        cleanupOutdatedCaches: true
      }
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: API_PROXY
  },
  // `vite preview` serves the real production build, which is the only way to
  // exercise the service worker locally; it needs the same /api proxy as dev.
  preview: {
    proxy: API_PROXY
  }
})
