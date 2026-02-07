<template>
  <v-dialog v-model="dialog" max-width="500" persistent>
    <v-card>
      <v-card-title>Create Plan</v-card-title>
      <v-card-subtitle>Select a budget item to create a plan for {{ monthName }} {{ year }}</v-card-subtitle>

      <v-card-text>
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = null">
          {{ error }}
        </v-alert>

        <v-select
          v-model="selectedBudgetItemId"
          :items="budgetItems"
          item-title="displayName"
          item-value="id"
          label="Budget Item"
          :rules="[v => !!v || 'Please select a budget item']"
          :loading="loadingBudget"
        >
          <template #item="{ item, props }">
            <v-list-item v-bind="props">
              <template #prepend>
                <v-chip size="x-small" :color="item.raw.isIncome ? 'success' : 'default'" class="mr-2">
                  {{ item.raw.sectionName }}
                </v-chip>
              </template>
            </v-list-item>
          </template>
        </v-select>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn
          color="primary"
          variant="flat"
          :disabled="!selectedBudgetItemId"
          :loading="creating"
          @click="create"
        >
          Create
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useBudgetStore } from '@/stores/budget'
import { usePlanStore } from '@/stores/plan'
import { storeToRefs } from 'pinia'

const props = defineProps({
  modelValue: Boolean,
  year: { type: Number, required: true },
  month: { type: Number, required: true }
})

const emit = defineEmits(['update:modelValue', 'created'])

const budgetStore = useBudgetStore()
const planStore = usePlanStore()
const { currentBudget } = storeToRefs(budgetStore)
const { plans } = storeToRefs(planStore)

const dialog = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const selectedBudgetItemId = ref(null)
const loadingBudget = ref(false)
const creating = ref(false)
const error = ref(null)

const monthName = computed(() => {
  return new Date(props.year, props.month - 1).toLocaleString('default', { month: 'long' })
})

const budgetItems = computed(() => {
  if (!currentBudget.value?.sections) return []

  // Get IDs of items that already have plans
  const existingPlanItemIds = new Set(plans.value.map(p => p.budgetItemId))

  const items = []
  for (const section of currentBudget.value.sections) {
    for (const item of section.items) {
      // Skip items that already have a plan
      if (existingPlanItemIds.has(item.id)) continue

      items.push({
        id: item.id,
        name: item.name,
        sectionName: section.name,
        isIncome: section.isIncome,
        displayName: `${item.name} (${section.name})`
      })
    }
  }
  return items
})

async function loadBudget() {
  if (!currentBudget.value || currentBudget.value.year !== props.year || currentBudget.value.month !== props.month) {
    loadingBudget.value = true
    await budgetStore.fetchBudget(props.year, props.month)
    loadingBudget.value = false
  }
}

async function create() {
  if (!selectedBudgetItemId.value) return

  creating.value = true
  error.value = null

  try {
    const plan = await planStore.createPlan(selectedBudgetItemId.value, props.year, props.month)
    emit('created', plan)
    close()
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to create plan'
  } finally {
    creating.value = false
  }
}

function close() {
  dialog.value = false
  selectedBudgetItemId.value = null
  error.value = null
}

watch(dialog, (v) => {
  if (v) {
    loadBudget()
  }
})
</script>
