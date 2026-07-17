<template>
  <v-dialog :model-value="modelValue" max-width="500" persistent @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="text-title-large pt-4 px-4">
        <v-icon start color="teal">mdi-bank-transfer</v-icon>
        Bulk Link to Savings
      </v-card-title>

      <v-card-text class="px-4">
        <!-- Budget item summary -->
        <v-sheet rounded="lg" color="surface-variant" class="pa-3 mb-4">
          <div class="d-flex justify-space-between align-center">
            <span class="text-body-medium text-medium-emphasis">Budget Item</span>
            <span class="font-weight-medium">{{ budgetItem?.name }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mt-1">
            <span class="text-body-medium text-medium-emphasis">Period</span>
            <span>{{ monthLabel }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mt-1">
            <span class="text-body-medium text-medium-emphasis">Transactions</span>
            <span>{{ linkStatus ? linkStatus.totalTransactions : '—' }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mt-1">
            <span class="text-body-medium text-medium-emphasis">Total Actual</span>
            <span class="font-weight-medium">{{ formatCurrency(budgetItem?.actualAmount) }}</span>
          </div>
        </v-sheet>

        <div v-if="statusLoading" class="text-center py-4">
          <v-progress-circular indeterminate color="teal" size="24" />
        </div>

        <template v-else>
          <!-- ── Savings Account Section ── -->
          <div class="text-title-small text-medium-emphasis mb-2">
            <v-icon size="small" class="mr-1">mdi-bank-outline</v-icon>
            Savings Account
          </div>

          <!-- Account: all linked state -->
          <template v-if="linkStatus?.allLinkedToAccount">
            <v-alert type="success" variant="tonal" density="compact" icon="mdi-check-circle" class="mb-2">
              All transactions linked to <strong>{{ linkStatus.accountName }}</strong>
            </v-alert>
          </template>

          <!-- Account: link form -->
          <template v-else>
            <v-select
              v-model="selectedAccountId"
              :items="savingsStore.accounts"
              item-title="name"
              item-value="id"
              label="Savings Account"
              variant="outlined"
              density="comfortable"
              clearable
              no-data-text="No savings accounts found"
              class="mb-2"
            />
            <div class="d-flex align-center gap-2 mb-2">
              <v-btn-toggle v-model="accountEventType" mandatory color="teal" density="comfortable" rounded="lg">
                <v-btn value="DEPOSIT" prepend-icon="mdi-arrow-down-circle-outline" size="small">Deposit</v-btn>
                <v-btn value="WITHDRAWAL" prepend-icon="mdi-arrow-up-circle-outline" size="small">Withdrawal</v-btn>
              </v-btn-toggle>
              <v-spacer />
              <v-btn
                color="teal"
                variant="tonal"
                size="small"
                :disabled="!selectedAccountId || linkingAccount"
                :loading="linkingAccount"
                @click="linkToAccount"
              >
                <v-icon start size="small">mdi-link</v-icon>
                Link All
              </v-btn>
            </div>
            <v-alert v-if="accountOperationResult" type="info" variant="tonal" density="compact" class="mb-2">
              {{ accountOperationResult }}
            </v-alert>
          </template>

          <v-alert v-if="accountError" type="error" variant="tonal" density="compact" class="mb-2">
            {{ accountError }}
          </v-alert>

          <v-divider class="my-4" />

          <!-- ── Savings Fund Section ── -->
          <div class="text-title-small text-medium-emphasis mb-2">
            <v-icon size="small" class="mr-1">mdi-piggy-bank-outline</v-icon>
            Savings Fund
          </div>

          <!-- Fund: all linked state -->
          <template v-if="linkStatus?.allLinkedToFund">
            <v-alert type="success" variant="tonal" density="compact" icon="mdi-check-circle" class="mb-2">
              All transactions linked to <strong>{{ linkStatus.fundName }}</strong>
            </v-alert>
          </template>

          <!-- Fund: link form -->
          <template v-else>
            <v-select
              v-model="selectedFundId"
              :items="savingsStore.userFunds"
              item-title="name"
              item-value="id"
              label="Savings Fund"
              variant="outlined"
              density="comfortable"
              clearable
              no-data-text="No savings funds found"
              class="mb-2"
            />
            <div class="d-flex align-center gap-2 mb-2">
              <v-btn-toggle v-model="fundEventType" mandatory color="teal" density="comfortable" rounded="lg">
                <v-btn value="DEPOSIT_ALLOCATED" prepend-icon="mdi-arrow-down-circle-outline" size="small">Deposit</v-btn>
                <v-btn value="WITHDRAWAL" prepend-icon="mdi-arrow-up-circle-outline" size="small">Withdrawal</v-btn>
              </v-btn-toggle>
              <v-spacer />
              <v-btn
                color="teal"
                variant="tonal"
                size="small"
                :disabled="!selectedFundId || linkingFund"
                :loading="linkingFund"
                @click="linkToFund"
              >
                <v-icon start size="small">mdi-link</v-icon>
                Link All
              </v-btn>
            </div>
            <v-alert v-if="fundOperationResult" type="info" variant="tonal" density="compact" class="mb-2">
              {{ fundOperationResult }}
            </v-alert>
          </template>

          <v-alert v-if="fundError" type="error" variant="tonal" density="compact" class="mb-2">
            {{ fundError }}
          </v-alert>
        </template>
      </v-card-text>

      <v-card-actions class="px-4 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="close">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useSavingsStore } from '@/stores/savings'
import { savingsApi } from '@/services/api'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  budgetItem: { type: Object, default: null },
  year: { type: Number, required: true },
  month: { type: Number, required: true }
})

const emit = defineEmits(['update:modelValue', 'status-changed'])

const savingsStore = useSavingsStore()

// Status fetched from API
const linkStatus = ref(null)
const statusLoading = ref(false)

// Account section state
const selectedAccountId = ref(null)
const accountEventType = ref('DEPOSIT')
const linkingAccount = ref(false)
const accountError = ref(null)
const accountOperationResult = ref(null)

// Fund section state
const selectedFundId = ref(null)
const fundEventType = ref('DEPOSIT_ALLOCATED')
const linkingFund = ref(false)
const fundError = ref(null)
const fundOperationResult = ref(null)

const monthLabel = computed(() => {
  if (!props.year || !props.month) return ''
  return new Date(props.year, props.month - 1, 1)
    .toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
})

const dateRange = computed(() => {
  const y = props.year
  const m = props.month
  const startDate = `${y}-${String(m).padStart(2, '0')}-01`
  const lastDay = new Date(y, m, 0).getDate()
  const endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  return { startDate, endDate }
})

async function fetchStatus() {
  if (!props.budgetItem?.id) return
  statusLoading.value = true
  try {
    const response = await savingsApi.getBudgetItemLinkStatuses(
      [props.budgetItem.id],
      dateRange.value.startDate,
      dateRange.value.endDate
    )
    linkStatus.value = response.data[props.budgetItem.id] ?? null
    emit('status-changed', props.budgetItem.id, linkStatus.value)
  } catch {
    linkStatus.value = null
  } finally {
    statusLoading.value = false
  }
}

watch(() => props.modelValue, (open) => {
  if (open) {
    accountError.value = null
    fundError.value = null
    accountOperationResult.value = null
    fundOperationResult.value = null
    selectedAccountId.value = null
    selectedFundId.value = null
    if (savingsStore.accounts.length === 0) savingsStore.fetchAccounts()
    if (savingsStore.funds.length === 0) savingsStore.fetchFunds()
    fetchStatus()
  }
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

async function linkToAccount() {
  if (!selectedAccountId.value) return
  linkingAccount.value = true
  accountError.value = null
  accountOperationResult.value = null
  try {
    const result = await savingsStore.bulkLinkBudgetItemToAccount(selectedAccountId.value, {
      budgetItemId: props.budgetItem.id,
      eventType: accountEventType.value,
      startDate: dateRange.value.startDate,
      endDate: dateRange.value.endDate
    })
    if (result.skipped > 0 && result.linked === 0) {
      accountOperationResult.value = `All ${result.total} transaction(s) were already linked — nothing to do.`
    } else if (result.skipped > 0) {
      accountOperationResult.value = `${result.linked} linked, ${result.skipped} already linked (skipped).`
    }
    // Re-fetch to show linked state if now all linked
    await fetchStatus()
  } catch (e) {
    accountError.value = e.response?.data?.message || 'Failed to link. Please try again.'
  } finally {
    linkingAccount.value = false
  }
}

async function linkToFund() {
  if (!selectedFundId.value) return
  linkingFund.value = true
  fundError.value = null
  fundOperationResult.value = null
  try {
    const result = await savingsStore.bulkLinkBudgetItemToFund({
      budgetItemId: props.budgetItem.id,
      fundId: selectedFundId.value,
      eventType: fundEventType.value,
      startDate: dateRange.value.startDate,
      endDate: dateRange.value.endDate
    })
    if (result.skipped > 0 && result.linked === 0) {
      fundOperationResult.value = `All ${result.total} transaction(s) were already linked — nothing to do.`
    } else if (result.skipped > 0) {
      fundOperationResult.value = `${result.linked} linked, ${result.skipped} already linked (skipped).`
    }
    await fetchStatus()
  } catch (e) {
    fundError.value = e.response?.data?.message || 'Failed to link. Please try again.'
  } finally {
    linkingFund.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}
</script>
