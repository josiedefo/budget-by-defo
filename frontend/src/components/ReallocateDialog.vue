<template>
  <v-dialog :model-value="modelValue" max-width="450" @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="d-flex justify-space-between align-center">
        Move Funds
        <v-btn icon variant="text" @click="close"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>

      <v-card-text>
        <v-select
          v-model="form.sourceFundId"
          :items="fundOptions"
          item-title="name"
          item-value="id"
          label="From fund"
          class="mb-1"
        />
        <div v-if="sourceFund" class="text-caption text-medium-emphasis mb-3">
          Available: ${{ formatAmount(sourceFund.balance) }}
        </div>

        <v-select
          v-model="form.destinationFundId"
          :items="destinationOptions"
          item-title="name"
          item-value="id"
          label="To fund"
          class="mb-3"
        />

        <v-text-field v-model="form.amount" label="Amount" type="number" prefix="$" class="mb-3" />
        <v-text-field v-model="form.note" label="Note (optional)" class="mb-3" />

        <v-alert v-if="insufficientBalance" type="warning" variant="tonal" density="compact" class="mb-2">
          Amount exceeds source fund balance
        </v-alert>
        <v-alert v-if="error" type="error" variant="tonal" density="compact">{{ error }}</v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="elevated" :loading="loading"
          :disabled="!form.sourceFundId || !form.destinationFundId || !form.amount || insufficientBalance"
          @click="save">
          Move Funds
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

const props = defineProps({
  modelValue: Boolean,
  sourceFundId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue'])

const savingsStore = useSavingsStore()
const { funds, loading, error } = storeToRefs(savingsStore)

const form = ref({ sourceFundId: null, destinationFundId: null, amount: '', note: '' })

watch(() => props.sourceFundId, (id) => { if (id) form.value.sourceFundId = id }, { immediate: true })

const fundOptions = computed(() => funds.value.filter(f => f.isActive))
const destinationOptions = computed(() => fundOptions.value.filter(f => f.id !== form.value.sourceFundId))
const sourceFund = computed(() => funds.value.find(f => f.id === form.value.sourceFundId))
const insufficientBalance = computed(() => {
  if (!sourceFund.value || !form.value.amount) return false
  return parseFloat(form.value.amount) > parseFloat(sourceFund.value.balance)
})

function close() {
  emit('update:modelValue', false)
  form.value = { sourceFundId: null, destinationFundId: null, amount: '', note: '' }
}

async function save() {
  try {
    await savingsStore.reallocate({
      sourceFundId: form.value.sourceFundId,
      destinationFundId: form.value.destinationFundId,
      amount: parseFloat(form.value.amount),
      note: form.value.note || null
    })
    close()
  } catch {}
}

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>
