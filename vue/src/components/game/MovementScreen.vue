<template>
  <div>
    <b-modal class="flex justify-content-center ml-auto mr-auto" size="lg" ok-title="Complete Movement Action" title="Movement Turn Control" v-bind:visible="true"
             v-bind:ok-disabled="disableMovement"
             @ok="sendMovePacket" @cancel="resetMovingTerritories"
             no-close-on-esc no-close-on-backdrop hide-header-close>
      <div class="territory-images ml-auto mr-auto">
        <div class="territory-portrait">
          <svg width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="originPath" v-bind:fill="originColor"></path>
          </svg>
          <p2 class="territory-text"> Territory {{ getOriginTerritoryName }}</p2>
          <p2 class="region-text"> Region {{ getOriginRegionName }}</p2>
          <p2 class="army-text"> {{ getOriginArmies }} Armies </p2>
        </div>
        <div class="ml-5 mr-5 mt-auto mb-auto territory-text-container">
          <div>
            <p3 class="territory-text text-center">Moving armies to</p3>
          </div>
        </div>
        <div class="territory-portrait">
          <svg width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="goalPath" v-bind:fill="goalColor"></path>
          </svg>
          <p2 class="territory-text"> Territory {{ getGoalTerritoryName }}</p2>
          <p2 class="region-text"> Region {{ getGoalRegionName }}</p2>
          <p2 class="army-text"> {{ getGoalArmies }} Armies </p2>
        </div>
      </div>
      <p class="mt-2">Enter the amount of armies you wish to move: </p>
      <div class="mb-3 mt-2">
        <b-form-group
            id="army-input"
            label="Enter the amount of troops you wish to move: "
            label-for="input-1">
          <b-form-input
              id="input-1"
              v-model="armyNumber"
              type="armyCount"
              required
              placeholder="Enter a Number">
          </b-form-input>
        </b-form-group>
      </div>
      <p>Note: You must leave at least one army behind. </p>
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
        console.log(packet);
        this.$socket.sendObj(packet);
        this.resetMovingTerritories;
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
    font-family: $roboto-font;
    text-align: center;
    font-size: 20px;
  }

  .army-text {
    font-family: $roboto-font;
    font-size: 22px;
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
