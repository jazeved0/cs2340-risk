<template>
    <div class = "wrapper">
        <div v-for = "player in players" :key= 'player.name' :style = '{color: player.color, opacity: player.currentTurn ? 1 : 0.7, flex: player.currentTurn ? 2 : 1, order: player.turnorder}'>
            <span v-if= "player.currentTurn"> It is {{player.name}}'s turn</span> <br>
            {{player.turnorder}} <br>
            {{player.name}} <br>
            Number of Armies: {{player.armies}} <br>
        </div>
    </div>
</template>
<script>
    export default {
        data: function(){
            return {
                players: [
                    {name: 'bopHAA', color: 'blue', armies: 5, turnorder: 2, currentTurn: false},
                    {name: 'joHAA', color: 'red', armies: 5, turnorder: 5, currentTurn: false},
                    {name: 'juwugu', color: 'green', armies: 5, turnorder: 3, currentTurn: false},
                    {name: 'flaskBTW', color: 'black', armies: 5, turnorder: 4, currentTurn: true},
                    {name: 'aBortnite', color: 'purple', armies: 5, turnorder: 1, currentTurn: false}
                ]
            }
        },
        methods: {
        }
    }
</script>
<style lang="scss">
 .wrapper {
    display: flex;
    font-family: 'Roboto', sans-serif;
    font-size: 1.4em;
 }
    /* .wrapper > div:nth-child(even) {
        background: repeating-linear-gradient(
        45deg,
        #606dbc,
        #606dbc 10px,
        #465298 10px,
        #465298 20px
        );
    } */

    .wrapper div {
        background: repeating-linear-gradient(
                45deg,
                #606dbc,
                #606dbc 10px,
                #465298 10px,
                #465298 20px
        );
        text-align: center;
        border: 10px #d9b34f ;
        border-style:double
    }
    .wrapper span {
        font-size: 1.5em;
    }
</style>
