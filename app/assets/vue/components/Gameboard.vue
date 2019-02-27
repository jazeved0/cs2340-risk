<template>
  <body>
    <div class = "gameboard">
      <link href="https://fonts.googleapis.com/css?family=ZCOOL+QingKe+HuangYou" rel="stylesheet">
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.0/css/bootstrap.min.css">
      <nav class="navbar fixed-top navbar-dark bg-dark">
            <h1 style = "color:white">RISK</h1>
            <a class="navbar-brand" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" target="_blank" rel="noopener">
                <i class="fab fa-github repoImg pl-2"></i>
            </a>
            <form class="form-inline mr-2">
                <div class = "content mr-3 mt-3">
                    <p class="p1" style = "color: white">Join Existing Lobby</p>
                </div>
                <input id="search_field" class="form-control mr-sm-2" type="search" maxlength="4" placeholder="Lobby Code" Roboto-label="Search">
                <input id="search_button" class="btn btn-primary my-2 my-sm-0 white dark_accent"
                      type="submit" value="Join" disabled/>
            </form>
      </nav>
      <img src = "./assets/gameboardplaceholder.jpg" >
    </div>
    <PlayerInfoBar> </PlayerInfoBar>
    <footer>
    </footer>
  </body>
</template>

<script>
  import PlayerInfoBar from './components/PlayerInfoBar.vue'
  export default {
    components: {
        PlayerInfoBar
    }
  }
</script>

<style>
  .gameboard {
    background-color: #362a4d;
  }
  img {
    display: block;
    margin-left: auto;
    margin-right: auto;
    width: 75%;
    max-width: 100%;
    max-height: 100%;
    padding: 0;
  }
  footer {
    background-color: #21252b;
    padding: 5%;
    min-height: 10%;
  }
</style>
