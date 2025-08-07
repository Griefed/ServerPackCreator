<template>
  <q-page>
    <div class="col-lg-6 col-md-6 col-sm-12 col-xs-12">
      <q-card bordered flat>
        <q-card-section horizontal>
          <q-card-section class="q-pt-xs">
            <div class="text-h5 q-mt-sm q-mb-xs">About ServerPackCreator</div>
            <div class="text-caption">
              <br>
              ServerPackCreator creates a server pack from any given Forge, Fabric, Quilt, LegacyFabric and NeoForge modpack.<br>
              <br>
              Whenever you are working on an update to your modpack, you simply run ServerPackCreator and BAM! You've got yourself a
              server pack for your new modpack version.<br>
            </div>
          </q-card-section>
          <q-card-section class="col-5 flex flex-center" style="background: radial-gradient(circle, rgba(144,144,144,1) 0%, rgba(255,255,255,1) 60%);">
            <q-img
              class="rounded-borders"
              height="256px"
              src="~assets/icon.svg"
              width="256px"
            />
          </q-card-section>
        </q-card-section>

        <q-separator/>

        <q-card-section>
          <p>
            ServerPackCreator is a passion project on which I am working in my spare time. It is also the main reason for why I got into programming in the first place.<br>
            Patience with issues and support is appreciated!<br>
            <br>
            I started getting into Java programming in February 2021, when I was working on an update for one of my Minecraft modpacks,<br>
            Survive Create Prosper 4, and got tired and bored with manually creating a server pack everytime I wanted to update and<br>
            test something, or just release a new server pack for the new modpack version.<br>
            <br>
            The thought <strong>"There must be a way to automate this, or at least make it less tedious..."</strong> was the spark which sent me off into the world of programming. And here we are.
          </p>
          <p>
            You are still expected to be knowledgeable about your modpack, server packs in general, server administration and managing your Java installations.<br>
            ServerPackCreator is not intended to take all the work off your shoulders!<br>
            <br>
            <strong>When using alpha, beta or in-dev version of ServerPackCreator, please keep in mind:</strong><br>
          </p>
          <ul>
            <li>Things will break with alpha releases, stuff may break when using beta releases.</li>
            <li>If you distribute server packs generated with a pre-release (alpha, beta) of ServerPackCreator, you do so at your own risk.</li>
            <li>I will not be held responsible for errors in your server pack caused by you using a pre-release.</li>
            <li>I will not be held responsible for errors in your server pack in general. <strong>Test your server packs before you ship them!</strong></li>
            <li><strong>TL;DR </strong>Don't use test to ship to prod! Test before shipping!</li>
          </ul>
        </q-card-section>
      </q-card>
    </div>

    <div class="col-lg-6 col-md-6 col-sm-12 col-xs-12">
      <div class="q-gutter-md">
        <q-card>
          <AboutItem
            v-for="item in aboutItems"
            :key="item.title"
            v-bind="item"
          />
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script >
import { defineComponent } from 'vue';
import AboutItem from 'components/AboutItem.vue';

const aboutItems = [
  {
    title: 'Source',
    link: 'https://github.com/Griefed/ServerPackCreator',
    icon: 'account_tree'
  },
  {
    title: 'Bug Reports',
    link: 'https://github.com/Griefed/ServerPackCreator/issues',
    icon: 'bug_report'
  },
  {
    title: 'Discord',
    link: 'https://discord.griefed.de',
    icon: 'chat'
  }
]

export default defineComponent({
  name: 'AboutPage',
  components: { AboutItem },
  setup () {
    return {
      aboutItems: aboutItems
    };
  }
})
</script>
