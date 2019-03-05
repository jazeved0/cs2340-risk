<template>
  <div>
    <nav class="navbar navbar-fixed-bottom navbar-dark bg-dark">
      <div class="d-flex flex-auto flex-row justify-content-between flexbox align-items-center">
        <div>
          <div class="d-flex flex-auto flex-row justify-content-start flexbox align-items-center">
            <div>
              <div id="repo-logo" class="d-inline">
                <a class="navbar-brand pr-2" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" name="github" target="_blank" rel="noopener">
                  <fa-icon :icon="['fab', 'github']" size="lg" class="repoImg pl-2"></fa-icon>
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
  }
  .flexbox {
    width: 100%;
  }
</style>