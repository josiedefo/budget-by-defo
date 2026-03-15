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

onMounted(() => {
  if (route.name === 'yearly') {
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
  if (name === 'yearly') {
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
  const now = new Date()
  const year = route.params.year || now.getFullYear()
  const month = route.params.month || now.getMonth() + 1

  if (newValue === 'monthly' && route.name !== 'monthly') {
    router.push({ name: 'monthly', params: { year, month } })
  } else if (newValue === 'yearly' && route.name !== 'yearly') {
    router.push({ name: 'yearly', params: { year } })
  } else if (newValue === 'transactions' && route.name !== 'transactions') {
    router.push({ name: 'transactions' })
  } else if (newValue === 'planner' && route.name !== 'planner') {
    router.push({ name: 'planner' })
  } else if (newValue === 'savings' && route.name !== 'savings') {
    router.push({ name: 'savings' })
  }
})
</script>
