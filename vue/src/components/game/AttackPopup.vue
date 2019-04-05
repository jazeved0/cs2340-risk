<template>
  <div>
    <b-modal ok-only ok-title="Cancel" title="Attack Turn Control" v-bind:visible="true" @ok="resetAttackingTerritories"
             no-close-on-esc no-close-on-backdrop hide-header-close>
      <p class="army-text">Number of armies in your attacking territory: {{getAttackingArmies}}</p>
      <p class="army-text">Number of enemy armies in the defending territory: {{getDefendingArmies}}</p>
      <p class="army-text">Select the number of armies you want to attack with: </p>
      <div class="flex-buttons">
        <b-button-group v-if="getAttackingArmies > 1">
          <b-button class="mr-4 mr-4" variant="secondary">One Army</b-button>
          <b-button v-if="getAttackingArmies > 2" variant="secondary" class="mr-4 mr-4">Two Armies</b-button>
          <b-button v-if="getAttackingArmies > 3" variant="secondary" class="mr-4 mr-4">Three Armies</b-button>
        </b-button-group>
      </div>
    </b-modal>
  </div>
</template>

<script>
  import {UPDATE_DEFEND_TERRITORY, UPDATE_ATTACK_TERRITORY} from "../../store/mutation-types.js"

  export default {
    computed: {
      getAttackingArmies: function () {
        const attackingIndex = this.$store.state.game.attackingTerritory;
        const territoryArmies = this.$store.getters.boardStates;
        if (attackingIndex === -1) {
          return 0;
        } else if (typeof territoryArmies === 'undefined') {
          return 0;
        }
        return territoryArmies[attackingIndex].amount;
      },
      getDefendingArmies: function() {
        const defendingIndex = this.$store.state.game.defendingTerritory;
        const territoryArmies = this.$store.getters.boardStates;
        if (defendingIndex === -1) {
          return 0;
        } else if (typeof territoryArmies === 'undefined') {
          return 0;
        }
        return territoryArmies[defendingIndex].amount;
      }
    },
    methods: {
      resetAttackingTerritories: function(evt) {
        this.$store.commit(UPDATE_ATTACK_TERRITORY, -1);
        this.$store.commit(UPDATE_DEFEND_TERRITORY, -1);
      },
    },
    name: "AttackPopup"
  };
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';

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