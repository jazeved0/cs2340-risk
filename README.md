# CS 2340 Scala Project: Risk

[![Status](https://img.shields.io/uptimerobot/status/m782165527-5f127672eaae6df89c7b070a.svg?color=%235B78BB&style=for-the-badge)](https://status.riskgame.ga/) [![Uptime](https://img.shields.io/uptimerobot/ratio/m782165527-5f127672eaae6df89c7b070a.svg?color=%235B78BB&style=for-the-badge)](https://status.riskgame.ga/) [![Version](https://img.shields.io/badge/version-M5-blue.svg?color=%235B78BB&style=for-the-badge)](https://github.com/jazevedo620/cs2340-risk/releases/tag/M5) [![Contributors](https://img.shields.io/github/contributors/jazevedo620/cs2340-risk.svg?color=%235B78BB&style=for-the-badge)](https://github.com/jazevedo620/cs2340-risk/graphs/contributors)

> CS 2340 Risk is a Scala web app that exposes a WebSocket API, leveraging the Akka Actor system to manage state.

### [Live Version](http://riskgame.ga/)

At a high level, the frontend is built with Vue.js and HTML Canvases, while the backend is built with Scala Play and Akka. The project itself was produced for CS 2340 at Georgia Tech with Professor Christopher Simpkins ([class website](https://cs2340.gitlab.io/))

### Design Docs

Detailed documentation on the project and its components (including the frontend, backend, and deployment) is [available here](https://riskgame.ga/docs).

![game screen](https://i.imgur.com/GaguGHa.png "Example game screen")

## Setup

To setup the Risk web application, both the front end, written in Vue, and the back end, written in Scala, need to be built.

### Front End

To compile the web application from its sources, make sure you have [Node.js](https://nodejs.org/en/) installed. Run the following commands in the `/vue` directory to install dependencies and then build the front-end codebase:

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

### Frontend

- [Konva](https://konvajs.org/) - Used for HTML canvas rendering
- [Vue.js](https://vuejs.org/) - Progressive SPA framework for Javascript application
- [Popper.js](https://popper.js.org/) - Javascript framework to create responsive tooltips
- [BootstrapVue](https://bootstrap-vue.js.org/) - Bootstrap bindings for Vue to develop responsive web applications
- [FontAwesome](https://fontawesome.com/) - Large selection of web icons

### Backend

- [Play Framework](https://www.playframework.com/) - JVM web server framework
- [Akka](https://akka.io/) - Scala network runtime/webserver
- [Guice](https://github.com/google/guice) - JVM Dependency Injection library
- [Caffeine](https://github.com/ben-manes/caffeine) - JVM Caching library

### Deployment

- [Docker](https://www.docker.com/) - Container virtualization software
- [Nginx proxy](https://github.com/jwilder/nginx-proxy) - Proxy container providing support for https on live
- [Alpine Linux](https://hub.docker.com/_/alpine) - Container image used to package application

## Contributors

Our team for CS 2340 consisted of the following members:

- Joseph Azevedo ([jazevedo620](https://github.com/jazevedo620))
- Andrew Chafos ([andrewjc2000](https://github.com/andrewjc2000))
- Julian Gu ([julian-g99](https://github.com/julian-g99))
- Thomas Lang ([bopas2](https://github.com/bopas2))
- Patrick Liu ([PatrickLiu2000](https://github.com/PatrickLiu2000))
