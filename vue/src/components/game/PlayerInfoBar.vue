<template>
  <div class="d-flex outer-info-bar" style="width: 100%">
    <div class="flex-fill d-flex flex-row-reverse">
      <div v-for="player in this.$store.getters.playerStates"
          :key='player.name'
          class="info-card frosted-glass-dark">
        <div class="d-flex">
          <fa-icon class="color" icon="circle" v-bind:style="{ color: player.color }"></fa-icon>
          <p class="name mb-2">
            {{ player.name }}
          </p>
        </div>
        <hr class="mt-0">
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      overdraw: Number
    }
  }
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';
  $card-margin: 6px;

  .outer-info-bar {
    width: 100%;
    margin-left: -$card-margin;
    margin-right: -$card-margin;
    pointer-events: none;
  }

  .info-card {
    color: $light-shades;
    font-family: $roboto-font;
    border-top-right-radius: $card-corner-radius;
    border-top-left-radius: $card-corner-radius;
    padding: 24px;
    margin-left: $card-margin;
    margin-right: $card-margin;
    pointer-events: initial;

    -webkit-flex-grow: 1;
    flex-grow: 1;
    min-width: 100px;
    max-width: 320px;
    height: 200px;
    overflow: hidden;
  }

  .info-card .name {
    font-size: 24px;
    font-family: $roboto-slab-font;
  }

  .info-card .color {
    margin-top: 10px;
    margin-right: 12px;
  }
</style>
