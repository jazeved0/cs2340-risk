anchors.options.placement = 'left';
anchors.add('h2');
anchors.add('h3');

hljs.initHighlightingOnLoad();

$(document).on('click', '[data-toggle="lightbox"]', function(event) {
  event.preventDefault();
  $(this).ekkoLightbox();
});
