<template>
  <v-container fluid class="pa-4">
    <!-- Filter Section -->
    <v-card class="mb-4">
      <v-card-text>
        <v-row>
          <v-col cols="12" sm="6" md="2">
            <v-text-field
              v-model="localFilters.startDate"
              label="Start Date"
              type="date"
              variant="outlined"
              density="compact"
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="2">
            <v-text-field
              v-model="localFilters.endDate"
              label="End Date"
              type="date"
              variant="outlined"
              density="compact"
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="2">
            <v-select
              v-model="localFilters.type"
              :items="typeOptions"
              label="Type"
              variant="outlined"
              density="compact"
              clearable
            ></v-select>
          </v-col>
          <v-col cols="12" sm="6" md="3">
            <v-text-field
              v-model="localFilters.merchant"
              label="Merchant"
              variant="outlined"
              density="compact"
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" sm="6" md="3" class="d-flex align-center ga-2">
            <v-btn color="primary" variant="tonal" @click="applyFilters">
              <v-icon start>mdi-magnify</v-icon>
              Search
            </v-btn>
            <v-btn variant="text" @click="handleClearFilters">
              Clear
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- Add Transaction Button -->
    <div class="d-flex justify-end mb-4">
      <v-btn color="primary" @click="openAddDialog">
        <v-icon start>mdi-plus</v-icon>
        Add Transaction
      </v-btn>
    </div>

    <!-- Loading State -->
    <v-row v-if="loading && transactions.length === 0" class="justify-center mt-8">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
    </v-row>

    <!-- Error State -->
    <v-alert
      v-else-if="error"
      type="warning"
      variant="tonal"
      class="mt-4"
    >
      {{ error }}
    </v-alert>

    <!-- Transactions List -->
    <template v-else>
      <v-card v-if="transactions.length === 0" class="text-center pa-8">
        <v-icon size="64" color="grey">mdi-swap-horizontal</v-icon>
        <p class="text-h6 mt-4">No transactions found</p>
        <p class="text-grey">Add your first transaction or adjust your filters</p>
      </v-card>

      <v-card v-else>
        <v-table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th>Merchant</th>
              <th>Category</th>
              <th class="text-right">Amount</th>
              <th>Note</th>
              <th class="text-center">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="transaction in transactions" :key="transaction.id">
              <td>{{ formatDate(transaction.transactionDate) }}</td>
              <td>
                <v-chip
                  :color="transaction.type === 'INCOME' ? 'success' : 'error'"
                  size="small"
                >
                  {{ transaction.type }}
                </v-chip>
              </td>
              <td>{{ transaction.merchant }}</td>
              <td>
                <span v-if="transaction.sectionName">{{ transaction.sectionName }}</span>
                <span v-if="transaction.budgetItemName"> / {{ transaction.budgetItemName }}</span>
                <span v-if="!transaction.sectionName && !transaction.budgetItemName" class="text-grey">-</span>
              </td>
              <td class="text-right">
                <span :class="transaction.type === 'INCOME' ? 'text-success' : 'text-error'">
                  {{ transaction.type === 'INCOME' ? '+' : '-' }}{{ formatCurrency(transaction.amount) }}
                </span>
              </td>
              <td class="text-truncate" style="max-width: 200px;">{{ transaction.note }}</td>
              <td class="text-center">
                <v-btn icon="mdi-pencil" variant="text" size="small" @click="openEditDialog(transaction)"></v-btn>
                <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="confirmDelete(transaction)"></v-btn>
              </td>
            </tr>
          </tbody>
        </v-table>

        <!-- Load More -->
        <div v-if="hasMore" class="text-center pa-4">
          <v-btn variant="text" :loading="loading" @click="loadMore">
            Load More
          </v-btn>
        </div>
      </v-card>
    </template>

    <!-- Add/Edit Dialog -->
    <TransactionDialog
      v-model="showDialog"
      :transaction="editingTransaction"
      @save="handleSave"
      @close="closeDialog"
    />

    <!-- Delete Confirmation -->
    <v-dialog v-model="showDeleteDialog" max-width="400">
      <v-card>
        <v-card-title>Delete Transaction</v-card-title>
        <v-card-text>
          Are you sure you want to delete this transaction?
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="showDeleteDialog = false">Cancel</v-btn>
          <v-btn color="error" variant="flat" @click="handleDelete">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { storeToRefs } from 'pinia'
import { useTransactionStore } from '@/stores/transaction'
import { useBudgetStore } from '@/stores/budget'
import TransactionDialog from '@/components/TransactionDialog.vue'

const transactionStore = useTransactionStore()
const budgetStore = useBudgetStore()
const { transactions, loading, error, hasMore } = storeToRefs(transactionStore)

const showDialog = ref(false)
const showDeleteDialog = ref(false)
const editingTransaction = ref(null)
const deletingTransaction = ref(null)

const typeOptions = [
  { title: 'Income', value: 'INCOME' },
  { title: 'Expense', value: 'EXPENSE' }
]

const localFilters = reactive({
  startDate: null,
  endDate: null,
  type: null,
  merchant: ''
})

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString()
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

function applyFilters() {
  transactionStore.setFilters(localFilters)
}

function handleClearFilters() {
  localFilters.startDate = null
  localFilters.endDate = null
  localFilters.type = null
  localFilters.merchant = ''
  transactionStore.clearFilters()
}

function openAddDialog() {
  editingTransaction.value = null
  showDialog.value = true
}

function openEditDialog(transaction) {
  editingTransaction.value = { ...transaction }
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
  editingTransaction.value = null
}

async function handleSave(data) {
  try {
    if (editingTransaction.value?.id) {
      await transactionStore.updateTransaction(editingTransaction.value.id, data)
    } else {
      await transactionStore.createTransaction(data)
    }
    closeDialog()
  } catch (e) {
    console.error('Error saving transaction:', e)
  }
}

function confirmDelete(transaction) {
  deletingTransaction.value = transaction
  showDeleteDialog.value = true
}

async function handleDelete() {
  if (deletingTransaction.value) {
    try {
      await transactionStore.deleteTransaction(deletingTransaction.value.id)
      showDeleteDialog.value = false
      deletingTransaction.value = null
    } catch (e) {
      console.error('Error deleting transaction:', e)
    }
  }
}

onMounted(async () => {
  // Load current month's budget to populate section/budget item dropdowns
  const now = new Date()
  await budgetStore.fetchBudget(now.getFullYear(), now.getMonth() + 1)

  transactionStore.fetchTransactions()
})
</script>
