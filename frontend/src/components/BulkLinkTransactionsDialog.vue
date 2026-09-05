<template>
  <v-dialog :model-value="modelValue" max-width="500" persistent @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="text-title-large pt-4 px-4">
        <v-icon start color="teal">mdi-bank-transfer</v-icon>
        Bulk Link to Savings
      </v-card-title>

      <v-card-text class="px-4">
        <!-- Selection summary -->
        <v-sheet rounded="lg" color="surface-variant" class="pa-3 mb-4">
          <div class="d-flex justify-space-between align-center">
            <span class="text-body-medium text-medium-emphasis">Transactions selected</span>
            <span class="font-weight-medium">{{ transactionIds.length }}</span>
          </div>
        </v-sheet>

        <!-- ── Savings Account Section ── -->
        <div class="text-title-small text-medium-emphasis mb-2">
          <v-icon size="small" class="mr-1">mdi-bank-outline</v-icon>
          Savings Account
        </div>

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
        <v-alert v-if="accountError" type="error" variant="tonal" density="compact" class="mb-2">
          {{ accountError }}
        </v-alert>

        <v-divider class="my-4" />

        <!-- ── Savings Fund Section ── -->
        <div class="text-title-small text-medium-emphasis mb-2">
          <v-icon size="small" class="mr-1">mdi-piggy-bank-outline</v-icon>
          Savings Fund
        </div>

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
import { ref, watch } from 'vue'
import { useSavingsStore } from '@/stores/savings'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  transactionIds: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'linked'])

const savingsStore = useSavingsStore()

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

watch(() => props.modelValue, (open) => {
  if (open) {
    selectedAccountId.value = null
    selectedFundId.value = null
    accountError.value = null
    fundError.value = null
    accountOperationResult.value = null
    fundOperationResult.value = null
    if (savingsStore.accounts.length === 0) savingsStore.fetchAccounts()
    if (savingsStore.funds.length === 0) savingsStore.fetchFunds()
  }
})

function describeResult(result) {
  if (result.linked === 0 && result.skipped > 0) {
    return `All ${result.total} transaction(s) were already linked — nothing to do.`
  }
  if (result.skipped > 0) {
    return `${result.linked} linked, ${result.skipped} already linked (skipped).`
  }
  return `${result.linked} transaction(s) linked.`
}

async function linkToAccount() {
  if (!selectedAccountId.value) return
  linkingAccount.value = true
  accountError.value = null
  accountOperationResult.value = null
  try {
    const result = await savingsStore.bulkLinkTransactionsToAccount(selectedAccountId.value, {
      transactionIds: props.transactionIds,
      eventType: accountEventType.value
    })
    accountOperationResult.value = describeResult(result)
    emit('linked', { type: 'account', result })
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
    const result = await savingsStore.bulkLinkTransactionsToFund({
      transactionIds: props.transactionIds,
      fundId: selectedFundId.value,
      eventType: fundEventType.value
    })
    fundOperationResult.value = describeResult(result)
    emit('linked', { type: 'fund', result })
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
