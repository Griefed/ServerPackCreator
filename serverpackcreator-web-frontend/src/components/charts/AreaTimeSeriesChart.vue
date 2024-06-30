<template>
  <apexchart ref="chart1" height="300" type="area" :options="options" :series="series"></apexchart>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import { getCssVar } from 'quasar';
import { stats } from 'boot/axios';

export default defineComponent({
  name: 'AreaTimeSeriesChart',
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
          type: 'area'
        },
        yaxis: {
          labels: {
            formatter: (value) => { return value.toFixed(0) }
          }
        },
        colors: [
          getCssVar('primary'),
          getCssVar('secondary'),
          getCssVar('negative')
        ],
        markers: {
          size: 4,
          hover: {
            sizeOffset: 6
          }
        },
        xaxis: {
          type: 'datetime',
        },
        noData: {
          text: 'No data'
        },
        dataLabels: {
          enabled: true
        },
      },
      series: [{
        name: 'nodatayet',
        data: []
      }]
    };
  },
  mounted() {
    this.uChart()
  },
  methods: {
    uChart: function() {
      stats.get(this.$props.endpoint).then(response => {
        let data = response.data
        let formattedData = data.map((e) => {
          return {
            x: Object.values(e)[1],
            y: Object.values(e)[0]
          }
        })
        this.series = [{
          name: this.$props.seriesName,
          data: formattedData
        }]
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
