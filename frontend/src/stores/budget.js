import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { budgetApi, sectionApi, itemApi } from '@/services/api'

export const useBudgetStore = defineStore('budget', () => {
  const currentBudget = ref(null)
  const yearlySummary = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const sections = computed(() => currentBudget.value?.sections || [])

  const totalPlannedIncome = computed(() => currentBudget.value?.totalPlannedIncome || 0)
  const totalActualIncome = computed(() => currentBudget.value?.totalIncome || 0)
  const totalPlannedExpenses = computed(() => currentBudget.value?.totalPlannedExpenses || 0)
  const totalActualExpenses = computed(() => currentBudget.value?.totalExpenses || 0)

  const plannedBalance = computed(() => totalPlannedIncome.value - totalPlannedExpenses.value)
  const actualBalance = computed(() => totalActualIncome.value - totalActualExpenses.value)

  async function fetchBudget(year, month) {
    loading.value = true
    error.value = null
    try {
      const response = await budgetApi.getBudget(year, month)
      currentBudget.value = response.data
    } catch (e) {
      error.value = e.message
      currentBudget.value = null
    } finally {
      loading.value = false
    }
  }

  async function fetchYearlySummary(year) {
    loading.value = true
    error.value = null
    try {
      const response = await budgetApi.getYearlySummary(year)
      yearlySummary.value = response.data
    } catch (e) {
      error.value = e.message
      yearlySummary.value = null
    } finally {
      loading.value = false
    }
  }

  async function addSection(name, isIncome = false) {
    if (!currentBudget.value) return
    try {
      const response = await sectionApi.create(currentBudget.value.id, name, isIncome)
      currentBudget.value.sections.push(response.data)
    } catch (e) {
      error.value = e.message
    }
  }

  async function updateSection(sectionId, data) {
    try {
      const response = await sectionApi.update(sectionId, data)
      const index = currentBudget.value.sections.findIndex(s => s.id === sectionId)
      if (index !== -1) {
        currentBudget.value.sections[index] = response.data
      }
    } catch (e) {
      error.value = e.message
    }
  }

  async function deleteSection(sectionId) {
    try {
      await sectionApi.delete(sectionId)
      currentBudget.value.sections = currentBudget.value.sections.filter(s => s.id !== sectionId)
    } catch (e) {
      error.value = e.message
    }
  }

  async function addItem(sectionId, name, plannedAmount = 0) {
    try {
      const response = await itemApi.create(sectionId, name, plannedAmount, 0)
      const section = currentBudget.value.sections.find(s => s.id === sectionId)
      if (section) {
        section.items.push(response.data)
        recalculateTotals()
      }
    } catch (e) {
      error.value = e.message
    }
  }

  async function updateItem(sectionId, itemId, data) {
    try {
      const response = await itemApi.update(itemId, data)
      const section = currentBudget.value.sections.find(s => s.id === sectionId)
      if (section) {
        const index = section.items.findIndex(i => i.id === itemId)
        if (index !== -1) {
          section.items[index] = response.data
          recalculateTotals()
        }
      }
    } catch (e) {
      error.value = e.message
    }
  }

  async function deleteItem(sectionId, itemId) {
    try {
      await itemApi.delete(itemId)
      const section = currentBudget.value.sections.find(s => s.id === sectionId)
      if (section) {
        section.items = section.items.filter(i => i.id !== itemId)
        recalculateTotals()
      }
    } catch (e) {
      error.value = e.message
    }
  }

  function recalculateTotals() {
    if (!currentBudget.value) return

    let plannedIncome = 0
    let actualIncome = 0
    let plannedExpenses = 0
    let actualExpenses = 0

    for (const section of currentBudget.value.sections) {
      let sectionPlanned = 0
      let sectionActual = 0

      for (const item of section.items) {
        sectionPlanned += parseFloat(item.plannedAmount) || 0
        sectionActual += parseFloat(item.actualAmount) || 0
      }

      section.totalPlanned = sectionPlanned
      section.totalActual = sectionActual

      if (section.isIncome) {
        plannedIncome += sectionPlanned
        actualIncome += sectionActual
      } else {
        plannedExpenses += sectionPlanned
        actualExpenses += sectionActual
      }
    }

    currentBudget.value.totalPlannedIncome = plannedIncome
    currentBudget.value.totalIncome = actualIncome
    currentBudget.value.totalPlannedExpenses = plannedExpenses
    currentBudget.value.totalExpenses = actualExpenses
  }

  return {
    currentBudget,
    yearlySummary,
    loading,
    error,
    sections,
    totalPlannedIncome,
    totalActualIncome,
    totalPlannedExpenses,
    totalActualExpenses,
    plannedBalance,
    actualBalance,
    fetchBudget,
    fetchYearlySummary,
    addSection,
    updateSection,
    deleteSection,
    addItem,
    updateItem,
    deleteItem
  }
})
