<template>
  <v-dialog :model-value="modelValue" max-width="460" persistent @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="text-h6 pt-4 px-4">
        <v-icon start color="teal">mdi-bank</v-icon>
        {{ isLinked ? 'Savings Account Link' : 'Link to Savings Account' }}
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

        <!-- Already linked state -->
        <template v-if="isLinked">
          <v-alert type="success" variant="tonal" density="compact" icon="mdi-check-circle">
            Linked to <strong>{{ transaction.linkedSavingsAccountName }}</strong>
            as a <strong>{{ transaction.linkedSavingsEventType }}</strong>
          </v-alert>
          <p class="text-body-2 text-medium-emphasis mt-3">
            Unlinking will reverse the {{ transaction.linkedSavingsEventType === 'DEPOSIT' ? 'deposit' : 'withdrawal' }}
            of {{ formatCurrency(transaction?.amount) }} from this account.
          </p>
        </template>

        <!-- Unlinked state: form -->
        <template v-else>
          <v-select
            v-model="selectedAccountId"
            :items="savingsStore.accounts"
            item-title="name"
            item-value="id"
            label="Savings Account *"
            variant="outlined"
            density="comfortable"
            :loading="savingsStore.loading && savingsStore.accounts.length === 0"
            no-data-text="No savings accounts found"
            class="mb-3"
          />

          <div class="mb-3">
            <p class="text-body-2 text-medium-emphasis mb-2">Event Type *</p>
            <v-btn-toggle v-model="eventType" mandatory color="teal" density="comfortable" rounded="lg">
              <v-btn value="DEPOSIT" prepend-icon="mdi-arrow-down-circle-outline">Deposit</v-btn>
              <v-btn value="WITHDRAWAL" prepend-icon="mdi-arrow-up-circle-outline">Withdrawal</v-btn>
            </v-btn-toggle>
          </div>

          <v-text-field
            v-model="note"
            label="Note (optional)"
            variant="outlined"
            density="comfortable"
            :placeholder="transaction?.merchant"
          />
        </template>

        <v-alert v-if="linkError" type="error" variant="tonal" density="compact" class="mt-2">
          {{ linkError }}
        </v-alert>
      </v-card-text>

      <v-card-actions class="px-4 pb-4">
        <v-spacer />
        <template v-if="isLinked">
          <v-btn variant="text" @click="close">Close</v-btn>
          <v-btn color="error" variant="tonal" :loading="linking" @click="unlink">
            <v-icon start>mdi-link-off</v-icon>
            Unlink
          </v-btn>
        </template>
        <template v-else>
          <v-btn variant="text" @click="close">Cancel</v-btn>
          <v-btn
            color="teal"
            variant="tonal"
            :disabled="!selectedAccountId || linking"
            :loading="linking"
            @click="link"
          >
            <v-icon start>mdi-link</v-icon>
            Link
          </v-btn>
        </template>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useSavingsStore } from '@/stores/savings'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  transaction: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'linked', 'unlinked'])

const savingsStore = useSavingsStore()

const selectedAccountId = ref(null)
const eventType = ref('DEPOSIT')
const note = ref('')
const linking = ref(false)
const linkError = ref(null)

const isLinked = computed(() => !!props.transaction?.linkedSavingsAccountEventId)

// Reset form state when dialog opens
watch(() => props.modelValue, (open) => {
  if (open) {
    linkError.value = null
    note.value = ''
    selectedAccountId.value = null
    // Default event type: EXPENSE transaction → DEPOSIT into savings, INCOME → WITHDRAWAL
    eventType.value = props.transaction?.type === 'INCOME' ? 'WITHDRAWAL' : 'DEPOSIT'
    // Ensure accounts are loaded
    if (savingsStore.accounts.length === 0) {
      savingsStore.fetchAccounts()
    }
  }
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value || 0)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

async function link() {
  if (!selectedAccountId.value) return
  linking.value = true
  linkError.value = null
  try {
    const eventDto = await savingsStore.linkTransactionToAccount(selectedAccountId.value, {
      transactionId: props.transaction.id,
      eventType: eventType.value,
      note: note.value || null
    })
    emit('linked', eventDto)
    close()
  } catch (e) {
    linkError.value = e.response?.data?.message || 'Failed to link transaction. Please try again.'
  } finally {
    linking.value = false
  }
}

async function unlink() {
  linking.value = true
  linkError.value = null
  try {
    await savingsStore.unlinkTransactionEvent(
      props.transaction.linkedSavingsAccountEventId,
      props.transaction.linkedSavingsAccountId,
      props.transaction.amount,
      props.transaction.linkedSavingsEventType
    )
    emit('unlinked')
    close()
  } catch (e) {
    linkError.value = e.response?.data?.message || 'Failed to unlink. Please try again.'
  } finally {
    linking.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}
</script>
