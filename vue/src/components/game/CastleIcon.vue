<template>
    <v-image :config="config"></v-image>
</template>

<script>
    const width = window.innerWidth;
    const height = window.innerHeight; 
    export default {
        props: {
            data: Object
        },
        data() {
            const imgWidth = 30;
            const imgHeight = 30;
            const img = new Image();
            img.src = '../static/images/corrected.png'
            
            const state = this.$store.state;
            const position = ('position' in this.data
                    && state.game.gameboard.castles.length > this.data.position)
                    ? state.game.gameboard.castles[this.data.position]
                    : { x: 0, y: 0 }
            return {
                stageSize: {
                    width: width,
                    height: height
                },
                config: {
                    x: position.a,
                    y: position.b,
                    image: img,
                    width: imgWidth,
                    height: imgHeight
                }
            }

        }
    }
</script>
