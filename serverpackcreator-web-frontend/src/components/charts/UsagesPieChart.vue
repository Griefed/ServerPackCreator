<template>
  <apexchart ref="chart2" height="300" type="donut" :options="options" :series="series"></apexchart>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import { getCssVar } from 'quasar';
import { stats } from 'boot/axios';

export default defineComponent({
  name: 'UsagesPieChart',
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
                show: true
              }
            }
          }
        },
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
      series: [0, 0]
    };
  },
  mounted() {
    this.uChart()
  },
  methods: {
    uChart: function() {
      stats.get(this.$props.endpoint).then(response => {
        let data = response.data
        const objectToMap = obj => new Map(Object.entries(obj));
        let loaders = objectToMap(data.modloaders)
        let mcv = objectToMap(data.minecraftVersions)
        let mlv = objectToMap(data.modloaderVersions)

        let labels = []
        let series = []

        for (let [key, value] of loaders) {
          labels.push(key)
          series.push(value)
        }
        for (let [key, value] of mcv) {
          labels.push(key)
          series.push(value)
        }
        for (let [key, value] of mlv) {
          labels.push(key)
          series.push(value)
        }

        this.$refs.chart2.updateOptions({
          labels: labels,
          series: series
        })

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
