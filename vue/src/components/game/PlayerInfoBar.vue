<template>
  <div class="d-flex outer-info-bar" style="width: 100%">
    <div class="flex-fill d-flex name-list">
      <div v-for="player in this.$store.getters.playerStates"
          :key='player.name'
          v-bind:class="{'glow': localTurn(player.name)}"
          class="info-card frosted-glass-dark">
        <div class="d-flex">
          <fa-icon class="color" icon="circle" v-bind:style="{ color: player.color }"></fa-icon>
          <p class="name mb-2">
            {{ player.name }}
          </p>
        </div>
        <hr class="mt-0 divider-bar">
        <p class="army-text">{{ player.armies !== 1 ? player.armies + ' armies' : '1 army' }}</p>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      overdraw: Number
    },
    computed: {
      localTurn: function () {
        return (name) => {
          const turnIndex = this.$store.state.game.turnIndex;
          if (turnIndex === -1) {
            return false;
          }
          return name === this.$store.state.game.playerStateList[turnIndex].player.settings.name;
        }
      }
    }
  }
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';
  $card-margin: 6px;

  .name-list {
    flex-direction: row-reverse;
  }

  .outer-info-bar {
    width: 100%;
    margin-left: -$card-margin;
    margin-right: -$card-margin;
    pointer-events: none;
  }

  .glow {
    transition: box-shadow 0.3s;
    box-shadow: 0px 0px 15px yellow;
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

  .info-card .army-text {
    opacity: 0.5;
  }

  @media screen and (max-width: 600px) {
    .name-list {
      flex-wrap: wrap-reverse;
      flex-direction: row-reverse;
      height: min-content;
    }

    .divider-bar {
      display:none;
    }

    .info-card {
      color: $light-shades;
      font-family: $roboto-font;
      padding: 6px;
      pointer-events: initial;
      margin-left: 5px;

      border-bottom-right-radius: $card-corner-radius;
      border-bottom-left-radius: $card-corner-radius;

      -webkit-flex-grow: 1;
      flex-grow: 1;
      min-width: min-content;
      max-width: 100%;
      height: min-content;
      overflow: hidden;
    }

    .name {
      font-size: 14px;
    }
  }
</style>
