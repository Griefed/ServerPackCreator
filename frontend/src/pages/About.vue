<template>
  <q-page>
    <div class="col-lg-6 col-md-6 col-sm-12 col-xs-12">
      <q-card bordered flat>
        <q-card-section horizontal>
          <q-card-section class="q-pt-xs">
            <div class="text-h5 q-mt-sm q-mb-xs">About ServerPackCreator</div>
            <div class="text-caption">
              <br>
              ServerPackCreator is a Java-program which creates a server pack from any given Forge or Fabric
              modpack.<br>
              Customize the configuration to your liking and off you go.<br>
              Whenever you are working on an update to your modpack, you simply run ServerPackCreator and BAM!<br>
              You've got yourself a server pack for your new modpack version.
            </div>
          </q-card-section>

          <q-card-section class="col-5 flex flex-center">
            <q-img
                class="rounded-borders"
                height="256px"
                src="~assets/app.png"
                width="256px"
            />
          </q-card-section>
        </q-card-section>

        <q-separator/>

        <q-card-section>
          <p>
            <strong>ServerPackCreator</strong> was made as a training ground for Java and because I wanted an easier way
            to update/make server packs for updates to my modpack.<br>
            It has grown substantially since the first release, which was when it was CLI only.<br>
            Now it provides a GUI as well!
          </p>
          <p>
            The fact that this is, and always will be, a playground for me, in Java, still remains though.<br>
            As with every application, bugs are bound to appear, and I will try to fix them whenever I can.<br>
            I am working on this program in my spare time, please keep that in mind when reporting issues or requesting
            new features to be added.
          </p>
          <p>
            This project would not be where it is today without the help from my contributors:
          </p>
          <p><strong>
            Whitebear60<br>
          </strong></p>
        </q-card-section>
      </q-card>
    </div>
    <div class="col-lg-6 col-md-6 col-sm-12 col-xs-12">
      <div class="q-gutter-md">
        <q-card>
          <q-field filled label="GitHub" square stack-label>
            <template v-slot:control>
              <div class="self-center full-width no-outline" tabindex="0">
                https://github.com/Griefed/ServerPackCreator
              </div>
            </template>
            <template v-slot:append>
              <q-btn icon="fab fa-github"
                     round style="background:radial-gradient(circle, #325358 0%, #45a395 100%);color: floralwhite;"/>
              <!--@click="const {openURL} = require('quasar');openURL('https://github.com/Griefed/ServerPackCreator')"-->

            </template>
          </q-field>

          <q-field filled label="Issues" square stack-label>
            <template v-slot:control>
              <div class="self-center full-width no-outline" tabindex="0">
                https://github.com/Griefed/ServerPackCreator/issues
              </div>
            </template>
            <template v-slot:append>
              <q-btn icon="fas fa-bug"
                     round style="background:radial-gradient(circle, #325358 0%, #45a395 100%); color: floralwhite;"/>
              <!--@click="const {openURL} = require('quasar');openURL('https://github.com/Griefed/ServerPackCreator/issues')"-->

            </template>
          </q-field>

          <q-field filled label="Chat and Support" square stack-label>
            <template v-slot:control>
              <div class="self-center full-width no-outline" tabindex="0">
                https://discord.griefed.de
              </div>
            </template>
            <template v-slot:append>
              <q-btn icon="fab fa-discord"
                     round style="background:radial-gradient(circle, #325358 0%, #45a395 100%); color: floralwhite;"/>
              <!--@click="const {openURL} = require('quasar');openURL('https://discord.griefed.de')"-->

            </template>
          </q-field>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script>
export default {
  name: "Configuration.vue",
  data() {
    return {}
  },
}
</script>

<style>

</style>
