<template>
  <v-container fluid class="pa-4">
    <MonthSelector :year="year" :month="month" @change="navigateToMonth" />

    <v-row v-if="loading" class="justify-center mt-8">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
    </v-row>

    <v-alert v-else-if="error" type="warning" variant="tonal" class="mt-4">
      {{ error }}
    </v-alert>

    <template v-else>
      <v-row class="mt-4">
        <v-col cols="12">
          <div class="d-flex justify-space-between align-center mb-4">
            <h2 class="text-h5">Plans for {{ monthName }} {{ year }}</h2>
            <v-btn color="primary" @click="showCreateDialog = true">
              <v-icon start>mdi-plus</v-icon>
              Create Plan
            </v-btn>
          </div>
        </v-col>
      </v-row>

      <v-row v-if="plans.length === 0">
        <v-col cols="12">
          <v-card variant="outlined" class="text-center pa-8">
            <v-icon size="64" color="grey">mdi-calendar-blank</v-icon>
            <p class="text-h6 mt-4 text-medium-emphasis">No plans for this month</p>
            <p class="text-body-2 text-medium-emphasis">Create a plan to set planned amounts for budget items</p>
          </v-card>
        </v-col>
      </v-row>

      <v-row v-else>
        <v-col v-for="plan in plans" :key="plan.id" cols="12" md="6" lg="4">
          <v-card variant="outlined" class="plan-card" @click="openPlan(plan)">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2" color="primary">mdi-calendar-text</v-icon>
              {{ plan.budgetItemName }}
            </v-card-title>
            <v-card-subtitle>{{ plan.sectionName }}</v-card-subtitle>
            <v-card-text>
              <div class="d-flex justify-space-between align-center">
                <span class="text-medium-emphasis">{{ plan.items.length }} items</span>
                <span class="text-h6 font-weight-bold">{{ formatCurrency(plan.total) }}</span>
              </div>
            </v-card-text>
            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn icon size="small" color="error" @click.stop="confirmDeletePlan(plan)">
                <v-icon>mdi-delete</v-icon>
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <CreatePlanDialog
      v-model="showCreateDialog"
      :year="year"
      :month="month"
      @created="handlePlanCreated"
    />

    <PlanDialog
      v-model="showPlanDialog"
      :plan="selectedPlan"
      @saved="handlePlanSaved"
      @deleted="handlePlanDeleted"
    />
  </v-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { usePlanStore } from '@/stores/plan'
import MonthSelector from '@/components/MonthSelector.vue'
import CreatePlanDialog from '@/components/CreatePlanDialog.vue'
import PlanDialog from '@/components/PlanDialog.vue'

const route = useRoute()
const router = useRouter()
const planStore = usePlanStore()
const { plans, loading, error } = storeToRefs(planStore)

const now = new Date()
const year = ref(parseInt(route.query.year) || now.getFullYear())
const month = ref(parseInt(route.query.month) || now.getMonth() + 1)

const showCreateDialog = ref(false)
const showPlanDialog = ref(false)
const selectedPlan = ref(null)

const monthName = computed(() => {
  return new Date(year.value, month.value - 1).toLocaleString('default', { month: 'long' })
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

async function loadPlans() {
  await planStore.fetchPlans(year.value, month.value)
}

function navigateToMonth({ year: y, month: m }) {
  year.value = y
  month.value = m
  router.replace({ query: { year: y, month: m } })
  loadPlans()
}

function openPlan(plan) {
  selectedPlan.value = plan
  showPlanDialog.value = true
}

function handlePlanCreated(plan) {
  showCreateDialog.value = false
  selectedPlan.value = plan
  showPlanDialog.value = true
}

function handlePlanSaved() {
  loadPlans()
}

function handlePlanDeleted() {
  showPlanDialog.value = false
  loadPlans()
}

function confirmDeletePlan(plan) {
  if (confirm(`Delete plan for "${plan.budgetItemName}"? This will reset the planned amount to $0.`)) {
    planStore.deletePlan(plan.id).then(() => loadPlans())
  }
}

onMounted(loadPlans)

watch(() => route.query, (query) => {
  if (query.year) year.value = parseInt(query.year)
  if (query.month) month.value = parseInt(query.month)
  if (query.planId) {
    planStore.fetchPlan(parseInt(query.planId)).then(plan => {
      if (plan) {
        selectedPlan.value = plan
        showPlanDialog.value = true
      }
    })
  }
}, { immediate: true })
</script>

<style scoped>
.plan-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.plan-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
