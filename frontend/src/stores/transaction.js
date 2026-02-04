import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { transactionApi } from '@/services/api'

export const useTransactionStore = defineStore('transaction', () => {
  const transactions = ref([])
  const loading = ref(false)
  const error = ref(null)
  const pagination = ref({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  })
  const filters = ref({
    startDate: null,
    endDate: null,
    type: null,
    sectionId: null,
    budgetItemId: null,
    sectionName: null,
    budgetItemName: null,
    merchant: ''
  })

  const hasMore = computed(() => pagination.value.page < pagination.value.totalPages - 1)

  async function fetchTransactions(resetList = true) {
    loading.value = true
    error.value = null
    try {
      const params = {
        page: resetList ? 0 : pagination.value.page,
        size: pagination.value.size,
        ...filters.value
      }
      // Remove null/empty params
      Object.keys(params).forEach(key => {
        if (params[key] === null || params[key] === '') {
          delete params[key]
        }
      })

      const response = await transactionApi.getTransactions(params)
      if (resetList) {
        transactions.value = response.data.content
      } else {
        transactions.value = [...transactions.value, ...response.data.content]
      }
      pagination.value = {
        page: response.data.number,
        size: response.data.size,
        totalElements: response.data.totalElements,
        totalPages: response.data.totalPages
      }
    } catch (e) {
      error.value = 'Unable to load transactions. Please try again later.'
      transactions.value = []
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (hasMore.value && !loading.value) {
      pagination.value.page++
      await fetchTransactions(false)
    }
  }

  function setFilters(newFilters) {
    filters.value = { ...filters.value, ...newFilters }
    fetchTransactions(true)
  }

  function clearFilters() {
    filters.value = {
      startDate: null,
      endDate: null,
      type: null,
      sectionId: null,
      budgetItemId: null,
      sectionName: null,
      budgetItemName: null,
      merchant: ''
    }
    fetchTransactions(true)
  }

  async function createTransaction(data) {
    try {
      const response = await transactionApi.create(data)
      transactions.value.unshift(response.data)
      pagination.value.totalElements++
      return response.data
    } catch (e) {
      error.value = e.response?.data?.error || e.message
      throw e
    }
  }

  async function updateTransaction(id, data) {
    try {
      const response = await transactionApi.update(id, data)
      const index = transactions.value.findIndex(t => t.id === id)
      if (index !== -1) {
        transactions.value[index] = response.data
      }
      return response.data
    } catch (e) {
      error.value = e.response?.data?.error || e.message
      throw e
    }
  }

  async function deleteTransaction(id) {
    try {
      await transactionApi.delete(id)
      transactions.value = transactions.value.filter(t => t.id !== id)
      pagination.value.totalElements--
    } catch (e) {
      error.value = e.response?.data?.error || e.message
      throw e
    }
  }

  return {
    transactions,
    loading,
    error,
    pagination,
    filters,
    hasMore,
    fetchTransactions,
    loadMore,
    setFilters,
    clearFilters,
    createTransaction,
    updateTransaction,
    deleteTransaction
  }
})
