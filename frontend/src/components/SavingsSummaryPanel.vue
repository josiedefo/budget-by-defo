<template>
  <div>
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <template v-else-if="summary">
      <v-row class="mb-4">
        <v-col cols="12" sm="6" md="3">
          <v-card variant="outlined" class="text-center pa-4">
            <div class="text-caption text-medium-emphasis">Total Pool</div>
            <div class="text-h5 font-weight-bold">${{ formatAmount(summary.totalPool) }}</div>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card variant="outlined" class="text-center pa-4">
            <div class="text-caption text-medium-emphasis">Allocated</div>
            <div class="text-h5">${{ formatAmount(summary.totalAllocated) }}</div>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card variant="outlined" class="text-center pa-4">
            <div class="text-caption text-medium-emphasis">Unassigned</div>
            <div class="text-h5">${{ formatAmount(summary.totalUnallocated) }}</div>
          </v-card>
        </v-col>
        <v-col cols="12" sm="6" md="3">
          <v-card variant="outlined" class="text-center pa-4">
            <div class="text-caption text-medium-emphasis">Remaining to Save</div>
            <div class="text-h5 text-warning">${{ formatAmount(summary.totalRemainingToSave) }}</div>
          </v-card>
        </v-col>
      </v-row>

      <div v-if="summary.upcomingDeadlines && summary.upcomingDeadlines.length > 0">
        <div class="text-subtitle-1 font-weight-medium mb-2">Upcoming Deadlines & Payouts</div>
        <v-list>
          <v-list-item
            v-for="fund in summary.upcomingDeadlines"
            :key="fund.id"
            :prepend-icon="fund.goalType === 'SPEND_DOWN' ? 'mdi-calendar-export' : 'mdi-calendar-clock'"
          >
            <template #title>{{ fund.name }}</template>
            <template #subtitle>
              <span v-if="fund.goalType === 'SPEND_DOWN'">
                Payout ${{ formatAmount(fund.payoutAmount) }} — {{ formatDate(fund.payoutDate) }}
              </span>
              <span v-else>
                Target ${{ formatAmount(fund.targetAmount) }} by {{ formatDate(fund.deadline) }}
                <span v-if="fund.remaining > 0"> — ${{ formatAmount(fund.remaining) }} remaining</span>
              </span>
            </template>
            <template #append>
              <v-chip size="x-small" :color="fund.status === 'COMPLETE' ? 'success' : 'warning'" variant="tonal">
                {{ fund.status === 'COMPLETE' ? 'Complete' : 'In Progress' }}
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
      </div>

      <v-alert v-else type="info" variant="tonal">
        No upcoming deadlines or payouts.
      </v-alert>
    </template>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

const savingsStore = useSavingsStore()
const { summary, loading } = storeToRefs(savingsStore)

onMounted(() => savingsStore.fetchSummary())

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>
