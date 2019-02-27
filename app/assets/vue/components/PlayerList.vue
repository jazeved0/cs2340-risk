<template>
    <div class="container">
        <div class="d-flex flex-wrap">
            <player-slot class="player-slot" v-for="player in players" :key="player.name" v-bind:name="player.name" v-bind:color="player.color"></player-slot>
        </div>
    </div>
</template>

<script>
    import PlayerSlot from './PlayerSlot.vue';
    export default {
        name: "player-list",
        props: ['playersArray'],
        data: function() {
            return {
                text: "OMEGALUL",
                players: []
            }
        },
        components: {
            'player-slot': PlayerSlot
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

<style lang="scss">
.player-slot {
    -webkit-box-flex: 0;
    -ms-flex: 0 0 33.333333%;
    flex: 0 0 33.333333%;
    max-width: 33.333333%;
}
</style>