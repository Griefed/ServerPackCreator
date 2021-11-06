<template>
  <q-intersection
    class="intersection"
    once
    transition="scale">
    <q-page>
      <div class="col-lg-6 col-md-6 col-sm-12 col-xs-12">

        <q-card bordered flat>
          <q-card-section>
            <div class="text-h4">Downloads</div>
          </q-card-section>

          <q-separator/>

          <q-card-section>
            <div class="q-pa-md">
              <q-table
                :rows="this.rows"
                :columns="this.columns"
                :filter="filter"
                row-key="id"
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
                      {{ col.value }}
                    </q-td>
                    <q-td auto-width>
                      <q-btn
                        v-if="props.row.status!=='Queued'"
                        size="sm" color="info" round dense @click="download(props.row.id, props.row.fileDiskName)" icon="download" />
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
            </div>
          </q-card-section>
        </q-card>

      </div>
    </q-page>
  </q-intersection>
</template>

<script lang="js">
import { api } from "boot/axios";
import { ref } from 'vue';
import { saveAs, FileSaver } from 'file-saver';
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
      { name: 'size',             required: false, label: 'Size',              align: 'left',  field: 'size',              sortable: true },
      { name: 'downloads',        required: false, label: 'Downloads',         align: 'left',  field: 'downloads',         sortable: true },
      { name: 'dateCreated',      required: false, label: 'Created at',        align: 'left',  field: 'dateCreated',       sortable: true },
      { name: 'lastModified',     required: false, label: 'Last modified at',  align: 'left',  field: 'lastModified',      sortable: true },
      { name: 'confirmedWorking', required: false, label: 'Confirmed working', align: 'left',  field: 'confirmedWorking',  sortable: true },
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
    }
  },
  methods: {
    download(id, fileDiskName) {
      api.get('/packs/download/' + id, {responseType: 'blob'})
      .then((response) => {
        console.log(response);
        saveAs(response.data, fileDiskName);
      })
      .catch((error) => {
        console.log("Couldn't download pack with id " + id + ".", error);
      });
    }
  },
  mounted() {
  }
}
</script>

<style>

</style>
