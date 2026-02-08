<template>
  <v-dialog v-model="dialog" max-width="600" persistent>
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="success">mdi-currency-usd</v-icon>
        Add from Salaries
      </v-card-title>
      <v-card-subtitle>Select salaries to add to your plan (as income)</v-card-subtitle>

      <v-card-text>
        <v-list v-if="salaries.length > 0" density="compact" select-strategy="classic">
          <v-list-item
            v-for="salary in salaries"
            :key="salary.id"
            @click="toggleSelection(salary.id)"
          >
            <template #prepend>
              <v-checkbox-btn
                :model-value="selectedIds.includes(salary.id)"
                @update:model-value="toggleSelection(salary.id)"
              ></v-checkbox-btn>
            </template>

            <v-list-item-title>{{ salary.name }}</v-list-item-title>
            <v-list-item-subtitle>
              Net Pay: {{ formatCurrency(salary.netPay) }}
              <span class="text-caption ml-2">(Gross: {{ formatCurrency(salary.regularAmount) }})</span>
            </v-list-item-subtitle>
          </v-list-item>
        </v-list>

        <div v-else class="text-center text-medium-emphasis py-4">
          No salaries available.
          <br>
          <v-btn variant="text" size="small" color="success" class="mt-2" @click="openManageSalaries">
            Manage Salaries
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
          color="success"
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
import { useSalaryStore } from '@/stores/salary'
import { storeToRefs } from 'pinia'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'add', 'manage-salaries'])

const salaryStore = useSalaryStore()
const { salaries } = storeToRefs(salaryStore)

const dialog = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const selectedIds = ref([])

const selectedTotal = computed(() => {
  return salaries.value
    .filter(s => selectedIds.value.includes(s.id))
    .reduce((sum, s) => sum + (s.netPay || 0), 0)
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
  const selectedSalaries = salaries.value.filter(s => selectedIds.value.includes(s.id))
  const items = selectedSalaries.map(s => ({
    name: s.name,
    amount: s.netPay
  }))
  emit('add', items)
  close()
}

function openManageSalaries() {
  close()
  emit('manage-salaries')
}

function close() {
  dialog.value = false
  selectedIds.value = []
}

watch(dialog, (v) => {
  if (v) {
    salaryStore.fetchSalaries()
    selectedIds.value = []
  }
})
</script>
