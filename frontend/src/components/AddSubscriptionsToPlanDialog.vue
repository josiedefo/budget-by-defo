<template>
  <v-dialog v-model="dialog" max-width="600" persistent>
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="primary">mdi-repeat</v-icon>
        Add from Recurring Payments
      </v-card-title>
      <v-card-subtitle>Select recurring payments to add to your plan</v-card-subtitle>

      <v-card-text>
        <v-list v-if="subscriptions.length > 0" density="compact" select-strategy="classic">
          <v-list-item
            v-for="sub in subscriptions"
            :key="sub.id"
            @click="toggleSelection(sub.id)"
          >
            <template #prepend>
              <v-checkbox-btn
                :model-value="selectedIds.includes(sub.id)"
                @update:model-value="toggleSelection(sub.id)"
              ></v-checkbox-btn>
            </template>

            <v-list-item-title>{{ sub.name }}</v-list-item-title>
            <v-list-item-subtitle>
              {{ formatCurrency(sub.amount) }}
              <span v-if="sub.category" class="ml-2">
                <v-chip size="x-small" variant="outlined">{{ sub.category }}</v-chip>
              </span>
            </v-list-item-subtitle>

            <template #append>
              <span class="text-caption text-medium-emphasis">{{ sub.recurrence }}</span>
            </template>
          </v-list-item>
        </v-list>

        <div v-else class="text-center text-medium-emphasis py-4">
          No recurring payments available.
          <br>
          <v-btn variant="text" size="small" color="primary" class="mt-2" @click="openManageSubscriptions">
            Manage Recurring Payments
          </v-btn>
        </div>
      </v-card-text>

      <v-card-actions>
        <div class="text-caption text-medium-emphasis">
          {{ selectedIds.length }} selected - Total: {{ formatCurrency(selectedTotal) }}
        </div>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn
          color="primary"
          variant="flat"
          :disabled="selectedIds.length === 0"
          @click="addSelected"
        >
          Add Selected
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useSubscriptionStore } from '@/stores/subscription'
import { storeToRefs } from 'pinia'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'add', 'manage-subscriptions'])

const subscriptionStore = useSubscriptionStore()
const { subscriptions } = storeToRefs(subscriptionStore)

const dialog = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const selectedIds = ref([])

const selectedTotal = computed(() => {
  return subscriptions.value
    .filter(s => selectedIds.value.includes(s.id))
    .reduce((sum, s) => sum + (s.amount || 0), 0)
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

function toggleSelection(id) {
  const index = selectedIds.value.indexOf(id)
  if (index === -1) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

function addSelected() {
  const selectedSubs = subscriptions.value.filter(s => selectedIds.value.includes(s.id))
  const items = selectedSubs.map(s => ({
    name: s.name,
    amount: s.amount
  }))
  emit('add', items)
  close()
}

function openManageSubscriptions() {
  close()
  emit('manage-subscriptions')
}

function close() {
  dialog.value = false
  selectedIds.value = []
}

watch(dialog, (v) => {
  if (v) {
    subscriptionStore.fetchSubscriptions()
    selectedIds.value = []
  }
})
</script>
