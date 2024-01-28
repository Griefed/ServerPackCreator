<template>
  <q-card flat bordered style="max-width: 100vw;" class="relative-position" v-if="visible">
    <q-card-section>
      <transition
        appear
        enter-active-class="animated fadeIn"
        leave-active-class="animated fadeOut"
      >
      </transition>
    </q-card-section>
    <q-inner-loading :showing="visible">
      <q-spinner-gears size="50px" color="accent" />
    </q-inner-loading>
  </q-card>
  <q-table
    v-else
    class="sticky-header-table"
    :rows="rows"
    :columns="columns"
    row-key="id"
    title="Modpacks"
    bordered dense
    style="max-width: 100vw;"
    no-data-label="No modpacks available (yet)..."
    no-results-label="The search didn't uncover any results"
    :pagination="initialPagination">
    <template v-slot:header="props">
      <q-tr :props="props">
        <q-th auto-width>
          <q-btn push size="xs" color="primary" round icon="sync" @click="loadData" />
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
              Click to see available server packs
            </q-tooltip>
          </q-btn>
        </q-td>
        <q-td v-for="col in props.cols" :key="col.name" :props="props">
          <span v-if="col.field === 'name'">
            {{ col.value }}
            <q-btn :href="buildDownloadUrl(props.row.id)" color="info" dense icon="download" round size="sm"
                   style="margin-left: 5px;" type="a">
              <q-tooltip>
                Download modpack
              </q-tooltip>
            </q-btn>
          </span>
          <span v-else>
            {{ col.value }}
          </span>
        </q-td>
      </q-tr>
      <q-tr v-show="props.expand" :props="props">
        <q-td colspan="100%" style="max-width: 100vw;" v-if="props.expand">
          <ServerPacksTable :id="props.row.id" />
        </q-td>
      </q-tr>
    </template>
  </q-table>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import { modpacks } from 'boot/axios';
import { date } from 'quasar';
import ServerPacksTable from 'components/ServerPacksTable.vue';

const columns = [
  { name: 'id',           label: 'Modpack ID',    field: 'id',          sortable: true,   align: 'left' },
  { name: 'name',         label: 'Name',          field: 'name',        sortable: false,  align: 'left' },
  { name: 'projectID',    label: 'Project ID',    field: 'projectID',   sortable: true,   align: 'left' },
  { name: 'versionID',    label: 'Version ID',    field: 'versionID',   sortable: true,   align: 'left' },
  { name: 'source',       label: 'Source',        field: 'source',      sortable: false,  align: 'left' },
  { name: 'status',       label: 'Status',        field: 'status',      sortable: false,  align: 'left' },
  { name: 'size',         label: 'Size',          field: 'size',        sortable: false,  align: 'left' },
  { name: 'serverPacks',  label: 'Server Packs',  field: 'serverPacks', sortable: false,  align: 'left', format: (val: Array<never>) => val.length },
  { name: 'sha256',       label: 'SHA256 Hash',   field: 'sha256',      sortable: false,  align: 'left' },
  { name: 'dateCreated',  label: 'Date and Time', field: 'dateCreated', sortable: true,   align: 'left', format: (val: number) => date.formatDate(val, 'YYYY-MM-DD : HH:mm') }
];

export default defineComponent({
  name: 'ModpacksTable',
  components: { ServerPacksTable },
  setup() {
    const visible = ref(true);
    const showSimulatedReturnData = ref(false);
    return {
      visible,
      showSimulatedReturnData,
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
        sortBy: 'id',
        descending: true,
        page: 1,
        rowsPerPage: 25
        // rowsNumber: xx if getting data from a server
      }
    };
  },
  methods: {
    /*download(id: number) {
      modpacks.get('download/' + id, {
        responseType: 'blob'
      }).then(response => {
        //console.log(response)
        const href = URL.createObjectURL(response.data)
        const link = document.createElement('a')
        const disposition = response.headers['content-disposition']
        const fileName = disposition.replace('attachment; filename="','').replace('"','')
        link.href = href
        link.setAttribute('download',fileName)
        document.body.appendChild(link);
        link.click();

        // clean up "a" element & remove ObjectURL
        document.body.removeChild(link);
        URL.revokeObjectURL(href);
        this.loadData()
      })
    },*/
    buildDownloadUrl(id: number): string {
      return '/api/v2/modpacks/download/' + id
    },
    loadData() {
      modpacks.get('all').then(response => {
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
