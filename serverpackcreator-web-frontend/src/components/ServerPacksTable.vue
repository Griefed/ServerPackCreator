<template>
  <q-card flat bordered style="max-width: 100vw;" class="relative-position" v-if="visible">
    <q-card-section>
      <transition
        appear
        enter-active-class="animated fadeIn"
        leave-active-class="animated fadeOut">
      </transition>
    </q-card-section>
    <q-inner-loading :showing="visible">
      <q-spinner-gears size="50px" color="accent"/>
    </q-inner-loading>
  </q-card>
  <q-table
    v-else :rows="rows" :columns="columns" row-key="id" title="Server Packs" id="serverpacktable"
    style="max-width: 100%;"
    bordered dense no-data-label="No server packs available (yet)..."
    no-results-label="The search didn't uncover any results" :pagination="initialPagination">
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
              Click to see the run-configuration used in creating this server pack
            </q-tooltip>
          </q-btn>
        </q-td>
        <q-td v-for="col in props.cols" :key="col.name" :props="props">
          <span v-if="col.field==='confirmedWorking'">
            <span v-if="props.row.confirmedWorking < 0" class="text-red text-bold">
              {{ col.value }}
            </span>
            <span v-else-if="props.row.confirmedWorking > 0" class="text-green  text-bold">
              {{ col.value }}
            </span>
            <span v-else class="text-bold">
              {{ col.value }}
            </span>
            <q-btn color="positive" dense icon="thumb_up" push round size="sm" style="margin-left: 5px;"
                   @click="voteServerPack(props.row.id, 'up')">
            <q-tooltip>
              Upvote if this server pack is working.
            </q-tooltip>
            </q-btn>
            <q-btn color="negative" dense icon="thumb_down" push round size="sm" style="margin-left: 5px;"
                   @click="voteServerPack(props.row.id, 'down')">
              <q-tooltip>
                Downvote if this server pack is not working properly.
              </q-tooltip>
            </q-btn>
          </span>
          <span v-else-if="col.field === 'size'">
            {{ col.value }} MB
          </span>
          <span v-else-if="col.field === 'downloads'">
            {{ col.value }}
            <q-btn :href="buildDownloadUrl(props.row.id)" color="info" dense icon="download" round size="sm"
                   style="margin-left: 5px;" type="a" @click="props.row.downloads++">
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
        <q-td colspan="100%" style="max-width: 90vw;" v-if="props.expand">
          <div class="row">
            <div class="col">
              <RunConfigurationCard :id="props.row.runConfiguration.id"/>
            </div>
          </div>
        </q-td>
      </q-tr>
    </template>
  </q-table>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {modpacks, serverpacks} from 'boot/axios';
import {date} from 'quasar';
import RunConfigurationCard from 'components/RunConfigurationCard.vue';

const columns = [
  {name: 'id', label: 'ServerPack ID', field: 'id', sortable: true, align: 'left'},
  {name: 'size', label: 'Size', field: 'size', sortable: false, align: 'left'},
  {name: 'downloads', label: 'Downloads', field: 'downloads', sortable: true, align: 'left'},
  {name: 'confirmedWorking', label: 'Confirmed Working', field: 'confirmedWorking', sortable: true, align: 'left'},
  {name: 'sha256', label: 'SHA256 Hash', field: 'sha256', sortable: false, align: 'left'},
  {
    name: 'runConfiguration',
    label: 'RunConfiguration ID',
    field: 'runConfiguration',
    sortable: true,
    align: 'left',
    format: (val: { id: number; }) => val.id
  },
  {
    name: 'dateCreated',
    label: 'Date and Time',
    field: 'dateCreated',
    sortable: true,
    align: 'left',
    format: (val: number) => date.formatDate(val, 'YYYY-MM-DD : HH:mm')
  }
];

export default defineComponent({
  name: 'ServerPacksTable',
  components: {RunConfigurationCard},
  props: {
    id: {
      type: Number,
      required: true
    }
  },
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
      serverpacks.get('download/' + id, {
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
      return '/api/v2/serverpacks/download/' + id
    },
    voteServerPack(id: number, decision: string) {
      serverpacks.get('vote/' + id + '&' + decision).then(() => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'info',
          color: 'info',
          message: 'Voted server pack ' + id + ' ' + decision + '.'
        });
        this.loadData()
      }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Voting failed: ' + error
        });
      })
    },
    loadData() {
      modpacks.get(this.$props.id.toString()).then(response => {
        this.rows = [];
        this.rows = response.data.serverPacks;
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
          message: 'Could not retrieve server packs: ' + error
        });
      });
    }
  },
  mounted() {
    this.loadData();
  }
});
</script>
