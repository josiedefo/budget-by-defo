<template>
  <div>
    <!-- Unassigned Fund (system fund) -->
    <div v-if="unassignedFund" class="mb-4">
      <div class="text-subtitle-2 text-medium-emphasis mb-2">Unassigned</div>
      <v-row>
        <v-col cols="12" sm="6" md="4" lg="3">
          <SavingsFundCard
            :fund="unassignedFund"
            @withdraw="$emit('open-withdraw', unassignedFund)"
            @reallocate="$emit('open-reallocate', unassignedFund)"
            @edit="$emit('open-edit', unassignedFund)"
          />
        </v-col>
      </v-row>
    </div>

    <!-- User Funds (draggable) -->
    <div v-if="orderedFunds.length > 0">
      <div class="text-subtitle-2 text-medium-emphasis mb-2">
        Funds
        <span class="text-caption text-disabled ml-1">(drag to reorder)</span>
      </div>
      <draggable
        v-model="orderedFunds"
        item-key="id"
        class="v-row"
        handle=".drag-handle"
        :animation="200"
        ghost-class="drag-ghost"
        @end="saveOrder"
      >
        <template #item="{ element: fund }">
          <v-col cols="12" sm="6" md="4" lg="3">
            <SavingsFundCard
              :fund="fund"
              @withdraw="$emit('open-withdraw', fund)"
              @reallocate="$emit('open-reallocate', fund)"
              @edit="$emit('open-edit', fund)"
              @delete="$emit('delete-fund', fund.id)"
              @payout="$emit('open-payout', fund)"
            />
          </v-col>
        </template>
      </draggable>
    </div>

    <v-alert v-if="!unassignedFund && orderedFunds.length === 0" type="info" variant="tonal">
      No funds yet. Create your first savings fund to get started.
    </v-alert>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import draggable from 'vuedraggable'
import { useSavingsStore } from '@/stores/savings'
import SavingsFundCard from './SavingsFundCard.vue'

const savingsStore = useSavingsStore()
const { unassignedFund, userFunds } = storeToRefs(savingsStore)

defineEmits(['open-withdraw', 'open-reallocate', 'open-edit', 'delete-fund', 'open-payout'])

const STORAGE_KEY = 'savings-fund-order'
const orderedFunds = ref([])

function applyStoredOrder(funds) {
  const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  if (stored.length === 0) return [...funds]
  const indexMap = Object.fromEntries(stored.map((id, i) => [id, i]))
  return [...funds].sort((a, b) => {
    const ia = indexMap[a.id] ?? Infinity
    const ib = indexMap[b.id] ?? Infinity
    return ia - ib
  })
}

function saveOrder() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(orderedFunds.value.map(f => f.id)))
}

watch(userFunds, (funds) => {
  orderedFunds.value = applyStoredOrder(funds)
}, { immediate: true })
</script>

<style scoped>
.drag-ghost {
  opacity: 0.4;
}
</style>
