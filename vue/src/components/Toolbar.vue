<template>
  <div>
    <nav class="navbar navbar-fixed-bottom navbar-dark bg-dark">
      <div class="d-flex flex-auto justify-content-between flexbox align-items-center">
        <div>
          <div class="d-none d-lg-flex flex-auto flex-row justify-content-start flexbox align-items-center">
            <div>
              <div id="repo-logo" class="d-inline">
                <a class="navbar-brand pr-2" href="https://github.com/jazevedo620/cs2340-risk" name="github" target="_blank" rel="noopener">
                  <fa-icon :icon="['fab', 'github']" size="lg" class="repoImg"></fa-icon>
                </a>
              </div>
            </div>
            <div>
              <slot name="left-element" class="d-inline"></slot>
            </div>
          </div>
        </div>
        <div>
          <slot name="middle-element" class="d-inline"></slot>
        </div>
        <div>
          <slot name="right-element" class="d-inline"></slot>
        </div>
      </div>
    </nav>
  </div>
</template>

<script>
  export default {
    name: "ToolBar"
  };
</script>

<style scoped>
  .repoImg {
    position: relative;
    top: 1px;
    padding-left: 0.6rem;
    font-size: 40px;
    color: #F5F2F2;
    opacity: 0.88;
    transform: scale(1.23, 1.23);
  }

  nav {
    padding: .4rem 1rem !important;
    box-shadow: 0 0 72px rgba(0, 0, 0, .5);
    z-index: 999;
  }

  .flexbox {
    width: 100%;
    flex-direction: row;
  }

  @media screen and (max-width: 600px) {
    .flexbox {
      width: 100%;
      flex-direction: column;
      align-items: center;
    }
  }
</style>