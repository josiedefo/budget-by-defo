<template>
  <v-card class="mb-4">
    <v-card-text class="d-flex align-center justify-center">
      <v-btn icon variant="text" @click="previousMonth">
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>

      <v-select
        v-model="selectedMonth"
        :items="months"
        item-title="name"
        item-value="value"
        density="compact"
        hide-details
        variant="outlined"
        class="mx-2"
        style="max-width: 150px"
      ></v-select>

      <v-select
        v-model="selectedYear"
        :items="years"
        density="compact"
        hide-details
        variant="outlined"
        class="mx-2"
        style="max-width: 100px"
      ></v-select>

      <v-btn icon variant="text" @click="nextMonth">
        <v-icon>mdi-chevron-right</v-icon>
      </v-btn>

      <v-btn variant="text" color="primary" class="ml-4" @click="goToToday">
        Today
      </v-btn>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true }
})

const emit = defineEmits(['change'])

const months = [
  { name: 'January', value: 1 },
  { name: 'February', value: 2 },
  { name: 'March', value: 3 },
  { name: 'April', value: 4 },
  { name: 'May', value: 5 },
  { name: 'June', value: 6 },
  { name: 'July', value: 7 },
  { name: 'August', value: 8 },
  { name: 'September', value: 9 },
  { name: 'October', value: 10 },
  { name: 'November', value: 11 },
  { name: 'December', value: 12 }
]

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 11 }, (_, i) => currentYear - 5 + i)

const selectedYear = ref(props.year)
const selectedMonth = ref(props.month)

watch([selectedYear, selectedMonth], () => {
  emit('change', { year: selectedYear.value, month: selectedMonth.value })
})

watch(() => [props.year, props.month], ([y, m]) => {
  selectedYear.value = y
  selectedMonth.value = m
})

function previousMonth() {
  if (selectedMonth.value === 1) {
    selectedMonth.value = 12
    selectedYear.value--
  } else {
    selectedMonth.value--
  }
}

function nextMonth() {
  if (selectedMonth.value === 12) {
    selectedMonth.value = 1
    selectedYear.value++
  } else {
    selectedMonth.value++
  }
}

function goToToday() {
  const now = new Date()
  selectedYear.value = now.getFullYear()
  selectedMonth.value = now.getMonth() + 1
}
</script>
