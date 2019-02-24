var usernameField = document.getElementById('username_field');
var usernameButton = document.getElementById('username_button');
var username = $("#username_field")

usernameField.addEventListener('keyup', function (event) {
  var isValidUsername = username.val() != null
      && username.val().length > 2
      && username.val().length <=16
      && /^[a-zA-Z0-9]+$/.test(username.val());

  if (isValidUsername) {
    usernameButton.disabled = false;
  } else {
    usernameButton.disabled = true;
  }
});

var codeField = document.getElementById('search_field');
var codeButton = document.getElementById('search_button');
var nameRegex = /^[BCEFGHJMPQRTVYWX]+$/;
var code = $("#search_field")

codeField.addEventListener('keyup', function (event) {
  var isValidCode = code.val() != null
      && code.val().length == 4
      && code.val().match(nameRegex);

  if (isValidCode) {
    codeButton.disabled = false;
  } else {
    codeButton.disabled = true;
  }
});

$('#search_button').click(function() {
  window.location.href = "lobby/" + $("#search_field").val();
  return false;
});

$('#username_button').click(function() {
  var username = $("#username_field").val();
  var colorIndex = $('input[name=colorButton]:checked', '#colorButtonGroup').val();
  if (colorIndex == undefined) {
    colorIndex = 0;
  }
  var postUrl = 'url/lobby/make?name=' + username + '&color=' + colorIndex;
  $.post(postUrl)
});

window.onColorButtonClick = function(matchid) {
  $("#colorButtonGroup label").each(function() {
    if ($(this).attr("id") == (matchid)) {
      $(this).fadeTo("slow", 1);
    } else {
      $(this).fadeTo("slow", 0.4);
    }
  });
}
