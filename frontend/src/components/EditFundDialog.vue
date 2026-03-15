<template>
  <v-dialog :model-value="modelValue" max-width="500" @update:model-value="$emit('update:modelValue', $event)">
    <v-card v-if="fund">
      <v-card-title class="d-flex justify-space-between align-center">
        Edit Fund
        <v-btn icon variant="text" @click="close"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>

      <v-card-text>
        <v-text-field v-model="form.name" label="Fund name" :disabled="fund.isSystemFund" class="mb-3" />

        <v-select
          v-model="form.goalType"
          :items="goalTypeOptions"
          item-title="label"
          item-value="value"
          label="Goal type"
          class="mb-3"
        />

        <v-text-field
          v-if="needsTarget"
          v-model="form.targetAmount"
          label="Target amount"
          type="number"
          prefix="$"
          class="mb-3"
        />

        <v-text-field
          v-if="form.goalType === 'TARGET_WITH_DEADLINE'"
          v-model="form.deadline"
          label="Deadline"
          type="date"
          class="mb-3"
        />

        <v-text-field
          v-if="form.goalType === 'SPEND_AS_YOU_GO'"
          v-model="form.ceiling"
          label="Spending ceiling"
          type="number"
          prefix="$"
          class="mb-3"
        />

        <template v-if="form.goalType === 'SPEND_DOWN'">
          <v-text-field v-model="form.payoutDate" label="Payout date" type="date" class="mb-3" />
          <v-text-field v-model="form.payoutAmount" label="Payout amount" type="number" prefix="$" class="mb-3" />
        </template>

        <v-alert v-if="error" type="error" variant="tonal" density="compact">{{ error }}</v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="elevated" :loading="loading" @click="save">Save</v-btn>
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
  fund: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const savingsStore = useSavingsStore()
const { loading, error } = storeToRefs(savingsStore)

const goalTypeOptions = [
  { value: 'TARGET', label: 'Target' },
  { value: 'TARGET_WITH_DEADLINE', label: 'Target + Deadline' },
  { value: 'SPEND_DOWN', label: 'Spend-Down' },
  { value: 'SPEND_AS_YOU_GO', label: 'Spend-As-You-Go' },
  { value: 'NO_GOAL', label: 'No Goal' }
]

const form = ref({
  name: '',
  goalType: 'NO_GOAL',
  targetAmount: '',
  deadline: '',
  ceiling: '',
  payoutDate: '',
  payoutAmount: ''
})

watch(() => props.fund, (fund) => {
  if (fund) {
    form.value = {
      name: fund.name,
      goalType: fund.goalType,
      targetAmount: fund.targetAmount ?? '',
      deadline: fund.deadline ?? '',
      ceiling: fund.ceiling ?? '',
      payoutDate: fund.payoutDate ?? '',
      payoutAmount: fund.payoutAmount ?? ''
    }
  }
}, { immediate: true })

const needsTarget = computed(() =>
  ['TARGET', 'TARGET_WITH_DEADLINE', 'SPEND_DOWN'].includes(form.value.goalType)
)

function close() {
  emit('update:modelValue', false)
}

async function save() {
  try {
    await savingsStore.updateFund(props.fund.id, {
      name: form.value.name || null,
      goalType: form.value.goalType || null,
      targetAmount: form.value.targetAmount !== '' ? parseFloat(form.value.targetAmount) : null,
      deadline: form.value.deadline || null,
      ceiling: form.value.ceiling !== '' ? parseFloat(form.value.ceiling) : null,
      payoutDate: form.value.payoutDate || null,
      payoutAmount: form.value.payoutAmount !== '' ? parseFloat(form.value.payoutAmount) : null
    })
    close()
  } catch {}
}
</script>
