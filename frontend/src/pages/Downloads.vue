<template>
  <q-intersection
    class="intersection"
    once
    transition="scale">

        <q-card bordered flat>
          <q-card-section>
            <div class="text-h4">Downloads</div>
          </q-card-section>

          <q-card-section >

              <q-table
                :rows="this.rows"
                :columns="this.columns"
                row-key="id"
                :filter="filter"
                :visible-columns="visibleColumns"
                no-data-label="Couldn't retrieve server packs.... :("
                no-results-label="The search didn't uncover any results"
              >
                <template v-slot:body="props">
                  <q-tr :props="props">
                    <q-td
                      v-for="col in props.cols"
                      :key="col.name"
                      :props="props"

                    >
                      <span v-if="col.field==='confirmedWorking' && props.row.confirmedWorking<0" class="text-red text-bold">
                        {{ col.value }}
                      </span>
                      <span v-if="col.field==='confirmedWorking' && props.row.confirmedWorking>0" class="text-green text-bold">
                        {{ col.value }}
                      </span>
                      <span v-if="col.field==='confirmedWorking' && props.row.confirmedWorking===0" class="text-bold">
                        {{ col.value }}
                      </span>
                      <span v-if="col.field==='size'">
                        {{ col.value }} MB
                      </span>
                      <span v-if="col.field!=='confirmedWorking' && col.field!=='size'">
                        {{ col.value }}
                      </span>

                      <!-- TODO: when column is size, add MB -->

                      <q-btn v-if="col.field==='confirmedWorking'" style="margin-left: 5px;" :disable="vote" push color="positive" icon="thumb_up"   size="sm" round dense @click="works(props.row.id, 'up')">
                        <q-tooltip v-if="!vote" :disable="this.$q.platform.is.mobile">
                          Upvote this pack!
                        </q-tooltip>
                      </q-btn>
                      <q-btn v-if="col.field==='confirmedWorking'" :disable="vote" push color="negative" icon="thumb_down" size="sm" round dense @click="works(props.row.id, 'down')">
                        <q-tooltip v-if="!vote" :disable="this.$q.platform.is.mobile">
                          Downvote this pack!
                        </q-tooltip>
                      </q-btn>

                      <q-btn v-if="props.row.status!=='Queued' && col.field==='status'" style="margin-left: 5px;" v-model="props.row.status" size="sm" color="info" round dense type="a" :href="download(props.row.id)" icon="download" />

                    </q-td>
                  </q-tr>
                </template>

                <template v-slot:top-left>
                  <q-input borderless dense debounce="300" v-model="filter" placeholder="Search">
                    <template v-slot:append>
                      <q-icon name="search" />
                      <q-icon v-if="this.filter!==''" name="clear" @click="this.filter=''"/>
                    </template>
                  </q-input>
                </template>

                <template v-slot:top-right>
                  <div v-if="this.$q.screen.gt.xs" class="col">
                    <q-toggle v-model="visibleColumns" val="id" label="ID" />
                    <q-toggle v-model="visibleColumns" val="projectID" label="Project (ID)" />
                    <q-toggle v-model="visibleColumns" val="fileID" label="File (ID)" />
                    <q-toggle v-model="visibleColumns" val="size" label="Size" />
                    <q-toggle v-model="visibleColumns" val="downloads" label="Downloads" />
                    <q-toggle v-model="visibleColumns" val="fileDiskName" label="File disk name" />
                    <q-toggle v-model="visibleColumns" val="dateCreated" label="Created at" />
                    <q-toggle v-model="visibleColumns" val="lastModified" label="Last modified at" />
                    <q-toggle v-model="visibleColumns" val="confirmedWorking" label="Confirmed working" />
                  </div>
                  <q-select
                    v-else
                    v-model="visibleColumns"
                    multiple
                    borderless
                    dense
                    options-dense
                    :display-value="this.$q.lang.table.columns"
                    emit-value
                    map-options
                    :options="columns"
                    option-value="name"
                    style="min-width: 150px"
                  />
                </template>

                <template v-slot:no-data="{ icon, message, filter }">
                  <div class="full-width row flex-center text-negative q-gutter-sm">
                    <q-icon size="2em" name="search_off" />
                    <span style="font-weight: bold; font-size: 14px;">
                      Well this is sad... {{ message }}
                    </span>
                    <q-icon size="2em" name='sentiment_very_dissatisfied' />
                  </div>
                </template>

              </q-table>

          </q-card-section>
        </q-card>

  </q-intersection>
</template>

<script lang="js">
import { api } from "boot/axios";
import { ref } from 'vue';
import { useQuasar } from 'quasar';

export default {
  name: "Downloads",
  setup() {
    const columns = [
      { name: 'id',               required: false, label: 'ID',                align: 'left',  field: 'id',                sortable: true },
      { name: 'projectID',        required: false, label: 'Project (ID)',      align: 'left',  field: 'projectID',         sortable: true },
      { name: 'fileID',           required: false, label: 'File (ID)',         align: 'left',  field: 'fileID',            sortable: true },
      { name: 'projectName',      required: true,  label: 'Project',           align: 'left',  field: 'projectName',       sortable: true },
      { name: 'fileName',         required: true,  label: 'Display name',      align: 'left',  field: 'fileName',          sortable: true },
      { name: 'fileDiskName',     required: false, label: 'File disk name',    align: 'left',  field: 'fileDiskName',      sortable: true },
      { name: 'size',             required: false, label: 'Size (MB)',         align: 'right',  field: 'size',              sortable: true },
      { name: 'downloads',        required: false, label: 'Downloads',         align: 'center',  field: 'downloads',         sortable: true },
      { name: 'dateCreated',      required: false, label: 'Created at',        align: 'left',  field: 'dateCreated',       sortable: true },
      { name: 'lastModified',     required: false, label: 'Last modified at',  align: 'left',  field: 'lastModified',      sortable: true },
      { name: 'confirmedWorking', required: false, label: 'Confirmed working', align: 'center',  field: 'confirmedWorking',  sortable: true },
      { name: 'status',           required: true,  label: 'Download',          align: 'left',  field: 'status',            sortable: false }
    ];

    let rows = ref([]);

    api.get("/packs/all")
      .then(response => {
        rows.value = response.data;
      })
      .catch(error => {
        console.log("An error occurred trying to fetch all available server packs.");
        console.log(error);
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'An error occurred trying to fetch all available server packs: ' + error
        })
      });

    return {
      visibleColumns: ref([ 'projectName', 'fileName', 'size', 'downloads', 'lastModified', 'confirmedWorking', 'status' ]),
      columns,
      rows,
      filter: ref(''),
      vote: ref(false)
    }
  },
  methods: {
    download(id) {
      return '/api/packs/download/' + id;
    },
    works(id, doesItWork) {
      api.get("/packs/vote/" + id + "," + doesItWork)
      .catch(error => {
        console.log("An error occurred trying to vote for server pack with id: " + id + ".");
        console.log(error);
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'An error occurred trying to vote for server pack with id: ' + id + ". " + error
        })
      })
      this.vote=true;
      setTimeout(() => {
        this.vote=false;
      }, 60000)
    }
  },
  mounted() {
  }
}
</script>

<style>

</style>
