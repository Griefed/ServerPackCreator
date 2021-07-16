<template>
  <q-layout class="background" view="hHh Lpr lff">
    <q-header class="header" elevated reveal>
      <q-toolbar class="shadow-5 toolbar">

        <q-btn
            class="q-mr-sm"
            dense
            flat
            icon="menu"
            round
            @click="drawer = !drawer"/>

        <q-item to="/">
          <img :ratio="323/54" alt="header" src="~assets/serverpackcreator.webp" width="256">
        </q-item>

        <q-toolbar-title></q-toolbar-title>

        <a href="https://blog.griefed.de" rel="noreferrer noopener" target="_blank">
          <q-btn flat stretch>
            <span class="has-inline-color" style="color:#c0ffee">News</span>
          </q-btn>
        </a>

        <a href="https://github.com/Griefed/ServerPackCreator" rel="noreferrer noopener" target="_blank">
          <q-btn flat stretch>
            <span class="has-inline-color" style="color:#c0ffee">Source</span>
          </q-btn>
        </a>

        <a href="https://github.com/Griefed/ServerPackCreator/issues" rel="noreferrer noopener" target="_blank">
          <q-btn flat stretch>
            <span class="has-inline-color" style="color:#c0ffee">Issues</span>
          </q-btn>
        </a>

        <a href="https://wiki.griefed.de/en/Documentation/ServerPackCreator/HowTo" rel="noreferrer noopener" target="_blank">
          <q-btn flat stretch>
            <span class="has-inline-color" style="color:#c0ffee">Help</span>
          </q-btn>
        </a>

        <a href="https://discord.griefed.de" rel="noreferrer noopener" target="_blank">
          <q-btn flat stretch>
            <span class="has-inline-color" style="color:#c0ffee">Discord</span>
          </q-btn>
        </a>

        <q-btn
          :icon="$q.fullscreen.isActive ? 'fullscreen_exit' : 'fullscreen'"
          class="q-mr-xs"
          dense
          flat
          round
          @click="$q.fullscreen.toggle()">
          <q-tooltip
            v-close-popup
            :disable="$q.platform.is.mobile">
            {{ $q.fullscreen.isActive ? 'Exit Fullscreen' : 'Toggle Fullscreen' }}
          </q-tooltip>
        </q-btn>

      </q-toolbar>
    </q-header>

    <q-drawer
        v-model="drawer"
        :breakpoint="500"
        :width="230"
        bordered
        overlay
        class="left-navigation text-white drawer"
        side="left">
      <div
          class="full-height">
        <q-scroll-area class="fit">
          <q-list padding>

            <q-item
                v-ripple
                active-class="tab-active"
                clickable
                exact
                to="/configuration">
              <q-item-section avatar>
                <q-icon name="mdi-settings-transfer"/>
              </q-item-section>
              <q-item-section>
                Configuration
              </q-item-section>
            </q-item>

            <q-item
                v-ripple
                active-class="tab-active"
                clickable
                to="/downloads">
              <q-item-section avatar>
                <q-icon name="mdi-download"/>
              </q-item-section>
              <q-item-section>
                Downloads
              </q-item-section>
            </q-item>

            <q-item
                v-ripple
                active-class="tab-active"
                clickable
                to="/logs">
              <q-item-section avatar>
                <q-icon name="fas fa-history"/>
              </q-item-section>
              <q-item-section>
                Logs
              </q-item-section>
            </q-item>

            <q-item
                v-ripple
                active-class="tab-active"
                clickable
                to="/about">
              <q-item-section avatar>
                <q-icon name="mdi-information-outline"/>
              </q-item-section>
              <q-item-section>
                About
              </q-item-section>

            </q-item>
          </q-list>
        </q-scroll-area>
      </div>
    </q-drawer>

    <q-page-container>
      <q-page class="row no-wrap">
        <div class="col">
          <div class="full-height full-width">
            <q-scroll-area class="full-height full-width page">
              <router-view/>
            </q-scroll-area>
          </div>
        </div>
      </q-page>

    </q-page-container>

  </q-layout>
</template>

<script>
export default {
  name: 'MainLayout',
  data() {
    return {
      minecraft: false,
      drawer: false
    }
  }
}
</script>

<style>
.toolbar {
  border-bottom: #c0ffee 1px solid;
}

.drawer {
  background-image: url("~assets/tile.webp");
  background-repeat: repeat-y;
}

.page {
  background-image: url("~assets/background.webp");
  background-repeat: repeat;
  background-attachment: fixed;
}

.background {
  background: #fff8eb;
}

.header {
  background: url("~assets/tile.webp");
  background-repeat: repeat-x;

}

.footer {
  border-top: #c0ffee 1px solid;
  background: url("~assets/tile.webp");
  background-repeat: repeat-x;

}

a:link {
  text-decoration: none;
}

a:visited {
  text-decoration: none;
}

a:hover {
  text-decoration: none;
}

a:active {
  text-decoration: none;
}
</style>
