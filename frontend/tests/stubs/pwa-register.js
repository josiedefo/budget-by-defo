// Stand-in for vite-plugin-pwa's `virtual:pwa-register` module, which only
// exists when vite.config.js is loaded. vitest.config.js is standalone, so any
// test that mounts App.vue would otherwise fail to resolve the import.
export const registerSW = () => () => Promise.resolve()
