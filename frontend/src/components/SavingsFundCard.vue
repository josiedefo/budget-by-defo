<template>
  <v-card class="fund-card h-100" variant="outlined">
    <v-card-title class="d-flex align-center justify-space-between pb-1">
      <span class="text-truncate">{{ fund.name }}</span>
      <div class="d-flex align-center ga-1">
        <v-chip size="x-small" :color="goalTypeColor" variant="tonal">
          {{ goalTypeLabel }}
        </v-chip>
        <v-icon size="small" class="cursor-pointer" @click="$emit('edit')">mdi-pencil</v-icon>
        <v-icon v-if="!fund.isSystemFund" size="small" class="cursor-pointer text-error"
          @click="$emit('delete')">mdi-delete</v-icon>
      </div>
    </v-card-title>

    <v-card-text class="pt-0">
      <div class="text-h5 font-weight-bold mb-2">${{ formatAmount(fund.balance) }}</div>

      <!-- TARGET / TARGET_WITH_DEADLINE / SPEND_DOWN progress -->
      <template v-if="fund.goalType === 'TARGET' || fund.goalType === 'TARGET_WITH_DEADLINE' || fund.goalType === 'SPEND_DOWN'">
        <v-progress-linear
          :model-value="fund.progressPercent || 0"
          :color="fund.status === 'COMPLETE' ? 'success' : 'primary'"
          rounded height="8" class="mb-1"
        />
        <div class="d-flex justify-space-between text-caption text-medium-emphasis">
          <span>{{ fund.progressPercent || 0 }}% of ${{ formatAmount(fund.targetAmount) }}</span>
          <span v-if="fund.remaining > 0">${{ formatAmount(fund.remaining) }} left</span>
        </div>
        <div v-if="fund.deadline" class="text-caption mt-1">
          <v-icon size="x-small">mdi-calendar</v-icon>
          Deadline: {{ formatDate(fund.deadline) }}
        </div>
        <div v-if="fund.payoutDate" class="text-caption mt-1">
          <v-icon size="x-small">mdi-calendar-export</v-icon>
          Payout: {{ formatDate(fund.payoutDate) }}
          <span v-if="fund.payoutAmount"> (${{ formatAmount(fund.payoutAmount) }})</span>
        </div>
        <v-chip size="x-small" class="mt-2" :color="fund.status === 'COMPLETE' ? 'success' : 'warning'" variant="tonal">
          {{ fund.status === 'COMPLETE' ? 'Complete' : 'In Progress' }}
        </v-chip>
      </template>

      <!-- SPEND_AS_YOU_GO -->
      <template v-else-if="fund.goalType === 'SPEND_AS_YOU_GO'">
        <div class="text-caption text-medium-emphasis">
          Ceiling: ${{ formatAmount(fund.ceiling) }}
        </div>
        <div v-if="fund.ytdSpent != null" class="text-caption text-medium-emphasis">
          Spent YTD: ${{ formatAmount(fund.ytdSpent) }}
        </div>
        <v-chip size="x-small" class="mt-2"
          :color="fund.status === 'AT_LIMIT' ? 'error' : 'success'" variant="tonal">
          {{ fund.status === 'AT_LIMIT' ? 'At Limit' : 'OK' }}
        </v-chip>
      </template>
    </v-card-text>

    <v-card-actions class="pt-0">
      <v-btn size="small" variant="text" color="primary" @click="$emit('withdraw')">
        <v-icon start size="small">mdi-minus-circle</v-icon>
        Withdraw
      </v-btn>
      <v-btn size="small" variant="text" @click="$emit('reallocate')">
        <v-icon start size="small">mdi-swap-horizontal</v-icon>
        Move
      </v-btn>
      <v-btn v-if="showPayoutButton" size="small" variant="text" color="warning" @click="$emit('payout')">
        <v-icon start size="small">mdi-calendar-export</v-icon>
        Payout
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fund: { type: Object, required: true }
})

defineEmits(['withdraw', 'reallocate', 'edit', 'delete', 'payout'])

const goalTypeLabels = {
  TARGET: 'Target',
  TARGET_WITH_DEADLINE: 'Target + Deadline',
  SPEND_DOWN: 'Spend-Down',
  SPEND_AS_YOU_GO: 'Spend-As-You-Go',
  NO_GOAL: 'No Goal'
}

const goalTypeColors = {
  TARGET: 'blue',
  TARGET_WITH_DEADLINE: 'deep-purple',
  SPEND_DOWN: 'orange',
  SPEND_AS_YOU_GO: 'teal',
  NO_GOAL: 'grey'
}

const goalTypeLabel = computed(() => goalTypeLabels[props.fund.goalType] || props.fund.goalType)
const goalTypeColor = computed(() => goalTypeColors[props.fund.goalType] || 'grey')

const showPayoutButton = computed(() => {
  if (props.fund.goalType !== 'SPEND_DOWN' || !props.fund.payoutDate) return false
  return new Date(props.fund.payoutDate) <= new Date()
})

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>

<style scoped>
.fund-card {
  transition: box-shadow 0.2s;
}
.fund-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.15) !important;
}
.cursor-pointer {
  cursor: pointer;
}
</style>
