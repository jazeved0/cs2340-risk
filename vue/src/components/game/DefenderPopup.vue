<!--suppress CheckEmptyScriptTag -->
<template>
    <div>
        <b-modal
            class="flex justify-content-center ml-auto mr-auto"
            size="lg"
            :title="title" v-bind:visible="true"
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
                    <svg class="d-none d-md-block" version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg"
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
                    <label class="btn btn-secondary" v-on:click="armySelected(1)">
                        <input type="radio" name="attackingUnits" id="one" autocomplete="off"> One Army
                    </label>
                    <label v-if="hasTwoArmies" class="btn btn-secondary" v-on:click="armySelected(2)">
                        <input type="radio" name="attackingUnits" id="two" autocomplete="off"> Two Armies
                    </label>
                </b-button-group>
            </div>
            <div slot="modal-footer" class="w-100">
                <b-button
                    variant="primary"
                    size="sm"
                    class="float-right"
                    :disabled="disableDefendButton"
                    @click="sendDefensePacket"
                >Defend
                </b-button>
            </div>
        </b-modal>
    </div>
</template>

<script>
    import {UPDATE_DEFEND_TERRITORY, UPDATE_ATTACK_TERRITORY} from "../../store/mutation-types.js"
    import {UPDATE_DEFENDERS} from "../../store/mutation-types";
    export default {
        computed: {
            hasTwoArmies: function() {
                const store = this.$store;
                const boardStates = store.getters.boardStates;
                const defending = boardStates.filter(bs => bs["territory"] === this.defendingTerritory);
                if (defending.length > 0) {
                    return defending[0]["amount"] >= 2;
                } else return false;
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
            title () {
              return this.AttackerPlayerName + ' is attacking you!';
            },
            AttackerPlayerName: function() {
                const current = this.$store.state.game.turnIndex;
                const playerObj = this.$store.state.game.playerStateList[current];
                return playerObj.player.settings.name;
            },
            DefenderPlayerName: function() {
                const index = this.defendingTerritory;
                const playerIndex = this.$store.getters.boardStates[index].owner;
                // noinspection JSConstructorReturnsPrimitive
                return playerIndex < 0
                    ? "Neutral"
                    : this.$store.state.game.playerStateList[playerIndex].player.settings.name;
            },
        },

        methods: {
            resetAttackingTerritories: function() {
                this.$store.commit(UPDATE_ATTACK_TERRITORY, -1);
                this.$store.commit(UPDATE_DEFEND_TERRITORY, -1);
            },
            armySelected: function(armyCount) {
                this.disableDefendButton = false;
                this.defendNumber = armyCount;
            },
            sendDefensePacket: function() {
                const packet = {
                    _type: "controllers.DefenseResponse",
                    gameId: this.$store.state.gameId,
                    playerId: this.$store.state.playerId,
                    defenders: this.defendNumber
                };
                this.$store.commit(UPDATE_DEFENDERS, this.defendNumber);
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

        name: "DefenderPopup",

        data () {
            return {
                disableDefendButton: true,
                defendNumber: 0
            }
        }
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
