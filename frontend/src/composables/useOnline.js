import { onMounted, onUnmounted, ref } from 'vue'

/**
 * Tracks browser connectivity. Data always comes live from /api, so the app
 * shell can load from the service worker cache while requests still fail —
 * this is what lets us say so explicitly instead of showing empty screens.
 */
export function useOnline() {
  const isOnline = ref(true)

  const update = () => {
    isOnline.value = navigator.onLine
  }

  onMounted(() => {
    update()
    window.addEventListener('online', update)
    window.addEventListener('offline', update)
  })

  onUnmounted(() => {
    window.removeEventListener('online', update)
    window.removeEventListener('offline', update)
  })

  return { isOnline }
}
