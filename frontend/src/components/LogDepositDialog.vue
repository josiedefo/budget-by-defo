<template>
  <v-dialog :model-value="modelValue" max-width="450" @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="d-flex justify-space-between align-center">
        Log Deposit
        <v-btn icon variant="text" @click="close"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>

      <v-card-text>
        <v-text-field v-model="form.amount" label="Amount" type="number" prefix="$" class="mb-3" />
        <v-text-field v-model="form.eventDate" label="Date" type="date" class="mb-3" />
        <v-select
          v-model="form.targetFundId"
          :items="fundOptions"
          item-title="name"
          item-value="id"
          label="Allocate to fund (optional)"
          clearable
          hint="Leave empty to add to Unassigned"
          persistent-hint
          class="mb-3"
        />
        <v-text-field v-model="form.note" label="Note (optional)" class="mb-3" />
        <v-alert v-if="error" type="error" variant="tonal" density="compact">{{ error }}</v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="elevated" :loading="loading" :disabled="!form.amount || !form.eventDate" @click="save">
          Log Deposit
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])

const savingsStore = useSavingsStore()
const { funds, loading, error } = storeToRefs(savingsStore)

const today = new Date().toISOString().split('T')[0]
const form = ref({ amount: '', eventDate: today, targetFundId: null, note: '' })

const fundOptions = computed(() => funds.value.filter(f => f.isActive))

function close() {
  emit('update:modelValue', false)
  form.value = { amount: '', eventDate: today, targetFundId: null, note: '' }
}

async function save() {
  try {
    await savingsStore.logDeposit({
      amount: parseFloat(form.value.amount),
      eventDate: form.value.eventDate,
      targetFundId: form.value.targetFundId || null,
      note: form.value.note || null
    })
    close()
  } catch {}
}
</script>
