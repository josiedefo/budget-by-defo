import { defineStore } from 'pinia'
import { ref } from 'vue'
import { salaryApi } from '@/services/api'

export const useSalaryStore = defineStore('salary', () => {
  const salaries = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetchSalaries() {
    loading.value = true
    error.value = null
    try {
      const response = await salaryApi.getAll()
      salaries.value = response.data
    } catch (e) {
      error.value = 'Failed to fetch salaries'
      salaries.value = []
    } finally {
      loading.value = false
    }
  }

  async function createSalary(data) {
    loading.value = true
    error.value = null
    try {
      const response = await salaryApi.create(data)
      salaries.value.push(response.data)
      return response.data
    } catch (e) {
      error.value = e.response?.data?.message || 'Failed to create salary'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateSalary(id, data) {
    loading.value = true
    error.value = null
    try {
      const response = await salaryApi.update(id, data)
      const index = salaries.value.findIndex(s => s.id === id)
      if (index !== -1) {
        salaries.value[index] = response.data
      }
      return response.data
    } catch (e) {
      error.value = 'Failed to update salary'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function deleteSalary(id) {
    loading.value = true
    error.value = null
    try {
      await salaryApi.delete(id)
      salaries.value = salaries.value.filter(s => s.id !== id)
    } catch (e) {
      error.value = 'Failed to delete salary'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    salaries,
    loading,
    error,
    fetchSalaries,
    createSalary,
    updateSalary,
    deleteSalary
  }
})
