<template>
  <q-table class="sticky-header-table" :rows="rows" :columns="columns" row-key="id" bordered dense :filter="filter"
           no-data-label="No history available (yet)..." title="History / Event Log"
           no-results-label="The search didn't uncover any results"
           :pagination="initialPagination" :loading="visible" :visible-columns="visibleColumns">
    <template v-slot:loading>
      <q-inner-loading showing color="accent"/>
    </template>

    <template v-slot:top-right>
      <q-input borderless dense debounce="300" v-model="filter" placeholder="Search">
        <template v-slot:append>
          <q-icon name="search"/>
        </template>
      </q-input>
      <q-separator inset spaced/>
      <q-select
        v-model="visibleColumns"
        multiple
        outlined
        dense
        options-dense
        :display-value="$q.lang.table.columns"
        emit-value
        map-options
        :options="columns"
        option-value="name"
        options-cover
        style="min-width: 150px"
      >
        <template v-slot:option="{ itemProps, opt, selected, toggleOption }">
          <q-item v-bind="itemProps">
            <q-item-section>
              <q-item-label>{{ opt.label }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-toggle :model-value="selected" @update:model-value="toggleOption(opt)" />
            </q-item-section>
          </q-item>
        </template>
      </q-select>
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
          <span v-if="col.name === 'errors' && col.value > 0" class="text-bold text-red-14">
            {{ col.value }}
          </span>
          <span v-else>
            {{ col.value }}
          </span>
        </q-td>
      </q-tr>
      <q-tr v-show="props.expand" :props="props">
        <q-td colspan="100%" style="max-width: 1100px;" v-if="props.expand" auto-width>
          <div class="row">
            <div class="col">
              <ModPackCard :id="props.row.modPackId"/>
            </div>
            <div class="col" v-if="props.row.serverPackId !== null" style="margin-left: 5px;">
              <ServerPackCard :id="props.row.serverPackId"/>
            </div>
            <div class="col" v-if="props.row.serverPackId !== null" style="margin-left: 5px;">
              <RunConfigurationCard :id="props.row.serverPackId"/>
            </div>
            <div class="col" v-if="props.row.errors.length > 0" style="margin-left: 5px;">
              <ErrorsCard :errors="props.row.errors"/>
            </div>
          </div>
        </q-td>
      </q-tr>
    </template>
  </q-table>
</template>


<script >
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
    format: (val) => date.formatDate(val, 'YYYY-MM-DD : HH:mm')
  },
  {
    name: 'errors',
    label: 'Errors',
    field: 'errors',
    sortable: false,
    align: 'left',
    format: (val) => val.length
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
      visibleColumns: ref([ 'modPackId', 'serverPackId', 'status', 'message', 'timestamp', 'errors' ]),
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
