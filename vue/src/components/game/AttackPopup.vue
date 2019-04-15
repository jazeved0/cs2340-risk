<template>
  <div>
    <b-modal class="flex justify-content-center ml-auto mr-auto" size="lg" ok-title="Attack" title="Attack Turn Control" v-bind:visible="true"
             v-bind:ok-disabled="disableAttack"
             @ok="sendAttackPacket" @cancel="resetAttackingTerritories"
             no-close-on-esc no-close-on-backdrop hide-header-close>
      <div class="territory-images ml-auto mr-auto">
        <div class="territory-portrait">
          <svg width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="attackerPath" v-bind:fill="attackerColor"></path>
          </svg>
          <p2 class="territory-text"> Territory {{ getAttackingTerritoryName }}</p2>
          <p2 class="region-text"> Region {{ getAttackingRegionName }}</p2>
          <p2 class="army-text"> {{ getAttackingArmies }} Armies </p2>
        </div>
        <div class="ml-5 mr-5 mt-auto mb-auto territory-portrait">
          <div>
            <p3 class="territory-text text-center">{{AttackerPlayerName}}</p3>
          </div>
          <div>
            <p3 class="territory-text text-center">Is Attacking</p3>
          </div>
          <div>
            <p3 class="territory-text text-center">{{DefenderPlayerName}}</p3>
          </div>
        </div>
        <div class="territory-portrait">
          <svg width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="defenderPath" v-bind:fill="defenderColor"></path>
          </svg>
          <p2 class="territory-text"> Territory {{ getDefendingTerritoryName }}</p2>
          <p2 class="region-text"> Region {{ getDefendingRegionName }}</p2>
          <p2 class="army-text"> {{ getDefendingArmies }} Armies </p2>
        </div>
      </div>
      <p class="army-text mt-4">Attack with: </p>
      <div class="flex-buttons">
        <b-button-group v-if="getAttackingArmies > 1">
          <b-button class="mr-4 mr-4" variant="primary" v-on:click="armySelected(1)">One Army</b-button>
          <b-button v-if="getAttackingArmies > 2" variant="primary" class="mr-4 mr-4" v-on:click="armySelected(2)">Two Armies</b-button>
          <b-button v-if="getAttackingArmies > 3" variant="primary" class="mr-4 mr-4" v-on:click="armySelected(3)">Three Armies</b-button>
        </b-button-group>
      </div>
    </b-modal>
  </div>
</template>

<script>
  import {UPDATE_DEFEND_TERRITORY, UPDATE_ATTACK_TERRITORY} from "../../store/mutation-types.js"

  export default {
    data: function() {
      return {
        disableAttackButton: true,
        armyNumber: 0
      }
    },
    computed: {
      disableAttack() {
        return this.disableAttackButton;
      },
      attackingTerritory() {
        return this.$store.state.game.attackingTerritory;
      },
      defendingTerritory() {
        return this.$store.state.game.defendingTerritory;
      },
      attackerPath: function() {
        return this.getPath(this.attackingTerritory);
      },
      defenderPath: function() {
        return this.getPath(this.defendingTerritory);
      },
      attackerColor: function() {
        return this.getColor(this.attackingTerritory);
      },
      defenderColor: function() {
        return this.getColor(this.defendingTerritory);
      },
      getBoardState: function() {
        return this.$store.getters.boardStates;
      },
      getAttackingTerritoryName: function () {
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.attackingTerritory].territory.toString();
      },
      getDefendingTerritoryName: function() {
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.defendingTerritory].territory.toString();
      },
      getAttackingRegionName: function() {
        return this.$store.state.game.gameboard.regions.findIndex(r => r.includes(this.attackingTerritory)).toString();
      },
      getDefendingRegionName: function() {
        return this.$store.state.game.gameboard.regions.findIndex(r => r.includes(this.defendingTerritory)).toString();
      },
      getAttackingArmies: function () {
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.attackingTerritory].amount;
      },
      getDefendingArmies: function() {
        const territoryArmies = this.getBoardState;
        return territoryArmies[this.defendingTerritory].amount;
      },
      AttackerPlayerName: function() {
        const current = this.$store.state.game.turnIndex;
        const playerObj = this.$store.state.game.playerStateList[current];
        return playerObj.player.settings.name;
      },
      DefenderPlayerName: function() {
        const indx = this.defendingTerritory;
        const playerIndex = this.$store.getters.boardStates[indx].owner;
        if (playerIndex < 0){
            return "Neutral";
        }
        return this.$store.state.game.playerStateList[playerIndex].player.settings.name;
      },
    },
    methods: {
      resetAttackingTerritories: function() {
        this.$store.commit(UPDATE_ATTACK_TERRITORY, -1);
        this.$store.commit(UPDATE_DEFEND_TERRITORY, -1);
        this.disableAttackButton = true;
        this.armyNumber = 0;
      },
      armySelected: function(armyCount) {
        this.disableAttackButton = false;
        this.armyNumber = armyCount;
      },
      sendAttackPacket: function() {
        const packet = {
          _type: "controllers.RequestAttack",
          gameId: this.$store.state.gameId,
          playerId: this.$store.state.playerId,
          attack: [this.attackingTerritory, this.defendingTerritory, this.armyNumber]
        };
        this.$socket.sendObj(packet);
        this.resetAttackingTerritories();
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
    name: "AttackPopup"
  };
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';

  .territory-portrait {
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

  .flex-buttons {
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
