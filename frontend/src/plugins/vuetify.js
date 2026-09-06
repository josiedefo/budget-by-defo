import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'

export default createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        dark: false,
        colors: {
          // Warmer finance palette: deep teal primary on a soft off-white canvas.
          primary: '#0F766E',
          secondary: '#475569',
          accent: '#14B8A6',
          error: '#E11D48',
          info: '#0EA5E9',
          success: '#15803D',
          warning: '#D97706',
          background: '#F5F5F0',
          surface: '#FFFFFF',
          'surface-variant': '#E7E5DE',
          'on-surface-variant': '#4B5563',
          // Kept so existing color="income|expense" usages still resolve.
          income: '#15803D',
          expense: '#E11D48'
        }
      }
    }
  },
  defaults: {
    VCard: { rounded: 'lg', border: false, elevation: 1 },
    VBtn: { rounded: 'lg' },
    VChip: { rounded: 'lg' },
    VTextField: { variant: 'outlined', density: 'comfortable' },
    VSelect: { variant: 'outlined', density: 'comfortable' },
    VDialog: { rounded: 'lg' }
  }
})
