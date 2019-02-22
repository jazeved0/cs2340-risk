<template>
    <div class="container">
        <player-list></player-list>
        <hr>
    </div>
</template>

<script>
    import PlayerList from './PlayerList.vue';
    export default {
        components: {
            // 'app-footer': Footer,
            'player-list': PlayerList,
        }
    }
</script>

<style>

</style>
