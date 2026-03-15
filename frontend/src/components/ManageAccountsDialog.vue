<template>
  <v-dialog :model-value="modelValue" max-width="600" @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="d-flex justify-space-between align-center">
        Manage Savings Accounts
        <v-btn icon variant="text" @click="$emit('update:modelValue', false)">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>

      <v-card-text>
        <!-- Existing accounts -->
        <v-list v-if="accounts.length > 0" lines="two" class="mb-4">
          <v-list-item v-for="account in accounts" :key="account.id">
            <template #title>
              <template v-if="editingId === account.id">
                <v-row dense>
                  <v-col cols="5">
                    <v-text-field v-model="editForm.name" label="Name" density="compact" hide-details />
                  </v-col>
                  <v-col cols="4">
                    <v-text-field v-model="editForm.balance" label="Balance" type="number" density="compact" hide-details prefix="$" />
                  </v-col>
                  <v-col cols="3">
                    <v-text-field v-model="editForm.asOfDate" label="As of" type="date" density="compact" hide-details />
                  </v-col>
                </v-row>
              </template>
              <template v-else>
                {{ account.name }}
              </template>
            </template>
            <template #subtitle>
              <template v-if="editingId !== account.id">
                ${{ formatAmount(account.balance) }}
                <span v-if="account.asOfDate" class="ml-2 text-caption">as of {{ formatDate(account.asOfDate) }}</span>
              </template>
            </template>
            <template #append>
              <template v-if="editingId === account.id">
                <v-btn icon size="small" variant="text" color="primary" :loading="loading" @click="saveEdit(account.id)">
                  <v-icon>mdi-check</v-icon>
                </v-btn>
                <v-btn icon size="small" variant="text" @click="cancelEdit">
                  <v-icon>mdi-close</v-icon>
                </v-btn>
              </template>
              <template v-else>
                <v-btn icon size="small" variant="text" @click="openHistory(account)">
                  <v-icon>mdi-history</v-icon>
                </v-btn>
                <v-btn icon size="small" variant="text" @click="startEdit(account)">
                  <v-icon>mdi-pencil</v-icon>
                </v-btn>
                <v-btn icon size="small" variant="text" color="error" :loading="loading" @click="removeAccount(account.id)">
                  <v-icon>mdi-delete</v-icon>
                </v-btn>
              </template>
            </template>
          </v-list-item>
        </v-list>

        <v-divider v-if="accounts.length > 0" class="mb-4" />

        <!-- Add new account -->
        <div class="text-subtitle-2 mb-2">Add Account</div>
        <v-row dense>
          <v-col cols="12" sm="5">
            <v-text-field v-model="newForm.name" label="Account name" density="compact" hide-details />
          </v-col>
          <v-col cols="12" sm="4">
            <v-text-field v-model="newForm.balance" label="Balance" type="number" density="compact" hide-details prefix="$" />
          </v-col>
          <v-col cols="12" sm="3">
            <v-text-field v-model="newForm.asOfDate" label="As of date" type="date" density="compact" hide-details />
          </v-col>
        </v-row>
        <v-alert v-if="error" type="error" variant="tonal" class="mt-2" density="compact">{{ error }}</v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn @click="$emit('update:modelValue', false)">Close</v-btn>
        <v-btn color="primary" variant="elevated" :loading="loading" :disabled="!newForm.name || newForm.balance === ''" @click="addAccount">
          Add Account
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <SavingsAccountEventHistory v-model="historyDialogOpen" :account="historyAccount" />
</template>

<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'
import SavingsAccountEventHistory from './SavingsAccountEventHistory.vue'

defineProps({ modelValue: Boolean })
defineEmits(['update:modelValue'])

const savingsStore = useSavingsStore()
const { accounts, loading, error } = storeToRefs(savingsStore)

const editingId = ref(null)
const editForm = ref({ name: '', balance: 0, asOfDate: '' })
const newForm = ref({ name: '', balance: '', asOfDate: '' })
const historyDialogOpen = ref(false)
const historyAccount = ref(null)

function openHistory(account) {
  historyAccount.value = account
  historyDialogOpen.value = true
}

function startEdit(account) {
  editingId.value = account.id
  editForm.value = { name: account.name, balance: account.balance, asOfDate: account.asOfDate || '' }
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(id) {
  try {
    await savingsStore.updateAccount(id, {
      name: editForm.value.name,
      balance: parseFloat(editForm.value.balance),
      asOfDate: editForm.value.asOfDate || null
    })
    editingId.value = null
  } catch {}
}

async function removeAccount(id) {
  try {
    await savingsStore.deleteAccount(id)
  } catch {}
}

async function addAccount() {
  try {
    await savingsStore.createAccount({
      name: newForm.value.name,
      balance: parseFloat(newForm.value.balance),
      asOfDate: newForm.value.asOfDate || null
    })
    newForm.value = { name: '', balance: '', asOfDate: '' }
  } catch {}
}

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>
