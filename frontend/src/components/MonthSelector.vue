<template>
  <div class="month-selector mb-2 mb-sm-4">
    <v-btn icon variant="text" size="small" @click="previousMonth">
      <v-icon size="18">mdi-chevron-left</v-icon>
    </v-btn>

    <v-select
      v-model="selectedMonth"
      :items="months"
      item-title="name"
      item-value="value"
      density="compact"
      hide-details
      variant="outlined"
      class="month-select mx-1"
    ></v-select>

    <v-select
      v-model="selectedYear"
      :items="years"
      density="compact"
      hide-details
      variant="outlined"
      class="year-select mx-1"
    ></v-select>

    <v-btn icon variant="text" size="small" @click="nextMonth">
      <v-icon size="18">mdi-chevron-right</v-icon>
    </v-btn>

    <v-btn variant="text" color="primary" size="small" class="ml-1 ml-sm-3 today-btn" @click="goToToday">
      Today
    </v-btn>
  </div>
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

<style scoped>
.month-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-radius: 8px;
  padding: 4px 8px;
}

.month-select {
  flex: 0 0 130px;
  width: 130px;
}

.year-select {
  flex: 0 0 88px;
  width: 88px;
}

.today-btn {
  flex-shrink: 0;
}

@media (max-width: 599px) {
  .month-select {
    flex: 0 0 110px;
    width: 110px;
  }

  .year-select {
    flex: 0 0 80px;
    width: 80px;
  }
}
</style>
