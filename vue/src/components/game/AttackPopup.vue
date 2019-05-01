<!--suppress CheckEmptyScriptTag -->
<template>
  <div>
    <b-modal class="flex justify-content-center ml-auto mr-auto" size="lg" ok-title="Attack" title="Attack Turn Control" v-bind:visible="true"
             v-bind:ok-disabled="disableAttack"
             @ok="sendAttackPacket" @cancel="resetAttackingTerritories"
             no-close-on-esc no-close-on-backdrop hide-header-close>
      <div class="territory-display d-flex flex-column flex-md-row mb-3">
        <div class="border text-center">
          <svg class="d-none d-md-inline" width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="attackerPath" v-bind:fill="attackerColor"></path>
          </svg>
          <h2>Territory {{ getAttackingTerritoryName }}</h2>
          <h3>Region {{ getAttackingRegionName }}</h3>
          <p> {{ getAttackingArmies }} Armies </p>
        </div>
        <div class="middle-section">
          <svg class="d-none d-md-block" version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
              x="0px" y="0px" viewBox="0 0 32 32" style="enable-background:new 0 0 32 32;" xml:space="preserve">
            <line class="st0" x1="0.6" y1="16" x2="30.6" y2="16"/>
            <polygon class="st1" points="28.1,19.1 27.6,18.5 30.3,16 27.6,13.5 28.1,12.9 31.4,16"/>
          </svg>
          <p class="d-md-none d-lg-block">is attacking</p>
        </div>
        <div class="border text-center">
          <svg class="d-none d-md-inline" width="150" height="150" viewBox="-4 -4 108 108">
            <path v-bind:d="defenderPath" v-bind:fill="defenderColor"></path>
          </svg>
          <h2>Territory {{ getDefendingTerritoryName }}</h2>
          <h3>Region {{ getDefendingRegionName }}</h3>
          <p> {{ getDefendingArmies }} Armies </p>
        </div>
      </div>
      <div class="d-flex flex-row">
        <span class="mr-3" style="margin-top: 6px;">Attack with:</span>
        <b-button-group class="btn-group-toggle" v-if="getAttackingArmies > 1" data-toggle="buttons">
          <label class="btn btn-secondary"                               v-on:click="armySelected(1)">
            <input type="radio" name="attackingUnits" id="one" autocomplete="off"> One Army
          </label>
          <label class="btn btn-secondary" v-if="getAttackingArmies > 2" v-on:click="armySelected(2)">
            <input type="radio" name="attackingUnits" id="two" autocomplete="off"> Two Armies
          </label>
          <label class="btn btn-secondary" v-if="getAttackingArmies > 3" v-on:click="armySelected(3)">
            <input type="radio" name="attackingUnits" id="three" autocomplete="off"> Three Armies
          </label>
        </b-button-group>
      </div>
    </b-modal>
  </div>
</template>

<script>
  import {UPDATE_DEFEND_TERRITORY, UPDATE_ATTACK_TERRITORY} from "../../store/mutation-types.js"
  import {UPDATE_ATTACKERS} from "../../store/mutation-types";
  export default {
    data: function() {
      return {
        disableAttackButton: true,
        armyNumber: 0,
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
        const index = this.defendingTerritory;
        const playerIndex = this.$store.getters.boardStates[index].owner;
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
        this.$store.commit(UPDATE_ATTACKERS, this.armyNumber);
        this.resetAttackingTerritories();
        this.$socket.sendObj(packet);
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
    color: #751b1b;
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
    stroke: #751b1b;
    stroke-width: 0.75;
    stroke-miterlimit: 10;
  }

  div.territory-display div.middle-section svg .st1 {
    fill: #751b1b;
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

</style>
