<template>
  <div>
    <v-select
      v-model="selectedId"
      :items="allFunds"
      item-title="name"
      item-value="id"
      label="Select fund"
      class="mb-4"
      style="max-width: 320px"
      @update:model-value="onFundSelected"
    />

    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <v-alert v-else-if="!selectedId" type="info" variant="tonal">
      Select a fund to view its event history.
    </v-alert>

    <v-alert v-else-if="events.length === 0" type="info" variant="tonal">
      No events recorded for this fund yet.
    </v-alert>

    <template v-else>
      <v-alert v-if="editError" type="error" variant="tonal" density="compact" class="mb-2" closable @click:close="editError = null">
        {{ editError }}
      </v-alert>

      <v-table density="compact">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th class="text-right">Amount</th>
            <th>Note</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="event in events" :key="event.id">
            <!-- Edit row -->
            <tr v-if="editingEventId === event.id">
              <td>
                <v-text-field v-model="editForm.eventDate" type="date" density="compact" hide-details style="min-width:130px" />
              </td>
              <td>
                <v-chip size="x-small" :color="eventTypeColor(event.eventType)" variant="tonal">
                  {{ eventTypeLabel(event.eventType) }}
                </v-chip>
              </td>
              <td class="text-right">
                <v-text-field v-model="editForm.amount" type="number" prefix="$" density="compact" hide-details style="min-width:100px" />
              </td>
              <td>
                <v-text-field v-model="editForm.note" density="compact" hide-details placeholder="Note" />
              </td>
              <td class="text-no-wrap">
                <v-btn icon size="small" variant="text" color="primary" :loading="loading" @click="saveEdit(event.id)">
                  <v-icon>mdi-check</v-icon>
                </v-btn>
                <v-btn icon size="small" variant="text" @click="cancelEdit">
                  <v-icon>mdi-close</v-icon>
                </v-btn>
              </td>
            </tr>
            <!-- Normal row -->
            <tr v-else>
              <td class="text-no-wrap">{{ formatDate(event.eventDate) }}</td>
              <td>
                <v-chip size="x-small" :color="eventTypeColor(event.eventType)" variant="tonal">
                  {{ eventTypeLabel(event.eventType) }}
                </v-chip>
              </td>
              <td class="text-right" :class="isCredit(event.eventType) ? 'text-success' : 'text-error'">
                {{ isCredit(event.eventType) ? '+' : '-' }}${{ formatAmount(event.amount) }}
              </td>
              <td class="text-medium-emphasis">{{ event.note || '—' }}</td>
              <td class="text-no-wrap">
                <template v-if="isEditable(event.eventType)">
                  <v-btn icon size="small" variant="text" @click="startEdit(event)">
                    <v-icon size="small">mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn icon size="small" variant="text" color="error" @click="confirmDelete(event)">
                    <v-icon size="small">mdi-delete</v-icon>
                  </v-btn>
                </template>
              </td>
            </tr>
          </template>
        </tbody>
      </v-table>
    </template>

    <!-- Delete confirmation dialog -->
    <v-dialog v-model="deleteDialogOpen" max-width="400">
      <v-card>
        <v-card-title>Delete Transaction</v-card-title>
        <v-card-text>
          Delete this transaction? This will reverse the balance effect on the fund.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="deleteDialogOpen = false">Cancel</v-btn>
          <v-btn color="error" variant="elevated" :loading="loading" @click="executeDelete">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

const savingsStore = useSavingsStore()
const { funds, events, loading } = storeToRefs(savingsStore)

const selectedId = ref(null)
const editingEventId = ref(null)
const editForm = ref({ amount: '', eventDate: '', note: '' })
const editError = ref(null)
const deleteDialogOpen = ref(false)
const deletingEvent = ref(null)

const allFunds = computed(() => funds.value.filter(f => f.isActive))

async function onFundSelected(id) {
  if (id) {
    editingEventId.value = null
    editError.value = null
    await savingsStore.fetchEventsForFund(id)
  }
}

function isEditable(type) {
  return type === 'DEPOSIT_ALLOCATED' || type === 'WITHDRAWAL'
}

function startEdit(event) {
  editingEventId.value = event.id
  editForm.value = { amount: event.amount, eventDate: event.eventDate, note: event.note || '' }
  editError.value = null
}

function cancelEdit() {
  editingEventId.value = null
  editError.value = null
}

async function saveEdit(id) {
  editError.value = null
  try {
    await savingsStore.updateEvent(id, {
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
    await savingsStore.deleteEvent(deletingEvent.value.id)
    deleteDialogOpen.value = false
    deletingEvent.value = null
  } catch {}
}

const EVENT_TYPE_LABELS = {
  DEPOSIT_ALLOCATED: 'Deposit',
  WITHDRAWAL: 'Withdrawal',
  REALLOCATION_IN: 'Reallocation In',
  REALLOCATION_OUT: 'Reallocation Out',
  PAYOUT: 'Payout'
}

const EVENT_TYPE_COLORS = {
  DEPOSIT_ALLOCATED: 'success',
  WITHDRAWAL: 'error',
  REALLOCATION_IN: 'blue',
  REALLOCATION_OUT: 'orange',
  PAYOUT: 'purple'
}

function eventTypeLabel(type) { return EVENT_TYPE_LABELS[type] || type }
function eventTypeColor(type) { return EVENT_TYPE_COLORS[type] || 'grey' }
function isCredit(type) { return ['DEPOSIT_ALLOCATED', 'REALLOCATION_IN'].includes(type) }

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>
