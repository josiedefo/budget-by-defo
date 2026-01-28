<template>
  <v-app>
    <v-app-bar color="primary" density="comfortable">
      <v-app-bar-title>Budget App</v-app-bar-title>
      <v-spacer></v-spacer>
      <v-btn-toggle v-model="viewMode" mandatory>
        <v-btn value="monthly" variant="text">
          <v-icon start>mdi-calendar-month</v-icon>
          Monthly
        </v-btn>
        <v-btn value="yearly" variant="text">
          <v-icon start>mdi-calendar</v-icon>
          Yearly
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
  viewMode.value = route.name === 'yearly' ? 'yearly' : 'monthly'
})

watch(() => route.name, (name) => {
  viewMode.value = name === 'yearly' ? 'yearly' : 'monthly'
})

watch(viewMode, (newValue) => {
  const now = new Date()
  const year = route.params.year || now.getFullYear()
  const month = route.params.month || now.getMonth() + 1

  if (newValue === 'monthly' && route.name !== 'monthly') {
    router.push({ name: 'monthly', params: { year, month } })
  } else if (newValue === 'yearly' && route.name !== 'yearly') {
    router.push({ name: 'yearly', params: { year } })
  }
})
</script>
