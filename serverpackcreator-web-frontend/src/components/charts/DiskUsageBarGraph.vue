<template>
  <apexchart ref="chart1" height="400" type="bar" :options="options" :series="series"></apexchart>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import { getCssVar } from 'quasar';
import { stats } from 'boot/axios';

export default defineComponent({
  name: 'DiskUsageBarGraph',
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
          type: 'bar',
          stacked: true,
          stackType: '100%'
        },
        xaxis: {
          categories: [
            'Home',
            'ModPacks',
            'Server Packs',
            'Properties',
            'Configs',
            'Server Files',
            'Icons',
            'Plugins',
            'Plugin Configs',
            'Manifests',
            'Minecraft Server Manifests',
            'Installer Cache',
            'Logs',
            'Tomcat Base',
            'Tomcat Logs',
            'Work',
            'Temp'
          ],
        },
        fill: {
          opacity: 1
        },
        legend: {
          position: 'right',
          offsetX: 0,
          offsetY: 50
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
        yaxis: {
          labels: {
            formatter: (value) => { return value.toFixed(0) + '%' }
          }
        },
        tooltip: {
          y: {
            formatter: (value) => { return value.toFixed(2) + ' GB' }
          }
        },
        responsive: [{
          breakpoint: 480,
          options: {
            legend: {
              position: 'bottom',
              offsetX: -10,
              offsetY: 0
            }
          }
        }],
      },
      series: []
    };
  },
  mounted() {
    this.uChart()
  },
  methods: {
    uChart: function() {
      stats.get(this.$props.endpoint).then(response => {
        let data = response.data
        let freeSpace = []
        let usedSpace = []
        let usedBySPC = []
        for (let disk of data) {
          freeSpace.push(disk.freeSpace / (1024**3))
          usedSpace.push((disk.totalSpace - disk.freeSpace - disk.usedBySPC) / (1024**3))
          usedBySPC.push(disk.usedBySPC / (1024**3))
        }
        let freeSeries = {
          name: 'Free Disk Space',
          data: freeSpace
        }
        let usedSeries = {
          name: 'Used Disk Space',
          data: usedSpace
        }
        let usageSeries = {
          name: 'Used by SPC',
          data: usedBySPC
        }
        this.series = [freeSeries, usedSeries, usageSeries]
        this.$refs.chart1.updateSeries(this.series, true);
      })
    }
  },
});
</script>


<style scoped>

</style>
