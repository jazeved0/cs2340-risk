# CS 2340 Scala Project: Risk

> Scala Play + Vue.js web application running multiplayer risk

[![Uptime Robot status](https://img.shields.io/uptimerobot/status/m782165527-5f127672eaae6df89c7b070a.svg?color=%235B78BB&style=for-the-badge)](https://stats.uptimerobot.com/OZ659UjoL) [![Version](https://img.shields.io/badge/version-M4-blue.svg?color=%235B78BB&style=for-the-badge)](https://github.gatech.edu/achafos3/CS2340Sp19Team10/releases/tag/M4) [![City](https://img.shields.io/badge/city-england-blue.svg?color=%235B78BB&style=for-the-badge)](https://www.youtube.com/watch?v=hSlb1ezRqfA)

[Hosted here](http://riskgame.ga/). Design documentation is WIP and will appear [here](http://riskgame.ga/docs).

![game screen](https://i.imgur.com/rRD9CM2.png "Example game screen")

## Setup

To setup the Risk web application, both the front end, written in Vue, and the back end, written in Scala, need to be built.

### Front End

To compile the web application from its sources, make sure you have [Node.js](https://nodejs.org/en/) installed. Run the following commands in the `/vue` directory to install dependencies and then build the front-end codebase,:

```bash
npm install
npm runScript buildProd
```

### Back End

To start up the server and compile all Scala source files, run the following command from the project directory:

```bash
sbt run
```

And open [http://localhost:9000/](http://localhost:9000/)

## Development setup

For development mode, run the following commands instead, which will (in addition to installing dependencies like before) generate source mappings, more verbose modules, and enable the [Vue devtools extension](https://github.com/vuejs/vue-devtools): (again, in the `/vue` directory)

```bash
npm install
npm runScript build
```

When starting the server, you can enable the file watcher and start the Scala build/run process simultaneously:

```bash
sbt ~run
```

## Dependencies

### Front-End

- [Konva](https://konvajs.org/) - Used for HTML canvas rendering
- [Vue.js](https://vuejs.org/) - Front-end javascript framework
- [Popper.js](https://popper.js.org/) - Javascript framework to create responsive tooltips
- [BootstrapVue](https://bootstrap-vue.js.org/) - Bootstrap bindings for Vue to develop responsive web applications
- [FontAwesome](https://fontawesome.com/) - Large selection of web icons

### Back-End

- [Play Framework](https://www.playframework.com/) - Scala back-end framework
- [Akka](https://akka.io/) - Scala network runtime/webserver
- [Guice](https://github.com/google/guice) - JVM Dependency Injection library
- [Caffeine](https://github.com/ben-manes/caffeine) - JVM Caching library

## [Proof of Design Process (UML Diagram)](http://cs2340-risk.ga/docs/backend.uml.pdf)

## Credits

- Jake Paul
- Team 10
- This README has been produced for thy viewing eyes by Andrew Chafos
