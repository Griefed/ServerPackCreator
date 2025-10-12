<template>
  <q-page class="items-center justify-center">
    <div class="row" style="height: 40vh; max-height: 40vh">
      <q-card class="col card-margins" style="max-width: 40vw; height: 40vh">
        <q-scroll-area style="max-width: 40vw; height: 40vh" :thumb-style="thumbStyle"
                       :bar-style="barStyle">
          <SubmitModPackForm/>
        </q-scroll-area>
      </q-card>

      <q-card class="col card-margins" style="max-width: 60vw; height: 40vh">
        <HistoryTable class="table-size" style="max-width: 60vw; height: 40vh"/>
      </q-card>
    </div>
    <q-separator inset spaced/>
    <div class="row card-margins" style="height: 54vh; max-height: 54vh">
      <ModpacksTable style="max-height: 54vh; max-width: 100vw; width: 100%;"/>
    </div>
  </q-page>
</template>

<script >
import { defineComponent } from 'vue';
import HistoryTable from 'components/HistoryTable.vue';
import SubmitModPackForm from 'components/SubmitModPackForm.vue';
import ModpacksTable from 'components/ModpacksTable.vue';

export default defineComponent({
  name: 'SubmissionPage',
  components: { ModpacksTable, SubmitModPackForm, HistoryTable },
  setup() {
    return {
      thumbStyle: {
        right: '4px',
        borderRadius: '5px',
        backgroundColor: '#325358',
        width: '5px',
        opacity: 0.75
      },
      barStyle: {
        right: '2px',
        borderRadius: '9px',
        backgroundColor: '#54A896',
        width: '9px',
        opacity: 0.2
      }
    };
  }
});
</script>

<style scoped>
</style>
