<template>
  <v-dialog :model-value="modelValue" max-width="450" @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="d-flex justify-space-between align-center">
        Log Withdrawal
        <v-btn icon variant="text" @click="close"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>

      <v-card-text>
        <v-select
          v-model="form.fundId"
          :items="fundOptions"
          item-title="name"
          item-value="id"
          label="Fund"
          class="mb-3"
        />
        <div v-if="selectedFund" class="text-caption text-medium-emphasis mb-3">
          Available balance: ${{ formatAmount(selectedFund.balance) }}
        </div>
        <v-text-field v-model="form.amount" label="Amount" type="number" prefix="$" class="mb-3" />
        <v-text-field v-model="form.eventDate" label="Date" type="date" class="mb-3" />
        <v-text-field v-model="form.note" label="Note (optional)" class="mb-3" />
        <v-alert v-if="insufficientBalance" type="warning" variant="tonal" density="compact" class="mb-2">
          Amount exceeds fund balance
        </v-alert>
        <v-alert v-if="error" type="error" variant="tonal" density="compact">{{ error }}</v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="elevated" :loading="loading"
          :disabled="!form.fundId || !form.amount || !form.eventDate || insufficientBalance"
          @click="save">
          Log Withdrawal
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
  fundId: { type: Number, default: null }
})
const emit = defineEmits(['update:modelValue'])

const savingsStore = useSavingsStore()
const { funds, loading, error } = storeToRefs(savingsStore)

const today = new Date().toISOString().split('T')[0]
const form = ref({ fundId: null, amount: '', eventDate: today, note: '' })

watch(() => props.fundId, (id) => { if (id) form.value.fundId = id }, { immediate: true })

const fundOptions = computed(() => funds.value.filter(f => f.isActive))
const selectedFund = computed(() => funds.value.find(f => f.id === form.value.fundId))
const insufficientBalance = computed(() => {
  if (!selectedFund.value || !form.value.amount) return false
  return parseFloat(form.value.amount) > parseFloat(selectedFund.value.balance)
})

function close() {
  emit('update:modelValue', false)
  form.value = { fundId: null, amount: '', eventDate: today, note: '' }
}

async function save() {
  try {
    await savingsStore.logWithdrawal({
      fundId: form.value.fundId,
      amount: parseFloat(form.value.amount),
      eventDate: form.value.eventDate,
      note: form.value.note || null
    })
    close()
  } catch {}
}

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>
