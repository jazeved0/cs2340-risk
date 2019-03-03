<template>
  <div class="gameboard">
    <nav class="navbar fixed-top navbar-dark bg-dark">
      <h1 style="color:white">RISK</h1>
      <a class="navbar-brand" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" target="_blank" rel="noopener">
        <i class="fab fa-github repoImg pl-2"></i>
      </a>
    </nav>
    <div :style="{ paddingTop: navHeight + 'px' }">
      <v-stage :config="configKonva">
        <v-layer>
          <v-circle :config="configCircle">
          </v-circle>
        </v-layer>
      </v-stage>
    </div>
    <player-info-bar>
    </player-info-bar>
  </div>
</template>

<script>
  import PlayerInfoBar from './PlayerInfoBar.vue'
  import VueKonva from 'vue-konva';
  // noinspection ES6UnusedImports
  import Vue from "vue";

  Vue.use(VueKonva);

  export default {
    components: {
      'player-info-bar': PlayerInfoBar
    },
    data() {
      return {
        configKonva: {
          width: 200,
          height: 200
        },
        configCircle: {
          x: 100,
          y: 100,
          radius: 70,
          fill: "red",
          stroke: "black",
          strokeWidth: 4
        },
        navHeight: 62
      };
    }
  }
</script>

<style lang="scss">
  .gameboard {
    background-color: #362a4d;
  }

  img {
    display: block;
    margin-left: auto;
    margin-right: auto;
    width: 75%;
    max-width: 100%;
    max-height: 100%;
    padding: 0;
  }

  footer {
    background-color: #21252b;
    padding: 5%;
    min-height: 10%;
  }
</style>
