<template>
  <v-dialog v-model="dialog" max-width="440">
    <v-card v-if="item">
      <v-card-title class="d-flex align-center">
        <span class="text-truncate">Edit item</span>
        <v-spacer></v-spacer>
        <v-btn
          icon
          size="small"
          variant="text"
          :color="item.isKeyItem ? 'amber' : 'grey-lighten-1'"
          :title="item.isKeyItem ? 'Remove key tag' : 'Tag as key item for yearly view'"
          @click="emit('toggle-key')"
        >
          <v-icon>{{ item.isKeyItem ? 'mdi-bookmark' : 'mdi-bookmark-outline' }}</v-icon>
        </v-btn>
      </v-card-title>

      <v-card-text>
        <v-text-field
          v-model="name"
          label="Item name"
          autofocus
          class="mb-2"
          :rules="[v => !!v || 'Name is required']"
        ></v-text-field>

        <template v-if="item.planId">
          <v-alert
            type="info"
            variant="tonal"
            density="compact"
            class="mb-2"
          >
            Planned amount comes from a linked plan.
          </v-alert>
          <v-btn
            variant="tonal"
            color="primary"
            block
            class="mb-2"
            prepend-icon="mdi-link"
            @click="emit('view-plan')"
          >
            View plan ({{ formatCurrency(item.plannedAmount) }})
          </v-btn>
        </template>
        <v-text-field
          v-else
          v-model.number="plannedAmount"
          label="Planned amount"
          type="number"
          prefix="$"
          :rules="[v => v >= 0 || 'Must be positive']"
        ></v-text-field>

        <div class="d-flex align-center justify-space-between mt-1 mb-2">
          <span class="text-medium-emphasis">Actual spent</span>
          <a class="actual-link" @click="emit('view-transactions')">
            {{ formatCurrency(item.actualAmount) }}
            <v-icon size="x-small">mdi-open-in-new</v-icon>
          </a>
        </div>

        <v-switch
          :model-value="!item.isExcludedFromBudget"
          color="primary"
          density="compact"
          hide-details
          label="Include in budget totals"
          @update:model-value="val => emit('toggle-exclusion', !val)"
        ></v-switch>
      </v-card-text>

      <v-divider></v-divider>

      <v-card-actions>
        <v-btn
          v-if="item.actualAmount > 0"
          variant="text"
          :color="fullyLinked ? 'teal' : undefined"
          prepend-icon="mdi-bank-transfer"
          @click="emit('link-savings')"
        >
          {{ fullyLinked ? 'Linked' : 'Link savings' }}
        </v-btn>
        <v-btn variant="text" color="error" prepend-icon="mdi-delete" @click="emit('delete')">
          Delete
        </v-btn>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!name" @click="save">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  item: { type: Object, default: null },
  fullyLinked: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:modelValue',
  'save',
  'delete',
  'toggle-exclusion',
  'toggle-key',
  'view-transactions',
  'view-plan',
  'link-savings'
])

const dialog = ref(props.modelValue)
const name = ref('')
const plannedAmount = ref(0)

watch(() => props.modelValue, (val) => {
  dialog.value = val
  if (val && props.item) {
    name.value = props.item.name
    plannedAmount.value = props.item.plannedAmount
  }
})

watch(dialog, (val) => {
  emit('update:modelValue', val)
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

function close() {
  dialog.value = false
}

function save() {
  emit('save', { name: name.value, plannedAmount: parseFloat(plannedAmount.value) || 0 })
  dialog.value = false
}
</script>

<style scoped>
.actual-link {
  cursor: pointer;
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
  text-decoration-style: dotted;
}
.actual-link:hover {
  text-decoration-style: solid;
}
</style>
