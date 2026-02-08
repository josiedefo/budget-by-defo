import { defineStore } from 'pinia'
import { ref } from 'vue'
import { subscriptionApi } from '@/services/api'

export const useSubscriptionStore = defineStore('subscription', () => {
  const subscriptions = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchSubscriptions() {
    loading.value = true
    error.value = null
    try {
      const response = await subscriptionApi.getAll()
      subscriptions.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch subscriptions'
      subscriptions.value = []
    } finally {
      loading.value = false
    }
  }

  async function createSubscription(data) {
    loading.value = true
    error.value = null
    try {
      const response = await subscriptionApi.create(data)
      subscriptions.value.push(response.data)
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to create subscription'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateSubscription(id, data) {
    loading.value = true
    error.value = null
    try {
      const response = await subscriptionApi.update(id, data)
      const index = subscriptions.value.findIndex(s => s.id === id)
      if (index !== -1) {
        subscriptions.value[index] = response.data
      }
      return response.data
    } catch (e) {
      error.value = 'Failed to update subscription'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteSubscription(id) {
    loading.value = true
    error.value = null
    try {
      await subscriptionApi.delete(id)
      subscriptions.value = subscriptions.value.filter(s => s.id !== id)
    } catch (e) {
      error.value = 'Failed to delete subscription'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    subscriptions,
    loading,
    error,
    fetchSubscriptions,
    createSubscription,
    updateSubscription,
    deleteSubscription
  }
})
