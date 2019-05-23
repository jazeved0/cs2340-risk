<!--suppress HtmlUnknownTag -->
<template>
  <div class="stage-wrapper" ref="stageWrapper">
    <v-stage :config="stageConfig" ref="stage" class="stage">
      <v-layer>
        <v-line v-for="waterConnection in waterConnectionConfigs"
            :key="waterConnection.num"
            :config="waterConnection"></v-line>
      </v-layer>
      <v-layer>
        <!-- Territory custom shapes -->
        <v-territory
            v-for="(territory, index) in territoryData"
            :key="index"
            :castle=   "territory.castle"
            :highlight="territory.highlight"
            :baseColor="territory.baseColor"
            :path=     "territory.path"
            @territory-click-raw="handleTerritoryMouseDown(index)">
        </v-territory>
      </v-layer>
      <v-layer>
        <v-castle-icon v-for="castle in castleData"
            v-bind="castle" :key="castle.num"></v-castle-icon>
        <v-army-shape v-for="army in armyData"
            v-bind="army" :key="army.num"></v-army-shape>
      </v-layer>
    </v-stage>
  </div>
</template>

<script>

  import {clamp, distance, exists, logError} from "./util";

  // Components
  import ArmyShape from './ArmyShape';
  import CastleIcon from './CastleIcon';

  // hook Konva
  import VueKonva from 'vue-konva';
  import Vue from "vue";
  import Territory from "./Territory";
  // noinspection JSUnresolvedFunction
  Vue.use(VueKonva);

  // Smart component: accesses state
  export default {
    props: {
      // Array of territory indices to highlight
      highlight:      Array,
      // Border color to add to the highlighted territories
      highlightColor: String
    },

    components: {
      'v-army-shape': ArmyShape,
      'v-territory': Territory,
      'v-castle-icon': CastleIcon
    },

    data () {
      return {
        // DOM object for the stage, gotten through refs upon mount
        stageObj: undefined,
        // Current stage dimensions updated upon resize
        stageDimensions: {
          w: 400,
          h: window.appHeight
        },
        // Current touch state (unused if on desktop)
        touchState: {
          lastDist: 0,
          point: undefined
        },
        // Current scale bounds (may be updated upon initialization as appropriate)
        scaleBounds: {
          min: 0.8,
          max: 5
        },
        // Current index of territory with mouse over
        mouseOver: -1
      }
    },

    computed: {
      // Configuration objects for the stage
      stageConfig () {
        return {
          // Depend on dimensions changed upon resize
          width: this.stageDimensions.w,
          height: this.stageDimensions.h,
          draggable: true,
          // Clamp dragging
          dragBoundFunc: (pos) => this.clampPosition(pos)
        }
      },

      // *****************
      // Rendering configs
      // *****************

      // Creates array of territory data
      territoryData () {
        return [{"castle":null,"highlight":null,"baseColor":"C5C7CF","path":"m50 73.7h18.1l9.6 5.9 4.7 11.4 12.2 5.8 18.5-8.5 1.6-19.3-10.8-15.7 20.2-13.8 4.2-12.8-0.3-1.5-8.2 0.5-9.3-4.7-12.3 8.2-8.2-5.8-15.7 5.8h-19.3l-15.8-0.6-10 12.9v19.3l19.9 7.6 0.9 5.3"},{"castle":{"a":175.60000610351562,"b":63.29999923706055},"highlight":"blue","baseColor":"C5C7CF","path":"m137.8 72.7 9 14.1c3 0.9 25 7.3 26.1 7.6 1 0.3 17.4 3.6 26 5.3l0.3 0.1 7.1-13.1 24.6-6.4 14.6 1.8s-0.6-11.1-2.3-11.1c-1.8 0-31.6-3.5-31.6-3.5s-7-5.8-7-7.6 4.7-16.4 4.7-16.4 6.4-5.3 1.8-7.6-7-6.4-12.3-7-15.5-4.1-18-1.8-5.4-4.7-7.8-5.3-13.4-0.6-16.9 2.9-18.7 0-18.7 0l-7.5 0.5 0.4 1.8-4.6 13.9-19.1 13.1 9.7 14c0-0.1 21.5 4.7 21.5 4.7"},{"castle":null,"highlight":null,"baseColor":"C5C7CF","path":"m34.6 147.7 17.7-3.4 20-9.4 28.2-15.9c21.4-14.8 23.1-14.7 24.7-13.9 1.9 0.6 15 2.8 23.9 4.3l30.3 4.6 24 15 6.6-1.8c-3.3-5.1-7.5-11.7-9-12.5-1.9-1-2.3-10.1-2.3-13.1h-0.2c-1-0.2-24.8-5-26.1-5.3s-25.5-7.4-26.6-7.7l-0.4-0.1-8.9-14-20.1-4.5-1.7 19.7-20.3 9.4-13.7-6.5-4.7-11.6-8.7-5.2h-17l0.6 3.9s-9.9 3.5-11.7 3.5-15.8 8.8-15.8 8.8v20.5 14.6l10.5 17.5 0.7 3.1"},{"castle":null,"highlight":"blue","baseColor":"C5C7CF","path":"m42.1 216.9 12.3 7.6-13.4 7 26.9 8.2 14.8 7.4 13.1-14.7 1.6-25.2-19.4-5-7.1-31.2 8.7-10.1-7.4-11.4 2.1-9.1-20.5 9.5-17.9 3.5 3.3 15.6 4.1 22.2-11.1 14s-15.8-2.9-15.8-1.2c0 1.8 25.7 12.9 25.7 12.9"},{"castle":null,"highlight":"blue","baseColor":"C5C7CF","path":"m82.8 197.5 20.6 5.3-2.1 31.9-3.5 4 14.4 0.1 8.7 4 23.9-2.4c2.3-13.2 3.2-13.2 4-13.2 0.7-0.2 6.3-3 11.4-5.7v-32.1l-11.5-23.4 5.9-23.8 20.9-9.9 6.9-9.6-5.1-3.2-29.1-4.4c-16.5-2.7-22.2-3.8-24.2-4.3-2.7 1.2-12.1 7.4-20.4 13.1l-0.2 0.1-22.4 12.6-2.8 11.8 8.4 12.9-9.6 11.2 5.8 25"},{"castle":null,"highlight":null,"baseColor":"C8E5C1","path":"m176.9 133.7-20.5 9.7-5.5 22.2 5.2 10.6 38.7-3.6 21.3 13.3 29.6-11.4 25.8 7.9 18.1-31.7 0.5-0.1c11.5-1.9 24.3-4.4 26.4-5.3 0.2-2.7 0.1-19.7 0-31.2l-13.8-30.1 0.5-23.8-16.2 4.7-14.6 10.5 4.1 11.1 17 5.3-8.8 18.1s0.6 5.3 2.3 7.6c1.8 2.3 14.6 5.3 14.6 5.3s-8.2 9.9-10.5 9.9-32.1-0.6-32.1-0.6v9.9l-11.1 5.3v11.7l-14.6 0.6s-5.9-12.9-5.9-14.6-2.3-11.7-2.3-11.7-9.4 1.2-11.1-0.6c-0.1-0.1-0.3-0.3-0.4-0.5l-1.1 0.2-9.8 2.7-18.5-11.5-7.3 10.1"},{"castle":{"a":105.19999694824219,"b":286},"highlight":null,"baseColor":"C8E5C1","path":"m133.9 330.9 11.8-3.7-5.7-29-11.3-24.8h-28.3l-5.9-4.6c-0.6 0.9-1.2 1.5-1.6 1.8-2.3 1.8-25.7 3.5-28.1 4.7s-19.3 5.3-21 5.3-14-8.2-15.8-11.1-6.4-11.7-11.7-11.7-15.7 14.6-16.3 17.5 14.6 17 14.6 17l19.3 1.2 21 3.5 17.5 22.6 33.3-6.9 28.2 18.2"},{"castle":null,"highlight":"blue","baseColor":"C8E5C1","path":"m101.1 271.5h28.8l11.9 26.2 5.7 28.9 22-7 19.9-24.4 17.7-7.1-2.5-9.4c-3 0.6-12.6 2.4-15.4 2.4-3.5 0-6.1-12.5-7.3-20v-0.2l4.1-11.8-1-8.1-12.3-8.4-11.5-9.5c-6.3 3.4-10.2 5.4-11.7 5.8-0.6 1.2-1.9 7.1-2.8 12.4l-0.1 0.7-25.9 2.6-9-4.2h-15.6l-8.1 9.1 14.2 6.4s-3.7 6.8-6.7 11.2l5.6 4.4"},{"castle":null,"highlight":"blue","baseColor":"C8E5C1","path":"m240.1 280.1v-29.8l-17.9-40.6 5.9-26.5-12.4 4.8-21.5-13.5-37.3 3.5 5.3 10.8v32.7l11.6 9.6 12.9 8.8 1.2 9.4-4.1 11.7c1.6 9.4 4.1 17.7 5.4 18.1 3 0 15.8-2.5 15.9-2.5l0.9-0.2 2.9 10.9 6.8-2.7 24.4-4.5"},{"castle":{"a":252.3000030517578,"b":198},"highlight":"blue","baseColor":"C8E5C1","path":"m292.1 187.8-19.8-3.3-0.1 0.1-26.6-8.2-15.3 6-6.1 27.1 17.9 40.6v29.7l5.2-1 13.5 9.1 34.6-51.7-3.3-48.4"},{"castle":null,"highlight":null,"baseColor":"C8E5C1","path":"m297.3 235.8 23.8 8-5.1-8.7 18.8-10-2.1-19.5 9.9-4.6c-3.5-6.4-8.8-15.9-9.5-16.8 8.1 6.5-3.2-2.7-16.5-15.4l-0.4-0.4 2.5-15.3-2.1-6.1c-2.4 0.9-8.7 2.4-26 5.3l-17.3 30.4 20.6 3.4 3.4 49.7"},{"castle":null,"highlight":null,"baseColor":"A0B394","path":"m273.6 319.6c0 0.5-0.9 2-2.3 3.9l13 19c4.7 2.5 9.7 5 13.6 6.9l11.5-9-8.4-8-2.2-9.4 2.1-15.2-13.4-24.7-15-9.5-10.2 15.2 9.4 6.3c0.1 0.1 1.9 22.6 1.9 24.5"},{"castle":null,"highlight":null,"baseColor":"A0B394","path":"m333.3 325.9c4.6 0.5 8.3 0.9 11.3 1.3l-5.8-21.2 3-19.8-14.4-17.3-5-22.7-25.6-8.6-23.1 34.5 15.4 9.7 13.9 25.7-2.1 15.5 2 8.5 8.2 7.8 22-13.4h0.2"},{"castle":null,"highlight":"blue","baseColor":"A0B394","path":"m242.3 356.5-6.3 10 3.6 9.8 17.4 5 29.3 5.9 4.8-9 15.3-7.3c-1.3-5.6-2.7-10.8-3.4-12.7-2.3-0.9-7.9-3.5-22-10.9l-0.6-0.3-12.6-18.5c-4.1 5.4-8.8 11.2-8.8 11.2l-22.6 10 5.9 6.8"},{"castle":{"a":243.39999389648438,"b":435.70001220703125},"highlight":null,"baseColor":"A0B394","path":"m193.7 502.6 10.2-2.2 23.8-36.9 25.6 9.3 28.3-2.7 9 4.2c-0.7-4.3-1.3-7.4-1.8-8.3-1.2-2.3-12.3-24-9.9-28.6s0-36.2 0-36.2l6.5-12-28.8-5.8-18.4-5.3-3.4-9.3-3.7 5.9-17.9 25.4 14.4 25.9-21.5 34-14.9 29.3 6.4-0.6-3.9 13.9"},{"castle":null,"highlight":"blue","baseColor":"A0B394","path":"m204.4 459.1 20.8-33.5-14.4-25.9 18.6-26.4 10.5-16.6-5.4-6.3-5.9 2.6-22.8-8.8s-14 9.4-8.8 9.4 9.9 6.4 9.9 6.4l12.3 4.1-16.4 11.1-18.1 14-3.9 7s-14.2 2.9-18.9 4.1-27.5 8.8-29.2 9.4c-1.8 0.6-19.3 5.9-21 6.4s-24 12.3-24 12.3 7 15.8 2.9 15.8h25.1l39.2-10.5 15.2-11.1 10.7 11.1v22.2l-10.7 7.6-15.8 8.8-1.8 18.7h21l15.3-1.5 15.6-30.4"},{"castle":null,"highlight":"blue","baseColor":"F3E4D4","path":"m336.6 224.1 9.3-4.9 26.4 20.4 20.1 11.9 9-10.9c-0.5-0.6-0.8-1.1-0.9-1.4-0.6-1.8-2.9-16.4-2.9-16.4l4.7-7.6 8.8-4.7-13.4-3.5-8.2 3.5-2.9-11.1s0.5-1 1.1-2.3l-14.2-0.4-12.4-5.6-17.3 7.5c0.4 0.7 0.8 1.5 1.3 2.3l0.5 0.9-10.5 4.9 1.5 17.4"},{"castle":{"a":356.79998779296875,"b":286},"highlight":null,"baseColor":"F3E4D4","path":"m358.5 329.8c0.5 0.6 6.5 5.3 10.8 8.8 1 0.8 1.9 1.5 2.7 2.2l16.3-1.3-7.6-20.3 8.4-31 2.5-34.9-20.4-12.1-25.5-19.7-27.1 14.4 5.2 8.9 0.2 0.1 5.2 23.3 14.6 17.6-3 20.3 6 21.6c10.9 1.2 11.5 1.9 11.7 2.1"},{"castle":null,"highlight":null,"baseColor":"F3E4D4","path":"m436.4 309.4v-15.8l9.5-2.1v-9.3l-11-4.8 6.2-13 0.4-0.7-4.7-10.6s-5.9-8.3-7.6-7.6-6.4-6.4-8.2-6.4-12.3 9.9-12.3 9.9-3.7-3.9-6.1-6.9l-9 11-2.5 35.4-8.3 30.6 7.4 19.7 9.2-7.4 14.1 1.6h22.3l-7.1-14 7.7-9.6"},{"castle":null,"highlight":null,"baseColor":"F3E4D4","path":"m387 414.4 1.7-5.7 0.9 0.3c0.6 0.2 15 4.3 16.2 4.7 0.7 0 4.4-1.3 7.9-2.8l-8.5-9.3 7-14.8-20.5-14.9c-4-6.7-14.5-24.1-14.9-24.7-0.4-0.4-4.8-3.9-8.7-7-6.8-5.4-10.2-8.2-11-9-1.5-0.7-13.1-2.2-23.6-3.4l-22 13.4-11.6 9.1c2.7 1.3 4.7 2.3 5.5 2.5 1.9 0.1 3.1 1 6.5 15.4l7.4-3.5 20.5 7.6s10.5 3.5 13.4 7 19.3 18.7 19.3 18.7l14 15.8c-0.1 0.1 0.1 0.3 0.5 0.6"},{"castle":null,"highlight":null,"baseColor":"F3E4D4","path":"m378.4 346.1c0.4 0.6 13 21.4 14.8 24.6l20.4 14.8 24.7-2.4 16.3-13.1c2.6-3.4 5.3-7.6 5.2-8.6-0.6-1.4-4.6-10.9-5.3-12.5l-17.5-14h-23.6l-13.5-1.6-9.8 7.9-15.9 1.3c3.2 2.7 4 3.3 4.2 3.6"},{"castle":{"a":452.1000061035156,"b":304.1000061035156},"highlight":"blue","baseColor":"F3E4D4","path":"m518.5 319.6-7 3.6-12.3-7h14l-1.8-11.1-18.1 4.7-7-4.7-11.7-5.9-0.6-9.4-20.5-7s18.1-5.3 17-9.9-12.3-9.9-13.7-9.9-13.1 5.9-13.1 5.9l-1.3-2.9-5 10.5 10.3 4.5v12l-9.5 2.1v14.9l-7.4 9.1 7.2 14.1 18 14.4 0.1 0.2c0 0.1 4.7 11.2 5.4 12.8 0.7 1.5-1.6 5.2-4.1 8.6l21.3-3.9 15.3 4.3c0.5-1.3 2.1-6.4-2.5-7.9l-5.3-1.8 5.3-9.9 1.8-7 9.9-2.9s13.4-8.8 15.2-8.8 8.8-12 8.8-11.8c0.1 0.3-8.7 0.1-8.7 0.1"},{"castle":null,"highlight":null,"baseColor":"F3E4D4","path":"m478.7 367.4-23 4.2-16.6 13.4-25.1 2.4-6.5 13.8 9.4 10.3-1.2 0.5c-4 1.8-9.1 3.9-10.5 3.4-1.1-0.4-12-3.5-15.2-4.4l-1.4 4.8c3.8 3.7 11.4 11.3 11.4 12.6 0 1.8 18.7 5.3 18.7 5.3l7.6-5.3 17.5 5.3 10.5-9.9s2.9-12.3 5.3-12.9 23.4-9.4 23.4-9.4l15.8-12.9-4.2-16.9-15.9-4.3"},{"castle":{"a":138.60000610351562,"b":540.7999877929688},"highlight":null,"baseColor":"EAE7C4","path":"m169.7 564.6c17.1 16.2 17.1 17.3 17.1 18.7 0 0.7-0.4 5-0.8 9.3l19.8-6.3 19.7-47-11.2-33h-10.8l-25.6 5.5-21.5 16.8-23.8 1.3c-20.4 30.2-23.3 31.5-25.4 31-1.9 0.3-8.6 3.1-15.1 6.1l13.6 17.2 13.4-9.3 24.9-8.4 24.8-2.7 0.9 0.8"},{"castle":null,"highlight":null,"baseColor":"EAE7C4","path":"m215.7 504.5 11.6 34.3 34.2 18 47.3-43-9.2-29-19.1-8.9-27.9 2.7-22.6-8.1-21.9 34h7.6"},{"castle":null,"highlight":null,"baseColor":"EAE7C4","path":"m200.7 658 21.8-33.2 0.2-0.1 27.2-13.2 36.9-16.2-25.8-36.6-34-17.9-19.8 47.2-21.4 6.8c-0.3 2.8-0.5 5.3-0.7 6.8l-0.1 0.7-18.6 28.5 11.7 19.4 2.1 17.6 17.2 11.5-1-26.7 4.3 5.4"},{"castle":null,"highlight":null,"baseColor":"EAE7C4","path":"m309.9 517.1-0.4-1.3-46.6 42.3 25.9 36.7 69.2 5.7 2.3-15.1 1.7-0.5c2.9-0.9 6.3-2.1 8.2-3-0.4-1.8-0.5-4.9 0-10.2l-10.9 4.5-0.8-3.2c-7-26.7-6.6-28.3-6.4-29.1 0.3-1.4 3.6-10.7 5.4-15.9-8.1-1.8-18-3.8-19.3-3.9-1.6 0-15.4-3.5-26.6-6.5l-1.7-0.5"},{"castle":null,"highlight":"blue","baseColor":"EAE7C4","path":"m342.9 666.2-1.5-1 3-16.8 6.2-17.3 19-16.1-3.8-4.5-8.2-7.7v-0.3l-69.3-5.7-37.7 16.5-26.9 13.1-21.9 33.3 12.7 16.2 20-8 32.1 4.7 19.5-8.4 14.8-3.3 23.1 24.9 25.6 18.7 23.3-5.3c1.4-7.8 2.7-15.9 3-19.1-3.2-1.1-12-2.5-14.1-2.5-0.7-0.2-3.3-0.2-18.9-11.4"},{"castle":null,"highlight":null,"baseColor":"B4CDE2","path":"m320 69.1 18.1 7.1 7.2 3 6.6-20.2 16-6.2 12.9-3.7v-12l-7.6-3.2h-9.1l-6.8 4.7-12-11.2-0.4-0.2-8.2 3.8-9.3 12.8-15.1 11.9 7.7 13.4"},{"castle":null,"highlight":null,"baseColor":"B4CDE2","path":"m357.5 36.2 6-4.2 10.3 0.1 8.9 3.8v14.8l-14.1 4-15.1 5.9-6.4 19.4 9.3 3.8 14-2.4 5.8-7 15.2 3.3c0.8 0.5 1.5 1 2.1 1.5l5.4-12 8.5-9v-16.2l12.3-8.7 5.8-19.4 9.1-3 15.1-6.8h19.9l-13.6-4.1h-40.3s-9.4 10.5-13.4 8.2-40.3 11.1-40.3 11.1l-15 7 10.5 9.9"},{"castle":{"a":322.5,"b":87.0999984741211},"highlight":null,"baseColor":"B4CDE2","path":"m318.1 114.9 24.3-4.5 21.7 13 20.9-3.6 12-16.2 1.6-3.2c1.7-5.3 3.4-11.5 3.5-12.8-0.9-0.9-7.3-5.3-11.8-8.4l-13.5-2.8-5.5 6.6-15.2 2.7-18.8-7.7-18.6-7.4-8-13.8-2.1 1.6-3.8 1.1-0.5 23.9 13.7 30.2c0.1 0.5 0.1 0.9 0.1 1.3"},{"castle":null,"highlight":null,"baseColor":"B4CDE2","path":"m320.7 153-2.4 14.8c6.1 5.8 15.2 14.3 16 14.9 0.2 0.1 0.8 0.4 8.4 14.3l18.3-8 10.6 4.8 12.9-12.3-12.2-25.8 20.6-15.7-7.9-18.2-21.2 3.6-21.7-13-23.9 4.5c0.1 8.8 0.2 25.9 0 28.7l2.5 7.4"},{"castle":null,"highlight":null,"baseColor":"B4CDE2","path":"m420.3 150.8 4.7-18.1 12.9-14.6 2.4-0.7-3.7-9c-7.5-3.2-19-7.8-20.5-8.1-1.5-0.2-10.5 1.4-16.1 2.5l-13.4 18 8.6 19.7-20.6 15.6 12.1 25.6-13.3 12.7 0.5 0.2 14.6 0.4c1.1-1.9 2.3-3.8 3.1-4 1.8-0.6 7.6-14 7.6-14l-2.9-9.9 12.9-10.5 11.1-5.8"},{"castle":{"a":423.70001220703125,"b":61.79999923706055},"highlight":null,"baseColor":"F2F2F2","path":"m450.1 19.9c-2.3 1.2-2.9-10.5-2.9-10.5l-8.7 2.2-11.6 3.8-5.7 19.1-12 8.5v15.9l-8.9 9.4-5.3 11.9c5.6 3.9 8.6 6.2 8.9 6.7 0.2 0.4 0.6 1.2-3.4 13.8 3.7-0.7 13.8-2.6 15.8-2.3 2.1 0.3 16.8 6.5 20.6 8.1l6-8.6 14.5-15 16.6 12.1h18.7l4.3-20.9 24.3-7.4 8.8-34 2-18.1-11.5 0.6-25.7 12.9 13.1-13.5s-2.3-1.2-7.6-1.2-33.3 2.9-33.3 2.9-14.6 2.4-17 3.6"},{"castle":null,"highlight":null,"baseColor":"F2F2F2","path":"m520.7 97-19.2 12-8.3-12.1h-19.6l-15.8-11.5-13.3 13.7-6.1 8.7 3.7 9 7.5-2.2 24.5 12.3 12.3 18.7-2.9 13.4 9.9 3.5-3.5 11.1s-7 10.5-9.4 11.7-14-0.6-14-0.6-13.4 2.9-14.6 5.3-5.3 17.5-5.3 17.5l16.4-5.3 3.5 5.9 13.4-14.6 18.1-1.8 5.9-13.4-2.9-16.3 1.2-13.4-2.9-10.5 7.6-11.7 15.8-8.2-0.6-8.2 8.8-7.3-10.2-5.7"},{"castle":null,"highlight":null,"baseColor":"F2F2F2","path":"m587.7 72.2-6.9-37 0.1-0.2 10.2-22.4-7.8 2-30.9-1.2-18.1 1-2.1 18.6-9.1 35.2-24.2 7.3-4.2 20.2 7.3 10.7 18.7-11.6 11.7 6.5 3.1-2.5 19.3-15.8 17.5 6.4 1.9 1.8 13.5-19"},{"castle":null,"highlight":null,"baseColor":"F2F2F2","path":"m593.5 12-10.7 23.5 3.6 19.5 52-7.4 50.8-12.6 2-21h-0.1c-3.1-1.8-27.6-4.1-27.6-4.1l-1.8 12.3-11.8-4.1s-17.4-7-19.1-8.2-29.2 0-29.2 0l-8.1 2.1"},{"castle":null,"highlight":null,"baseColor":"F2F2F2","path":"m689.3 37-50.6 12.5-52 7.4 2.9 15.7-14 19.8 7.6 6.9s-3.7 8.8 0 8.8 15.9-8.2 15.9-8.2l-8.8-11.7 12.9-9.3s31-11.1 33.9-9.9 41.5-0.1 41.5-0.1l17.3 3.9-6.6-35.8"},{"castle":{"a":727.5,"b":51.29999923706055},"highlight":null,"baseColor":"F2F2F2","path":"m758.8 31.6-20.5-5.3s-7 1.2-8.8 0-14-12.3-14-12.3-16.3 1.3-22.5 0.5l-2 21.2 6.9 37.5 7 1.6 33.9 6.4 21-4.1v-15.7l11.1-21-12.1-8.8"},{"castle":null,"highlight":null,"baseColor":"DBA9A9","path":"m628.9 357.1 14.8-17.1v-19.8h32.6l17.8-12.5 5.3-12.3c-0.8-2.2-1.5-4.3-1.7-4.8-0.4-1.3-9.2-2.6-9.2-2.6s-18 3.1-19.3 3.5-11.4 2.6-11.8 1.3-5.5-3.5-5.5-3.5l-8.3 1.3-7.2-20.2h-8.4l-5.3 8.8-4.8-1.8-10.1 2.6-10.1-5.7h-11l-5.3 4-6.6 11.4 2.6 9.6 16.7 18-7.5 4.4-4 11.4 8.8 21.9 8.3 18 4.4-6.1h9.2l5.3-7.9 8.8-4 1.5 2.1"},{"castle":{"a":771.2000122070312,"b":268.70001220703125},"highlight":"blue","baseColor":"DBA9A9","path":"m703.8 262.2c-2.6 0.9-3.5 8.1-3.5 8.1l3.5 0.2s9.6-0.4 10.9 0 12.7 3.5 12.7 3.5l8.8-3.5s17.5 3.5 19.7 4.8 4 11 4 11v15.3l-11 3.5h-8.8s-10.5 4.4-11.8 5.3-10.1 0.9-10.1 0.9l-9.2-10.1-7.5 0.4s-0.6-1.6-1.3-3.5l-4.7 10.9-17.3 12.1 22.7 14.8 37.2 10.7 32.1-20.5 13-28.4 24.3-14.1 9-14.8-8.9-5-13.2-7.5-11.4-4.8-14.9 4.8-17.1-7.4s-19.3 1.3-21 1.3-12.7-3.1-15.3-3.9-18.4-1.3-20.2-0.4-7.5 6.1-7.5 6.1 11.4 3.1 13.6 3.1c2.3 0.1 5.8 6.2 3.2 7.1"},{"castle":null,"highlight":null,"baseColor":"DBA9A9","path":"m842.8 345.2-3-9.9v-11.8l-3.5-0.9s-4.4 10.1-5.7 10.1-2.2-7.9-2.2-7.9l2.2-13.6v-6.1l-4-16.7v-14l-8.3-4.6-0.1 0.1-9.2 15.2-24.2 14-13 28.3-32.3 20.6 9.7 25.4 93.6-28.2"},{"castle":null,"highlight":"blue","baseColor":"DBA9A9","path":"m683 405.4 5.6-3.2 12.7 0.5 0.8 1c1.1 1.4 2.5 3.1 3.4 4.6l35.2-4.7 0.2 0.1c2.2 0.9 4.3 1.7 6.3 2.6l6.7-14.8-16.3-43-37.5-10.9-23.7-15.5h-30.7v18.6l-15.6 18.1 6.7 9.5v11l6.5 16.5 11.7 6.8 16.9-1.7 11.1 4.5"},{"castle":null,"highlight":"blue","baseColor":"FFD8B3","path":"m859.5 377.9c-0.9-1.3-5.7-9.2-7-10.5s0-10.1 0-10.1l-9.2 3.5v-13.6l-93.4 28.2 6.1 16.1-7 15.5c30.3 12.4 37.7 16.4 39.5 17.8l12 1.5 7.3-5.7v-9l15-2.4 0.2 0.1c2 0.8 4.5 1.6 5.2 1.7 1.1-0.5 7.4-4 11.2-6.3l0.2-0.1 25.9-6.6v-8.3c0.1 0.1-5.2-10.5-6-11.8"},{"castle":null,"highlight":null,"baseColor":"FFD8B3","path":"m698.4 408.4-8.4-0.4-6.6 3.8-12.5-5.1-17.3 1.8-14.8-8.6-12.1-10.2-15.3 7.7-6.6-8.5-2 2.7v14.2h9.5l10 21.6-6 16.1c17 7.1 35.4 14.5 38.1 14.9h2.5l3.3-2.3 6.7-8.3 1-5.4-13.6-3.2 7.6-13.8 18.8 2.9 12.3-0.4 1-6.2 0.4-0.6c2.5-3.4 5.3-7.6 6.3-9.4-0.5-0.9-1.4-2.2-2.3-3.3"},{"castle":{"a":729.2999877929688,"b":453},"highlight":"blue","baseColor":"FFD8B3","path":"m749.7 492.6 17-11.2-3.8-42.3 0.6-0.3c10.3-5.1 21.3-11.1 23.5-13-3.9-2.6-30-13.6-46.5-20.3l-34 4.6c0 0.1 0.1 0.1 0.1 0.2 0.4 1.3 0.9 2.7-7 13.5l-1.6 9.5-17.5 0.5-15.4-2.4-2 3.7 11.6 2.7-2.3 12.3-5.3 6.6 1.7 4.5 9.2 10.1 9.2 8.8 4.4 9.6s13.6 6.6 14.9 6.6 8.8-2.2 8.8-3.5 0.9-4.8 0.9-4.8l-7.5-4-4.8-4.4-3.1-7 8.8 3.1 10.5 3.1 9.7 4.8 19.3 8.8 0.6 0.2"},{"castle":null,"highlight":"blue","baseColor":"FFD8B3","path":"m840.3 406.4c-5 2.9-11 6.3-11.6 6.4-0.3 0.1-0.9 0.2-6.1-1.7l-12.8 2.1v8.3l-8.7 6.8-12.4-1.6c-1.4 1.5-6.3 5-23.7 13.6l3.8 42.1-16.9 11.1 14.2 5.8 14-3.1 11-3.5s9.7-6.1 10.5-7.9c0.9-1.8 7.5-5.7 7.5-5.7l11.8-3.5 6.6 6.1 7.9-4 6.6-10.5 3.1-14.9 3.5-12.3 6.1-5.3 12.3-4.4 6.6-2.2v-21l-7.4-7.4-25.9 6.7"},{"castle":null,"highlight":null,"baseColor":"FFD8B3","path":"m635.9 485.3c-2.2 0-16.7 4-16.7 4l-3.1 18.4 19.7 4.8 36.8 4.4 7.9-15.8-11-14-7.5-11.4h-13.8c-1.8 0-12.3 9.6-12.3 9.6"}];
      },

      // Config data for water connection paths
      waterConnectionConfigs () {
        return [{"x":0,"y":0,"points":[473.20001220703125,337.70001220703125,618.2000122070312,309.8999938964844],"bezier":false,"tension":0,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":0,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[736.7000122070312,431.20001220703125,648.4000244140625,496.29998779296875],"bezier":false,"tension":0,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":1,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[637,426.29998779296875,648.4000244140625,496.29998779296875],"bezier":false,"tension":0,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":2,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[76.5,294.29998779296875,96.0999984741211,333.5,105.69999694824219,349.1000061035156,133.3000030517578,362.20001220703125,156.89999389648438,376,194.10000610351562,417.70001220703125],"bezier":false,"tension":0.30000001192092896,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":3,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[414.29998779296875,292,435.5,239.10000610351562,440.20001220703125,220.10000610351562,438.20001220703125,193.1999969482422,443.6000061035156,169.1999969482422,461.70001220703125,148.39999389648438,484.3999938964844,112.9000015258789],"bezier":false,"tension":0.30000001192092896,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":4,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[670.5999755859375,55.29999923706055,673.4000244140625,134.3000030517578,662.2999877929688,229.10000610351562,669.4000244140625,261.8999938964844,692.9000244140625,276.5,715.9000244140625,288.20001220703125,725.7999877929688,304.1000061035156,734.2999877929688,322.1000061035156],"bezier":false,"tension":0.30000001192092896,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":5,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[314.29998779296875,560.5999755859375,438.3999938964844,405.70001220703125],"bezier":false,"tension":0,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":6,"isRootInsert":false,"elm":"[object Text]"},{"x":0,"y":0,"points":[648.4000244140625,496.29998779296875,314.29998779296875,560.5999755859375],"bezier":false,"tension":0,"strokeWidth":3,"stroke":"white","dash":[6,6],"opacity":0.7,"num":7,"isRootInsert":false,"elm":"[object Text]"}];
      },

      // Rendering data for each castle
      castleData () {
        return [{"x":175.60000610351562,"y":63.29999923706055},{"x":105.19999694824219,"y":286},{"x":252.3000030517578,"y":198},{"x":243.39999389648438,"y":435.70001220703125},{"x":356.79998779296875,"y":286},{"x":452.1000061035156,"y":304.1000061035156},{"x":138.60000610351562,"y":540.7999877929688},{"x":322.5,"y":87.0999984741211},{"x":423.70001220703125,"y":61.79999923706055},{"x":727.5,"y":51.29999923706055},{"x":771.2000122070312,"y":268.70001220703125},{"x":729.2999877929688,"y":453}];
      },

      // Rendering data for each army
      armyData () {
        return [{"size":2,"color":"#8e44ad","x":77.0999984741211,"y":54.70000076293945,"num":0},{"size":2,"color":"#2980b9","x":153.39999389648438,"y":50.20000076293945,"num":1},{"size":2,"color":"#27ae60","x":54.400001525878906,"y":112.19999694824219,"num":2},{"size":2,"color":"#2980b9","x":56.900001525878906,"y":196.89999389648438,"num":3},{"size":2,"color":"#2980b9","x":123.19999694824219,"y":174.5,"num":4},{"size":2,"color":"#27ae60","x":194.5,"y":151.3000030517578,"num":5},{"size":2,"color":"#8e44ad","x":76.5,"y":294.29998779296875,"num":6},{"size":2,"color":"#2980b9","x":158.8000030517578,"y":268.20001220703125,"num":7},{"size":2,"color":"#2980b9","x":194,"y":205.5,"num":8},{"size":2,"color":"#2980b9","x":262.29998779296875,"y":239.10000610351562,"num":9},{"size":2,"color":"#27ae60","x":314.79998779296875,"y":198.60000610351562,"num":10},{"size":2,"color":"#27ae60","x":284.20001220703125,"y":310.70001220703125,"num":11},{"size":2,"color":"#27ae60","x":317.8999938964844,"y":287.8999938964844,"num":12},{"size":2,"color":"#2980b9","x":269.70001220703125,"y":361.5,"num":13},{"size":2,"color":"#8e44ad","x":251.1999969482422,"y":409.79998779296875,"num":14},{"size":2,"color":"#2980b9","x":194.10000610351562,"y":417.70001220703125,"num":15},{"size":2,"color":"#2980b9","x":369.20001220703125,"y":213.10000610351562,"num":16},{"size":2,"color":"#27ae60","x":352.6000061035156,"y":254.60000610351562,"num":17},{"size":2,"color":"#27ae60","x":414.29998779296875,"y":292,"num":18},{"size":2,"color":"#8e44ad","x":338.3999938964844,"y":350.3999938964844,"num":19},{"size":2,"color":"#8e44ad","x":421.20001220703125,"y":358.70001220703125,"num":20},{"size":2,"color":"#2980b9","x":473.20001220703125,"y":337.70001220703125,"num":21},{"size":2,"color":"#8e44ad","x":438.3999938964844,"y":405.70001220703125,"num":22},{"size":2,"color":"#27ae60","x":190.5,"y":542.5999755859375,"num":23},{"size":2,"color":"#27ae60","x":252.1999969482422,"y":511.8999938964844,"num":24},{"size":2,"color":"#8e44ad","x":240.89999389648438,"y":582,"num":25},{"size":2,"color":"#27ae60","x":314.29998779296875,"y":560.5999755859375,"num":26},{"size":2,"color":"#2980b9","x":286.79998779296875,"y":634.2000122070312,"num":27},{"size":2,"color":"#8e44ad","x":341,"y":52.900001525878906,"num":28},{"size":2,"color":"#8e44ad","x":397.5,"y":26.899999618530273,"num":29},{"size":2,"color":"#8e44ad","x":370.29998779296875,"y":100.30000305175781,"num":30},{"size":2,"color":"#27ae60","x":346.8999938964844,"y":151.60000610351562,"num":31},{"size":2,"color":"#8e44ad","x":411.29998779296875,"y":125.4000015258789,"num":32},{"size":2,"color":"#8e44ad","x":474.5,"y":47.599998474121094,"num":33},{"size":2,"color":"#27ae60","x":484.3999938964844,"y":112.9000015258789,"num":34},{"size":2,"color":"#27ae60","x":555.7999877929688,"y":58,"num":35},{"size":2,"color":"#8e44ad","x":616.0999755859375,"y":27.200000762939453,"num":36},{"size":2,"color":"#27ae60","x":670.5999755859375,"y":55.29999923706055,"num":37},{"size":2,"color":"#27ae60","x":713.5,"y":36.20000076293945,"num":38},{"size":2,"color":"#8e44ad","x":618.2000122070312,"y":309.8999938964844,"num":39},{"size":2,"color":"#2980b9","x":734.2999877929688,"y":322.1000061035156,"num":40},{"size":2,"color":"#8e44ad","x":803.5999755859375,"y":327.3999938964844,"num":41},{"size":2,"color":"#2980b9","x":688.2000122070312,"y":368.3999938964844,"num":42},{"size":2,"color":"#2980b9","x":794.2000122070312,"y":392.6000061035156,"num":43},{"size":2,"color":"#27ae60","x":637,"y":426.29998779296875,"num":44},{"size":2,"color":"#2980b9","x":736.7000122070312,"y":431.20001220703125,"num":45},{"size":2,"color":"#2980b9","x":800.7000122070312,"y":453.6000061035156,"num":46},{"size":2,"color":"#8e44ad","x":648.4000244140625,"y":496.29998779296875,"num":47}];
      },

      // Gets the size of the gameboard from the store
      gameboardSize () {
        return {"a":873.5,"b":704.2000122070312};
      },

      // Gets the effective player card component width
      playerInfoWidth () {
        return 320;
      },
    },

    methods: {
      // ***************
      // Utility methods
      // ***************

      // Calculates the initial position & scale of the map in the viewport
      calculateInitialTransform () {
        const bounds = this.stageDimensions;
        let totalW = bounds.w;
        let totalH = bounds.h;
        const playerInfoHeight = 200;
        if ('playerInfo' in this.$refs) {
          if (this.playerInfoWidth > (bounds.w * 0.7)) {
            totalH -= playerInfoHeight;
          }
        }
        const size = this.gameboardSize;
        // make initial map take up 3/4 of smaller dimension
        const margin = Math.min(totalW, totalH) / 8;
        const kw = (totalW - 2 * margin) / size.a;
        const kh = (totalH - 2 * margin) / size.b;
        const k = Math.min(kw, kh);
        return {
          x: (totalW - (size.a * k)) / 2,
          y: (totalH - (size.b * k)) / 2,
          scale: k
        };
      },

      // Translates the pointer mouse pointer minus the stage offset
      relativePointer (clientX, clientY, stage) {
        return {
          x: clientX - stage.getContent().offsetLeft,
          y: clientY - stage.getContent().offsetTop,
        }
      },

      // Clamps the scale according to the current bounds
      clampScale (scale) {
        return clamp(scale, this.scaleBounds.min, this.scaleBounds.max);
      },

      // Clamps the position according to newly calculated position bounds
      clampPosition (pos) {
        const bounds = this.calculatePositionBounds();
        return {
          x: clamp(pos.x, bounds.x.min, bounds.x.max),
          y: clamp(pos.y, bounds.y.min, bounds.y.max)
        }
      },

      // Calculates the position bounds according to the current scale & dimensions
      calculatePositionBounds () {
        if (exists(this.stageObj)) {
          const scale  = this.stageObj.scale();
          const bounds = this.stageDimensions;
          const size   = this.gameboardSize;
          return {
            x: this.axisBounds(size.a * scale.x, bounds.w),
            y: this.axisBounds(size.b * scale.y, bounds.h)
          };
        } else {
          logError("StageObj or Gameboard are undefined. Ignoring");
          // default value
          return {
            x: {
              min: 0,
              max: 1
            },
            y: {
              min: 0,
              max: 1
            },
          };
        }
      },

      // Applies bounds, wrapping as necessary
      axisBounds (size, bound) {
        return (size < bound) ? {
          min: -(size / 2),
          max: bound - (size / 2)
        } : {
          min: bound / 2 - size,
          max: bound / 2
        }
      },

      // ***************
      // Event listeners
      // ***************

      // Handles territory clicks
      handleTerritoryMouseDown (index) {
        this.$emit('territory-click', index)
      },

      // Handles resize events
      handleResize () {
        if (exists(this.$refs) && exists(this.$refs.stageWrapper)) {
          this.stageDimensions = {
            w: this.$refs.stageWrapper.clientWidth,
            h: this.$refs.stageWrapper.clientHeight
          }
        } else {
          logError("StageWrapper not found. Skipping resize event");
        }
      },

      // Handles scroll wheel input (event callback)
      handleScroll (event) {
        const stage = this.stageObj;
        if (exists(stage)) {
          event.preventDefault();
          const oldScale = stage.scaleX();

          const pointer = stage.getPointerPosition();
          const startPos = {
            x: pointer.x / oldScale - stage.x() / oldScale,
            y: pointer.y / oldScale - stage.y() / oldScale,
          };

          let deltaYBounded = 0;
          if (!(event.deltaY % 1)) {
            deltaYBounded = Math.abs(Math.min(-10, Math.max(10, event.deltaY)))
          } else {
            // noinspection JSSuspiciousNameCombination
            deltaYBounded = Math.abs(event.deltaY)
          }
          const scaleBy = 1.01 + deltaYBounded / 70;
          let newScale = 0;
          if (event.deltaY > 0) {
            newScale = oldScale / scaleBy;
          } else {
            newScale = oldScale * scaleBy;
          }
          newScale = this.clampScale(newScale);
          stage.scale({x: newScale, y: newScale});

          const newPosition = {
            x: (pointer.x / newScale - startPos.x) * newScale,
            y: (pointer.y / newScale - startPos.y) * newScale,
          };
          stage.position(this.clampPosition(newPosition));
          stage.batchDraw();
        } else {
          logError("StageObj not found. Scroll events will not work");
        }
      },

      // Handles touch move input (event callback)
      handleTouchMove (event) {
        const t1 = event.touches[0];
        const t2 = event.touches[1];
        if (t1 && t2) {
          event.preventDefault();
          event.stopPropagation();
          const stage = this.stageObj;
          const touchState = this.touchState;
          if (exists(stage)) {
            const oldScale = stage.scaleX();
            const dist = distance(
              {x: t1.clientX, y: t1.clientY},
              {x: t2.clientX, y: t2.clientY}
            );
            if (!touchState.lastDist) touchState.lastDist = dist;
            const delta = dist - touchState.lastDist;
            const px = (t1.clientX + t2.clientX) / 2;
            const py = (t1.clientY + t2.clientY) / 2;
            let pointer = {};
            if (typeof touchState.point === 'undefined') {
              pointer = this.relativePointer(px, py, stage);
              touchState.point = pointer;
            } else {
              pointer = touchState.point;
            }
            const startPos = {
              x: pointer.x / oldScale - stage.x() / oldScale,
              y: pointer.y / oldScale - stage.y() / oldScale,
            };
            const scaleBy = 1.01 + Math.abs(delta) / 100;
            const newScale = this.clampScale(delta < 0 ? oldScale / scaleBy : oldScale * scaleBy);
            stage.scale({x: newScale, y: newScale});
            const newPosition = {
              x: (pointer.x / newScale - startPos.x) * newScale,
              y: (pointer.y / newScale - startPos.y) * newScale,
            };
            stage.position(this.clampPosition(newPosition));
            stage.batchDraw();
            // Update touch state
            touchState.lastDist = dist;
          } else {
            logError("StageObj not found. Touch events will not work");
          }
        }
      },

      // Handles touch end input (event callback)
      handleTouchEnd () {
        // Reset touch state
        this.touchState.lastDist = 0;
        this.touchState.point = undefined;
      },

      // Attach event listeners upon mounting
      attachEventListeners (stageRef) {
        window.addEventListener('resize', this.handleResize);
        // attach events to the stage
        if (exists(stageRef)) {
          stageRef.addEventListener('wheel',     this.handleScroll);
          stageRef.addEventListener('touchmove', this.handleTouchMove);
          stageRef.addEventListener('touchend',  this.handleTouchEnd);
        } else {
          logError("StageRef not found. Canvas events will not work");
        }
      },

      // Detach any event listeners upon being destroyed
      detachEventListeners (stageRef) {
        window.removeEventListener('resize', this.handleResize);
        if (exists(stageRef)) {
          stageRef.removeEventListener('wheel',     this.handleScroll);
          stageRef.removeEventListener('touchmove', this.handleTouchMove);
          stageRef.removeEventListener('touchend',  this.handleTouchEnd);
        }
      },

      // *********************
      // Async synchronization
      // *********************

      // Attempts to unlock the GUI side of the async initialization lock
      unlockInitialization () {
        this.initializeTransforms();
      },

      // Initializes transforms once information from the server is loaded
      initializeTransforms () {
        console.log({
          w: this.$refs.stageWrapper.clientWidth,
          h: this.$refs.stageWrapper.clientHeight});
        const stage            = this.stageObj;
        const initialTransform = this.calculateInitialTransform();
        const scaleBounds      = this.scaleBounds;
        if (exists(stage)) {
          // Scale
          stage.scale({
            x: initialTransform.scale,
            y: initialTransform.scale
          });
          // Translate
          stage.x(initialTransform.x);
          stage.y(initialTransform.y);
          // Update scale bounds
          scaleBounds.min = Math.min(scaleBounds.min, initialTransform.scale);
          scaleBounds.max = Math.max(scaleBounds.max, initialTransform.scale)
        } else {
          logError("StageObj not found. Canvas will not be positioned");
        }
      }
    },

    // Mounted lifecycle hook
    mounted () {
      this.$nextTick(function () {
        // initialize canvas dimensions
        this.handleResize();
        if (exists(this.$refs.stage)) {
          // load the stageObj
          this.stageObj = this.$refs.stage.getStage();
          this.attachEventListeners(this.stageObj.getContent());
          this.unlockInitialization();
        }
      });
    },

    // Destruction lifecycle hook
    beforeDestroy () {
      this.detachEventListeners();
    }
  }
</script>

<style scoped>
  /* Wrapping div */
  .stage-wrapper, .stage-wrapper > div, .stage {
    overflow: hidden;
    height: 100%;
  }
</style>
