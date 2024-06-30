<template>
  <apexchart ref="chart1" height="300" type="donut" :options="options" :series="series"></apexchart>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import { getCssVar } from 'quasar';
import { stats } from 'boot/axios';

export default defineComponent({
  name: 'ModServerRunPieChart',
  components: {},
  props: {
    endpoint: String,
    title: String,
    seriesName: String
  },
  setup () {
    return {
      options: {
        chart: {
          type: 'donut'
        },
        plotOptions: {
          pie: {
            donut: {
              labels: {
                show: true,
                total: {
                  label: 'Total',
                  show: true,
                  showAlways: true,
                  formatter: function (w) {
                    return w.globals.series[0]
                      + w.globals.series[1]
                      + w.globals.series[2]
                  }
                }
              }
            }
          }
        },
        labels: [
          'Modpacks',
          'Server Packs',
          'Run Configurations'
        ],
        colors: [
          getCssVar('primary'),
          getCssVar('secondary'),
          getCssVar('negative')
        ],
        noData: {
          text: 'No data'
        },
        dataLabels: {
          enabled: true
        },
        responsive: [{
          breakpoint: 480,
          options: {
            chart: {
              width: 200
            },
            legend: {
              position: 'bottom'
            }
          }
        }]
      },
      series: [0, 0, 0]
    };
  },
  mounted() {
    this.uChart()
  },
  methods: {
    uChart: function() {
      stats.get(this.$props.endpoint).then(response => {
        let data = response.data
        this.series = [data.modPacks, data.serverPacks, data.runConfigurations]
        this.$refs.chart1.updateSeries(this.series, true);
      }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Could not retrieve data: ' + error
        });
      })
    }
  },
});
</script>


<style scoped>

</style>
