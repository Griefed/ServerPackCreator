<template>
  <q-table :rows="rows" :columns="columns" row-key="id" title="Server Packs" id="serverpacktable" :filter="filter"
           bordered dense no-data-label="No server packs available (yet)..."
           no-results-label="The search didn't uncover any results" :pagination="initialPagination" :loading="visible"
           :visible-columns="visibleColumns">
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
              Click to see the run-configuration used in creating this server pack
            </q-tooltip>
          </q-btn>
        </q-td>
        <q-td v-for="col in props.cols" :key="col.name" :props="props" auto-width>
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
          <span v-else-if="col.name === 'download'">
            <q-btn :to="'/download/serverpack/' + props.row.id" color="info" dense icon="download" round size="sm"
                   @click="props.row.downloads++" v-if="props.row.size > 0">
              <q-tooltip>
                Download server pack
              </q-tooltip>
            </q-btn>
          </span>
          <span v-else>
            {{ col.value }}
          </span>
        </q-td>
      </q-tr>
      <q-tr v-show="props.expand" :props="props">
        <q-td colspan="100%" style="max-width: 90vw;" v-if="props.expand" auto-width>
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

<script >
import {defineComponent, ref} from 'vue';
import {modpacks, serverpacks} from 'boot/axios';
import {date} from 'quasar';
import RunConfigurationCard from 'components/RunConfigurationCard.vue';

const columns = [
  {name: 'id', label: 'ServerPack ID', field: 'id', sortable: true, align: 'left'},
  {name: 'size', label: 'Size', field: 'size', sortable: false, align: 'left'},
  {name: 'fileID', label: 'File ID', field: 'fileID', sortable: false, align: 'left'},
  {name: 'download', label: 'Download', sortable: false, align: 'center'},
  {name: 'downloads', label: 'Downloads', field: 'downloads', sortable: true, align: 'left'},
  {name: 'confirmedWorking', label: 'Confirmed Working', field: 'confirmedWorking', sortable: true, align: 'left'},
  {name: 'sha256', label: 'SHA256 Hash', field: 'sha256', sortable: false, align: 'left'},
  {
    name: 'runConfiguration',
    label: 'RunConfiguration ID',
    field: 'runConfiguration',
    sortable: true,
    align: 'left',
    format: (val) => val.id
  },
  {
    name: 'dateCreated',
    label: 'Date and Time',
    field: 'dateCreated',
    sortable: true,
    align: 'left',
    format: (val) => date.formatDate(val, 'YYYY-MM-DD : HH:mm')
  }
];

export default defineComponent({
  name: 'ServerPacksTable',
  components: {RunConfigurationCard},
  props: {
    id: {
      type: String,
      required: true
    }
  },
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
      visibleColumns: ref([ 'id', 'size', 'download', 'downloads', 'confirmedWorking', 'sha256', 'dateCreated' ]),
      initialPagination: {
        sortBy: 'id',
        descending: true,
        page: 1,
        rowsPerPage: 25
      }
    };
  },
  methods: {
    voteServerPack(id, decision) {
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
      modpacks.get(this.$props.id).then(response => {
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
