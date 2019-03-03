<template>
  <div class="gameboard">
    <nav class="navbar fixed-top navbar-dark bg-dark">
      <h1 style="color:white">RISK</h1>
      <a class="navbar-brand" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" target="_blank" rel="noopener">
        <i class="fab fa-github repoImg pl-2"></i>
      </a>
    </nav>
    <img src="https://derekmeetsworld.files.wordpress.com/2016/12/world-domination-game-board-2-1-1.jpg">
    <player-info-bar>
    </player-info-bar>
  </div>
</template>

<script>
  import PlayerInfoBar from './PlayerInfoBar.vue'

  export default {
    components: {
      'player-info-bar': PlayerInfoBar
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
