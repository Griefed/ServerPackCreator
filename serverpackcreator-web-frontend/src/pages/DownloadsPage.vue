<template>
  <q-page class="q-pa-sm">
    <q-card rounded>
      <ModpacksTable class="table-size"/>
    </q-card>
  </q-page>
</template>

<script >
import { defineComponent } from 'vue';
import ModpacksTable from 'components/ModpacksTable.vue';

export default defineComponent({
  name: 'DownloadsPage',
  components: { ModpacksTable },
  setup() {
    return {

    };
  }
});
</script>

<style scoped>
.table-size {
  height: calc(100vh - 67px);;
  box-sizing: border-box;
}
</style>
