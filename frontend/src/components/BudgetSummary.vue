<template>
  <v-card elevation="2">
    <v-card-text>
      <v-row dense>
        <v-col cols="6" sm="6" md="3">
          <div class="text-caption text-medium-emphasis">Planned Income</div>
          <div class="text-body-1 text-sm-h5 font-weight-bold text-success">{{ formatCurrency(totalPlannedIncome) }}</div>
        </v-col>
        <v-col cols="6" sm="6" md="3">
          <div class="text-caption text-medium-emphasis">Actual Income</div>
          <div class="text-body-1 text-sm-h5 font-weight-bold text-success">{{ formatCurrency(totalActualIncome) }}</div>
        </v-col>
        <v-col cols="6" sm="6" md="3">
          <div class="text-caption text-medium-emphasis">Planned Expenses</div>
          <div class="text-body-1 text-sm-h5 font-weight-bold text-error">{{ formatCurrency(totalPlannedExpenses) }}</div>
        </v-col>
        <v-col cols="6" sm="6" md="3">
          <div class="text-caption text-medium-emphasis">Actual Expenses</div>
          <div class="text-body-1 text-sm-h5 font-weight-bold text-error">{{ formatCurrency(totalActualExpenses) }}</div>
        </v-col>
      </v-row>
      <v-divider class="my-2"></v-divider>
      <v-row dense>
        <v-col cols="6" sm="6">
          <div class="text-caption text-medium-emphasis">Planned Balance</div>
          <div class="text-body-1 text-sm-h5 font-weight-bold" :class="plannedBalance >= 0 ? 'text-success' : 'text-error'">
            {{ formatCurrency(plannedBalance) }}
          </div>
        </v-col>
        <v-col cols="6" sm="6">
          <div class="text-caption text-medium-emphasis">Actual Balance</div>
          <div class="text-body-1 text-sm-h5 font-weight-bold" :class="actualBalance >= 0 ? 'text-success' : 'text-error'">
            {{ formatCurrency(actualBalance) }}
          </div>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useBudgetStore } from '@/stores/budget'

const budgetStore = useBudgetStore()
const {
  totalPlannedIncome,
  totalActualIncome,
  totalPlannedExpenses,
  totalActualExpenses,
  plannedBalance,
  actualBalance
} = storeToRefs(budgetStore)

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}
</script>
