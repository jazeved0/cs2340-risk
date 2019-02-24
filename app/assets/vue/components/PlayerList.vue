<template>
    <div>
        <ul>
            <li v-for="player in players" :key='player.name' :style='{color: player.color}'>{{ player.name }}</li>
        </ul>
    </div>
</template>

<script>
    export default {
        name: "player-list",
        props: ['playersArray'],
        data: function() {
            return {
                text: "OMEGALUL",
                players: []
            }
        },
        methods: {
            logType: function() {
                console.log(typeof this.playersArray);
                console.log(this.playersArray);
            },
            parseArray: function() {
                this.players = JSON.parse(this.playersArray);
            }
        },
        beforeMount() {
            this.parseArray();
            console.log(this.baseUrl);
        },
    }
</script>