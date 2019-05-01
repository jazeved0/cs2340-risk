<template>
    <div>
        <b-modal class="flex justify-content-center ml-auto mr-auto" :visible="true" no-close-on-esc no-close-on-backdrop hide-header-close ok-only
             @ok="resetAttack">
            <p>
                The attacker attacked with {{ this.$store.state.game.attackers }} armies, and the defender chose to defend with {{ this.$store.state.game.defenders }}
                armies. The results are:
            </p>
            <div id="overallContainer">
                <p>Attacking rolls: </p>
                <div class="diceImageContainer">
                    <!--attack rolls-->
                    <img v-for="image in attackImages" :src="image" height="80" width="80" alt="dice roll">
                </div>
                <p>Defending Rolls: </p>
                <div class="diceImageContainer">
                    <!--defend rolls-->
                    <img v-for="image in defendImages" :src="image" height="80" width="80" alt="dice roll">
                </div>
                <p>
                    {{ this.$store.state.game.attackResults[0] }} attackers were destroyed, and
                    {{ this.$store.state.game.attackResults[1] }} defenders were destroyed.
                </p>
            </div>
        </b-modal>
    </div>
</template>


<script>
    import {RESET_ATTACK} from "../../store/mutation-types";

    export default {
        data() {
            return {
                numDice: 3,
            }
        },
        computed: {
            attackArmies: function() {
                return this.$store.state.game.attackers;
            },
            defendArmies: function() {
                return this.$store.state.game.defenders;
            },
            attackDice: function() {
                return this.$store.state.game.diceRolls.slice(0, this.attackArmies)
            },
            defendDice: function() {
                return this.$store.state.game.diceRolls.slice(this.attackArmies, this.attackArmies + this.defendArmies)
            },
            attackImages: function() {
                return this.diceImages(this.attackDice);
            },
            defendImages: function() {
                return this.diceImages(this.defendDice);
            }
        },
        methods: {
            diceImages: function(rolls) {
                let arr = [];
                rolls.forEach(element => {
                    arr.push("../static/images/Alea_" + (element) + ".png")
                });
                return arr;
            },
            resetAttack: function() {
                this.$store.commit(RESET_ATTACK);
            }
        }
    }
</script>

<style>
    /* #diceRollModal {
        position: relative
    } */

    .diceImageContainer {
        display: flex;
        flex-direction: row;
        flex-wrap: nowrap;
        justify-content: space-around;
        align-items: center;
        align-content: stretch;
    }
/*
    #overallContainer {
        display: flex;
        flex-direction: column;
        flex-wrap: no-wrap;
        justify-content: space-around;
    } */
</style>
