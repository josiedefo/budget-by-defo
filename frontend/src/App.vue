<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title class="text-body-1 text-sm-h6">Budget App</v-app-bar-title>
      <v-spacer></v-spacer>
      <v-btn-toggle v-model="viewMode" mandatory density="compact">
        <v-btn value="monthly" variant="text">
          <v-icon>mdi-calendar-month</v-icon>
          <span class="d-none d-sm-inline ml-1">Monthly</span>
        </v-btn>
        <v-btn value="yearly" variant="text">
          <v-icon>mdi-calendar</v-icon>
          <span class="d-none d-sm-inline ml-1">Yearly</span>
        </v-btn>
        <v-btn value="transactions" variant="text">
          <v-icon>mdi-swap-horizontal</v-icon>
          <span class="d-none d-sm-inline ml-1">Transactions</span>
        </v-btn>
        <v-btn value="planner" variant="text">
          <v-icon>mdi-calendar-text</v-icon>
          <span class="d-none d-sm-inline ml-1">Planner</span>
        </v-btn>
        <v-btn value="savings" variant="text">
          <v-icon>mdi-piggy-bank</v-icon>
          <span class="d-none d-sm-inline ml-1">Savings</span>
        </v-btn>
      </v-btn-toggle>
    </v-app-bar>

    <v-main>
      <router-view></router-view>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const viewMode = ref('monthly')

// Track the last monthly year/month so navigating back to Monthly
// returns to the same month the user was on, not the current month.
const now = new Date()
const lastMonthlyYear = ref(now.getFullYear())
const lastMonthlyMonth = ref(now.getMonth() + 1)

onMounted(() => {
  if (route.name === 'monthly') {
    lastMonthlyYear.value = Number(route.params.year)
    lastMonthlyMonth.value = Number(route.params.month)
    viewMode.value = 'monthly'
  } else if (route.name === 'yearly') {
    viewMode.value = 'yearly'
  } else if (route.name === 'transactions') {
    viewMode.value = 'transactions'
  } else if (route.name === 'planner') {
    viewMode.value = 'planner'
  } else if (route.name === 'savings') {
    viewMode.value = 'savings'
  } else {
    viewMode.value = 'monthly'
  }
})

watch(() => route.name, (name) => {
  if (name === 'monthly') {
    lastMonthlyYear.value = Number(route.params.year)
    lastMonthlyMonth.value = Number(route.params.month)
    viewMode.value = 'monthly'
  } else if (name === 'yearly') {
    viewMode.value = 'yearly'
  } else if (name === 'transactions') {
    viewMode.value = 'transactions'
  } else if (name === 'planner') {
    viewMode.value = 'planner'
  } else if (name === 'savings') {
    viewMode.value = 'savings'
  } else {
    viewMode.value = 'monthly'
  }
})

watch(viewMode, (newValue) => {
  if (newValue === 'monthly' && route.name !== 'monthly') {
    router.push({ name: 'monthly', params: { year: lastMonthlyYear.value, month: lastMonthlyMonth.value } })
  } else if (newValue === 'yearly' && route.name !== 'yearly') {
    router.push({ name: 'yearly', params: { year: lastMonthlyYear.value } })
  } else if (newValue === 'transactions' && route.name !== 'transactions') {
    router.push({ name: 'transactions' })
  } else if (newValue === 'planner' && route.name !== 'planner') {
    router.push({ name: 'planner' })
  } else if (newValue === 'savings' && route.name !== 'savings') {
    router.push({ name: 'savings' })
  }
})
</script>
