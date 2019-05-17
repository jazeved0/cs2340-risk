<template>
    <v-image :config="config"></v-image>
</template>

<script>
    import {exists} from "../../../util";

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
            let position = null;
            if ('position' in this.data) {
              const castle = state.game.gameboard.territories[this.data.position].castle;
              if (exists(castle)) position = castle;
            }
            if (position === null) position = { x: 0, y: 0 };
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
