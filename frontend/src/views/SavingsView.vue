<template>
  <v-container fluid class="pa-2 pa-sm-4">
    <!-- Pool Header -->
    <SavingsPoolHeader />

    <!-- Action Buttons -->
    <div class="d-flex ga-2 mb-4 flex-wrap">
      <v-btn variant="outlined" prepend-icon="mdi-bank" @click="showManageAccounts = true">
        <span class="d-none d-sm-inline">Manage Accounts</span>
        <span class="d-sm-none">Accounts</span>
      </v-btn>
      <v-btn color="primary" variant="elevated" prepend-icon="mdi-plus" @click="showDeposit = true">
        <span class="d-none d-sm-inline">Log Deposit</span>
        <span class="d-sm-none">Deposit</span>
      </v-btn>
      <v-btn color="secondary" variant="elevated" prepend-icon="mdi-plus" @click="showCreateFund = true">
        <span class="d-none d-sm-inline">New Fund</span>
        <span class="d-sm-none">Fund</span>
      </v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable>{{ error }}</v-alert>

    <!-- Tabs -->
    <v-tabs v-model="activeTab" class="mb-4">
      <v-tab value="funds">
        <v-icon start>mdi-piggy-bank</v-icon>
        Funds
      </v-tab>
      <v-tab value="history">
        <v-icon start>mdi-history</v-icon>
        History
      </v-tab>
      <v-tab value="summary">
        <v-icon start>mdi-chart-bar</v-icon>
        Summary
      </v-tab>
    </v-tabs>

    <v-tabs-window v-model="activeTab">
      <v-tabs-window-item value="funds">
        <v-progress-circular v-if="loading" indeterminate color="primary" class="d-block mx-auto mt-8" />
        <SavingsFundList
          v-else
          @open-withdraw="openWithdraw"
          @open-reallocate="openReallocate"
          @open-edit="openEdit"
          @delete-fund="confirmDelete"
          @open-payout="confirmPayout"
        />
      </v-tabs-window-item>

      <v-tabs-window-item value="history">
        <SavingsEventHistory />
      </v-tabs-window-item>

      <v-tabs-window-item value="summary">
        <SavingsSummaryPanel />
      </v-tabs-window-item>
    </v-tabs-window>

    <!-- Dialogs -->
    <ManageAccountsDialog v-model="showManageAccounts" />
    <LogDepositDialog v-model="showDeposit" />
    <LogWithdrawalDialog v-model="showWithdrawal" :fund-id="selectedFundForAction?.id" />
    <ReallocateDialog v-model="showReallocate" :source-fund-id="selectedFundForAction?.id" />
    <CreateFundDialog v-model="showCreateFund" />
    <EditFundDialog v-model="showEdit" :fund="selectedFundForAction" />

    <!-- Delete confirm -->
    <v-dialog v-model="showDeleteConfirm" max-width="400">
      <v-card>
        <v-card-title>Delete Fund</v-card-title>
        <v-card-text>Are you sure you want to delete "{{ selectedFundForAction?.name }}"?</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="showDeleteConfirm = false">Cancel</v-btn>
          <v-btn color="error" variant="elevated" :loading="loading" @click="doDelete">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Payout confirm -->
    <v-dialog v-model="showPayoutConfirm" max-width="400">
      <v-card>
        <v-card-title>Process Payout</v-card-title>
        <v-card-text>
          Process the scheduled payout for "{{ selectedFundForAction?.name }}"?
          <span v-if="selectedFundForAction?.payoutAmount">
            Amount: ${{ formatAmount(selectedFundForAction.payoutAmount) }}
          </span>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="showPayoutConfirm = false">Cancel</v-btn>
          <v-btn color="warning" variant="elevated" :loading="loading" @click="doPayout">Process</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'

import SavingsPoolHeader from '@/components/SavingsPoolHeader.vue'
import SavingsFundList from '@/components/SavingsFundList.vue'
import SavingsEventHistory from '@/components/SavingsEventHistory.vue'
import SavingsSummaryPanel from '@/components/SavingsSummaryPanel.vue'
import ManageAccountsDialog from '@/components/ManageAccountsDialog.vue'
import LogDepositDialog from '@/components/LogDepositDialog.vue'
import LogWithdrawalDialog from '@/components/LogWithdrawalDialog.vue'
import ReallocateDialog from '@/components/ReallocateDialog.vue'
import CreateFundDialog from '@/components/CreateFundDialog.vue'
import EditFundDialog from '@/components/EditFundDialog.vue'

const savingsStore = useSavingsStore()
const { loading, error } = storeToRefs(savingsStore)

const activeTab = ref('funds')
const selectedFundForAction = ref(null)

const showManageAccounts = ref(false)
const showDeposit = ref(false)
const showWithdrawal = ref(false)
const showReallocate = ref(false)
const showCreateFund = ref(false)
const showEdit = ref(false)
const showDeleteConfirm = ref(false)
const showPayoutConfirm = ref(false)

onMounted(async () => {
  await Promise.all([
    savingsStore.fetchAccounts(),
    savingsStore.fetchFunds()
  ])
})

function openWithdraw(fund) {
  selectedFundForAction.value = fund
  showWithdrawal.value = true
}

function openReallocate(fund) {
  selectedFundForAction.value = fund
  showReallocate.value = true
}

function openEdit(fund) {
  selectedFundForAction.value = fund
  showEdit.value = true
}

function confirmDelete(fundId) {
  selectedFundForAction.value = savingsStore.funds.find(f => f.id === fundId)
  showDeleteConfirm.value = true
}

function confirmPayout(fund) {
  selectedFundForAction.value = fund
  showPayoutConfirm.value = true
}

async function doDelete() {
  try {
    await savingsStore.deleteFund(selectedFundForAction.value.id)
    showDeleteConfirm.value = false
  } catch {}
}

async function doPayout() {
  try {
    await savingsStore.processPayout(selectedFundForAction.value.id)
    showPayoutConfirm.value = false
  } catch {}
}

function formatAmount(val) {
  return parseFloat(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>
