<template>
  <div>
    <!-- Unassigned Fund (system fund) -->
    <div v-if="unassignedFund" class="mb-4">
      <div class="text-title-small text-medium-emphasis mb-2">Unassigned</div>
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
      <div class="text-title-small text-medium-emphasis mb-2">
        Funds
        <span class="text-body-small text-disabled ml-1">(drag to reorder)</span>
      </div>
      <draggable
        v-model="orderedFunds"
        item-key="id"
        class="v-row v-row--density-default"
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
              @close="$emit('close-fund', fund)"
            />
          </v-col>
        </template>
      </draggable>
    </div>

    <!-- Closed funds -->
    <div v-if="closedFunds.length > 0" class="mt-4">
      <div class="text-title-small text-medium-emphasis mb-2">Closed</div>
      <v-row>
        <v-col v-for="fund in closedFunds" :key="fund.id" cols="12" sm="6" md="4" lg="3">
          <SavingsFundCard
            :fund="fund"
            @delete="$emit('delete-fund', fund.id)"
            @history="$emit('open-history', fund)"
          />
        </v-col>
      </v-row>
    </div>

    <v-alert v-if="!unassignedFund && orderedFunds.length === 0 && closedFunds.length === 0" type="info" variant="tonal">
      No funds yet. Create your first savings fund to get started.
    </v-alert>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import draggable from 'vuedraggable'
import { useSavingsStore } from '@/stores/savings'
import SavingsFundCard from './SavingsFundCard.vue'

const savingsStore = useSavingsStore()
const { unassignedFund, userFunds } = storeToRefs(savingsStore)

defineEmits(['open-withdraw', 'open-reallocate', 'open-edit', 'delete-fund', 'open-payout', 'close-fund', 'open-history'])

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

const closedFunds = computed(() => userFunds.value.filter(f => f.isClosed))

watch(userFunds, (funds) => {
  orderedFunds.value = applyStoredOrder(funds.filter(f => !f.isClosed))
}, { immediate: true })
</script>

<style scoped>
.drag-ghost {
  opacity: 0.4;
}
</style>
