<template>
    <div>
        <p>Get your friends to join at https://{{ baseUrl }}/lobby/{{ lobbyId }}</p>
        <hr>
        <player-list :players-array='players'></player-list>
        <hr>
        <button v-if='isHost' @click='logIsHost' class='btn-success'>Start Game</button>
        <button v-else-if='isReady' class='btn-danger' @click='changeReadyState'>Unready</button>
        <button v-else class='btn-success' @click='changeReadyState'>Get Ready</button>
    </div>
</template>

<script>
    import PlayerList from './PlayerList.vue';
    export default {
        props: ['players', 'baseUrl', 'lobbyId', 'isHost'],
        data: function() {
            return {
                isReady: false
            }
        },
        components: {
            // 'app-footer': Footer,
            'player-list': PlayerList,
        },
        methods: {
            logIsHost: function() {
                console.log(this.isHost);
            },
            changeReadyState: function() {
                this.isReady = !this.isReady;
            }
        },
        beforeMount() {
            this.isHost = this.isHost == "true"
        },
    }
</script>

<style>

</style>
