<template>
  <v-dialog :model-value="modelValue" max-width="700" @update:model-value="close">
    <v-card v-if="account">
      <v-card-title class="d-flex justify-space-between align-center">
        {{ account.name }} — Transaction History
        <v-btn icon variant="text" @click="close"><v-icon>mdi-close</v-icon></v-btn>
      </v-card-title>

      <v-card-text>
        <!-- Log form -->
        <v-expand-transition>
          <div v-if="showForm" class="mb-4">
            <div class="text-subtitle-2 mb-2">{{ logForm.type === 'DEPOSIT' ? 'Log Deposit' : 'Log Withdrawal' }}</div>
            <v-row dense>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model="logForm.amount"
                  label="Amount"
                  type="number"
                  prefix="$"
                  density="compact"
                  hide-details
                />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model="logForm.eventDate"
                  label="Date"
                  type="date"
                  density="compact"
                  hide-details
                />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model="logForm.note"
                  label="Note (optional)"
                  density="compact"
                  hide-details
                />
              </v-col>
            </v-row>
            <v-alert v-if="formError" type="error" variant="tonal" density="compact" class="mt-2">{{ formError }}</v-alert>
            <div class="d-flex gap-2 mt-2">
              <v-btn size="small" color="primary" variant="elevated" :loading="loading" :disabled="!logForm.amount || !logForm.eventDate" @click="submitLog">
                Save
              </v-btn>
              <v-btn size="small" variant="text" @click="cancelForm">Cancel</v-btn>
            </div>
          </div>
        </v-expand-transition>

        <div v-if="loading && !showForm" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" />
        </div>

        <v-alert v-else-if="!loading && accountEvents.length === 0" type="info" variant="tonal">
          No transactions recorded for this account yet.
        </v-alert>

        <v-alert v-if="editError" type="error" variant="tonal" density="compact" class="mb-2" closable @click:close="editError = null">
          {{ editError }}
        </v-alert>

        <v-table v-if="accountEvents.length > 0" density="compact">
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th class="text-right" style="width:150px">Amount</th>
              <th class="text-right" style="width:130px">Balance After</th>
              <th style="width:220px">Note</th>
              <th style="width:56px"></th>
            </tr>
          </thead>
          <tbody>
            <template v-for="event in accountEvents" :key="event.id">
              <!-- Edit row -->
              <tr v-if="editingEventId === event.id">
                <td>
                  <v-text-field v-model="editForm.eventDate" type="date" density="compact" hide-details style="min-width:130px" />
                </td>
                <td>
                  <v-chip size="x-small" :color="event.eventType === 'DEPOSIT' ? 'success' : 'error'" variant="tonal">
                    {{ event.eventType === 'DEPOSIT' ? 'Deposit' : 'Withdrawal' }}
                  </v-chip>
                </td>
                <td class="text-right">
                  <v-text-field v-model="editForm.amount" type="number" prefix="$" density="compact" hide-details style="min-width:100px" />
                </td>
                <td class="text-right text-medium-emphasis">—</td>
                <td>
                  <v-text-field v-model="editForm.note" density="compact" hide-details placeholder="Note" />
                </td>
                <td>
                  <div class="action-icons">
                    <v-btn icon size="x-small" variant="text" color="primary" :loading="loading" @click="saveEdit(event.id)">
                      <v-icon size="16">mdi-check</v-icon>
                    </v-btn>
                    <v-btn icon size="x-small" variant="text" @click="cancelEdit">
                      <v-icon size="16">mdi-close</v-icon>
                    </v-btn>
                  </div>
                </td>
              </tr>
              <!-- Normal row -->
              <tr v-else>
                <td class="text-no-wrap">{{ formatDate(event.eventDate) }}</td>
                <td>
                  <v-chip size="x-small" :color="event.eventType === 'DEPOSIT' ? 'success' : 'error'" variant="tonal">
                    {{ event.eventType === 'DEPOSIT' ? 'Deposit' : 'Withdrawal' }}
                  </v-chip>
                </td>
                <td class="text-right text-no-wrap" :class="event.eventType === 'DEPOSIT' ? 'text-success' : 'text-error'">
                  {{ event.eventType === 'DEPOSIT' ? '+' : '-' }}${{ formatAmount(event.amount) }}
                </td>
                <td class="text-right">${{ formatAmount(event.balanceAfter) }}</td>
                <td class="text-medium-emphasis">{{ event.note || '—' }}</td>
                <td>
                  <div class="action-icons">
                    <v-btn icon size="x-small" variant="text" @click="startEdit(event)">
                      <v-icon size="16">mdi-pencil</v-icon>
                    </v-btn>
                    <v-btn icon size="x-small" variant="text" color="error" @click="confirmDelete(event)">
                      <v-icon size="16">mdi-delete</v-icon>
                    </v-btn>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </v-table>

        <!-- Delete confirmation -->
        <v-dialog v-model="deleteDialogOpen" max-width="400">
          <v-card>
            <v-card-title>Delete Transaction</v-card-title>
            <v-card-text>Delete this transaction? The account balance will be reversed.</v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn @click="deleteDialogOpen = false">Cancel</v-btn>
              <v-btn color="error" variant="elevated" :loading="loading" @click="executeDelete">Delete</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-card-text>

      <v-card-actions>
        <v-btn v-if="!showForm" prepend-icon="mdi-plus" color="success" variant="tonal" size="small" @click="openForm('DEPOSIT')">
          Deposit
        </v-btn>
        <v-btn v-if="!showForm" prepend-icon="mdi-minus" color="error" variant="tonal" size="small" @click="openForm('WITHDRAWAL')">
          Withdrawal
        </v-btn>
        <v-spacer />
        <v-btn @click="close">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

const props = defineProps({
  modelValue: Boolean,
  account: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const savingsStore = useSavingsStore()
const { accountEvents, loading } = storeToRefs(savingsStore)

const showForm = ref(false)
const formError = ref(null)
const logForm = ref({ type: 'DEPOSIT', amount: '', eventDate: today(), note: '' })

const editingEventId = ref(null)
const editForm = ref({ amount: '', eventDate: '', note: '' })
const editError = ref(null)
const deleteDialogOpen = ref(false)
const deletingEvent = ref(null)

watch(() => props.modelValue, (open) => {
  if (open && props.account?.id) {
    savingsStore.fetchEventsForAccount(props.account.id)
  }
  if (!open) {
    cancelForm()
    cancelEdit()
  }
})

function today() {
  return new Date().toISOString().slice(0, 10)
}

function openForm(type) {
  logForm.value = { type, amount: '', eventDate: today(), note: '' }
  formError.value = null
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  formError.value = null
}

async function submitLog() {
  formError.value = null
  const payload = {
    amount: parseFloat(logForm.value.amount),
    eventDate: logForm.value.eventDate,
    note: logForm.value.note || null
  }
  try {
    if (logForm.value.type === 'DEPOSIT') {
      await savingsStore.depositToAccount(props.account.id, payload)
    } else {
      await savingsStore.withdrawFromAccount(props.account.id, payload)
    }
    showForm.value = false
  } catch (e) {
    formError.value = e.response?.data?.message || 'Failed to save transaction'
  }
}

function startEdit(event) {
  editingEventId.value = event.id
  editForm.value = { amount: event.amount, eventDate: event.eventDate, note: event.note || '' }
  editError.value = null
  showForm.value = false
}

function cancelEdit() {
  editingEventId.value = null
  editError.value = null
}

async function saveEdit(id) {
  editError.value = null
  try {
    await savingsStore.updateAccountEvent(id, {
      amount: parseFloat(editForm.value.amount),
      eventDate: editForm.value.eventDate,
      note: editForm.value.note || null
    })
    editingEventId.value = null
  } catch (e) {
    editError.value = e.response?.data?.message || savingsStore.error || 'Failed to update event'
  }
}

function confirmDelete(event) {
  deletingEvent.value = event
  deleteDialogOpen.value = true
}

async function executeDelete() {
  try {
    await savingsStore.deleteAccountEvent(deletingEvent.value.id)
    deleteDialogOpen.value = false
    deletingEvent.value = null
  } catch {}
}

function close() {
  emit('update:modelValue', false)
}

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>

<style scoped>
.action-icons {
  display: flex;
  align-items: center;
  gap: 2px;
}
</style>
