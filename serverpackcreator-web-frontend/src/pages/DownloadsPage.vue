<template>
  <q-page class="col items-center justify-evenly">
    <q-card square>
      <ModpacksTable class="table-size"/>
    </q-card>
  </q-page>
</template>

<script lang="ts">
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
  height: calc(100vh - 53px);;
  box-sizing: border-box;
}
</style>
