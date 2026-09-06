import { ref } from 'vue'
import { registerSW } from 'virtual:pwa-register'

// Module-level state: the service worker is registered exactly once for the
// app's lifetime, no matter how many components read these refs.
const needRefresh = ref(false)
const offlineReady = ref(false)

const updateServiceWorker = registerSW({
  onNeedRefresh() {
    needRefresh.value = true
  },
  onOfflineReady() {
    offlineReady.value = true
  }
})

/**
 * Service worker update state.
 *
 * `needRefresh` goes true when a new build has been downloaded and is waiting.
 * Calling `applyUpdate()` activates it and reloads the page.
 */
export function usePwaUpdate() {
  return {
    needRefresh,
    offlineReady,
    applyUpdate: () => updateServiceWorker(true)
  }
}
