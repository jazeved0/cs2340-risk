export const NETWORK_CTX = 'gameboardNetwork';
export const GUI_CTX = 'gameboardScreen';

// Async invocation 'lock' to ensure gameboard is initialized only after:
//    1. the GUI component has been loaded
//    2. the gameboard data has been loaded from the network
export const initializeGameboardScreen = (function () {
  let lockState = {
    [NETWORK_CTX]: false,
    [GUI_CTX]: false
  };
  let executed = false;
  let initializeGameboard = () => {};
  return function (ctx, callback) {
    // only load callback with closures from GUI
    if (ctx === GUI_CTX) {
      initializeGameboard = callback;
    }
    lockState[ctx] = true;
    if (Object.keys(lockState).every(k => lockState[k] === true) && !executed) {
      initializeGameboard();
      executed = true;
    }
  };
})();
