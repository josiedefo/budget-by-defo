import { defineStore } from 'pinia'
import { ref } from 'vue'
import { planApi } from '@/services/api'

export const usePlanStore = defineStore('plan', () => {
  const plans = ref([])
  const currentPlan = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function fetchPlans(year, month) {
    loading.value = true
    error.value = null
    try {
      const response = await planApi.getPlans(year, month)
      plans.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch plans'
      plans.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchPlan(id) {
    loading.value = true
    error.value = null
    try {
      const response = await planApi.getPlan(id)
      currentPlan.value = response.data
      return response.data
    } catch (e) {
      error.value = 'Failed to fetch plan'
      currentPlan.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  async function createPlan(budgetItemId, year, month) {
    loading.value = true
    error.value = null
    try {
      const response = await planApi.create({ budgetItemId, year, month })
      plans.value.push(response.data)
      currentPlan.value = response.data
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to create plan'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updatePlan(id, items) {
    loading.value = true
    error.value = null
    try {
      const response = await planApi.update(id, { items })
      currentPlan.value = response.data
      // Update in plans list if present
      const index = plans.value.findIndex(p => p.id === id)
      if (index !== -1) {
        plans.value[index] = response.data
      }
      return response.data
    } catch (e) {
      error.value = 'Failed to update plan'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deletePlan(id) {
    loading.value = true
    error.value = null
    try {
      await planApi.delete(id)
      plans.value = plans.value.filter(p => p.id !== id)
      if (currentPlan.value?.id === id) {
        currentPlan.value = null
      }
    } catch (e) {
      error.value = 'Failed to delete plan'
      throw e
    } finally {
      loading.value = false
    }
  }

  function clearCurrentPlan() {
    currentPlan.value = null
  }

  return {
    plans,
    currentPlan,
    loading,
    error,
    fetchPlans,
    fetchPlan,
    createPlan,
    updatePlan,
    deletePlan,
    clearCurrentPlan
  }
})
