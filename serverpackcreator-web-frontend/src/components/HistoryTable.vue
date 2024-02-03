<template>
  <q-card flat bordered style="max-width: 100vw;" class="relative-position" v-if="visible">
    <q-card-section>
      <transition appear enter-active-class="animated fadeIn" leave-active-class="animated fadeOut"/>
    </q-card-section>
    <q-inner-loading :showing="visible">
      <q-spinner-gears size="50px" color="accent"/>
    </q-inner-loading>
  </q-card>
  <q-table
    v-else class="sticky-header-table" :rows="rows" :columns="columns" row-key="id" bordered dense :filter="filter"
    style="max-width: 100vw;" no-data-label="No history available (yet)..."
    no-results-label="The search didn't uncover any results"
    :pagination="initialPagination">
    <template v-slot:top-right>
      <q-input borderless dense debounce="300" v-model="filter" placeholder="Search">
        <template v-slot:append>
          <q-icon name="search"/>
        </template>
      </q-input>
    </template>

    <template v-slot:header="props">
      <q-tr :props="props">
        <q-th auto-width>
          <q-btn push size="xs" color="primary" round icon="sync" @click="loadData"/>
        </q-th>
        <q-th v-for="col in props.cols" :key="col.name" :props="props">
          <strong>{{ col.label }}</strong>
        </q-th>
      </q-tr>
    </template>

    <template v-slot:body="props">
      <q-tr :props="props">
        <q-td auto-width>
          <q-btn size="sm" color="primary" round dense @click="props.expand = !props.expand"
                 :icon="props.expand ? 'remove' : 'add'">
            <q-tooltip>
              Click to see details
            </q-tooltip>
          </q-btn>
        </q-td>
        <q-td v-for="col in props.cols" :key="col.name" :props="props">
          {{ col.value }}
        </q-td>
      </q-tr>
      <q-tr v-show="props.expand" :props="props">
        <q-td colspan="100%" style="max-width: 1100px;" v-if="props.expand" auto-width>
          <div class="row">
            <div class="col">
              <ModPackCard :id="props.row.modPackId"/>
            </div>
            <q-separator spaced inset v-if="props.row.serverPackId !== null"/>
            <div class="col" v-if="props.row.serverPackId !== null">
              <ServerPackCard :id="props.row.serverPackId"/>
            </div>
            <q-separator spaced inset v-if="props.row.serverPackId !== null"/>
            <div class="col" v-if="props.row.serverPackId !== null">
              <RunConfigurationCard :id="props.row.serverPackId"/>
            </div>
            <q-separator spaced inset v-if="props.row.errors.length > 0"/>
            <div class="col" v-if="props.row.errors.length > 0">
              <ErrorsCard :errors="props.row.errors"/>
            </div>
          </div>
        </q-td>
      </q-tr>
    </template>
  </q-table>
</template>


<script lang="ts">
import {defineComponent, ref} from 'vue';
import {events} from 'boot/axios';
import {date} from 'quasar';
import ModPackCard from 'components/ModPackCard.vue';
import ServerPackCard from 'components/ServerPackCard.vue';
import RunConfigurationCard from 'components/RunConfigurationCard.vue';
import ErrorsCard from 'components/ErrorsCard.vue';

const columns = [
  {name: 'modPackId', label: 'Modpack ID', field: 'modPackId', sortable: true, align: 'left'},
  {name: 'serverPackId', label: 'Server Pack ID', field: 'serverPackId', sortable: true, align: 'left'},
  {name: 'status', label: 'Status', field: 'status', sortable: true, align: 'left'},
  {name: 'message', label: 'Message', field: 'message', sortable: false, align: 'left'},
  {
    name: 'timestamp',
    label: 'Date and Time',
    field: 'timestamp',
    sortable: true,
    align: 'left',
    format: (val: number) => date.formatDate(val, 'YYYY-MM-DD : HH:mm')
  },
  {
    name: 'errors',
    label: 'Errors',
    field: 'errors',
    sortable: false,
    align: 'left',
    format: (val: string[]) => val.length
  }
];

export default defineComponent({
  name: 'HistoryTable',
  components: {ErrorsCard, RunConfigurationCard, ServerPackCard, ModPackCard},
  setup() {
    const visible = ref(true);
    const showSimulatedReturnData = ref(false);
    const filter = ref('');
    return {
      visible,
      showSimulatedReturnData,
      filter,
      showTextLoading() {
        visible.value = true;
        showSimulatedReturnData.value = false;

        setTimeout(() => {
          visible.value = false;
          showSimulatedReturnData.value = true;
        }, 3000);
      },
      rows: ref([]),
      columns,
      initialPagination: {
        sortBy: 'timestamp',
        descending: true,
        page: 1,
        rowsPerPage: 25
        // rowsNumber: xx if getting data from a server
      }
    };
  },
  methods: {
    loadData() {
      events.get('all').then(response => {
        this.rows = [];
        this.rows = response.data;
        this.visible = false;
        this.showSimulatedReturnData = true;
      }).catch(error => {
        this.visible = false;
        this.showSimulatedReturnData = true;
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Could not retrieve event history: ' + error
        });
      });
    }
  },
  mounted() {
    this.loadData();
  }
});
</script>
