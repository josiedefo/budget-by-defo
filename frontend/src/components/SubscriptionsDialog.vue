<template>
  <v-dialog v-model="dialog" max-width="1100" persistent>
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="primary">mdi-repeat</v-icon>
        Manage Subscriptions
      </v-card-title>
      <v-card-subtitle>Create subscription templates to quickly add to your plans</v-card-subtitle>

      <v-card-text>
        <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = null">
          {{ error }}
        </v-alert>

        <v-table density="comfortable" class="subscriptions-table">
          <thead>
            <tr>
              <th style="min-width: 180px">Name</th>
              <th class="text-right" style="width: 120px">Amount</th>
              <th style="width: 90px">Day</th>
              <th style="min-width: 150px">Category</th>
              <th style="width: 160px">Recurrence</th>
              <th style="width: 100px"></th>
            </tr>
          </thead>
          <tbody>
            <!-- Add new subscription row -->
            <tr v-if="showAddRow" class="add-row">
              <td>
                <v-text-field
                  v-model="newSubscription.name"
                  density="comfortable"
                  hide-details
                  variant="outlined"
                  placeholder="Name"
                  autofocus
                ></v-text-field>
              </td>
              <td>
                <v-text-field
                  v-model.number="newSubscription.amount"
                  type="number"
                  density="comfortable"
                  hide-details
                  variant="outlined"
                  prefix="$"
                  class="text-right"
                ></v-text-field>
              </td>
              <td>
                <v-text-field
                  v-model.number="newSubscription.billingDay"
                  type="number"
                  density="comfortable"
                  hide-details
                  variant="outlined"
                  min="1"
                  max="31"
                  placeholder="1-31"
                ></v-text-field>
              </td>
              <td>
                <v-text-field
                  v-model="newSubscription.category"
                  density="comfortable"
                  hide-details
                  variant="outlined"
                  placeholder="Category"
                ></v-text-field>
              </td>
              <td>
                <v-select
                  v-model="newSubscription.recurrence"
                  :items="recurrenceOptions"
                  density="comfortable"
                  hide-details
                  variant="outlined"
                ></v-select>
              </td>
              <td>
                <v-btn icon size="small" color="success" variant="text" @click="saveNewSubscription" :loading="saving">
                  <v-icon>mdi-check</v-icon>
                </v-btn>
                <v-btn icon size="small" variant="text" @click="cancelAdd">
                  <v-icon>mdi-close</v-icon>
                </v-btn>
              </td>
            </tr>

            <!-- Existing subscriptions -->
            <tr v-for="sub in subscriptions" :key="sub.id">
              <template v-if="editingId === sub.id">
                <td>
                  <v-text-field
                    v-model="editForm.name"
                    density="comfortable"
                    hide-details
                    variant="outlined"
                  ></v-text-field>
                </td>
                <td>
                  <v-text-field
                    v-model.number="editForm.amount"
                    type="number"
                    density="comfortable"
                    hide-details
                    variant="outlined"
                    prefix="$"
                    class="text-right"
                  ></v-text-field>
                </td>
                <td>
                  <v-text-field
                    v-model.number="editForm.billingDay"
                    type="number"
                    density="comfortable"
                    hide-details
                    variant="outlined"
                    min="1"
                    max="31"
                  ></v-text-field>
                </td>
                <td>
                  <v-text-field
                    v-model="editForm.category"
                    density="comfortable"
                    hide-details
                    variant="outlined"
                  ></v-text-field>
                </td>
                <td>
                  <v-select
                    v-model="editForm.recurrence"
                    :items="recurrenceOptions"
                    density="comfortable"
                    hide-details
                    variant="outlined"
                  ></v-select>
                </td>
                <td>
                  <v-btn icon size="small" color="success" variant="text" @click="saveEdit" :loading="saving">
                    <v-icon>mdi-check</v-icon>
                  </v-btn>
                  <v-btn icon size="small" variant="text" @click="cancelEdit">
                    <v-icon>mdi-close</v-icon>
                  </v-btn>
                </td>
              </template>
              <template v-else>
                <td>{{ sub.name }}</td>
                <td class="text-right">{{ formatCurrency(sub.amount) }}</td>
                <td>{{ sub.billingDay || '-' }}</td>
                <td>{{ sub.category || '-' }}</td>
                <td>{{ sub.recurrence }}</td>
                <td>
                  <v-btn icon size="small" variant="text" @click="startEdit(sub)">
                    <v-icon size="small">mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn icon size="small" variant="text" color="error" @click="confirmDelete(sub)">
                    <v-icon size="small">mdi-delete</v-icon>
                  </v-btn>
                </td>
              </template>
            </tr>

            <tr v-if="subscriptions.length === 0 && !showAddRow">
              <td colspan="6" class="text-center text-medium-emphasis py-4">
                No subscriptions yet. Click "Add Subscription" to create one.
              </td>
            </tr>
          </tbody>
        </v-table>

        <v-btn v-if="!showAddRow" variant="tonal" class="mt-4" @click="startAdd">
          <v-icon start>mdi-plus</v-icon>
          Add Subscription
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
import { useSubscriptionStore } from '@/stores/subscription'
import { storeToRefs } from 'pinia'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue'])

const subscriptionStore = useSubscriptionStore()
const { subscriptions } = storeToRefs(subscriptionStore)

const dialog = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const recurrenceOptions = ['WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUALLY', 'YEARLY']

const showAddRow = ref(false)
const saving = ref(false)
const error = ref(null)
const editingId = ref(null)

const newSubscription = ref({
  name: '',
  amount: 0,
  billingDay: 1,
  category: '',
  recurrence: 'MONTHLY'
})

const editForm = ref({
  name: '',
  amount: 0,
  billingDay: 1,
  category: '',
  recurrence: 'MONTHLY'
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

function startAdd() {
  showAddRow.value = true
  newSubscription.value = {
    name: '',
    amount: 0,
    billingDay: 1,
    category: '',
    recurrence: 'MONTHLY'
  }
}

function cancelAdd() {
  showAddRow.value = false
}

async function saveNewSubscription() {
  if (!newSubscription.value.name) {
    error.value = 'Name is required'
    return
  }
  if (!newSubscription.value.amount || newSubscription.value.amount <= 0) {
    error.value = 'Amount must be positive'
    return
  }

  saving.value = true
  error.value = null
  try {
    await subscriptionStore.createSubscription(newSubscription.value)
    showAddRow.value = false
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to create subscription'
  } finally {
    saving.value = false
  }
}

function startEdit(sub) {
  editingId.value = sub.id
  editForm.value = {
    name: sub.name,
    amount: sub.amount,
    billingDay: sub.billingDay,
    category: sub.category || '',
    recurrence: sub.recurrence
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
  if (!editForm.value.amount || editForm.value.amount <= 0) {
    error.value = 'Amount must be positive'
    return
  }

  saving.value = true
  error.value = null
  try {
    await subscriptionStore.updateSubscription(editingId.value, editForm.value)
    editingId.value = null
  } catch (e) {
    error.value = 'Failed to update subscription'
  } finally {
    saving.value = false
  }
}

function confirmDelete(sub) {
  if (confirm(`Delete subscription "${sub.name}"?`)) {
    subscriptionStore.deleteSubscription(sub.id)
  }
}

function close() {
  dialog.value = false
  showAddRow.value = false
  editingId.value = null
  error.value = null
}

watch(dialog, (v) => {
  if (v) {
    subscriptionStore.fetchSubscriptions()
  }
})
</script>

<style scoped>
.add-row {
  background-color: rgba(var(--v-theme-primary), 0.05);
}

.text-right :deep(input) {
  text-align: right;
}

.subscriptions-table {
  table-layout: fixed;
}

.subscriptions-table :deep(td) {
  padding: 8px 12px;
}

.subscriptions-table :deep(td:nth-child(1)) {
  width: 180px;
}

.subscriptions-table :deep(td:nth-child(2)) {
  width: 120px;
}

.subscriptions-table :deep(td:nth-child(3)) {
  width: 90px;
}

.subscriptions-table :deep(td:nth-child(4)) {
  width: 150px;
}

.subscriptions-table :deep(td:nth-child(5)) {
  width: 160px;
}

.subscriptions-table :deep(td:nth-child(6)) {
  width: 100px;
}

.subscriptions-table :deep(.v-field__input) {
  min-height: 40px;
  padding: 8px 12px;
}

.subscriptions-table :deep(.v-field) {
  font-size: 14px;
}
</style>
