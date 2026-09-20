<template>
  <div class="trend-chart">
    <div class="chart-legend">
      <span class="legend-entry">
        <span class="legend-swatch" style="background: rgb(var(--v-theme-success))"></span>
        {{ isIncome ? 'At/above planned' : 'At/under planned' }}
      </span>
      <span class="legend-entry">
        <span class="legend-swatch" style="background: rgb(var(--v-theme-error))"></span>
        {{ isIncome ? 'Below planned' : 'Over planned' }}
      </span>
      <span class="legend-entry">
        <span class="legend-line"></span>
        Planned
      </span>
    </div>

    <div class="chart-svg-wrap">
      <svg :viewBox="`0 0 ${width} ${height}`" class="chart-svg" role="img" :aria-label="ariaLabel">
        <!-- Gridlines + y-axis labels -->
        <g>
          <line
            v-for="tick in yTicks"
            :key="'grid-' + tick.value"
            :x1="leftPad" :x2="width - rightPad"
            :y1="tick.y" :y2="tick.y"
            class="gridline"
          />
          <text
            v-for="tick in yTicks"
            :key="'label-' + tick.value"
            :x="leftPad - 8" :y="tick.y"
            class="axis-label"
            text-anchor="end"
            dominant-baseline="middle"
          >{{ formatCompact(tick.value) }}</text>
        </g>

        <!-- Bars -->
        <g v-for="(bar, i) in bars" :key="'bar-' + i">
          <rect
            v-if="bar.hasData"
            :x="bar.x" :y="bar.y"
            :width="barWidth" :height="bar.barHeight"
            :rx="4" :ry="4"
            :fill="bar.color"
            class="bar-rect"
            :class="{ 'bar-rect--hover': hoveredIndex === i }"
          />
          <text v-else :x="bar.centerX" :y="baselineY - 6" text-anchor="middle" class="empty-mark">–</text>

          <!-- month label -->
          <text :x="bar.centerX" :y="baselineY + 18" text-anchor="middle" class="axis-label month-label">
            {{ monthAbbrev(bar.month) }}
          </text>

          <!-- transparent hit target, larger than the bar -->
          <rect
            :x="bar.hitX" :y="topPad"
            :width="hitWidth" :height="chartHeight"
            fill="transparent"
            class="hit-rect"
            tabindex="0"
            :aria-label="hitLabel(bar)"
            @mouseenter="hoveredIndex = i"
            @mouseleave="hoveredIndex = null"
            @focus="hoveredIndex = i"
            @blur="hoveredIndex = null"
          />
        </g>

        <!-- Planned reference line (drawn as separate sub-paths so gaps skip missing months) -->
        <g>
          <polyline
            v-for="(segment, si) in plannedSegments"
            :key="'seg-' + si"
            :points="segment"
            class="planned-line"
          />
          <circle
            v-for="(pt, pi) in plannedPoints"
            :key="'pt-' + pi"
            :cx="pt.x" :cy="pt.y" r="4"
            class="planned-marker"
          />
        </g>
      </svg>

      <div
        v-if="hoveredIndex !== null"
        class="chart-tooltip"
        :style="tooltipStyle"
      >
        <div class="tooltip-month">{{ monthName(bars[hoveredIndex].month) }}</div>
        <template v-if="bars[hoveredIndex].hasData">
          <div class="tooltip-row"><span class="tooltip-value">{{ formatCurrency(bars[hoveredIndex].actual) }}</span> actual</div>
          <div class="tooltip-row"><span class="tooltip-value">{{ formatCurrency(bars[hoveredIndex].planned) }}</span> planned</div>
          <div class="tooltip-row" :class="bars[hoveredIndex].diff >= 0 ? 'text-success' : 'text-error'">
            <span class="tooltip-value">{{ formatDiff(bars[hoveredIndex].diff) }}</span> {{ isIncome ? 'vs planned' : 'vs planned' }}
          </div>
        </template>
        <div v-else class="tooltip-row text-medium-emphasis">No budget for this month</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  // 12 entries, one per calendar month (1-12) in order. Missing budget months carry
  // planned/actual/monthTotal = null so the chart can render an empty slot instead of a
  // misleading $0 bar.
  months: { type: Array, required: true },
  isIncome: { type: Boolean, default: false }
})

const width = 720
const height = 300
const leftPad = 52
const rightPad = 12
const topPad = 16
const bottomPad = 34

const chartWidth = width - leftPad - rightPad
const chartHeight = height - topPad - bottomPad
const baselineY = topPad + chartHeight

const bandWidth = chartWidth / 12
const barWidth = Math.min(24, bandWidth * 0.55)
const hitWidth = bandWidth

const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
]

function monthName(m) { return monthNames[m - 1] }
function monthAbbrev(m) { return monthNames[m - 1].charAt(0) }

// "Nice" round max so gridlines land on clean numbers (0 / 25 / 50 / 75 / 100).
function niceMax(value) {
  if (!value || value <= 0) return 100
  const exponent = Math.floor(Math.log10(value))
  const magnitude = Math.pow(10, exponent)
  const residual = value / magnitude
  let niceResidual
  if (residual <= 1) niceResidual = 1
  else if (residual <= 2) niceResidual = 2
  else if (residual <= 5) niceResidual = 5
  else niceResidual = 10
  return niceResidual * magnitude
}

const scaleMax = computed(() => {
  const values = props.months.flatMap(m => [m.planned, m.actual]).filter(v => v != null)
  const rawMax = values.length ? Math.max(...values) : 0
  return niceMax(rawMax * 1.15)
})

function valueToY(value) {
  const clamped = Math.max(0, Math.min(value, scaleMax.value))
  return baselineY - (clamped / scaleMax.value) * chartHeight
}

const yTicks = computed(() => {
  const steps = 4
  const ticks = []
  for (let i = 0; i <= steps; i++) {
    const value = (scaleMax.value / steps) * i
    ticks.push({ value, y: valueToY(value) })
  }
  return ticks
})

function diffFor(entry) {
  // Favorable (>=0) mirrors BudgetSection.getItemDiff: expenses = planned - actual,
  // income = actual - planned.
  return props.isIncome ? entry.actual - entry.planned : entry.planned - entry.actual
}

const bars = computed(() => {
  return props.months.map((entry, i) => {
    const centerX = leftPad + bandWidth * i + bandWidth / 2
    const hasData = entry.actual != null
    const barHeight = hasData ? Math.max(0, baselineY - valueToY(entry.actual)) : 0
    const diff = hasData ? diffFor(entry) : 0
    const color = hasData
      ? (diff >= 0 ? 'rgb(var(--v-theme-success))' : 'rgb(var(--v-theme-error))')
      : 'transparent'
    return {
      month: entry.month,
      hasData,
      planned: entry.planned,
      actual: entry.actual,
      diff,
      centerX,
      x: centerX - barWidth / 2,
      hitX: leftPad + bandWidth * i,
      y: hasData ? valueToY(entry.actual) : baselineY,
      barHeight,
      color
    }
  })
})

// Planned line, broken into contiguous runs so a missing month leaves a visible gap
// instead of a misleading straight line across it.
const plannedPoints = computed(() =>
  bars.value
    .filter(b => b.planned != null)
    .map(b => ({ x: b.centerX, y: valueToY(b.planned) }))
)

const plannedSegments = computed(() => {
  const segments = []
  let current = []
  bars.value.forEach(b => {
    if (b.planned == null) {
      if (current.length > 1) segments.push(current.map(p => `${p.x},${p.y}`).join(' '))
      current = []
      return
    }
    current.push({ x: b.centerX, y: valueToY(b.planned) })
  })
  if (current.length > 1) segments.push(current.map(p => `${p.x},${p.y}`).join(' '))
  return segments
})

const hoveredIndex = ref(null)

const tooltipStyle = computed(() => {
  if (hoveredIndex.value === null) return {}
  const bar = bars.value[hoveredIndex.value]
  const leftPct = (bar.centerX / width) * 100
  return {
    left: `${leftPct}%`,
    top: '4px'
  }
})

function hitLabel(bar) {
  if (!bar.hasData) return `${monthName(bar.month)}: no budget`
  return `${monthName(bar.month)}: ${formatCurrency(bar.actual)} actual, ${formatCurrency(bar.planned)} planned`
}

const ariaLabel = computed(() => 'Monthly actual spending compared to planned amount')

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value || 0)
}

function formatDiff(value) {
  const n = value || 0
  const formatted = formatCurrency(Math.abs(n))
  return n >= 0 ? `+${formatted}` : `-${formatted}`
}

function formatCompact(value) {
  if (value >= 1000) return `$${(value / 1000).toFixed(value % 1000 === 0 ? 0 : 1)}K`
  return `$${Math.round(value)}`
}
</script>

<style scoped>
.trend-chart {
  width: 100%;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 0.8rem;
  color: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 75%, transparent);
}

.legend-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-swatch {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}

.legend-line {
  width: 14px;
  height: 2px;
  background: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 55%, transparent);
  display: inline-block;
}

.chart-svg-wrap {
  position: relative;
}

.chart-svg {
  width: 100%;
  height: auto;
  display: block;
}

.gridline {
  stroke: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 12%, transparent);
  stroke-width: 1;
}

.axis-label {
  fill: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 60%, transparent);
  font-size: 11px;
}

.month-label {
  font-size: 11px;
}

.empty-mark {
  fill: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 35%, transparent);
  font-size: 12px;
}

.bar-rect {
  transition: opacity 0.15s ease;
}

.bar-rect--hover {
  opacity: 0.8;
}

.hit-rect {
  cursor: pointer;
}

.hit-rect:focus-visible {
  outline: 2px solid rgb(var(--v-theme-primary));
  outline-offset: -2px;
}

.planned-line {
  fill: none;
  stroke: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 55%, transparent);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.planned-marker {
  fill: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 55%, transparent);
  stroke: rgb(var(--v-theme-surface));
  stroke-width: 2;
}

.chart-tooltip {
  position: absolute;
  transform: translateX(-50%);
  background: rgb(var(--v-theme-surface));
  border: 1px solid color-mix(in srgb, rgb(var(--v-theme-on-surface)) 15%, transparent);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 0.78rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  pointer-events: none;
  white-space: nowrap;
  z-index: 5;
}

.tooltip-month {
  font-weight: 600;
  margin-bottom: 2px;
}

.tooltip-row {
  color: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 70%, transparent);
}

.tooltip-value {
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
}
</style>
