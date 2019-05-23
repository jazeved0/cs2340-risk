<template>
    <v-image :config="config"></v-image>
</template>

<script>
    const image = new Image();
    const imgWidth = 28;
    const imgHeight = 28;
    image.src = '../../static/images/corrected.png';
    export default {
        props: {
            x: Number,
            y: Number
        },
        computed: {
            config: function () {
                const position = {
                    a: this.x,
                    b: this.y
                };
                return {
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
