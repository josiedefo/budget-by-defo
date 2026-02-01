<template>
  <v-dialog v-model="dialog" max-width="500" persistent>
    <v-card>
      <v-card-title>{{ isEditing ? 'Edit Transaction' : 'Add Transaction' }}</v-card-title>
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-select
            v-model="formData.type"
            :items="typeOptions"
            label="Type *"
            variant="outlined"
            :rules="[v => !!v || 'Type is required']"
            class="mb-2"
          ></v-select>

          <v-text-field
            v-model="formData.transactionDate"
            label="Date *"
            type="date"
            variant="outlined"
            :rules="[v => !!v || 'Date is required']"
            class="mb-2"
          ></v-text-field>

          <v-text-field
            v-model="formData.merchant"
            label="Merchant *"
            variant="outlined"
            :rules="[v => !!v || 'Merchant is required']"
            class="mb-2"
          ></v-text-field>

          <v-text-field
            v-model.number="formData.amount"
            label="Amount *"
            type="number"
            variant="outlined"
            prefix="$"
            :rules="[v => v > 0 || 'Amount must be positive']"
            class="mb-2"
          ></v-text-field>

          <v-textarea
            v-model="formData.note"
            label="Note"
            variant="outlined"
            rows="2"
            class="mb-2"
          ></v-textarea>

          <!-- Optional: Section Selection -->
          <v-autocomplete
            v-model="formData.sectionId"
            :items="availableSections"
            item-title="name"
            item-value="id"
            label="Category (optional)"
            variant="outlined"
            clearable
            class="mb-2"
          ></v-autocomplete>

          <!-- Optional: Budget Item Selection -->
          <v-autocomplete
            v-model="formData.budgetItemId"
            :items="availableBudgetItems"
            item-title="name"
            item-value="id"
            label="Budget Item (optional)"
            variant="outlined"
            clearable
            :disabled="!formData.sectionId"
          ></v-autocomplete>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!valid" @click="save">
          {{ isEditing ? 'Update' : 'Add' }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch, computed, reactive } from 'vue'
import { storeToRefs } from 'pinia'
import { useBudgetStore } from '@/stores/budget'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  transaction: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'save', 'close'])

const budgetStore = useBudgetStore()
const { sections } = storeToRefs(budgetStore)

const dialog = ref(props.modelValue)
const valid = ref(false)
const form = ref(null)

const typeOptions = [
  { title: 'Income', value: 'INCOME' },
  { title: 'Expense', value: 'EXPENSE' }
]

const formData = reactive({
  type: 'EXPENSE',
  transactionDate: new Date().toISOString().split('T')[0],
  merchant: '',
  amount: 0,
  note: '',
  sectionId: null,
  budgetItemId: null
})

const isEditing = computed(() => !!props.transaction?.id)

const availableSections = computed(() => {
  return sections.value || []
})

const availableBudgetItems = computed(() => {
  if (!formData.sectionId) return []
  const section = sections.value?.find(s => s.id === formData.sectionId)
  return section?.items || []
})

watch(() => props.modelValue, (val) => {
  dialog.value = val
  if (val) {
    resetForm()
  }
})

watch(dialog, (val) => {
  emit('update:modelValue', val)
})

watch(() => formData.sectionId, () => {
  formData.budgetItemId = null
})

function resetForm() {
  if (props.transaction) {
    formData.type = props.transaction.type
    formData.transactionDate = props.transaction.transactionDate
    formData.merchant = props.transaction.merchant
    formData.amount = props.transaction.amount
    formData.note = props.transaction.note
    formData.sectionId = props.transaction.sectionId
    formData.budgetItemId = props.transaction.budgetItemId
  } else {
    formData.type = 'EXPENSE'
    formData.transactionDate = new Date().toISOString().split('T')[0]
    formData.merchant = ''
    formData.amount = 0
    formData.note = ''
    formData.sectionId = null
    formData.budgetItemId = null
  }
}

function close() {
  dialog.value = false
  emit('close')
}

function save() {
  emit('save', { ...formData })
}
</script>
