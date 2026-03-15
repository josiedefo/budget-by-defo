<template>
  <v-card class="mb-4" color="primary" variant="tonal">
    <v-card-text>
      <v-row align="center">
        <v-col cols="12" sm="3" class="text-center">
          <div class="text-caption text-medium-emphasis">Total Pool</div>
          <div class="text-h5 font-weight-bold">${{ formatAmount(totalPoolBalance) }}</div>
        </v-col>
        <v-col cols="12" sm="3" class="text-center">
          <div class="text-caption text-medium-emphasis">Allocated</div>
          <div class="text-h6">${{ formatAmount(allocatedTotal) }}</div>
        </v-col>
        <v-col cols="12" sm="3" class="text-center">
          <div class="text-caption text-medium-emphasis">Unassigned</div>
          <div class="text-h6">${{ formatAmount(unassignedBalance) }}</div>
        </v-col>
        <v-col cols="12" sm="3" class="text-center">
          <div class="text-caption text-medium-emphasis d-inline-flex align-center gap-1">
            Untracked
            <v-tooltip text="How much account money have you not categorized yet? This is money sitting in your accounts that hasn't been deposited into any fund." location="bottom" max-width="260">
              <template #activator="{ props: tip }">
                <v-icon v-bind="tip" size="14" style="cursor:pointer">mdi-information-outline</v-icon>
              </template>
            </v-tooltip>
          </div>
          <div class="text-h6" :class="untracked < 0 ? 'text-error' : ''">
            ${{ formatAmount(untracked) }}
          </div>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

const savingsStore = useSavingsStore()
const { funds, totalPoolBalance, unassignedFund } = storeToRefs(savingsStore)

const allocatedTotal = computed(() =>
  funds.value
    .filter(f => !f.isSystemFund)
    .reduce((sum, f) => sum + parseFloat(f.balance || 0), 0)
)

const unassignedBalance = computed(() =>
  parseFloat(unassignedFund.value?.balance || 0)
)

const untracked = computed(() =>
  totalPoolBalance.value - allocatedTotal.value - unassignedBalance.value
)

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>
