<template>
    <div class="row">
        <ul>
            <li v-for="player in players" :key='player.name' :style='{color: player.color}'>{{ player.name }}</li>
        </ul>
    </div>
</template>

<script>
    export default {
        data: function() {
            return {
                text: "OMEGALUL",
                players: [
                    {name: 'Julian', color: 'green'},
                    {name: 'joalazer', color: 'blue'},
                    {name: 'bopas2', color: 'orange'},
                    {name: 'GT iPhish', color: 'red'},
                    {name: 'Chafos', color: 'purple'}
                ]
            }
        },
        methods: {
            changeText: function() {
                this.text = "POGGERS";
            }
        }
    }
</script>