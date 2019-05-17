<!--suppress CheckEmptyScriptTag -->
<template>
  <div>
    <b-modal class="flex justify-content-center ml-auto mr-auto" size="lg" ok-title="Complete Movement Action" title="Movement Turn Control" v-bind:visible="true"
             v-bind:ok-disabled="disableMovement"
             @ok="sendMovePacket" @cancel="resetMovingTerritories"
             no-close-on-esc no-close-on-backdrop hide-header-close>

      <div class="territory-display d-flex flex-column flex-md-row mb-3">
        <div class="border text-center">
          <svg class="d-none d-md-inline" width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="originPath" v-bind:fill="originColor"></path>
          </svg>
          <h2>Territory {{ getOriginTerritoryName }}</h2>
          <h3>Region {{ getOriginRegionName }}</h3>
          <p> {{ getOriginArmies }} Armies </p>
        </div>
        <div class="middle-section">
          <svg class="d-none d-md-block" version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
              x="0px" y="0px" viewBox="0 0 32 32" style="enable-background:new 0 0 32 32;" xml:space="preserve">
              <line class="st0" x1="0.6" y1="16" x2="30.6" y2="16"/>
            <polygon class="st1" points="28.1,19.1 27.6,18.5 30.3,16 27.6,13.5 28.1,12.9 31.4,16"/>
            </svg>
          <p class="d-md-none d-lg-block">moving armies to</p>
        </div>
        <div class="border text-center">
          <svg class="d-none d-md-inline" width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="goalPath" v-bind:fill="goalColor"></path>
          </svg>
          <h2>Territory {{ getGoalTerritoryName }}</h2>
          <h3>Region {{ getGoalRegionName }}</h3>
          <p> {{ getGoalArmies }} Armies </p>
        </div>
      </div>
      <div class="mb-3 mt-2">
        <b-form-group
            id="army-input"
            label="Select the amount of troops you wish to move: "
            label-for="armyCount">
          <div class="d-flex mt-1">
            <span class="d-block army-text">{{ armyNumber }}</span>
            <b-form-input id="armyCount" v-model="armyNumber" type="range" min="0" :max="maxArmies"></b-form-input>
          </div>
        </b-form-group>
      </div>
      <p class="font-italic">Note: You must leave at least one army behind. </p>
    </b-modal>
  </div>
</template>

<script>
  import {UPDATE_MOVE_TARGET, UPDATE_MOVE_ORIGIN} from "../../store/mutation-types.js"
  export default {
    data: function() {
      return {
         armyNumber: 0,
      }
    },
    computed: {
      maxArmies() {
        return this.getOriginArmies - 1;
      },
      disableMovement() {
        if (!isNaN(this.armyNumber)) {
          return !((parseInt(this.armyNumber) > 0) && (parseInt(this.armyNumber) < this.getOriginArmies));
        }
        return true;
      },
      originTerritory() {
        return this.$store.state.game.movingTerritoryOrigin;
      },
      goalTerritory() {
        return this.$store.state.game.movingTerritoryGoal;
      },
      originPath: function() {
        return this.getPath(this.originTerritory);
      },
      goalPath: function() {
        return this.getPath(this.goalTerritory);
      },
      originColor: function() {
        return this.getColor(this.originTerritory);
      },
      goalColor: function() {
        return this.getColor(this.goalTerritory);
      },
      getBoardState: function() {
        return this.$store.getters.boardStates;
      },
      getOriginTerritoryName: function () {
        if (this.originTerritory === -1) {
          return "";
        }
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.originTerritory].territory.toString();
      },
      getGoalTerritoryName: function() {
        if (this.goalTerritory === -1) {
          return "";
        }
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.goalTerritory].territory.toString();
      },
      getOriginRegionName: function() {
        return this.$store.state.game.gameboard.regions.findIndex(r => r.includes(this.originTerritory)).toString();
      },
      getGoalRegionName: function() {
        return this.$store.state.game.gameboard.regions.findIndex(r => r.includes(this.goalTerritory)).toString();
      },
      getOriginArmies: function () {
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.originTerritory].amount;
      },
      getGoalArmies: function() {
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.goalTerritory].amount;
      }
    },
    methods: {
      resetMovingTerritories: function() {
        this.$store.commit(UPDATE_MOVE_TARGET, -1);
        this.$store.commit(UPDATE_MOVE_ORIGIN, -1);
        this.armyNumber = 0;
      },
      sendMovePacket: function() {
        const packet = {
          _type: "controllers.RequestDoManeuver",
          gameId: this.$store.state.gameId,
          playerId: this.$store.state.playerId,
          origin: this.originTerritory,
          amount: parseInt(this.armyNumber),
          destination: this.goalTerritory
        };
        this.$socket.sendObj(packet);
        this.resetMovingTerritories();
      },
      getPath: function(territoryIndex) {
        return this.$store.state.game.gameboard.iconData[territoryIndex];
      },
      getColor: function(territoryIndex) {
        const region = this.$store.state.game.gameboard.regions.findIndex(r => r.includes(territoryIndex));
        return this.getTerritoryColor(region);
      },
      getTerritoryColor: function(regionIndex) {
        if (regionIndex < this.$store.state.settings.settings.territoryColors.length && regionIndex >= 0) {
          return '#' + this.$store.state.settings.settings.territoryColors[regionIndex];
        } else return 'lightgray'
      }
    },
    name: "MovementScreen"
  };
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';

  div.territory-display .border {
    background-color: rgba($dark-shades, 0.05);
    border-radius: 12px;
  }

  div.territory-display > div:not(.middle-section) {
    flex-grow: 1;
    padding: 16px;
  }

  div.territory-display > div:not(.middle-section) svg * {
    stroke: gray;
    stroke-width: 1;
  }

  div.territory-display h2 {
    font-family: $roboto-slab-font;
    color: #222222;
    font-size: 24px;
    margin-bottom: -2px;
  }

  div.territory-display h3 {
    font-family: $roboto-font;
    color: #222222;
    opacity: 0.6;
    font-size: 20px;
    margin-bottom: -2px;
    letter-spacing: 5px;
  }

  div.territory-display p {
    margin-bottom: 0;
  }

  div.territory-display div.middle-section {
    max-width: 200px;
    color: #2e2e75;
    font-weight: 500;
    padding-left: 24px;
    padding-right: 24px;
    align-self: center;
  }

  div.territory-display div.middle-section p {
    margin-top: -38px;
  }

  div.territory-display div.middle-section svg {
    min-width: 50px;
    height: auto;
  }

  div.territory-display div.middle-section svg .st0 {
    fill: none;
    stroke: #2e2e75;
    stroke-width: 0.75;
    stroke-miterlimit: 10;
  }

  div.territory-display div.middle-section svg .st1 {
    fill: #2e2e75;
  }

  @media(max-width: 767.95px) {
    div.territory-display div.middle-section p {
      margin-top: 0;
      padding-top: 8px;
      padding-bottom: 8px;
    }

    div.territory-display > div:not(.middle-section) {
      padding: 8px;
    }
  }

  .territory-portrait {
    display: flex;
    flex-direction: column;
    width: min-content;
    align-items: center;
    background: #EBEAEE;
    border-radius: 15px;
    padding: 15px;
  }

  .territory-text-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #EBEAEE;
    border-radius: 15px;
    padding: 15px;
  }

  .territory-images {
    display: flex;
    flex-direction: row;
    width: max-content;
    justify-content: space-around;
    align-content: center;
  }

  .territory-text {
    font-family: $roboto-font;
    text-align: center;
    font-size: 24px;
    font-weight: bold;
  }

  .region-text {
    font-family: $roboto-font;
    text-align: center;
    font-size: 18px;
    letter-spacing: 3px;
    color: #BBBBBB;
  }

  .army-text {
    margin-right: 12px;
    border: solid 1px #a2a2a2;
    padding: 2px 8px;
    margin-top: -4px;
    background-color: rgba(150, 150, 150, 0.1);
    font-weight: 600;
    border-radius: 4px;
  }

  @media screen and (max-width: 600px) {

    .territory-portrait {
      margin: 0 auto;
      justify-content: center;
    }

    .territory-images {
      display: flex;
      flex-direction: column;
      width: max-content;
      justify-content: space-around;
      align-content: center;
    }
  }

</style>
