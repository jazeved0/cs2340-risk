// Handle input on username field
var usernameButton = $('#username_button');
var usernameField = $('#username_field');
var usernameRegex = /^[a-zA-Z0-9]+$/
usernameField.on('input', function () {
  var username = String(usernameField.val());
  var isValidUsername = username != null
     && username.length >= 2
     && username.length <= 16
     && usernameRegex.test(username);

  if (isValidUsername) {
    usernameButton.prop('disabled', false);
  } else {
    usernameButton.prop('disabled', true);
  }
});

// Handle submission of the username form (and cancel the
// standard submission in favor of a POST)
$('#username-form').on('submit', function () {
  var username = String($("#username_field").val());
  var colorIndex = $('input[name=colorButton]:checked', '#colorButtonGroup').val();
  if (colorIndex === undefined) {
    colorIndex = 0;
  }
  // noinspection JSIgnoredPromiseFromCall
  $.post('/lobby/make', {"name": username, "color": colorIndex}, function() {});

  // var postUrl = '/lobby/make?name=' + username + "&color=" + colorIndex;
  // // noinspection JSIgnoredPromiseFromCall
  // $.post(postUrl, {}, function() {});

  return false;
});

// Handle input on lobby code field
var codeField = $('#search_field');
var codeButton = $('#search_button');
var codeRegex = /^[BCEFGHJMPQRTVYWXbcefghjmpqrtvywx]+$/;
codeField.on('input', function () {
  var code = String(codeField.val());
  var isValidUsername = code != null
     && code.length === 4
     && codeRegex.test(code);

  if (isValidUsername) {
    codeButton.prop('disabled', false);
  } else {
    codeButton.prop('disabled', true);
  }
});

// Handle button press of the lobby field and redirect
codeButton.on('click', function () {
  window.location.href = "lobby/" + String($("#search_field").val());
  return false;
});

// Handle color select button click
function onColorButtonClick(id) {
  $("#colorButtonGroup label").each(function () {
    if ($(this).attr("id") === (id)) {
      $(this).fadeTo(200, 1);
    } else {
      $(this).fadeTo(200, 0.4);
    }
  });
}
