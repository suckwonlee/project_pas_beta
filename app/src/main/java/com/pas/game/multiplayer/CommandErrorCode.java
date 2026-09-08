package com.pas.game.multiplayer;

public enum CommandErrorCode {
    OK,
    INVALID_PAYLOAD,
    VERSION_MISMATCH,
    MATCH_MISMATCH,
    UNKNOWN_CLIENT,
    NOT_OWNER,
    STALE_REVISION,
    REQUEST_ID_CONFLICT,
    COMMAND_REJECTED
}
