<template>
  <q-page class="q-pa-sm">
    <q-card square>
      <HistoryTable class="table-size"/>
    </q-card>
  </q-page>
</template>

<script>
import {defineComponent} from 'vue';
import HistoryTable from 'components/HistoryTable.vue';

export default defineComponent({
  name: 'HistoryPage',
  components: {HistoryTable},
  setup() {
    return {};
  }
});
</script>

<style scoped>
.table-size {
  height: calc(100vh - 53px);;
  box-sizing: border-box;
}
</style>
