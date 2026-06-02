<template>
  <v-dialog :model-value="modelValue" max-width="500" persistent @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="text-h6 pt-4 px-4">
        <v-icon start color="teal">mdi-bank</v-icon>
        Savings Links
      </v-card-title>

      <v-card-text class="px-4">
        <!-- Transaction summary chip -->
        <v-sheet rounded="lg" color="surface-variant" class="pa-3 mb-4">
          <div class="d-flex justify-space-between align-center">
            <span class="text-body-2 text-medium-emphasis">Transaction</span>
            <span class="font-weight-medium">{{ transaction?.merchant }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mt-1">
            <span class="text-body-2 text-medium-emphasis">Amount</span>
            <span :class="transaction?.type === 'INCOME' ? 'text-success font-weight-medium' : 'text-error font-weight-medium'">
              {{ transaction?.type === 'INCOME' ? '+' : '-' }}{{ formatCurrency(transaction?.amount) }}
            </span>
          </div>
          <div class="d-flex justify-space-between align-center mt-1">
            <span class="text-body-2 text-medium-emphasis">Date</span>
            <span>{{ formatDate(transaction?.transactionDate) }}</span>
          </div>
        </v-sheet>

        <!-- ── Savings Account Section ── -->
        <div class="text-subtitle-2 text-medium-emphasis mb-2">
          <v-icon size="small" class="mr-1">mdi-bank-outline</v-icon>
          Savings Account
        </div>

        <!-- Account: linked state -->
        <template v-if="isAccountLinked">
          <v-alert type="success" variant="tonal" density="compact" icon="mdi-check-circle" class="mb-2">
            Linked to
            <a class="savings-link" @click="goToAccountHistory">{{ transaction.linkedSavingsAccountName }}</a>
            as a <strong>{{ transaction.linkedSavingsEventType }}</strong>
          </v-alert>
          <div class="d-flex justify-end mb-1">
            <v-btn
              color="error"
              variant="text"
              size="small"
              :loading="unlinkingAccount"
              @click="unlinkAccount"
            >
              <v-icon start size="small">mdi-link-off</v-icon>
              Unlink Account
            </v-btn>
          </div>
        </template>

        <!-- Account: unlinked form -->
        <template v-else>
          <v-select
            v-model="selectedAccountId"
            :items="savingsStore.accounts"
            item-title="name"
            item-value="id"
            label="Savings Account"
            variant="outlined"
            density="comfortable"
            :loading="savingsStore.loading && savingsStore.accounts.length === 0"
            no-data-text="No savings accounts found"
            clearable
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
              @click="linkAccount"
            >
              <v-icon start size="small">mdi-link</v-icon>
              Link
            </v-btn>
          </div>
          <v-text-field
            v-model="accountNote"
            label="Note (optional)"
            variant="outlined"
            density="compact"
            :placeholder="transaction?.merchant"
            class="mb-1"
          />
        </template>

        <v-alert v-if="accountError" type="error" variant="tonal" density="compact" class="mb-2">
          {{ accountError }}
        </v-alert>

        <v-divider class="my-4" />

        <!-- ── Savings Fund Section ── -->
        <div class="text-subtitle-2 text-medium-emphasis mb-2">
          <v-icon size="small" class="mr-1">mdi-piggy-bank-outline</v-icon>
          Savings Fund
        </div>

        <!-- Fund: linked state -->
        <template v-if="isFundLinked">
          <v-alert type="success" variant="tonal" density="compact" icon="mdi-check-circle" class="mb-2">
            Linked to
            <a class="savings-link" @click="goToFundHistory">{{ transaction.linkedSavingsFundName }}</a>
            as a <strong>{{ fundEventTypeLabel(transaction.linkedSavingsFundEventType) }}</strong>
          </v-alert>
          <div class="d-flex justify-end mb-1">
            <v-btn
              color="error"
              variant="text"
              size="small"
              :loading="unlinkingFund"
              @click="unlinkFund"
            >
              <v-icon start size="small">mdi-link-off</v-icon>
              Unlink Fund
            </v-btn>
          </div>
        </template>

        <!-- Fund: unlinked form -->
        <template v-else>
          <v-select
            v-model="selectedFundId"
            :items="savingsStore.userFunds"
            item-title="name"
            item-value="id"
            label="Savings Fund"
            variant="outlined"
            density="comfortable"
            :loading="savingsStore.loading && savingsStore.funds.length === 0"
            no-data-text="No savings funds found"
            clearable
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
              @click="linkFund"
            >
              <v-icon start size="small">mdi-link</v-icon>
              Link
            </v-btn>
          </div>
          <v-text-field
            v-model="fundNote"
            label="Note (optional)"
            variant="outlined"
            density="compact"
            :placeholder="transaction?.merchant"
            class="mb-1"
          />
        </template>

        <v-alert v-if="fundError" type="error" variant="tonal" density="compact" class="mb-2">
          {{ fundError }}
        </v-alert>
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
import { useRouter } from 'vue-router'
import { useSavingsStore } from '@/stores/savings'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  transaction: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'linked', 'unlinked'])

const router = useRouter()
const savingsStore = useSavingsStore()

// Account section state
const selectedAccountId = ref(null)
const accountEventType = ref('DEPOSIT')
const accountNote = ref('')
const linkingAccount = ref(false)
const unlinkingAccount = ref(false)
const accountError = ref(null)

// Fund section state
const selectedFundId = ref(null)
const fundEventType = ref('DEPOSIT_ALLOCATED')
const fundNote = ref('')
const linkingFund = ref(false)
const unlinkingFund = ref(false)
const fundError = ref(null)

const isAccountLinked = computed(() => !!props.transaction?.linkedSavingsAccountEventId)
const isFundLinked = computed(() => !!props.transaction?.linkedSavingsFundEventId)

function fundEventTypeLabel(type) {
  return type === 'DEPOSIT_ALLOCATED' ? 'Deposit' : 'Withdrawal'
}

// Reset form state when dialog opens
watch(() => props.modelValue, (open) => {
  if (open) {
    accountError.value = null
    fundError.value = null
    accountNote.value = ''
    fundNote.value = ''
    selectedAccountId.value = null
    selectedFundId.value = null
    // Default event types based on transaction type
    accountEventType.value = props.transaction?.type === 'INCOME' ? 'WITHDRAWAL' : 'DEPOSIT'
    fundEventType.value = props.transaction?.type === 'INCOME' ? 'WITHDRAWAL' : 'DEPOSIT_ALLOCATED'
    // Ensure data is loaded
    if (savingsStore.accounts.length === 0) savingsStore.fetchAccounts()
    if (savingsStore.funds.length === 0) savingsStore.fetchFunds()
  }
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

// ── Account actions ──

async function linkAccount() {
  if (!selectedAccountId.value) return
  linkingAccount.value = true
  accountError.value = null
  try {
    const eventDto = await savingsStore.linkTransactionToAccount(selectedAccountId.value, {
      transactionId: props.transaction.id,
      eventType: accountEventType.value,
      note: accountNote.value || null
    })
    emit('linked', { type: 'account', eventDto })
  } catch (e) {
    accountError.value = e.response?.data?.message || 'Failed to link transaction. Please try again.'
  } finally {
    linkingAccount.value = false
  }
}

async function unlinkAccount() {
  unlinkingAccount.value = true
  accountError.value = null
  try {
    await savingsStore.unlinkTransactionEvent(
      props.transaction.linkedSavingsAccountEventId,
      props.transaction.linkedSavingsAccountId,
      props.transaction.amount,
      props.transaction.linkedSavingsEventType
    )
    emit('unlinked', { type: 'account' })
  } catch (e) {
    accountError.value = e.response?.data?.message || 'Failed to unlink. Please try again.'
  } finally {
    unlinkingAccount.value = false
  }
}

// ── Fund actions ──

async function linkFund() {
  if (!selectedFundId.value) return
  linkingFund.value = true
  fundError.value = null
  try {
    const eventDto = await savingsStore.linkTransactionToFund({
      transactionId: props.transaction.id,
      fundId: selectedFundId.value,
      eventType: fundEventType.value,
      note: fundNote.value || null
    })
    emit('linked', { type: 'fund', eventDto })
  } catch (e) {
    fundError.value = e.response?.data?.message || 'Failed to link transaction. Please try again.'
  } finally {
    linkingFund.value = false
  }
}

async function unlinkFund() {
  unlinkingFund.value = true
  fundError.value = null
  try {
    await savingsStore.unlinkTransactionFromFund(
      props.transaction.linkedSavingsFundEventId,
      props.transaction.linkedSavingsFundId,
      props.transaction.amount,
      props.transaction.linkedSavingsFundEventType
    )
    emit('unlinked', { type: 'fund' })
  } catch (e) {
    fundError.value = e.response?.data?.message || 'Failed to unlink. Please try again.'
  } finally {
    unlinkingFund.value = false
  }
}

function goToAccountHistory() {
  close()
  router.push({
    name: 'savings',
    query: {
      accountId: props.transaction.linkedSavingsAccountId,
      highlightEventId: props.transaction.linkedSavingsAccountEventId
    }
  })
}

function goToFundHistory() {
  close()
  router.push({
    name: 'savings',
    query: {
      tab: 'history',
      fundId: props.transaction.linkedSavingsFundId,
      highlightFundEventId: props.transaction.linkedSavingsFundEventId
    }
  })
}

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.savings-link {
  color: rgb(var(--v-theme-teal));
  font-weight: 600;
  text-decoration: underline;
  cursor: pointer;
}
.savings-link:hover {
  opacity: 0.8;
}
</style>
