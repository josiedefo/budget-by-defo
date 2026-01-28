<template>
  <v-container fluid class="pa-4">
    <v-card class="mb-4">
      <v-card-text class="d-flex align-center justify-center">
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
      </v-card-text>
    </v-card>

    <v-row v-if="loading" class="justify-center mt-8">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
    </v-row>

    <template v-else-if="yearlySummary">
      <v-alert
        v-if="isOffline"
        type="warning"
        variant="tonal"
        class="mb-4"
        closable
      >
        Unable to connect to server. No budget data available.
      </v-alert>

      <v-card class="mb-4">
        <v-card-title>{{ selectedYear }} Summary</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" sm="6" md="3">
              <div class="text-subtitle-2 text-medium-emphasis">Total Planned Income</div>
              <div class="text-h5 text-success">{{ formatCurrency(yearlySummary.totalPlannedIncome) }}</div>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <div class="text-subtitle-2 text-medium-emphasis">Total Actual Income</div>
              <div class="text-h5 text-success">{{ formatCurrency(yearlySummary.totalActualIncome) }}</div>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <div class="text-subtitle-2 text-medium-emphasis">Total Planned Expenses</div>
              <div class="text-h5 text-error">{{ formatCurrency(yearlySummary.totalPlannedExpenses) }}</div>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <div class="text-subtitle-2 text-medium-emphasis">Total Actual Expenses</div>
              <div class="text-h5 text-error">{{ formatCurrency(yearlySummary.totalActualExpenses) }}</div>
            </v-col>
          </v-row>
          <v-divider class="my-3"></v-divider>
          <v-row>
            <v-col cols="12" sm="6">
              <div class="text-subtitle-2 text-medium-emphasis">Total Planned Savings</div>
              <div class="text-h5" :class="yearlySummary.totalPlannedSavings >= 0 ? 'text-success' : 'text-error'">
                {{ formatCurrency(yearlySummary.totalPlannedSavings) }}
              </div>
            </v-col>
            <v-col cols="12" sm="6">
              <div class="text-subtitle-2 text-medium-emphasis">Total Actual Savings</div>
              <div class="text-h5" :class="yearlySummary.totalActualSavings >= 0 ? 'text-success' : 'text-error'">
                {{ formatCurrency(yearlySummary.totalActualSavings) }}
              </div>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <v-card>
        <v-card-title>Monthly Breakdown</v-card-title>
        <v-table>
          <thead>
            <tr>
              <th>Month</th>
              <th class="text-right">Planned Income</th>
              <th class="text-right">Actual Income</th>
              <th class="text-right">Planned Expenses</th>
              <th class="text-right">Actual Expenses</th>
              <th class="text-right">Savings</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="monthData in monthsData" :key="monthData.month">
              <td>{{ getMonthName(monthData.month) }}</td>
              <td class="text-right text-success">{{ formatCurrency(monthData.plannedIncome) }}</td>
              <td class="text-right text-success">{{ formatCurrency(monthData.actualIncome) }}</td>
              <td class="text-right text-error">{{ formatCurrency(monthData.plannedExpenses) }}</td>
              <td class="text-right text-error">{{ formatCurrency(monthData.actualExpenses) }}</td>
              <td class="text-right" :class="monthData.actualSavings >= 0 ? 'text-success' : 'text-error'">
                {{ formatCurrency(monthData.actualSavings) }}
              </td>
              <td>
                <v-btn size="small" variant="text" color="primary" @click="viewMonth(monthData.month)">
                  View
                </v-btn>
              </td>
            </tr>
            <tr v-if="monthsData.length === 0">
              <td colspan="7" class="text-center text-medium-emphasis py-4">
                No budgets created for {{ selectedYear }} yet.
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
    </template>
  </v-container>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useBudgetStore } from '@/stores/budget'

const props = defineProps({
  year: { type: Number, required: true }
})

const router = useRouter()
const budgetStore = useBudgetStore()
const { yearlySummary, loading } = storeToRefs(budgetStore)
const isOffline = computed(() => yearlySummary.value?.isOffline || false)

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 11 }, (_, i) => currentYear - 5 + i)
const selectedYear = ref(props.year)

const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
]

const monthsData = computed(() => yearlySummary.value?.months || [])

function getMonthName(month) {
  return monthNames[month - 1]
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

async function loadYearlySummary() {
  await budgetStore.fetchYearlySummary(selectedYear.value)
}

function viewMonth(month) {
  router.push({ name: 'monthly', params: { year: selectedYear.value, month } })
}

watch(selectedYear, (newYear) => {
  router.push({ name: 'yearly', params: { year: newYear } })
})

watch(() => props.year, (y) => {
  selectedYear.value = y
  loadYearlySummary()
})

onMounted(loadYearlySummary)
</script>
