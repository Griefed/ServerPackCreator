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
                  :rows="rows"
                  :columns="columns"
                  row-key="name"/>
            </div>
          </q-card-section>
        </q-card>

      </div>
    </q-page>
  </q-intersection>
</template>

<script lang="js">
const columns = [
  { name: 'filename', required: true, label: 'Filename',   align: 'left',   field: row => row.name, format: val => `${val}`,  sortable: false },
  { name: 'created',  required: true, label: 'Created at', align: 'center', field: 'calories',                                sortable: true },
  { name: 'size',     required: true, label: 'Size',                        field: 'fat',                                     sortable: false },
  { name: 'download', required: true, label: 'Download',                    field: 'carbs',                                   sortable: false },
  { name: 'delete',   required: true, label: 'Delete',                      field: 'protein',                                 sortable: false }
]
const rows = [
  {
    name: 'Frozen Yogurt',
    calories: 159,
    fat: 6.0,
    carbs: 24,
    protein: 4.0,
    sodium: 87,
    calcium: '14%',
    iron: '1%'
  },
  {
    name: 'Ice cream sandwich',
    calories: 237,
    fat: 9.0,
    carbs: 37,
    protein: 4.3,
    sodium: 129,
    calcium: '8%',
    iron: '1%'
  },
  {
    name: 'Eclair',
    calories: 262,
    fat: 16.0,
    carbs: 23,
    protein: 6.0,
    sodium: 337,
    calcium: '6%',
    iron: '7%'
  },
  {
    name: 'Cupcake',
    calories: 305,
    fat: 3.7,
    carbs: 67,
    protein: 4.3,
    sodium: 413,
    calcium: '3%',
    iron: '8%'
  },
  {
    name: 'Gingerbread',
    calories: 356,
    fat: 16.0,
    carbs: 49,
    protein: 3.9,
    sodium: 327,
    calcium: '7%',
    iron: '16%'
  },
  {
    name: 'Jelly bean',
    calories: 375,
    fat: 0.0,
    carbs: 94,
    protein: 0.0,
    sodium: 50,
    calcium: '0%',
    iron: '0%'
  },
  {
    name: 'Lollipop',
    calories: 392,
    fat: 0.2,
    carbs: 98,
    protein: 0,
    sodium: 38,
    calcium: '0%',
    iron: '2%'
  },
  {
    name: 'Honeycomb',
    calories: 408,
    fat: 3.2,
    carbs: 87,
    protein: 6.5,
    sodium: 562,
    calcium: '0%',
    iron: '45%'
  },
  {
    name: 'Donut',
    calories: 452,
    fat: 25.0,
    carbs: 51,
    protein: 4.9,
    sodium: 326,
    calcium: '2%',
    iron: '22%'
  },
  {
    name: 'KitKat',
    calories: 518,
    fat: 26.0,
    carbs: 65,
    protein: 7,
    sodium: 54,
    calcium: '12%',
    iron: '6%'
  }
]
export default {
  name: "Downloads",
  setup() {
    return {
      columns,
      rows
    }
  }
}
</script>

<style>

</style>
