<!--suppress HtmlUnknownTag -->
<template>
  <div class="slot-wrapper">
    <div class="d-flex flex-row slot-inner">
      <!--suppress HtmlUnknownAttribute -->
      <svg width="100" height="100" viewBox="-4 -4 108 108">
        <defs>
          <filter id="dropShadow" x="0" y="0">
            <feOffset result="offOut" in="SourceAlpha" dx="0" dy="0"></feOffset>
            <feGaussianBlur result="blurOut" in="offOut" stdDeviation="3"></feGaussianBlur>
            <feBlend in="SourceGraphic" in2="blurOut" mode="normal"></feBlend>
          </filter>
        </defs>
        <path v-bind:d="pathData" v-bind:fill="territoryColor" filter="url(#dropShadow)"></path>
      </svg>
      <div class="text-wrapper flex-fill">
        <h3 class="mb-0">Territory <span class="font-weight-bold">{{ territory }}</span></h3>
        <hr class="mt-1 mb-2">
        <div class="content">
          <p class="mb-0">Region <span class="font-weight-bold">{{ region }}</span></p>
          <div class="d-flex">
            <fa-icon class="player-color" icon="circle" size="xs" v-bind:style="{ color: color }"></fa-icon>
            <p class="army-text mb-0">{{ getArmyText }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      pathData: String,
      territory: String,
      region: String,
      armies: Number,
      color: String,
      territoryColor: String
    },
    computed: {
      getArmyText: function () {
        return this.armies !== 1 ? this.armies + ' armies' : '1 army';
      }
    }
  }
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';
  @import '../../assets/stylesheets/variables';

  .slot-wrapper {
    padding: $territory-assignment-slot-padding;
  }

  .slot-wrapper .slot-inner {
    border-style: dashed;
    border-color: rgba($light-shades, 0.25);
    border-width: 2px;
    border-radius: $card-corner-radius;
    background-color: rgba($light-shades, 0.03);
    width: 100%;
    padding: $territory-assignment-slot-inner-padding;
  }

  .slot-wrapper .text-wrapper {
    margin-left: 16px;
  }

  .slot-wrapper .text-wrapper .content {
    margin-left: 24px;
  }

  .slot-wrapper .text-wrapper .content .army-text {
    opacity: 0.5;
  }

  .slot-wrapper .text-wrapper .content .player-color {
    margin-top: 5px;
    margin-right: 8px;
    margin-left: -20px;
  }
</style>
