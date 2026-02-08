<template>
  <v-dialog v-model="dialog" max-width="900" persistent>
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="success">mdi-currency-usd</v-icon>
        Manage Salaries
      </v-card-title>
      <v-card-subtitle>Create salary templates with payroll deductions to add to your plans</v-card-subtitle>

      <v-card-text>
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = null">
          {{ error }}
        </v-alert>

        <!-- Add new salary form -->
        <v-expand-transition>
          <v-card v-if="showAddForm" variant="outlined" class="mb-4 pa-4 add-form">
            <div class="text-subtitle-1 font-weight-medium mb-3">Add New Salary</div>
            <v-row dense>
              <v-col cols="12">
                <v-text-field
                  v-model="newSalary.name"
                  label="Name *"
                  density="comfortable"
                  variant="outlined"
                  placeholder="e.g., Main Job, Side Gig"
                  autofocus
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row dense>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model.number="newSalary.regularAmount"
                  type="number"
                  label="Gross Pay *"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model.number="newSalary.federalTax"
                  type="number"
                  label="Federal Tax *"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row dense>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model.number="newSalary.medicare"
                  type="number"
                  label="Medicare *"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model.number="newSalary.socialSecurity"
                  type="number"
                  label="Social Security *"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-divider class="my-3"></v-divider>
            <div class="text-caption text-medium-emphasis mb-2">Optional Deductions</div>
            <v-row dense>
              <v-col cols="12" md="4">
                <v-text-field
                  v-model.number="newSalary.fourOhOneK"
                  type="number"
                  label="401K"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="4">
                <v-text-field
                  v-model.number="newSalary.extraTaxWithholding"
                  type="number"
                  label="Extra Tax Withholding"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="4">
                <v-text-field
                  v-model.number="newSalary.healthSavingsAccount"
                  type="number"
                  label="HSA"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row dense>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model.number="newSalary.medicalInsurance"
                  type="number"
                  label="Medical Insurance"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model.number="newSalary.flexibleSpendingAccount"
                  type="number"
                  label="FSA"
                  density="comfortable"
                  variant="outlined"
                  prefix="$"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-divider class="my-3"></v-divider>
            <div class="d-flex justify-space-between align-center">
              <div class="text-h6 text-success">
                Net Pay: {{ formatCurrency(computeNetPay(newSalary)) }}
              </div>
              <div>
                <v-btn variant="text" @click="cancelAdd">Cancel</v-btn>
                <v-btn color="success" variant="flat" @click="saveNewSalary" :loading="saving">Save</v-btn>
              </div>
            </div>
          </v-card>
        </v-expand-transition>

        <!-- Existing salaries list -->
        <div v-if="salaries.length > 0" class="salaries-list">
          <v-card
            v-for="salary in salaries"
            :key="salary.id"
            variant="outlined"
            class="mb-3"
          >
            <template v-if="editingId === salary.id">
              <!-- Edit form -->
              <v-card-text>
                <v-row dense>
                  <v-col cols="12">
                    <v-text-field
                      v-model="editForm.name"
                      label="Name *"
                      density="comfortable"
                      variant="outlined"
                    ></v-text-field>
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="editForm.regularAmount"
                      type="number"
                      label="Gross Pay *"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="editForm.federalTax"
                      type="number"
                      label="Federal Tax *"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="editForm.medicare"
                      type="number"
                      label="Medicare *"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="editForm.socialSecurity"
                      type="number"
                      label="Social Security *"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                </v-row>
                <v-divider class="my-3"></v-divider>
                <div class="text-caption text-medium-emphasis mb-2">Optional Deductions</div>
                <v-row dense>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="editForm.fourOhOneK"
                      type="number"
                      label="401K"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="editForm.extraTaxWithholding"
                      type="number"
                      label="Extra Tax Withholding"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" md="4">
                    <v-text-field
                      v-model.number="editForm.healthSavingsAccount"
                      type="number"
                      label="HSA"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="editForm.medicalInsurance"
                      type="number"
                      label="Medical Insurance"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                  <v-col cols="12" md="6">
                    <v-text-field
                      v-model.number="editForm.flexibleSpendingAccount"
                      type="number"
                      label="FSA"
                      density="comfortable"
                      variant="outlined"
                      prefix="$"
                    ></v-text-field>
                  </v-col>
                </v-row>
                <v-divider class="my-3"></v-divider>
                <div class="d-flex justify-space-between align-center">
                  <div class="text-h6 text-success">
                    Net Pay: {{ formatCurrency(computeNetPay(editForm)) }}
                  </div>
                  <div>
                    <v-btn variant="text" @click="cancelEdit">Cancel</v-btn>
                    <v-btn color="success" variant="flat" @click="saveEdit" :loading="saving">Save</v-btn>
                  </div>
                </div>
              </v-card-text>
            </template>
            <template v-else>
              <!-- Display mode -->
              <v-card-text class="d-flex justify-space-between align-center py-3">
                <div>
                  <div class="text-subtitle-1 font-weight-medium">{{ salary.name }}</div>
                  <div class="text-caption text-medium-emphasis">
                    Gross: {{ formatCurrency(salary.regularAmount) }}
                  </div>
                </div>
                <div class="d-flex align-center">
                  <div class="text-right mr-4">
                    <div class="text-h6 text-success">{{ formatCurrency(salary.netPay) }}</div>
                    <div class="text-caption text-medium-emphasis">Net Pay</div>
                  </div>
                  <v-btn icon size="small" variant="text" @click="startEdit(salary)">
                    <v-icon size="small">mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn icon size="small" variant="text" color="error" @click="confirmDelete(salary)">
                    <v-icon size="small">mdi-delete</v-icon>
                  </v-btn>
                </div>
              </v-card-text>
            </template>
          </v-card>
        </div>

        <div v-else-if="!showAddForm" class="text-center text-medium-emphasis py-4">
          No salaries yet. Click "Add Salary" to create one.
        </div>

        <v-btn v-if="!showAddForm" variant="tonal" color="success" class="mt-4" @click="startAdd">
          <v-icon start>mdi-plus</v-icon>
          Add Salary
        </v-btn>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Close</v-btn>
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

const emit = defineEmits(['update:modelValue'])

const salaryStore = useSalaryStore()
const { salaries } = storeToRefs(salaryStore)

const dialog = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const showAddForm = ref(false)
const saving = ref(false)
const error = ref(null)
const editingId = ref(null)

const emptySalary = {
  name: '',
  regularAmount: 0,
  federalTax: 0,
  medicare: 0,
  socialSecurity: 0,
  fourOhOneK: null,
  extraTaxWithholding: null,
  healthSavingsAccount: null,
  medicalInsurance: null,
  flexibleSpendingAccount: null
}

const newSalary = ref({ ...emptySalary })
const editForm = ref({ ...emptySalary })

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

function computeNetPay(salary) {
  let net = salary.regularAmount || 0
  net -= salary.federalTax || 0
  net -= salary.medicare || 0
  net -= salary.socialSecurity || 0
  net -= salary.fourOhOneK || 0
  net -= salary.extraTaxWithholding || 0
  net -= salary.healthSavingsAccount || 0
  net -= salary.medicalInsurance || 0
  net -= salary.flexibleSpendingAccount || 0
  return net
}

function startAdd() {
  showAddForm.value = true
  newSalary.value = { ...emptySalary }
}

function cancelAdd() {
  showAddForm.value = false
}

async function saveNewSalary() {
  if (!newSalary.value.name) {
    error.value = 'Name is required'
    return
  }
  if (!newSalary.value.regularAmount || newSalary.value.regularAmount <= 0) {
    error.value = 'Gross Pay must be positive'
    return
  }
  if (newSalary.value.federalTax == null || newSalary.value.federalTax < 0) {
    error.value = 'Federal Tax is required'
    return
  }
  if (newSalary.value.medicare == null || newSalary.value.medicare < 0) {
    error.value = 'Medicare is required'
    return
  }
  if (newSalary.value.socialSecurity == null || newSalary.value.socialSecurity < 0) {
    error.value = 'Social Security is required'
    return
  }

  saving.value = true
  error.value = null
  try {
    await salaryStore.createSalary(newSalary.value)
    showAddForm.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to create salary'
  } finally {
    saving.value = false
  }
}

function startEdit(salary) {
  editingId.value = salary.id
  editForm.value = {
    name: salary.name,
    regularAmount: salary.regularAmount,
    federalTax: salary.federalTax,
    medicare: salary.medicare,
    socialSecurity: salary.socialSecurity,
    fourOhOneK: salary.fourOhOneK,
    extraTaxWithholding: salary.extraTaxWithholding,
    healthSavingsAccount: salary.healthSavingsAccount,
    medicalInsurance: salary.medicalInsurance,
    flexibleSpendingAccount: salary.flexibleSpendingAccount
  }
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit() {
  if (!editForm.value.name) {
    error.value = 'Name is required'
    return
  }
  if (!editForm.value.regularAmount || editForm.value.regularAmount <= 0) {
    error.value = 'Gross Pay must be positive'
    return
  }

  saving.value = true
  error.value = null
  try {
    await salaryStore.updateSalary(editingId.value, editForm.value)
    editingId.value = null
  } catch (e) {
    error.value = 'Failed to update salary'
  } finally {
    saving.value = false
  }
}

function confirmDelete(salary) {
  if (confirm(`Delete salary "${salary.name}"?`)) {
    salaryStore.deleteSalary(salary.id)
  }
}

function close() {
  dialog.value = false
  showAddForm.value = false
  editingId.value = null
  error.value = null
}

watch(dialog, (v) => {
  if (v) {
    salaryStore.fetchSalaries()
  }
})
</script>

<style scoped>
.add-form {
  background-color: rgba(var(--v-theme-success), 0.05);
}
</style>
