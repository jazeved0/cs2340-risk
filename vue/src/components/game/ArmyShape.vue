<template>
	<v-shape :config="config">
	</v-shape>
</template>

<script>
  export default {
    props: {
      data: Object,
    },
    computed: {
      config: function () {
        const state = this.$store.state;
        if (state) {
          const color = ('color' in this.data
						&& state.settings.settings.colors.length > this.data.color)
            ? '#' + state.settings.settings.colors[this.data.color]
            : '#FFFFFF';
          const position = ('position' in this.data
						&& state.game.gameboard.centers.length > this.data.position)
						? state.game.gameboard.centers[this.data.position]
						: [0, 0];
          const radius = 10;
          return {
            fill: color,
            x: position[0],
            y: position[1],
						width: radius,
						height: radius,
            shadowBlur: 3,
						shadowOpacity: 0.2,
            listening : false,
            sceneFunc: function (context, shape) {
              context.beginPath();
              context.arc(
                0, 0,
								shape.hasAttr('width') ? shape.getAttr('width') : 4,
								0, 2 * Math.PI, false);
              context.closePath();
              context.fillStrokeShape(shape);
            }
          };
        } else return {};
      }
    }
  }
</script>
