<template>
  <div class="dash-grid">
    <!-- Left to spend hero -->
    <div class="dash-card flex flex-col">
      <div class="text-sm text-[color:var(--color-ink-soft)]">Left to spend this month</div>
      <div class="text-4xl font-medium mt-1" :class="leftToSpend >= 0 ? 'text-[color:var(--color-ink)]' : 'text-[color:var(--color-expense)]'">
        {{ formatCurrency(leftToSpend) }}
      </div>
      <div class="text-xs mt-1" :class="paceClass">
        <v-icon size="14">{{ pace.icon }}</v-icon>
        {{ pace.label }}
      </div>

      <div class="flex gap-4 mt-4">
        <div>
          <div class="text-xs text-[color:var(--color-ink-soft)]">Income</div>
          <div class="text-base font-medium text-[color:var(--color-income)]">{{ formatCurrency(totalActualIncome) }}</div>
        </div>
        <div>
          <div class="text-xs text-[color:var(--color-ink-soft)]">Spent</div>
          <div class="text-base font-medium text-[color:var(--color-expense)]">{{ formatCurrency(totalActualExpenses) }}</div>
        </div>
        <div>
          <div class="text-xs text-[color:var(--color-ink-soft)]">Balance</div>
          <div class="text-base font-medium" :class="actualBalance >= 0 ? 'text-[color:var(--color-income)]' : 'text-[color:var(--color-expense)]'">
            {{ formatCurrency(actualBalance) }}
          </div>
        </div>
      </div>

      <div v-if="trend.length > 1" class="mt-auto pt-3">
        <div class="text-xs text-[color:var(--color-ink-soft)] mb-1">Spending trend</div>
        <div class="trend-chart">
          <v-chart :option="trendOption" autoresize />
        </div>
      </div>
    </div>

    <!-- Spending donut -->
    <div class="dash-card">
      <div class="text-sm text-[color:var(--color-ink-soft)] mb-2">Where it went</div>
      <div v-if="categories.length" class="donut-wrap">
        <div class="donut-chart">
          <v-chart :option="donutOption" autoresize />
        </div>
        <div class="donut-legend">
          <div v-for="c in categories" :key="c.name" class="legend-row">
            <span class="legend-dot" :style="{ background: c.color }"></span>
            <span class="legend-name">{{ c.name }}</span>
            <span class="legend-pct">{{ c.pct }}%</span>
          </div>
        </div>
      </div>
      <div v-else class="text-sm text-[color:var(--color-ink-soft)] py-6 text-center">
        No spending recorded yet.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useBudgetStore } from '@/stores/budget'
import { budgetApi } from '@/services/api'
import { CATEGORY_PALETTE, CHART_COLORS } from '@/plugins/echarts'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true }
})

const budgetStore = useBudgetStore()
const {
  totalActualIncome,
  totalActualExpenses,
  totalPlannedExpenses,
  actualBalance,
  sections
} = storeToRefs(budgetStore)

const leftToSpend = computed(() => (totalPlannedExpenses.value || 0) - (totalActualExpenses.value || 0))

// --- Pace hint (only meaningful for the current calendar month) ---
const pace = computed(() => {
  const now = new Date()
  const isCurrentMonth = now.getFullYear() === props.year && (now.getMonth() + 1) === props.month
  const daysInMonth = new Date(props.year, props.month, 0).getDate()
  if (!isCurrentMonth) {
    return leftToSpend.value >= 0
      ? { icon: 'mdi-check-circle-outline', label: `${formatCurrency(leftToSpend.value)} under budget`, tone: 'ok' }
      : { icon: 'mdi-alert-circle-outline', label: `${formatCurrency(Math.abs(leftToSpend.value))} over budget`, tone: 'over' }
  }
  const daysLeft = daysInMonth - now.getDate()
  const expectedByNow = (totalPlannedExpenses.value || 0) * (now.getDate() / daysInMonth)
  const onPace = (totalActualExpenses.value || 0) <= expectedByNow
  return {
    icon: onPace ? 'mdi-trending-down' : 'mdi-trending-up',
    label: `${daysLeft} days left · ${onPace ? 'on pace' : 'ahead of pace'}`,
    tone: onPace ? 'ok' : 'over'
  }
})

const paceClass = computed(() =>
  pace.value.tone === 'ok' ? 'text-[color:var(--color-income)]' : 'text-[color:var(--color-expense)]'
)

// --- Donut categories (expense sections with spend) ---
const categories = computed(() => {
  const expense = sections.value.filter(s => !s.isIncome && (s.totalActual || 0) > 0)
  const total = expense.reduce((sum, s) => sum + (s.totalActual || 0), 0)
  return expense.map((s, i) => ({
    name: s.name,
    value: s.totalActual || 0,
    pct: total > 0 ? Math.round((s.totalActual / total) * 100) : 0,
    color: CATEGORY_PALETTE[i % CATEGORY_PALETTE.length]
  }))
})

const donutOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    valueFormatter: (v) => formatCurrency(v)
  },
  title: {
    text: formatCurrency(totalActualExpenses.value),
    subtext: 'spent',
    left: 'center',
    top: 'center',
    textStyle: { fontSize: 16, fontWeight: 500, color: CHART_COLORS.ink },
    subtextStyle: { fontSize: 11, color: CHART_COLORS.inkSoft }
  },
  series: [{
    type: 'pie',
    radius: ['58%', '82%'],
    center: ['50%', '50%'],
    avoidLabelOverlap: true,
    label: { show: false },
    labelLine: { show: false },
    data: categories.value.map(c => ({
      name: c.name,
      value: c.value,
      itemStyle: { color: c.color }
    }))
  }]
}))

// --- Trend sparkline (fetched locally to avoid clobbering the store's yearlySummary) ---
const trend = ref([])

async function loadTrend() {
  try {
    const res = await budgetApi.getYearlySummary(props.year)
    const months = res.data?.months || []
    trend.value = months
      .slice()
      .sort((a, b) => a.month - b.month)
      .map(m => ({ month: m.month, value: m.actualExpenses || 0 }))
  } catch {
    trend.value = []
  }
}

const monthAbbr = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const trendOption = computed(() => ({
  grid: { left: 2, right: 2, top: 6, bottom: 2, containLabel: false },
  xAxis: {
    type: 'category',
    show: false,
    data: trend.value.map(t => monthAbbr[t.month - 1])
  },
  yAxis: { type: 'value', show: false },
  tooltip: {
    trigger: 'axis',
    valueFormatter: (v) => formatCurrency(v)
  },
  series: [{
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 5,
    showSymbol: false,
    lineStyle: { color: CHART_COLORS.brand, width: 2 },
    areaStyle: { color: 'rgba(15,118,110,0.12)' },
    data: trend.value.map(t => t.value)
  }]
}))

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(value || 0)
}

onMounted(loadTrend)
watch(() => props.year, loadTrend)
</script>

<style scoped>
.dash-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 16px;
}
@media (max-width: 700px) {
  .dash-grid { grid-template-columns: 1fr; }
}
.dash-card {
  background: rgb(var(--v-theme-surface));
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.trend-chart { height: 56px; width: 100%; }
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}
.donut-chart {
  width: 160px;
  height: 160px;
  flex-shrink: 0;
}
.donut-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.82rem;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}
.legend-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgb(var(--v-theme-on-surface));
}
.legend-pct {
  color: var(--color-ink-soft);
  font-variant-numeric: tabular-nums;
}
</style>
