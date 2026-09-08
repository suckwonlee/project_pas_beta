package com.pas.game.network;

import com.pas.game.multiplayer.CommandEnvelope;
import com.pas.game.multiplayer.CommandReceipt;
import com.pas.game.multiplayer.MultiplayerProtocol;
import com.pas.game.multiplayer.MultiplayerTransport;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/** Serial socket state machine. Uncertain commands are never automatically replayed. */
public final class WebSocketBattleTransport implements MultiplayerTransport, Closeable {
    public enum State { CONNECTING, SYNCHRONIZING, CONNECTED, RECONNECTING, STOPPED, CLOSED }
    public interface StateListener { void changed(State state, String message); }
    private final ServerEndpoint endpoint;
    private final RoomApiClient.Connection connection;
    private final OkHttpClient http;
    private final Executor callbacks;
    private final StateListener stateListener;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private volatile SnapshotListener snapshots;
    private volatile boolean closed;
    private WebSocket socket;
    private State state = State.STOPPED;
    private int generation, retries;
    private Callback pending, syncCallback;
    private String pendingId;
    private ScheduledFuture<?> deadline;

    public WebSocketBattleTransport(ServerEndpoint endpoint, RoomApiClient.Connection connection,
            OkHttpClient http, Executor callbacks, StateListener stateListener) {
        connection.validate();
        this.endpoint = endpoint; this.connection = connection; this.callbacks = callbacks;
        this.stateListener = stateListener;
        this.http = http.newBuilder().followRedirects(false).followSslRedirects(false)
                .pingInterval(20, TimeUnit.SECONDS).build();
    }
    public void connect() { enqueue(() -> { if (state == State.STOPPED) { retries = 0; open(); } }); }

    private void open() {
        int attempt = ++generation;
        change(retries == 0 ? State.CONNECTING : State.RECONNECTING, "서버에 연결 중입니다.");
        Request request = new Request.Builder().url(endpoint.battle(connection.roomCode))
                .header("X-PAS-CLIENT", connection.clientId).header("X-PAS-TOKEN", connection.accessToken).build();
        socket = http.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response response) {
                enqueue(() -> { if (attempt != generation) return;
                    change(State.SYNCHRONIZING, "최신 전투 상태를 받는 중입니다.");
                    armTimeout(attempt, "상태 동기화 시간이 초과됐습니다."); });
            }
            @Override public void onMessage(WebSocket ws, String text) {
                enqueue(() -> { if (attempt == generation) receive(text); });
            }
            @Override public void onClosing(WebSocket ws, int code, String reason) {
                ws.close(code, null);
                enqueue(() -> { if (attempt == generation) disconnected(code == 1008,
                        code == 1008 ? "접속이 거부됐습니다. 방에 다시 참가하세요." : "연결이 종료돼 재접속합니다."); });
            }
            @Override public void onFailure(WebSocket ws, Throwable t, Response response) {
                boolean denied = response != null && response.code() >= 400 && response.code() < 500;
                if (response != null) response.close();
                enqueue(() -> { if (attempt == generation) disconnected(denied,
                        denied ? "방 또는 접속 정보를 확인하세요." : "연결이 끊겨 최신 상태를 다시 받습니다."); });
            }
        });
    }

    private void receive(String json) {
        final CommandReceipt receipt;
        try {
            receipt = CommandReceipt.fromJson(json);
            if (receipt.getProtocolVersion() != MultiplayerProtocol.VERSION) throw new IllegalArgumentException();
            if (receipt.getSnapshotJson() != null) RemoteBattleSnapshot.fromReceipt(receipt);
        } catch (RuntimeException invalid) { disconnected(true, "서버 데이터 또는 프로토콜 버전이 올바르지 않습니다."); return; }
        boolean synchronizedNow = state == State.SYNCHRONIZING || state == State.CONNECTING || state == State.RECONNECTING;
        if (synchronizedNow) {
            if (receipt.getSnapshotJson() == null || !receipt.isAccepted()) {
                disconnected(true, "전투 상태를 동기화할 수 없습니다."); return;
            }
            cancelTimeout(); retries = 0;
            change(State.CONNECTED, "서버와 연결됐습니다.");
        }
        if (pending != null && pendingId.equals(receipt.getRequestId())) {
            Callback callback = pending; pending = null; pendingId = null; cancelTimeout();
            deliver(() -> callback.onResponse(json));
        } else {
            if (synchronizedNow && syncCallback != null) {
                Callback callback = syncCallback; syncCallback = null;
                deliver(() -> callback.onResponse(json));
            } else deliver(() -> { if (snapshots != null) snapshots.onSnapshot(json); });
            if (pending != null && receipt.getRequestId() == null) {
                failPending("서버가 명령을 거부했습니다.");
            }
        }
    }

    @Override public void sendCommand(String json, Callback callback) {
        if (closed) { callbacks.execute(() -> callback.onFailure(new IOException("접속이 종료됐습니다."))); return; }
        enqueue(() -> {
            if (state != State.CONNECTED || pending != null) {
                deliver(() -> callback.onFailure(new IOException("동기화 또는 이전 행동이 끝날 때까지 기다리세요."))); return;
            }
            try {
                CommandEnvelope envelope = CommandEnvelope.fromJson(json);
                if (!connection.roomCode.equals(envelope.getMatchId()) || !connection.clientId.equals(envelope.getClientId())) {
                    throw new IllegalArgumentException("다른 방의 명령입니다.");
                }
                pendingId = envelope.getRequestId(); pending = callback;
                if (!socket.send(json)) { disconnected(false, "명령 전송에 실패했습니다. 상태를 다시 확인합니다."); return; }
                armTimeout(generation, "행동 결과 확인 시간이 초과됐습니다. 결과를 다시 동기화합니다.");
            } catch (RuntimeException e) { deliver(() -> callback.onFailure(e)); }
        });
    }
    @Override public void requestSnapshot(String matchId, String clientId, Callback callback) {
        enqueue(() -> {
            if (!connection.roomCode.equals(matchId) || !connection.clientId.equals(clientId)) {
                deliver(() -> callback.onFailure(new IOException("접속한 방과 다릅니다."))); return;
            }
            if (syncCallback != null) { deliver(() -> callback.onFailure(new IOException("이미 동기화 중입니다."))); return; }
            // This server sends SYNC on handshake; it has no client-side SYNC command type.
            syncCallback = callback; failPending("최신 상태를 다시 확인합니다.");
            generation++; if (socket != null) socket.cancel(); retries = 0; open();
        });
    }
    private void disconnected(boolean terminal, String message) {
        generation++; cancelTimeout(); if (socket != null) socket.cancel(); socket = null;
        failPending(message);
        if (terminal || retries >= 5) {
            change(State.STOPPED, message);
            if (syncCallback != null) { Callback c = syncCallback; syncCallback = null; deliver(() -> c.onFailure(new IOException(message))); }
            return;
        }
        change(State.RECONNECTING, message);
        long delay = Math.min(16, 1L << retries++); int expected = generation;
        worker.schedule(() -> { if (!closed && expected == generation) open(); }, delay, TimeUnit.SECONDS);
    }
    private void failPending(String message) {
        if (pending != null) { Callback c = pending; pending = null; pendingId = null; cancelTimeout(); deliver(() -> c.onFailure(new IOException(message))); }
    }
    private void armTimeout(int expected, String message) {
        cancelTimeout(); deadline = worker.schedule(() -> { if (expected == generation) disconnected(false, message); }, 15, TimeUnit.SECONDS);
    }
    private void cancelTimeout() { if (deadline != null) deadline.cancel(false); deadline = null; }
    private void change(State next, String message) { state = next; deliver(() -> stateListener.changed(next, message)); }
    private void deliver(Runnable action) { callbacks.execute(() -> { if (!closed) action.run(); }); }
    private void enqueue(Runnable action) {
        if (!closed) try { worker.execute(() -> { if (!closed) action.run(); }); } catch (RejectedExecutionException ignored) { }
    }
    @Override public void setSnapshotListener(SnapshotListener listener) { snapshots = listener; }
    @Override public void close() {
        if (closed) return; closed = true;
        worker.execute(() -> { generation++; cancelTimeout(); if (socket != null) socket.cancel();
            pending = null; syncCallback = null; snapshots = null; state = State.CLOSED; });
        worker.shutdown();
    }
}
