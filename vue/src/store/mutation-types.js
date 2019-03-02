// General mutations
export const SET_GAME_ID = 'SET_GAME_ID';
export const SET_PLAYER_ID = 'SET_PLAYER_ID';
export const SET_CURRENT = 'SET_CURRENT';
export const UPDATE_IS_HOST = 'UPDATE_IS_HOST';
export const START_RESPONSE_WAIT = 'START_RESPONSE_WAIT';
export const STOP_RESPONSE_WAIT = 'STOP_RESPONSE_WAIT';

// Websocket handling
export const SOCKET_ONOPEN = 'SOCKET_ONOPEN';
export const SOCKET_ONCLOSE = 'SOCKET_ONCLOSE';
export const SOCKET_ONERROR = 'SOCKET_ONERROR';

// Packet handling
export const ON_GAME_LOBBY_UPDATE = 'ON_GAME_LOBBY_UPDATE';
export const ON_REQUEST_REPLY = 'ON_REQUEST_REPLY';
export const ON_BAD_PACKET = 'ON_BAD_PACKET';
export const ON_START_GAME = 'ON_START_GAME';
export const ON_UPDATE_PLAYER_STATE = 'ON_UPDATE_PLAYER_STATE';
export const ON_PING_PLAYER = 'ON_PING_PLAYER';
export const ON_SEND_CONFIG = 'ON_SEND_CONFIG';
