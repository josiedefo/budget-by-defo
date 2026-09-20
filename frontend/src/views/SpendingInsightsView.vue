<template>
  <v-container fluid class="pa-2 pa-sm-4">
    <v-card class="mb-4">
      <v-card-text>
        <div class="d-flex align-center justify-center mb-3">
          <v-btn icon variant="text" @click="selectedYear--">
            <v-icon>mdi-chevron-left</v-icon>
          </v-btn>

          <v-select
            v-model="selectedYear"
            :items="years"
            density="compact"
            hide-details
            variant="outlined"
            class="mx-4"
            style="max-width: 120px"
          ></v-select>

          <v-btn icon variant="text" @click="selectedYear++">
            <v-icon>mdi-chevron-right</v-icon>
          </v-btn>
        </div>

        <v-select
          v-model="selectedPickerItem"
          :items="pickerItems"
          item-title="title"
          return-object
          density="compact"
          hide-details
          variant="outlined"
          label="Budget item"
          :disabled="pickerItems.length === 0"
          :placeholder="pickerItems.length === 0 ? 'No items available for this year' : undefined"
        ></v-select>
      </v-card-text>
    </v-card>

    <v-row v-if="loading" class="justify-center mt-8">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
    </v-row>

    <v-alert v-else-if="error" type="warning" variant="tonal" class="mt-4">
      {{ error }}
    </v-alert>

    <v-alert v-else-if="!hasSelection" type="info" variant="tonal" class="mt-4">
      Pick a budget item above to see its spending insights.
    </v-alert>

    <template v-else-if="insight">
      <v-card class="mb-4">
        <v-card-title class="d-flex align-center">
          <v-icon :color="insight.isIncome ? 'success' : 'error'" class="mr-2">
            {{ insight.isIncome ? 'mdi-arrow-down' : 'mdi-arrow-up' }}
          </v-icon>
          {{ insight.itemName }}
          <v-chip size="small" class="ml-2" :color="insight.isIncome ? 'success' : 'error'" variant="tonal">
            {{ insight.sectionName }}
          </v-chip>
        </v-card-title>

        <v-card-text v-if="insight.months.length === 0">
          <v-alert type="info" variant="tonal" density="compact">
            No budget data found for "{{ insight.itemName }}" in {{ selectedYear }}.
          </v-alert>
        </v-card-text>

        <v-card-text v-else>
          <v-row density="compact">
            <v-col cols="6" sm="3">
              <div class="text-body-small text-medium-emphasis">
                {{ insight.isIncome ? 'Share of' : 'Share of' }} {{ getMonthName(insight.currentMonth) }} {{ insight.isIncome ? 'income' : 'spending' }}
              </div>
              <div class="text-body-large text-sm-headline-small font-weight-bold">
                {{ formatPercent(insight.currentMonthShare) }}
              </div>
              <div class="text-caption text-medium-emphasis">{{ formatCurrency(insight.currentMonthActual) }}</div>
            </v-col>
            <v-col cols="6" sm="3">
              <div class="text-body-small text-medium-emphasis">{{ selectedYear }} total</div>
              <div class="text-body-large text-sm-headline-small font-weight-bold" :class="insight.isIncome ? 'text-success' : 'text-error'">
                {{ formatCurrency(insight.annualActual) }}
              </div>
              <div class="text-caption text-medium-emphasis">planned {{ formatCurrency(insight.annualPlanned) }}</div>
            </v-col>
            <v-col cols="6" sm="3">
              <div class="text-body-small text-medium-emphasis">Year to date</div>
              <div class="text-body-large text-sm-headline-small font-weight-bold">
                {{ formatCurrency(insight.ytdActual) }}
              </div>
            </v-col>
            <v-col cols="6" sm="3">
              <div class="text-body-small text-medium-emphasis">Monthly average</div>
              <div class="text-body-large text-sm-headline-small font-weight-bold">
                {{ formatCurrency(insight.monthlyAverageActual) }}
              </div>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <v-card v-if="insight.months.length > 0">
        <v-card-title class="d-flex align-center">
          Monthly Evolution
          <v-spacer></v-spacer>
          <v-btn size="small" variant="text" @click="showTable = !showTable">
            {{ showTable ? 'Show chart' : 'View as table' }}
          </v-btn>
        </v-card-title>
        <v-card-text>
          <ItemTrendChart v-if="!showTable" :months="monthsFull" :is-income="!!insight.isIncome" />

          <v-table v-else density="compact">
            <thead>
              <tr>
                <th>Month</th>
                <th class="text-right">Planned</th>
                <th class="text-right">Actual</th>
                <th class="text-right">Diff</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in monthsFull" :key="row.month">
                <td>{{ getMonthName(row.month) }}</td>
                <template v-if="row.actual != null">
                  <td class="text-right">{{ formatCurrency(row.planned) }}</td>
                  <td class="text-right">{{ formatCurrency(row.actual) }}</td>
                  <td class="text-right" :class="rowDiff(row) >= 0 ? 'text-success' : 'text-error'">
                    {{ formatCurrency(rowDiff(row)) }}
                  </td>
                </template>
                <template v-else>
                  <td colspan="3" class="text-medium-emphasis">No budget</td>
                </template>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>
    </template>
  </v-container>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { budgetApi } from '@/services/api'
import ItemTrendChart from '@/components/charts/ItemTrendChart.vue'

const props = defineProps({
  year: { type: Number, required: true }
})

const route = useRoute()
const router = useRouter()

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 11 }, (_, i) => currentYear - 5 + i)
const selectedYear = ref(props.year)

const loading = ref(false)
const error = ref(null)
const insight = ref(null)
const pickerItems = ref([])
const selectedPickerItem = ref(null)
let suppressPickerWatch = false

const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
]

function getMonthName(month) {
  return month ? monthNames[month - 1] : ''
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

function formatPercent(share) {
  return `${((share || 0) * 100).toFixed(1)}%`
}

function rowDiff(row) {
  if (row.actual == null) return 0
  return insight.value?.isIncome ? row.actual - row.planned : row.planned - row.actual
}

const hasSelection = computed(() => !!(route.query.section && route.query.item))

// Always 12 entries so the chart/table show every month, with nulls where no budget exists.
const monthsFull = computed(() => {
  const byMonth = new Map((insight.value?.months || []).map(m => [m.month, m]))
  return Array.from({ length: 12 }, (_, i) => {
    const m = i + 1
    const point = byMonth.get(m)
    return point
      ? { month: m, planned: point.planned, actual: point.actual, monthTotal: point.monthTotal }
      : { month: m, planned: null, actual: null, monthTotal: null }
  })
})

const showTable = ref(false)

function probeMonth(year) {
  const now = new Date()
  return year === now.getFullYear() ? now.getMonth() + 1 : 12
}

async function loadPickerItems(year, month) {
  if (!month) {
    pickerItems.value = []
    return
  }
  try {
    const res = await budgetApi.getBudget(year, month, false)
    if (!res.data) {
      pickerItems.value = []
      return
    }
    const items = []
    for (const section of res.data.sections || []) {
      for (const item of section.items || []) {
        items.push({
          title: `${section.name}: ${item.name}`,
          sectionName: section.name,
          itemName: item.name
        })
      }
    }
    pickerItems.value = items
  } catch {
    pickerItems.value = []
  }
}

async function loadInsight() {
  loading.value = true
  error.value = null
  const section = route.query.section
  const item = route.query.item

  try {
    if (section && item) {
      const res = await budgetApi.getItemInsight(selectedYear.value, section, item)
      insight.value = res.data
      await loadPickerItems(selectedYear.value, insight.value?.currentMonth || probeMonth(selectedYear.value))
    } else {
      insight.value = null
      await loadPickerItems(selectedYear.value, probeMonth(selectedYear.value))
    }

    // Sync the picker's selection to the current route without re-triggering navigation.
    suppressPickerWatch = true
    selectedPickerItem.value = pickerItems.value.find(
      p => p.sectionName === section && p.itemName === item
    ) || null
    suppressPickerWatch = false
  } catch (e) {
    error.value = 'Failed to load spending insight for this item.'
  } finally {
    loading.value = false
  }
}

function navigateTo(year, section, item) {
  router.push({
    name: 'insights',
    params: { year },
    query: { section, item }
  })
}

watch(selectedYear, (newYear) => {
  navigateTo(newYear, route.query.section, route.query.item)
})

watch(() => props.year, (y) => {
  selectedYear.value = y
})

watch(() => [route.query.section, route.query.item], () => {
  loadInsight()
})

watch(selectedPickerItem, (picked) => {
  if (suppressPickerWatch || !picked) return
  navigateTo(selectedYear.value, picked.sectionName, picked.itemName)
})

onMounted(() => {
  loadInsight()
})
</script>
