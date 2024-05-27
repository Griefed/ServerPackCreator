<template>
  <q-page class="q-pa-sm">
    <div class="row q-col-gutter-sm q-py-xs">
      <div class="col">
        <q-card>
          <q-card-section>
            <div class="text-h6">Disk Usage(s)</div>
            <DiskUsageBarGraph endpoint="disk" series-name="Disk Usage"></DiskUsageBarGraph>
          </q-card-section>
        </q-card>
      </div>
    </div>
    <div class="row q-col-gutter-sm q-py-xs">
      <div class="col-lg-3 col-md-6 col-sm-12 col-xs-12">
        <q-card>
          <q-card-section>
            <div class="text-h6">Modpack Uploads</div>
            <AreaTimeSeriesChart endpoint="creation/modpacks" series-name="Modpack Uploads"></AreaTimeSeriesChart>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-lg-3 col-md-6 col-sm-12 col-xs-12">
        <q-card>
          <q-card-section>
            <div class="text-h6">Server Pack Creations</div>
            <AreaTimeSeriesChart endpoint="creation/serverpacks" series-name="Server Pack Creations"></AreaTimeSeriesChart>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-lg-3 col-md-6 col-sm-12 col-xs-12">
        <q-card>
          <q-card-section>
            <div class="text-h6">Modpack Downloads</div>
            <AreaTimeSeriesChart endpoint="downloads/modpacks" series-name="Modpack Downloads"></AreaTimeSeriesChart>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-lg-3 col-md-6 col-sm-12 col-xs-12">
        <q-card>
          <q-card-section>
            <div class="text-h6">Server Pack Downloads</div>
            <AreaTimeSeriesChart endpoint="downloads/serverpacks" series-name="Server Pack Downloads"></AreaTimeSeriesChart>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-lg-3 col-md-6 col-sm-12 col-xs-12">
        <q-card>
          <q-card-section>
            <div class="text-h6">Modpacks - Server Packs - Run Configurations</div>
            <ModServerRunPieChart endpoint="packs" ></ModServerRunPieChart>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import AreaTimeSeriesChart from 'components/charts/AreaTimeSeriesChart.vue';
import DiskUsageBarGraph from 'components/charts/DiskUsageBarGraph.vue';
import ModServerRunPieChart from 'components/charts/ModServerRunPieChart.vue';

export default defineComponent({
  name: 'IndexPage',
  components: { ModServerRunPieChart, DiskUsageBarGraph,  AreaTimeSeriesChart },
  setup () {
    return {

    };
  },
  mounted() {},
  methods: {},
});
</script>
