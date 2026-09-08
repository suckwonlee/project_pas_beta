package com.pas.game.multiplayer;

/** WebSocket/HTTP/루프백 구현이 공유하는 문자열 기반 전송 경계. */
public interface MultiplayerTransport {
    interface Callback {void onResponse(String responseJson);void onFailure(Throwable error);}
    interface SnapshotListener {void onSnapshot(String responseJson);}
    void sendCommand(String requestJson,Callback callback);
    void requestSnapshot(String matchId,String clientId,Callback callback);
    void setSnapshotListener(SnapshotListener listener);
}
