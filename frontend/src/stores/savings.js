import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { savingsApi } from '@/services/api'

export const useSavingsStore = defineStore('savings', () => {
  const accounts = ref([])
  const funds = ref([])
  const events = ref([])
  const accountEvents = ref([])
  const summary = ref(null)
  const selectedFundId = ref(null)
  const selectedAccountId = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const totalPoolBalance = computed(() =>
    accounts.value.reduce((sum, a) => sum + parseFloat(a.balance || 0), 0)
  )

  const unassignedFund = computed(() =>
    funds.value.find(f => f.isSystemFund)
  )

  const userFunds = computed(() =>
    funds.value.filter(f => !f.isSystemFund)
  )

  async function fetchAccounts() {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.getAccounts()
      accounts.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch savings accounts'
      accounts.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchFunds() {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.getFunds()
      funds.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch savings funds'
      funds.value = []
    } finally {
      loading.value = false
    }
  }

  async function updateEvent(id, data) {
    loading.value = true
    error.value = null
    try {
      await savingsApi.updateEvent(id, data)
      await fetchFunds()
      if (selectedFundId.value) await fetchEventsForFund(selectedFundId.value)
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to update event'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteEvent(id) {
    loading.value = true
    error.value = null
    try {
      await savingsApi.deleteEvent(id)
      await fetchFunds()
      if (selectedFundId.value) await fetchEventsForFund(selectedFundId.value)
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to delete event'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchEventsForFund(fundId) {
    loading.value = true
    error.value = null
    try {
      selectedFundId.value = fundId
      const response = await savingsApi.getEventsForFund(fundId)
      events.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch fund events'
      events.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchSummary() {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.getSummary()
      summary.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch savings summary'
    } finally {
      loading.value = false
    }
  }

  async function createAccount(data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.createAccount(data)
      accounts.value.push(response.data)
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to create account'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateAccount(id, data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.updateAccount(id, data)
      const index = accounts.value.findIndex(a => a.id === id)
      if (index !== -1) accounts.value[index] = response.data
      return response.data
    } catch (e) {
      error.value = 'Failed to update account'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteAccount(id) {
    loading.value = true
    error.value = null
    try {
      await savingsApi.deleteAccount(id)
      accounts.value = accounts.value.filter(a => a.id !== id)
    } catch (e) {
      error.value = 'Failed to delete account'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function createFund(data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.createFund(data)
      funds.value.push(response.data)
      funds.value.sort((a, b) => a.name.localeCompare(b.name))
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to create fund'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateFund(id, data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.updateFund(id, data)
      const index = funds.value.findIndex(f => f.id === id)
      if (index !== -1) funds.value[index] = response.data
      return response.data
    } catch (e) {
      error.value = 'Failed to update fund'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteFund(id) {
    loading.value = true
    error.value = null
    try {
      await savingsApi.deleteFund(id)
      funds.value = funds.value.filter(f => f.id !== id)
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to delete fund'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function logDeposit(data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.logDeposit(data)
      await fetchFunds()
      if (selectedFundId.value && (data.targetFundId === selectedFundId.value || !data.targetFundId)) {
        await fetchEventsForFund(selectedFundId.value)
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to log deposit'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function logWithdrawal(data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.logWithdrawal(data)
      await fetchFunds()
      if (selectedFundId.value === data.fundId) {
        await fetchEventsForFund(selectedFundId.value)
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to log withdrawal'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function reallocate(data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.reallocate(data)
      await fetchFunds()
      if (selectedFundId.value &&
          (selectedFundId.value === data.sourceFundId || selectedFundId.value === data.destinationFundId)) {
        await fetchEventsForFund(selectedFundId.value)
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to reallocate'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function processPayout(fundId) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.processPayout(fundId)
      await fetchFunds()
      if (selectedFundId.value === fundId) {
        await fetchEventsForFund(fundId)
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to process payout'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function depositToAccount(accountId, data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.depositToAccount(accountId, data)
      const index = accounts.value.findIndex(a => a.id === accountId)
      if (index !== -1) accounts.value[index] = { ...accounts.value[index], balance: response.data.balanceAfter }
      if (selectedAccountId.value === accountId) {
        await fetchEventsForAccount(accountId)
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to log deposit'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function withdrawFromAccount(accountId, data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.withdrawFromAccount(accountId, data)
      const index = accounts.value.findIndex(a => a.id === accountId)
      if (index !== -1) accounts.value[index] = { ...accounts.value[index], balance: response.data.balanceAfter }
      if (selectedAccountId.value === accountId) {
        await fetchEventsForAccount(accountId)
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to log withdrawal'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateAccountEvent(eventId, data) {
    loading.value = true
    error.value = null
    try {
      const response = await savingsApi.updateAccountEvent(eventId, data)
      const idx = accountEvents.value.findIndex(e => e.id === eventId)
      if (idx !== -1) accountEvents.value[idx] = response.data
      const accountId = response.data.accountId
      const aIdx = accounts.value.findIndex(a => a.id === accountId)
      if (aIdx !== -1) accounts.value[aIdx] = { ...accounts.value[aIdx], balance: response.data.balanceAfter }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to update event'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteAccountEvent(eventId) {
    loading.value = true
    error.value = null
    try {
      const event = accountEvents.value.find(e => e.id === eventId)
      await savingsApi.deleteAccountEvent(eventId)
      accountEvents.value = accountEvents.value.filter(e => e.id !== eventId)
      if (event) {
        const aIdx = accounts.value.findIndex(a => a.id === event.accountId)
        if (aIdx !== -1) {
          const current = parseFloat(accounts.value[aIdx].balance)
          const amt = parseFloat(event.amount)
          const updated = event.eventType === 'DEPOSIT' ? current - amt : current + amt
          accounts.value[aIdx] = { ...accounts.value[aIdx], balance: updated }
        }
      }
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to delete event'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchEventsForAccount(accountId) {
    loading.value = true
    error.value = null
    try {
      selectedAccountId.value = accountId
      const response = await savingsApi.getAccountEvents(accountId)
      accountEvents.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch account events'
      accountEvents.value = []
    } finally {
      loading.value = false
    }
  }

  return {
    accounts,
    funds,
    events,
    accountEvents,
    summary,
    selectedFundId,
    selectedAccountId,
    loading,
    error,
    totalPoolBalance,
    unassignedFund,
    userFunds,
    fetchAccounts,
    fetchFunds,
    fetchEventsForFund,
    fetchEventsForAccount,
    depositToAccount,
    withdrawFromAccount,
    updateAccountEvent,
    deleteAccountEvent,
    fetchSummary,
    createAccount,
    updateAccount,
    deleteAccount,
    createFund,
    updateFund,
    deleteFund,
    logDeposit,
    logWithdrawal,
    reallocate,
    processPayout,
    updateEvent,
    deleteEvent
  }
})
