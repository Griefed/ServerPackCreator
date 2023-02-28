<template>
  <q-intersection
      class="intersection"
      once
      transition="scale">

    <q-card bordered flat>
      <q-card-section>
        <div class="text-h4">Downloads</div>
      </q-card-section>


      <q-card-section>

        <q-table
            :columns="this.columns"
            :filter="filter"
            :pagination="initialPagination"
            :rows="this.rows"
            :visible-columns="visibleColumns"
            no-data-label="Couldn't retrieve server packs.... :("
            no-results-label="The search didn't uncover any results"
            row-key="id"
        >
          <template v-slot:body="props">
            <q-tr :props="props">
              <q-td
                  v-for="col in props.cols"
                  :key="col.name"
                  :props="props"

              >
                      <span v-if="col.field==='confirmedWorking' && props.row.confirmedWorking<0"
                            class="text-red text-bold">
                        {{ col.value }}
                      </span>
                <span v-if="col.field==='confirmedWorking' && props.row.confirmedWorking>0"
                      class="text-green text-bold">
                        {{ col.value }}
                      </span>
                <span v-if="col.field==='confirmedWorking' && props.row.confirmedWorking===0"
                      class="text-bold">
                        {{ col.value }}
                      </span>
                <span v-if="col.field==='size'">
                        {{ col.value }} MB
                      </span>
                <span v-if="col.field!=='confirmedWorking' && col.field!=='size'">
                        {{ col.value }}
                      </span>

                <q-btn v-if="col.field==='confirmedWorking'" :disable="vote"
                       color="positive" dense icon="thumb_up" push round size="sm"
                       style="margin-left: 5px;"
                       @click="works(props.row.id, 'up')">
                  <q-tooltip v-if="!vote" :disable="this.$q.platform.is.mobile">
                    Upvote this pack!
                  </q-tooltip>
                </q-btn>
                <q-btn v-if="col.field==='confirmedWorking'" :disable="vote" color="negative" dense
                       icon="thumb_down" push round size="sm" @click="works(props.row.id, 'down')">
                  <q-tooltip v-if="!vote" :disable="this.$q.platform.is.mobile">
                    Downvote this pack!
                  </q-tooltip>
                </q-btn>

                <q-btn v-if="props.row.status==='Available' && col.field==='status'"
                       v-model="props.row.status" :href="download(props.row.id)" color="info" dense
                       icon="download" round size="sm" style="margin-left: 5px;" type="a"/>

              </q-td>
            </q-tr>
          </template>

          <template v-slot:top-left>
            <q-input v-model="filter" borderless debounce="300" dense placeholder="Search">
              <template v-slot:append>
                <q-icon name="search"/>
                <q-icon v-if="this.filter!==''" name="clear" @click="this.filter=''"/>
              </template>
            </q-input>
          </template>
          <template v-slot:top-right>
            <div v-if="this.$q.screen.gt.xs" class="col">
              <q-toggle v-model="visibleColumns" label="ID" val="id"/>
              <q-toggle v-model="visibleColumns" label="Size" val="size"/>
              <q-toggle v-model="visibleColumns" label="Downloads" val="downloads"/>
              <q-toggle v-model="visibleColumns" label="File name" val="fileName"/>
              <q-toggle v-model="visibleColumns" label="File disk name" val="fileDiskName"/>
              <q-toggle v-model="visibleColumns" label="Created at" val="dateCreated"/>
              <q-toggle v-model="visibleColumns" label="Last modified at" val="lastModified"/>
              <q-toggle v-model="visibleColumns" label="Confirmed working" val="confirmedWorking"/>
            </div>
            <q-select
                v-else
                v-model="visibleColumns"
                :display-value="this.$q.lang.table.columns"
                :options="columns"
                borderless
                dense
                emit-value
                map-options
                multiple
                option-value="name"
                options-dense
                style="min-width: 150px"
            />
          </template>

          <template v-slot:no-data="{ icon, message, filter }">
            <div class="full-width row flex-center text-negative q-gutter-sm">
              <q-icon name="search_off" size="2em"/>
              <span style="font-weight: bold; font-size: 14px;">
                      Well this is sad... {{ message }}
                    </span>
              <q-icon name='sentiment_very_dissatisfied' size="2em"/>
            </div>
          </template>

        </q-table>

      </q-card-section>
    </q-card>

  </q-intersection>
</template>

<script lang="js">
import {api} from "boot/axios";
import {ref} from 'vue';
import {date} from 'quasar';

export default {
  name: "Downloads",
  setup() {
    const columns = [
      {name: 'id', required: false, label: 'ID', align: 'left', field: 'id', sortable: true},
      {
        name: 'projectName',
        required: false,
        label: 'Project',
        align: 'left',
        field: 'projectName',
        sortable: true
      },
      {
        name: 'fileName',
        required: true,
        label: 'Display name',
        align: 'left',
        field: 'fileName',
        sortable: true
      },
      {
        name: 'fileDiskName',
        required: true,
        label: 'File disk name',
        align: 'left',
        field: 'fileDiskName',
        sortable: true
      },
      {
        name: 'size',
        required: true,
        label: 'Size (MB)',
        align: 'right',
        field: 'size',
        sortable: true
      },
      {
        name: 'downloads',
        required: true,
        label: 'Downloads',
        align: 'center',
        field: 'downloads',
        sortable: true
      },
      {
        name: 'dateCreated',
        required: false,
        label: 'Created at',
        align: 'left',
        field: 'dateCreated',
        sortable: true,
        format: val => date.formatDate(val, 'YYYY-MM-DD : HH:mm')
      },
      {
        name: 'lastModified',
        required: true,
        label: 'Last modified at',
        align: 'left',
        field: 'lastModified',
        sortable: true,
        format: val => date.formatDate(val, 'YYYY-MM-DD : HH:mm')
      },
      {
        name: 'confirmedWorking',
        required: false,
        label: 'Confirmed working',
        align: 'center',
        field: 'confirmedWorking',
        sortable: true
      },
      {
        name: 'status',
        required: true,
        label: 'Download',
        align: 'left',
        field: 'status',
        sortable: true
      }
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
      visibleColumns: ref(
          ['fileName', 'fileDiskName', 'size', 'downloads', 'lastModified', 'status']),
      columns,
      rows,
      filter: ref(''),
      vote: ref(false),
      initialPagination: {
        sortBy: 'lastModified',
        descending: true,
        page: 1,
        rowsPerPage: 20
      },
    }
  },
  methods: {
    download(id) {
      return '/api/v1/packs/download/' + id;
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
      this.vote = true;
      setTimeout(() => {
        this.vote = false;
      }, 60000)
    }
  },
  mounted() {
  }
}
</script>

<style>

</style>
