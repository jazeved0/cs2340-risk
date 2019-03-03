<template>
  <div class="wrapper">
    <div v-for="player in this.$store.getters.playerStates" :key='player.name' :style='{color: player.color, opacity: player.currentTurn ? 1 : 0.7, flex: player.currentTurn ? 2 : 1, order: player.turnOrder}'>
      <span v-if="player.currentTurn"> It is {{player.name}}'s turn</span> <br>
      {{player.turnOrder}} <br>
      {{player.name}} <br>
      Number of Armies: {{player.armies}} <br>
    </div>
  </div>
</template>
<script>
  export default {}
</script>
<style lang="scss">
  .wrapper {
    display: flex;
    font-family: 'Roboto', sans-serif;
    font-size: 1.4em;
  }

  /* .wrapper > div:nth-child(even) {
      background: repeating-linear-gradient(
      45deg,
      #606dbc,
      #606dbc 10px,
      #465298 10px,
      #465298 20px
      );
  } */

  .wrapper div {
    background-color: #BBBBBB;
    text-align: center;
    border: 10px double #d9b34f;
  }

  .wrapper span {
    font-size: 1.5em;
  }
</style>
