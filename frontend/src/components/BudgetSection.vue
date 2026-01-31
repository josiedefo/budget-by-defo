<template>
  <v-card :class="section.isIncome ? 'border-success' : ''" variant="outlined">
    <v-card-title class="d-flex align-center">
      <v-icon :color="section.isIncome ? 'success' : 'error'" class="mr-2">
        {{ section.isIncome ? 'mdi-arrow-down' : 'mdi-arrow-up' }}
      </v-icon>
      {{ section.name }}
      <v-chip size="small" class="ml-2" :color="section.isIncome ? 'success' : 'error'">
        {{ formatCurrency(section.totalActual) }} / {{ formatCurrency(section.totalPlanned) }}
      </v-chip>
      <v-spacer></v-spacer>
      <v-btn icon size="small" variant="text" @click="emit('add-item')">
        <v-icon>mdi-plus</v-icon>
      </v-btn>
      <v-btn icon size="small" variant="text" color="error" @click="confirmDelete">
        <v-icon>mdi-delete</v-icon>
      </v-btn>
    </v-card-title>

    <v-divider></v-divider>

    <v-table density="compact">
      <thead>
        <tr>
          <th>Item</th>
          <th class="text-right">Planned</th>
          <th class="text-right">Actual</th>
          <th class="text-right">Diff</th>
          <th style="width: 50px"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in section.items" :key="item.id">
          <td>{{ item.name }}</td>
          <td class="text-right">
            <v-text-field
              :model-value="item.plannedAmount"
              type="number"
              density="compact"
              hide-details
              variant="plain"
              class="text-right amount-input"
              @update:model-value="val => updateItem(item.id, 'plannedAmount', val)"
            ></v-text-field>
          </td>
          <td class="text-right">
            <v-text-field
              :model-value="item.actualAmount"
              type="number"
              density="compact"
              hide-details
              variant="plain"
              class="text-right amount-input"
              @update:model-value="val => updateItem(item.id, 'actualAmount', val)"
            ></v-text-field>
          </td>
          <td class="text-right" :class="getDiffClass(item)">
            {{ formatCurrency(item.difference) }}
          </td>
          <td>
            <v-btn icon size="x-small" variant="text" color="error" @click="deleteItem(item.id)">
              <v-icon size="small">mdi-delete</v-icon>
            </v-btn>
          </td>
        </tr>
        <tr v-if="section.items.length === 0">
          <td colspan="5" class="text-center text-medium-emphasis py-4">
            No items yet. Click + to add one.
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="total-row">
          <td class="font-weight-bold">Total</td>
          <td class="text-right font-weight-bold">
            {{ formatCurrency(section.totalPlanned) }}
            <span v-if="plannedPercentOfIncome" class="text-medium-emphasis percent-badge">
              ({{ plannedPercentOfIncome }}%)
            </span>
          </td>
          <td class="text-right font-weight-bold">
            {{ formatCurrency(section.totalActual) }}
            <span v-if="actualPercentOfIncome" class="text-medium-emphasis percent-badge">
              ({{ actualPercentOfIncome }}%)
            </span>
          </td>
          <td class="text-right font-weight-bold" :class="getTotalDiffClass">
            {{ formatCurrency(totalDifference) }}
          </td>
          <td></td>
        </tr>
      </tfoot>
    </v-table>
  </v-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  section: { type: Object, required: true },
  totalPlannedIncome: { type: Number, default: 0 },
  totalActualIncome: { type: Number, default: 0 }
})

const emit = defineEmits(['add-item', 'update-item', 'delete-item', 'delete-section'])

const totalDifference = computed(() => {
  return (props.section.totalPlanned || 0) - (props.section.totalActual || 0)
})

const getTotalDiffClass = computed(() => {
  const diff = totalDifference.value
  if (props.section.isIncome) {
    return diff <= 0 ? 'text-success' : 'text-error'
  }
  return diff >= 0 ? 'text-success' : 'text-error'
})

const plannedPercentOfIncome = computed(() => {
  if (props.section.isIncome || !props.totalPlannedIncome) return null
  return ((props.section.totalPlanned || 0) / props.totalPlannedIncome * 100).toFixed(1)
})

const actualPercentOfIncome = computed(() => {
  if (props.section.isIncome || !props.totalActualIncome) return null
  return ((props.section.totalActual || 0) / props.totalActualIncome * 100).toFixed(1)
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

function getDiffClass(item) {
  const diff = parseFloat(item.difference) || 0
  if (props.section.isIncome) {
    return diff <= 0 ? 'text-success' : 'text-error'
  }
  return diff >= 0 ? 'text-success' : 'text-error'
}

let updateTimeout = null
function updateItem(itemId, field, value) {
  clearTimeout(updateTimeout)
  updateTimeout = setTimeout(() => {
    emit('update-item', {
      sectionId: props.section.id,
      itemId,
      data: { [field]: parseFloat(value) || 0 }
    })
  }, 500)
}

function deleteItem(itemId) {
  emit('delete-item', { sectionId: props.section.id, itemId })
}

function confirmDelete() {
  if (confirm(`Delete section "${props.section.name}" and all its items?`)) {
    emit('delete-section', props.section.id)
  }
}
</script>

<style scoped>
.amount-input :deep(input) {
  text-align: right;
}

.total-row {
  background-color: rgba(var(--v-theme-surface-variant), 0.4);
  border-top: 2px solid rgba(var(--v-border-color), 0.4);
}

.percent-badge {
  font-size: 0.85em;
  font-weight: normal;
}
</style>
