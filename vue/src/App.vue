<template>
  <div id="app">
    <game-canvas highlight-color="blue"></game-canvas>
  </div>
</template>

<script>
  import GameCanvas from './GameCanvas'

  export default {
    name: 'app',
    components: {
      'game-canvas': GameCanvas
    }
  }
</script>

<style lang="scss">
  #app {
    height: 100%;
    width: 100%;
    min-height: 100%;
    min-width: 100%;
    background-color: #1D2951;
    overflow: hidden;
  }
</style>
