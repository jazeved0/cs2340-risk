<!--suppress CheckEmptyScriptTag -->
<template>
  <div class="d-flex outer-info-bar ml-auto mr-auto" style="width: 100%">
    <div class="flex-fill d-flex name-list">
      <button class="expand-button d-block d-md-none"
          @mousedown="handleExpand" @tap="handleExpand">
        <fa-icon class="color" v-bind:icon="expandIcon"></fa-icon>
      </button>
      <div v-bind:class="cardClass">
        <div class="info-card-item" v-for="player in this.$store.getters.playerStates" :key='player.name'>
          <div class="d-flex">
            <fa-icon class="current-turn" icon="chevron-right" v-bind:style="{ visibility: (localTurn(player.name) ? 'visible' : 'hidden') }"></fa-icon>
            <fa-icon class="color" icon="circle" v-bind:style="{ color: player.color }"></fa-icon>
            <p class="name">
              {{ player.name }}
            </p>
            <div class="army d-flex flex-row">
              <svg version="1.1" xmlns="http://www.w3.org/2000/svg"
                  x="0px" y="0px" viewBox="0 0 32 32" style="enable-background:new 0 0 32 32;"
                  xml:space="preserve" class="d-inline-block">
                  <circle class="st0" cx="7.7" cy="23.4" r="5.8"/>
                <circle class="st0" cx="24.3" cy="23.4" r="5.8"/>
                <circle class="st0" cx="15.8" cy="9.1" r="5.8"/>
                </svg>
              <p>
                {{ player.armies }}
              </p>
            </div>
          </div>
          <hr/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    data () {
      return {
        isExpanded: false
      };
    },

    computed: {
      localTurn () {
        return (name) => {
          const turnIndex = this.$store.state.game.turnIndex;
          if (turnIndex === -1) {
            return false;
          }
          return name === this.$store.state.game.playerStateList[turnIndex].player.settings.name;
        }
      },

      expandIcon () {
        if (this.isExpanded) return 'chevron-down';
        else return 'chevron-up';
      },

      cardClass () {
        if (this.isExpanded) return 'info-card frosted-glass-dark active';
        else return 'info-card frosted-glass-dark';
      }
    },

    methods: {
      handleExpand () {
        this.isExpanded = !this.isExpanded;
      }
    }
  }
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';
  $card-margin: 6px;

  button.expand-button {
    border-radius: 200px;
    width: 40px;
    height: 40px;
    border: none;
    background-color: rgba(0, 0, 0, 0.5);
    color: $light-shades;
    pointer-events: initial;
    position: absolute;
    bottom: 12px;
    right: 14px;
    z-index: 1000;
  }

  .name-list {
    flex-direction: row-reverse;
  }

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
    height: auto;
    min-height: 200px;
    overflow: hidden;

    transition: transform 0.5s;
    transform: none;
  }

  .info-card .name {
    font-size: 20px;
    font-family: $roboto-slab-font;
  }

  .info-card .current-turn, .info-card .color {
    margin-top: 10px;
    margin-right: 12px;
  }

  .info-card .color {
    transform: scale(0.8, 0.8);
  }

  .info-card .current-turn {
    color: #D9B34F;
    transform: scale(1.4, 1.4);
  }

  .info-card .army p {
    margin-top: 2px;
    font-size: 20px;
  }

  .info-card .army {
    margin-left: auto;
  }

  .info-card p {
    margin-bottom: 0!important;
  }

  .info-card hr {
    margin-top: 5px;
    margin-bottom: 6px;
    margin-left: 24px;
  }

  .info-card-item:last-child hr {
    display: none;
  }

  .info-card .army-text {
    opacity: 0.5;
  }

  body {
    overflow: hidden;
  }

  svg .st0{
    fill:#F0EFEF;
  }

  .info-card .army svg {
    transform: scale(0.6, 0.6);
    width: 32px;
    height: 32px;
    margin-right: 4px;
  }

  @media screen and (max-width: 600px) {
    .name-list {
      flex-wrap: wrap-reverse;
      flex-direction: row-reverse;
      align-items: center;
      justify-content: center;
      height: min-content;
    }

    .divider-bar {
      display:none;
    }

    .info-card {
      color: $light-shades;
      font-family: $roboto-font;
      padding: 16px;
      pointer-events: initial;
      margin-left: 5px;

      border-bottom-right-radius: $card-corner-radius;
      border-bottom-left-radius: $card-corner-radius;

      -webkit-flex-grow: 1;
      flex-grow: 1;
      width: min-content;
      height: min-content;
      overflow: hidden;
      max-width: none;
      min-height: 0;
      transform: translate3d(0,+115%,0);
    }

    .info-card.active {
      transform: translate3d(0, 0, 0);
      padding-bottom: 56px;
    }

    .name {
      font-size: 14px;
    }
  }
</style>
