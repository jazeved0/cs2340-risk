<template>
    <div>
        <b-modal class="container" flex justify-content-center ml-auto mr-auto :visible="true" no-close-on-esc no-close-on-backdrop hide-header-close 
            :cancel-title="cancelTitle" :ok-title="okTitle" @ok="handleOk" @cancel="handleCancel">
            <div class='container' v-if="isWinner">
                <h1>Congratulations!</h1>
                <img src="../../../../public/images/cheer.gif" alt="cheering gif">
                <p>You won the game.</p>
            </div>            
            <div class="container" v-else>
                <h1>Game Over</h1>
                <p>The winner of the game is {{ winner }}.</p>
            </div>
            <div class="container">
                <b-table striped hover :items="score"></b-table>
            </div>
        </b-modal>
    </div>
</template>


<script>
export default {
    methods: {
        handleOk: function() {
            if (this.isHost) {
                this.$socket.sendObj({
                    _type: "controllers.RequestStartGame",
                    gameId: this.$store.state.gameId,
                    playerId: this.$store.state.playerId
                })
            }
        },
        handleCancel: function() {
            if (this.isHost) {
                window.location.href = '/';
            }
        },
    },
    computed: {
        isHost: function() {
            return this.$store.state.isHost;
        },
        isWinner: function() {
            //return this.$store.state.playerIndex == this.$store.state.game.winnerIndex;
            return true;
        },
        cancelTitle: function() {
            if (this.isHost) {
                return "Return to Home";
            } else {
                return "Cancel";
            }
        },
        okTitle: function() {
            if (this.isHost) {
                return "Play Again";
            } else {
                return "Ok";
            }
        },
        score: function() {
            let result = [];
            let playerStates = this.$store.getters.playerStates;
            const boardStates = this.$store.getters.boardStates;
            playerStates.forEach(function(ps) {
                let entry = {};
                Object.entries(ps).forEach(function(e){
                    if (e[0] == "name" || e[0] == "armies") {
                        entry[e[0]] = e[1];
                    }
                    entry["Territories"] = count;
                });
                const index = ps["turnOrder"];
                let count = 0;
                boardStates.forEach(function(bs) {
                    if (bs["owner"] == index) {
                        count++;
                    }
                })
                entry["Territories"] = count;
                result.push(entry);
            });
            return result;
        }
    }
}
</script>

<style lang="scss">
.container {
	display: flex;
	flex-direction: column;
	flex-wrap: nowrap;
	justify-content: center;
	align-items: center;
	align-content: stretch;
}    
</style>