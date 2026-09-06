<template>
  <div class="dash-card">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-base font-medium text-[color:var(--color-ink)]">Where your money goes</h2>
      <button type="button" class="add-link" @click="$emit('add-section')">
        <v-icon size="18">mdi-plus</v-icon> Add category
      </button>
    </div>

    <div v-if="rows.length" class="flow-list">
      <button
        v-for="row in rows"
        :key="row.id"
        type="button"
        class="flow-row"
        @click="$emit('open', row.section)"
      >
        <span class="flow-icon" :style="{ background: row.tint, color: row.color }">
          <v-icon size="18">{{ row.icon }}</v-icon>
        </span>
        <div class="flow-body">
          <div class="flow-head">
            <span class="flow-name">{{ row.name }}</span>
            <span class="flow-amount">
              {{ formatCurrency(row.actual) }}
              <span class="flow-status" :class="row.over ? 'is-over' : 'is-ok'">{{ row.status }}</span>
            </span>
          </div>
          <div class="flow-track">
            <div class="flow-fill" :style="{ width: row.barPct + '%' }">
              <div class="flow-seg" :style="{ width: row.overSplitPct + '%', background: row.color }"></div>
              <div v-if="row.over" class="flow-seg" :style="{ width: (100 - row.overSplitPct) + '%', background: 'var(--color-expense)' }"></div>
            </div>
            <div
              v-if="row.plannedMarkPct !== null"
              class="flow-mark"
              :style="{ left: row.plannedMarkPct + '%' }"
              :title="'Budget: ' + formatCurrency(row.planned)"
            ></div>
          </div>
        </div>
        <v-icon size="18" class="flow-chevron">mdi-chevron-right</v-icon>
      </button>
    </div>

    <div v-else class="text-sm text-[color:var(--color-ink-soft)] py-6 text-center">
      No categories yet. Add one to start budgeting.
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CATEGORY_PALETTE } from '@/plugins/echarts'

const props = defineProps({
  sections: { type: Array, default: () => [] }
})

defineEmits(['open', 'add-section'])

const ICONS = {
  housing: 'mdi-home', rent: 'mdi-home', mortgage: 'mdi-home',
  food: 'mdi-silverware-fork-knife', groceries: 'mdi-basket',
  transport: 'mdi-car', transportation: 'mdi-car',
  utilities: 'mdi-flash', healthcare: 'mdi-heart-pulse', health: 'mdi-heart-pulse',
  entertainment: 'mdi-movie-open', fun: 'mdi-movie-open',
  savings: 'mdi-piggy-bank', shopping: 'mdi-shopping', travel: 'mdi-airplane',
  insurance: 'mdi-shield-check', education: 'mdi-school', kids: 'mdi-baby-carriage',
  subscriptions: 'mdi-refresh', debt: 'mdi-credit-card'
}

function iconFor(name) {
  const key = (name || '').trim().toLowerCase()
  return ICONS[key] || 'mdi-tag'
}

function tintFor(hex) {
  return `color-mix(in srgb, ${hex} 16%, transparent)`
}

const rows = computed(() => {
  const expense = props.sections.filter(s => !s.isIncome)
  const maxActual = Math.max(1, ...expense.map(s => s.totalActual || 0))
  return expense
    .map((s, i) => {
      const actual = s.totalActual || 0
      const planned = s.totalPlanned || 0
      const over = planned > 0 && actual > planned
      const diff = planned - actual
      const color = CATEGORY_PALETTE[i % CATEGORY_PALETTE.length]
      return {
        id: s.id,
        section: s,
        name: s.name,
        actual,
        planned,
        over,
        color,
        tint: tintFor(color),
        icon: iconFor(s.name),
        status: diff < 0 ? `${formatCurrency(Math.abs(diff))} over` : `${formatCurrency(diff)} left`,
        barPct: (actual / maxActual) * 100,
        // Portion of the fill (in category color) before the over-budget overflow.
        overSplitPct: over ? (planned / actual) * 100 : 100,
        // Budget marker position relative to the same max scale (hidden if off-scale).
        plannedMarkPct: planned > 0 && planned <= maxActual ? (planned / maxActual) * 100 : null
      }
    })
    .sort((a, b) => b.actual - a.actual)
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(value || 0)
}
</script>

<style scoped>
.dash-card {
  background: rgb(var(--v-theme-surface));
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.add-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 0.82rem;
  color: rgb(var(--v-theme-primary));
  background: transparent;
  cursor: pointer;
}
.flow-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.flow-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  border: none;
  padding: 4px 2px;
  cursor: pointer;
  text-align: left;
  border-radius: 10px;
  transition: background 0.12s ease;
}
.flow-row:hover {
  background: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 4%, transparent);
}
.flow-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.flow-body {
  flex: 1;
  min-width: 0;
}
.flow-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 6px;
}
.flow-name {
  font-size: 0.92rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
}
.flow-amount {
  font-size: 0.88rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.flow-status {
  font-size: 0.72rem;
  font-weight: 400;
  margin-left: 6px;
}
.flow-status.is-ok { color: #15803D; }
.flow-status.is-over { color: #E11D48; }
.flow-track {
  position: relative;
  height: 12px;
  border-radius: 99px;
  background: color-mix(in srgb, rgb(var(--v-theme-on-surface)) 7%, transparent);
  overflow: hidden;
}
.flow-fill {
  display: flex;
  height: 100%;
  border-radius: 99px;
  overflow: hidden;
  transition: width 0.4s ease;
  min-width: 3px;
}
.flow-seg { height: 100%; }
.flow-mark {
  position: absolute;
  top: -2px;
  bottom: -2px;
  width: 2px;
  background: rgba(0, 0, 0, 0.35);
}
.flow-chevron {
  color: var(--color-ink-soft);
  flex-shrink: 0;
}
</style>
