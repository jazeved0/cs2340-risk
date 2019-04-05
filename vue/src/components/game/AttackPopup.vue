<template>
  <div>
    <b-modal title="Attack Turn Control" v-bind:visible="true" @ok="resetAttackingTerritories"
             @cancel="resetAttackingTerritories">
      <p>Number of your armies avaliable in your attacking territory: {{getAttackingArmies}}</p>
      <p>Number of enemy armies in the defending territory: {{getDefendingArmies}}</p>
      <p>Enter the number of armies you want to attack with: </p>
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
        return territoryArmies[attackingIndex].amount - 1;
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