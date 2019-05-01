<template>
    <v-image :config="config"></v-image>
</template>

<script>
    const width = window.innerWidth;
    const height = window.innerHeight;
    const image = new Image();
    const imgWidth = 28;
    const imgHeight = 28;
    image.src = '../static/images/corrected.png';
    export default {
        props: {
            data: Object
        },
        data() {
            
            const state = this.$store.state;
            const position = ('position' in this.data
                    && state.game.gameboard.castles.length > this.data.position)
                    ? state.game.gameboard.castles[this.data.position]
                    : { x: 0, y: 0 };
            return {
                stageSize: {
                    width: width,
                    height: height
                },
                config: {
                    x: position.a - (imgWidth / 4),
                    y: position.b - (imgHeight / 4),
                    image: image,
                    width: imgWidth,
                    height: imgHeight,
                    listening: false
                }
            }

        }
    }
</script>
