<template>
  <div id="tsparticles"/>
  <q-layout view="hHh Lpr lff">
    <q-header class="header" elevated reveal>
      <q-toolbar :class="this.$q.platform.is.mobile ? 'shadow-5 toolbar scroll ' : 'shadow-5 toolbar'">

        <q-btn
          class="q-mr-sm"
          dense
          flat
          icon="menu"
          round
          v-if="!this.$q.platform.is.mobile"
          @click="miniState = !miniState"/>

        <q-btn
          class="q-mr-sm"
          dense
          flat
          icon="menu"
          round
          v-if="this.$q.platform.is.mobile"
          @click="drawer = !drawer"/>

        <q-item to="/" v-if="!this.$q.platform.is.mobile">
          <q-img alt="header" src="~assets/serverpackcreator.webp" :ratio="323/54" :width="this.$q.platform.is.mobile ? '256px' : '256px'" fit="contain"/>
        </q-item>

        <q-item class="flex-center"><b>Version {{version}}</b></q-item>

        <q-toolbar-title></q-toolbar-title>

        <q-btn v-if="!this.$q.platform.is.mobile" label="News" style="color: #C0FFEE" type="a" target="_blank" href="https://blog.griefed.de">
          <q-tooltip>
            Visit my blog!
          </q-tooltip>
        </q-btn>

        <q-btn v-if="!this.$q.platform.is.mobile" label="GitHub" style="color: #C0FFEE" type="a" target="_blank" href="https://github.com/Griefed/ServerPackCreator">
          <q-tooltip>
            Visit the project on GitHub!
          </q-tooltip>
        </q-btn>

        <q-btn v-if="!this.$q.platform.is.mobile" label="Support" style="color: #C0FFEE" type="a" target="_blank" href="https://github.com/Griefed/ServerPackCreator/issues">
          <q-tooltip>
            Report an issue!
          </q-tooltip>
        </q-btn>

        <q-btn v-if="!this.$q.platform.is.mobile" label="Discord" style="color: #C0FFEE" type="a" target="_blank" href="https://discord.griefed.de">
          <q-tooltip>
            Chat and support on Discord!
          </q-tooltip>
        </q-btn>

        <q-btn-dropdown label="Info" v-if="this.$q.platform.is.mobile">
          <q-item>
            <q-btn label="News" flat type="a" target="_blank" href="https://blog.griefed.de">
            </q-btn>
          </q-item>
          <q-item>
            <q-btn label="GitHub" flat type="a" target="_blank" href="https://github.com/Griefed/ServerPackCreator">
            </q-btn>
          </q-item>
          <q-item>
            <q-btn label="Support" flat type="a" target="_blank" href="https://github.com/Griefed/ServerPackCreator/issues">
            </q-btn>
          </q-item>
          <q-item>
            <q-btn label="Discord" flat type="a" target="_blank" href="https://discord.griefed.de">
            </q-btn>
          </q-item>
        </q-btn-dropdown>

        <q-btn
            :icon="this.$q.dark.isActive ? 'nights_stay' : 'wb_sunny'"
            class="q-mr-xs"
            dense
            @click="toggleDarkMode()">
          <q-tooltip :disable="this.$q.platform.is.mobile">
            {{ this.$q.dark.isActive ? 'Deactivate Dark Mode' : 'Activate Dark Mode' }}
          </q-tooltip>
        </q-btn>

        <q-btn
            :icon="this.$q.fullscreen.isActive ? 'fullscreen_exit' : 'fullscreen'"
            class="q-mr-xs"
            dense
            v-if="!this.$q.platform.is.mobile"
            @click="this.$q.fullscreen.toggle()">
          <q-tooltip :disable="this.$q.platform.is.mobile">
            {{ this.$q.fullscreen.isActive ? 'Exit Fullscreen' : 'Toggle Fullscreen' }}
          </q-tooltip>
        </q-btn>

      </q-toolbar>
    </q-header>

    <q-drawer
        v-model="drawer"
        :breakpoint="500"
        :width="this.$q.platform.is.mobile ? 165 : 200"
        bordered
        :mini="miniState"
        mini-to-overlay
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
              to="/request">
              <q-item-section avatar>
                <q-icon name="mdi-webpack"/>
              </q-item-section>
              <q-item-section>
                Request Server Pack
              </q-item-section>
            </q-item>

            <q-item
                v-ripple
                active-class="tab-active"
                clickable
                to="/downloads">
              <q-item-section avatar>
                <q-icon name="mdi-content-save"/>
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
              <div
                  id="particles-js"
                  :class="this.$q.dark.isActive ? 'dark_gradient' : 'normal_gradient'"
              ></div>
              <router-view/>
            </q-scroll-area>
          </div>
        </div>
      </q-page>
    </q-page-container>

  </q-layout>
</template>

<script>
import { defineComponent, ref } from 'vue';
import { useQuasar, Cookies } from 'quasar';
import { tsParticles } from 'tsparticles';
import {api} from "boot/axios";

export default defineComponent({
  name: 'MainLayout',
  data() {
    return {
      drawer: ref(true),
      miniState: ref(true),
      version: ref("dev")
    }
  },
  methods : {
    toggleDarkMode() {
      this.$q.dark.toggle();
      this.$q.cookies.set('dark.isActive', this.$q.dark.isActive)
    }
  },
  mounted() {
    api.get("/settings").then(response => {this.version = response.data.serverPackCreatorVersion});
    this.$q.platform.is.mobile ? this.drawer = false : this.drawer = true;
    this.$q.dark.set(this.$q.cookies.get('dark.isActive'));
    tsParticles.load("particles-js",{
      "fpsLimit": 30,
      "particles": {
        "number": {
          "value": 50,
          "density": {
            "enable": true,
            "value_area": 800
          }
        },
        "color": {
          "value": ["#325358","#C0FFEE","#31CCEC","#6A1A78"]
        },
        "shape": {
          "type": ["circle","triangle","edge","polygon"],
          "stroke": {
            "width": 0,
            "color": ["#325358","#C0FFEE","#31CCEC","#6A1A78"]
          },
          "polygon": {
            "nb_sides": 6
          }
        },
        "opacity": {
          "value": 1,
          "random": true,
          "anim": {
            "enable": true,
            "speed": 1,
            "opacity_min": 0.1,
            "sync": false
          }
        },
        "size": {
          "value": 3.5,
          "random": true,
          "anim": {
            "enable": true,
            "speed": 1,
            "size_min": 0.1,
            "sync": false
          }
        },
        "links": {
          "enable": true,
          "distance": 150,
          "color": "#C0FFEE",
          "opacity": 0.4,
          "width": 1
        },
        "move": {
          "enable": true,
          "speed": 1.5,
          "direction": "right",
          "random": true,
          "straight": false,
          "outModes": {
            "default": "out",
            "bottom": "out",
            "left": "out",
            "right": "out",
            "top": "out"
          },
          "bounce": false
        },
      },
      "interactivity": {
        "detect_on": "canvas",
        "events": {
          "onhover": {
            "enable": true,
            "mode": ["bubble","grab"]
          },
          "onclick": {
            "enable": true,
            "mode": "push"
          },
          "resize": true
        },
        "modes": {
          "grab": {
            "distance": 140,
            "line_linked": {
              "opacity": 1
            }
          },
          "bubble": {
            "distance": 200,
            "size": 4,
            "duration": 5,
            "opacity": 1,
            "speed": 0.1
          },
          "push": {
            "particles_nb": 4
          }
        }
      },
      "retina_detect": true
    });
  }
})
</script>

<style>
#particles-js {
  position: absolute;
  width: 100%;
  height: 100%;
  background-repeat: no-repeat;
  background-size: cover;
  background-position: 50% 50%;
}

.normal_gradient {
  background:
      radial-gradient(circle at 0% 0%,
      rgba(161, 232, 213, 0.4),
        rgba(50, 83, 88, 0.1),
        rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 100% 0%,
      rgba(133, 213, 212, 0.4),
        rgba(50, 83, 88, 0.1),
        rgba(50, 83, 88, 0.1) 100%
      ),
      radial-gradient(circle at 0% 100%,
      rgba(197, 142, 248, 0.4),
        rgba(50, 83, 88, 0.1),
        rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 100% 100%,
      rgba(171, 115, 224, 0.56),
        rgba(50, 83, 88, 0.1),
        rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 50% 50%,
      rgba(143, 147, 196, 0.4),
        rgba(50, 83, 88, 0.1),
        rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 25% 50%,
      rgba(96, 168, 151, 0.9),
        rgba(50, 83, 88, 0),
        rgba(50, 83, 88, 0) 100%
      ),
      radial-gradient(circle at 75% 50%,
      rgba(107, 67, 190, 0.9),
        rgba(50, 83, 88, 0),
        rgba(50, 83, 88, 0) 100%
      ),
      radial-gradient(circle at 50% 25%,
      rgba(97, 166, 176, 0.9),
        rgba(50, 83, 88, 0),
        rgba(50, 83, 88, 0) 100%
      ),
      radial-gradient(circle at 50% 75%,
      rgba(137, 200, 210, 0.9),
        rgba(50, 83, 88, 0),
        rgba(50, 83, 88, 0) 100%
      );
}

.dark_gradient {
  background:
      radial-gradient(circle at 0% 0%,
      rgba(34, 81, 114, 0.6),
      rgba(50, 83, 88, 0.1),
      rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 100% 0%,
      rgba(17, 87, 85, 0.4),
      rgba(50, 83, 88, 0.1),
      rgba(50, 83, 88, 0.1) 100%
      ),
      radial-gradient(circle at 0% 100%,
      rgb(49, 26, 133),
      rgba(50, 83, 88, 0.1),
      rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 100% 100%,
      rgba(80, 20, 136, 0.6),
      rgba(50, 83, 88, 0.1),
      rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 50% 50%,
      rgba(128, 134, 204, 0.4),
      rgba(50, 83, 88, 0.1),
      rgba(50, 83, 88, 0.05) 100%
      ),
      radial-gradient(circle at 25% 50%,
      rgba(66, 117, 105, 0.9),
      rgba(50, 83, 88, 0),
      rgba(50, 83, 88, 0) 100%
      ),
      radial-gradient(circle at 75% 50%,
      rgba(98, 69, 157, 0.9),
      rgba(50, 83, 88, 0),
      rgba(50, 83, 88, 0) 100%
      ),
      radial-gradient(circle at 50% 25%,
      rgba(26, 79, 87, 0.9),
      rgba(50, 83, 88, 0),
      rgba(50, 83, 88, 0) 100%
      ),
      radial-gradient(circle at 50% 75%,
      rgba(18, 35, 89, 1),
      rgba(50, 83, 88, 0),
      rgba(50, 83, 88, 0) 100%
      );
}

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

.header {
  background: url("~assets/tile.webp") repeat-x;
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
