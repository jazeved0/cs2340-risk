<template>
  <div>
    <div v-if="isEmpty" class="player-slot-outer placeholder">
      <div class="slot"></div>
    </div>
    <div v-else class="player-slot-outer" v-bind:class="{ current: isCurrent }">
      <div class="d-flex slot">
        <fa-icon class="color-indicator" icon="circle" v-bind:style="{ color: color }"></fa-icon>
        <p>{{ name }}</p>
        <popper class="crown-icon"
            v-if="isHost" trigger="hover"
            :options="{
              placement: 'top',
              modifiers: { offset: { offset: '0, -12px' }, flip: { enabled: false } } }"
            transition="fade"
            enter-active-class='fade-enter-active'
            leave-active-class='fade-leave-active'>
          <div class="popper">Lobby host</div>
          <div slot="reference">
            <fa-icon class="host-indicator shadow" icon="crown"></fa-icon>
          </div>
        </popper>
      </div>
    </div>
  </div>
</template>

<script>
  import Popper from 'vue-popperjs';

  export default {
    components: {
      'popper': Popper
    },
    props: {
      'isHost': Boolean,
      'color': String,
      'name': String,
      'isCurrent': Boolean
    },
    computed: {
      isEmpty: function () {
        return this.name === ""
      }
    }
  }
</script>

<style lang="scss">
  .player-slot-outer {
    padding: 12px 20px 12px 20px;
    margin: 12px;
    border-style: solid;
    border-radius: 8px;
    border-width: 2px;
    border-color: rgba(255, 255, 255, 0.0);
  }

  .player-slot-outer.current {
    background-color: rgba(217, 179, 79, 0.03);
    border-style: dashed;
    border-width: 2px;
    border-color: rgba(217, 179, 79, 0.69);
  }

  .player-slot-outer.placeholder {
    background-color: #3F3354;
    position: relative;
  }

  .player-slot-outer.placeholder::before {
    content: "";
    display: block;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    // noinspection CssUnknownTarget
    background-image: url("/static/images/darkStripes.svg");
    background-size: 250px;
  }

  .slot {
    min-height: 42px;
  }

  .slot i {
    font-size: 15px;
    margin-top: 14px;
  }

  .slot p {
    display: inline !important;
    margin-bottom: 0 !important;
    font-size: 28px;
    color: #F5F2F2;
    font-family: Roboto Slab, serif;
    overflow-x: hidden;
  }

  .color-indicator {
    margin-top: 15px;
    margin-right: 12px;
  }

  .host-indicator {
    margin-top: 12px;
    margin-left: 12px;
    color: #D9B34F;
  }

  .shadow {
    -webkit-filter: drop-shadow(0 0 7px rgba(255, 255, 255, .18));
    filter: drop-shadow(0 0 7px rgba(255, 255, 255, .18));
    /* Similar syntax to box-shadow */
  }
</style>