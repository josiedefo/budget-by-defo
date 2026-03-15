<template>
  <div>
    <!-- Unassigned Fund (system fund) -->
    <div v-if="unassignedFund" class="mb-4">
      <div class="text-subtitle-2 text-medium-emphasis mb-2">Unassigned</div>
      <v-row>
        <v-col cols="12" sm="6" md="4" lg="3">
          <SavingsFundCard
            :fund="unassignedFund"
            @withdraw="$emit('open-withdraw', unassignedFund)"
            @reallocate="$emit('open-reallocate', unassignedFund)"
            @edit="$emit('open-edit', unassignedFund)"
          />
        </v-col>
      </v-row>
    </div>

    <!-- User Funds -->
    <div v-if="userFunds.length > 0">
      <div class="text-subtitle-2 text-medium-emphasis mb-2">Funds</div>
      <v-row>
        <v-col
          v-for="fund in userFunds"
          :key="fund.id"
          cols="12" sm="6" md="4" lg="3"
        >
          <SavingsFundCard
            :fund="fund"
            @withdraw="$emit('open-withdraw', fund)"
            @reallocate="$emit('open-reallocate', fund)"
            @edit="$emit('open-edit', fund)"
            @delete="$emit('delete-fund', fund.id)"
            @payout="$emit('open-payout', fund)"
          />
        </v-col>
      </v-row>
    </div>

    <v-alert v-if="!unassignedFund && userFunds.length === 0" type="info" variant="tonal">
      No funds yet. Create your first savings fund to get started.
    </v-alert>
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useSavingsStore } from '@/stores/savings'
import SavingsFundCard from './SavingsFundCard.vue'

const savingsStore = useSavingsStore()
const { unassignedFund, userFunds } = storeToRefs(savingsStore)

defineEmits(['open-withdraw', 'open-reallocate', 'open-edit', 'delete-fund', 'open-payout'])
</script>
