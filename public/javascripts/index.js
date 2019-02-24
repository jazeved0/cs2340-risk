// Handle input on username field
var usernameButton = $('#username_button');
var usernameField = $('#username_field');
var usernameRegex = /^[a-zA-Z0-9]+$/
usernameField.on('input', function () {
  validateUsernameField()
});

$(document).ready(function() {
  validateUsernameField()
});

function validateUsernameField() {
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
}

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
      $("input", this).prop("checked", true);
    } else {
      $(this).fadeTo(200, 0.4);
      $("input", this).prop("checked", false);
    }
  });
}
