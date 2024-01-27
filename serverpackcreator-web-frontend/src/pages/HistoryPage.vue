<template>
  <q-page class="col items-center justify-evenly">
    <q-card square>
      <q-card-section class="text-h5 text-bold">
        History / Event Log
      </q-card-section>
      <HistoryTable class="table-size"/>
    </q-card>
  </q-page>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import HistoryTable from 'components/HistoryTable.vue';

export default defineComponent({
  name: 'HistoryPage',
  components: { HistoryTable },
  setup() {
    return {

    };
  }
});
</script>

<style scoped>
.table-size {
  height: calc(100vh - 115px);;
  box-sizing: border-box;
}
</style>
