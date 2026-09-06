// Tree-shaken ECharts setup. Register only the chart types and components the
// dashboard uses, then expose the <v-chart> component for global registration.
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart, GaugeChart } from 'echarts/charts'
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  PieChart,
  LineChart,
  GaugeChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  TitleComponent
])

// Shared palette for chart series — kept in sync with the Vuetify/Tailwind palette.
export const CHART_COLORS = {
  brand: '#0F766E',
  income: '#15803D',
  expense: '#E11D48',
  warn: '#D97706',
  neutral: '#64748B',
  track: '#E7E5DE',
  ink: '#1F2937',
  inkSoft: '#64748B'
}

// Ordered categorical palette for the spending donut (one color per category).
export const CATEGORY_PALETTE = [
  '#0F766E', '#D97706', '#E11D48', '#64748B',
  '#7C3AED', '#0EA5E9', '#15803D', '#DB2777'
]

export { VChart }
