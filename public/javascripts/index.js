// Settings injection object
var formSettings = $('#formsSettings');
var usernameRegex = new RegExp(formSettings.attr('data-name-regex'));
var gameRegex = new RegExp('^[' + formSettings.attr('data-game-chars') + ']+$');
var minNameLength = parseInt(formSettings.attr('data-name-min'));
var maxNameLength = parseInt(formSettings.attr('data-name-max'));
var gameLength = parseInt(formSettings.attr('data-game-length'));

// Handle input on username field
var usernameButton = $('#username_button');
var usernameField = $('#username_field');
usernameField.on('input', function () {
  validateUsernameField()
});

$(document).ready(function() {
  validateUsernameField()
});

function validateUsernameField() {
  var username = String(usernameField.val());
  var isValidUsername = username != null
     && username.length >= minNameLength
     && username.length <= maxNameLength
     && usernameRegex.test(username);

  if (isValidUsername) {
    usernameButton.prop('disabled', false);
  } else {
    usernameButton.prop('disabled', true);
  }
}

// Handle input on game code field
var codeField = $('#lobby-field');
var codeButton = $('#lobby-button');
codeField.on('input', function () {
  var code = String(codeField.val());
  var isValidCode = code != null
     && code.length === gameLength
     && gameRegex.test(code);

  if (isValidCode) {
    codeButton.prop('disabled', false);
  } else {
    codeButton.prop('disabled', true);
  }
});

// Handle button press of the lobby field and redirect
codeButton.on('click', function () {
  window.location.href = "lobby/" + String($("#lobby-field").val()).toLowerCase();
  return false;
});

// Handle color select button click
function onColorButtonClick(id) {
  $(".color-button-group label").each(function () {
    if ($(this).attr("id") === (id)) {
      $(this).fadeTo(200, 1);
      $("input", this).prop("checked", true);
    } else {
      $(this).fadeTo(200, 0.4);
      $("input", this).prop("checked", false);
    }
  });
}
