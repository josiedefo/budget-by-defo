import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import vuetify from './plugins/vuetify'
import { VChart } from './plugins/echarts'
// Import Tailwind AFTER Vuetify's styles (pulled in by ./plugins/vuetify) so
// utility classes win where used. tailwind.css omits preflight on purpose.
import './assets/tailwind.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(vuetify)
app.component('VChart', VChart)

app.mount('#app')
