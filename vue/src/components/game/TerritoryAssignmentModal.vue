<template>
  <b-modal id="territoryAssignmentModal"
      centered
      v-bind:visible="visible"
      title="Territory Assignments"
      size="lg">
    <!--suppress HtmlUnknownBooleanAttribute, XmlUnboundNsPrefix -->
    <template v-slot:modal-footer>
      <p class="d-none">&nbsp;</p>
      <!-- empty -->
    </template>
    <div class="d-flex flex-wrap slot-container">
      <territory-assignment-modal-slot
          class="col-12 col-lg-6"
          v-for="territory in territories"
          v-bind="territory"
          v-bind:key="territory.territory">
      </territory-assignment-modal-slot>
    </div>
  </b-modal>
</template>

<script>
  import TerritoryAssignmentModalSlot from './TerritoryAssignmentModalSlot'

  export default {
    props: {
      visible: Boolean
    },
    computed: {
      territories: function () {
        const store = this.$store;
        const current = store.getters.getPlayerIndex;
        return store.getters.boardStates.filter(t => t.owner === current).map(t => {
          const region = store.state.game.gameboard.regions.findIndex(r => r.includes(t.territory));
          return {
            territory: t.territory.toString(),
            armies: t.amount,
            region: region.toString(),
            color: this.getColor(t.owner),
            territoryColor: this.getTerritoryColor(region),
            pathData: store.state.game.gameboard.iconData[t.territory]
          };
        });
      }
    },
    methods: {
      getColor: function(playerIndex) {
        if (playerIndex < this.$store.state.game.playerStateList.length && playerIndex >= 0) {
          // noinspection JSUnresolvedVariable
          return '#' + this.$store.state.settings.settings.colors[
            this.$store.state.game.playerStateList[playerIndex].player.settings.ordinal
            ];
        } else return '#ffffff';
      },
      getTerritoryColor: function(regionIndex) {
        if (regionIndex < this.$store.state.settings.settings.territoryColors.length && regionIndex >= 0) {
          return '#' + this.$store.state.settings.settings.territoryColors[regionIndex];
        } else return 'lightgray'
      }
    },
    components: {
      'territory-assignment-modal-slot': TerritoryAssignmentModalSlot
    }
  }
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';
  @import '../../assets/stylesheets/variables';

  #territoryAssignmentModal .modal-content {
    @extend .frosted-glass-dark;
    border-style: none;
    border-radius: $card-corner-radius;
  }

  @supports ((-webkit-backdrop-filter: blur(1em)) or (backdrop-filter: blur(1em))) {
    #territoryAssignmentModal:not(.show) {
      -webkit-backdrop-filter: blur(0em)!important;
      backdrop-filter: blur(0em)!important;
    }

    #territoryAssignmentModal .fade {
      transition: backdrop-filter 0.15s;
    }
  }

  #territoryAssignmentModal .modal-footer {
    border-top: transparent;
    padding: 0;
  }

  #territoryAssignmentModal .modal-header {
    border-bottom: $frosted-glass-dark-border;
  }

  #territoryAssignmentModal .slot-container {
    margin: -$territory-assignment-slot-padding;
  }

  #territoryAssignmentModal .modal-title {
    font-family: $roboto-slab-font;
    font-size: 32px;
    margin-top: -8px;
    margin-bottom: -8px;
  }

  #territoryAssignmentModal {
    color: $light-shades!important;
    font-family: $roboto-font;
  }

  #territoryAssignmentModal .close {
    color: $light-shades!important;
    text-shadow: 0 1px 0 #000;
  }

  #territoryAssignmentModal .close:hover {
    color: $light-shades!important;
    opacity: 0.75;
  }
</style>
