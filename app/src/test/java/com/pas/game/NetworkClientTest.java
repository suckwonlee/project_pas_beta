package com.pas.game;

import static org.junit.Assert.*;
import com.google.gson.Gson;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.state.BattleState;
import com.pas.game.multiplayer.*;
import com.pas.game.network.*;
import com.pas.game.unit.PlayerUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NetworkClientTest {
    private MockWebServer server;
    private OkHttpClient http;
    private WebSocketBattleTransport socket;
    @Before public void before() throws Exception { server = new MockWebServer(); server.start(); http = new OkHttpClient(); }
    @After public void after() throws Exception {
        if (socket != null) socket.close();
        http.dispatcher().cancelAll(); http.connectionPool().evictAll(); http.dispatcher().executorService().shutdownNow(); server.shutdown();
    }
    private ServerEndpoint endpoint() { return new ServerEndpoint(server.url("/").toString(), true); }
    private RoomApiClient.Connection connection() {
        RoomApiClient.Connection c = new RoomApiClient.Connection();
        c.roomCode = "ABC234"; c.clientId = "client"; c.accessToken = "private-test-token";
        c.playerSlots = Collections.singletonList(1); c.mode = RoomApiClient.Mode.COOP; return c;
    }
    private String receipt(long revision, String requestId) {
        BattleState state = new BattleState();
        BattleSnapshot snapshot = BattleSnapshot.capture(state, revision);
        return new CommandReceipt(MultiplayerProtocol.VERSION, requestId, true, CommandErrorCode.OK, "",
                revision, snapshot.toJson(), snapshot.getDigest()).toJson();
    }
    private WebSocketBattleTransport transport(BlockingQueue<String> snapshots) {
        socket = new WebSocketBattleTransport(endpoint(), connection(), http, Runnable::run, (state, msg) -> {});
        socket.setSnapshotListener(snapshots::add); return socket;
    }
    private static <T> T take(BlockingQueue<T> queue) throws Exception { T value = queue.poll(8, TimeUnit.SECONDS); assertNotNull("Timed out", value); return value; }
    private static final class Result<T> implements RoomApiClient.Result<T> {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        public void success(T value) { values.add(value); }
        public void failure(Throwable error) { values.add(error); }
    }
    private static final class Callback implements MultiplayerTransport.Callback {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        public void onResponse(String json) { values.add(json); }
        public void onFailure(Throwable error) { values.add(error); }
    }

    @Test public void releaseRejectsHttpAndCredentialsInAddress() {
        for (String address : Arrays.asList("http://localhost:8080", "https://user:password@example.com", "https://example.com/?token=x")) {
            try { new ServerEndpoint(address, false); fail(address); } catch (IllegalArgumentException expected) { }
        }
        new ServerEndpoint("https://example.com", false);
    }
    @Test public void debugHttpIsLimitedToPrivateNetwork() {
        new ServerEndpoint("http://10.0.2.2:8080", true);
        new ServerEndpoint("http://192.168.0.2:8080", true);
        try { new ServerEndpoint("http://example.com", true); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void createUsesBetaHeaderAndParsesConnection() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201).setBody(new Gson().toJson(connection())));
        try (RoomApiClient api = new RoomApiClient(endpoint(), "beta-test", http, Runnable::run)) {
            Result<RoomApiClient.Connection> result = new Result<>(); api.create("테스터", RoomApiClient.Mode.COOP, result);
            assertEquals("ABC234", ((RoomApiClient.Connection) take(result.values)).roomCode);
            RecordedRequest request = server.takeRequest(); assertEquals("/api/v1/rooms", request.getPath());
            assertEquals("beta-test", request.getHeader("X-PAS-BETA-KEY"));
            assertTrue(request.getBody().readUtf8().contains("COOP"));
            assertNull(request.getHeader("X-PAS-TOKEN"));
        }
    }
    @Test public void lobbyAuthStaysInHeadersAndRedirectsAreNotFollowed() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", server.url("/leaked")));
        try (RoomApiClient api = new RoomApiClient(endpoint(), "", http, Runnable::run)) {
            Result<RoomApiClient.RoomStatus> result = new Result<>(); api.status(connection(), result);
            assertTrue(take(result.values) instanceof RoomApiClient.ApiException);
            RecordedRequest request = server.takeRequest();
            assertEquals("/api/v1/rooms/ABC234", request.getPath());
            assertEquals("private-test-token", request.getHeader("X-PAS-TOKEN"));
            assertEquals(1, server.getRequestCount());
        }
    }
    @Test public void websocketAuthenticatesAndMatchesCommandResult() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response r) { ws.send(receipt(1, "SYNC")); }
            @Override public void onMessage(WebSocket ws, String text) {
                ws.send(receipt(2, CommandEnvelope.fromJson(text).getRequestId()));
            }
        }));
        BlockingQueue<String> snapshots = new LinkedBlockingQueue<>(); transport(snapshots).connect(); take(snapshots);
        Callback result = new Callback();
        socket.sendCommand(CommandEnvelope.create("ABC234", "client", "r1", 1, new EndTurnCommand("P1")).toJson(), result);
        assertEquals(2, CommandReceipt.fromJson((String) take(result.values)).getRevision());
        RecordedRequest handshake = server.takeRequest();
        assertEquals("/ws/battle?roomCode=ABC234", handshake.getPath());
        assertEquals("private-test-token", handshake.getHeader("X-PAS-TOKEN"));
        assertEquals("client", handshake.getHeader("X-PAS-CLIENT"));
    }
    @Test public void reconnectGetsFreshSnapshotWithoutReplayingUncertainAction() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response r) { ws.send(receipt(1, "SYNC")); }
            @Override public void onMessage(WebSocket ws, String text) { ws.close(1012, "restart"); }
        }));
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response r) { ws.send(receipt(2, "SYNC")); }
        }));
        BlockingQueue<String> snapshots = new LinkedBlockingQueue<>(); transport(snapshots).connect(); take(snapshots);
        Callback result = new Callback(); socket.sendCommand(CommandEnvelope.create("ABC234", "client", "r1", 1, new EndTurnCommand("P1")).toJson(), result);
        assertTrue(take(result.values) instanceof Throwable);
        assertEquals(2, CommandReceipt.fromJson(take(snapshots)).getRevision());
        assertEquals(2, server.getRequestCount());
    }
    @Test public void explicitSyncReconnectsInsteadOfSendingUnsupportedCommand() throws Exception {
        for (int i = 1; i <= 2; i++) {
            final int revision = i;
            server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
                @Override public void onOpen(WebSocket ws, Response r) { ws.send(receipt(revision, "SYNC")); }
            }));
        }
        BlockingQueue<String> snapshots = new LinkedBlockingQueue<>(); transport(snapshots).connect(); take(snapshots);
        Callback result = new Callback(); socket.requestSnapshot("ABC234", "client", result);
        assertEquals(2, CommandReceipt.fromJson((String) take(result.values)).getRevision());
    }
    @Test public void doubleTapDoesNotSendTwoCommands() throws Exception {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response r) { ws.send(receipt(1, "SYNC")); }
        }));
        BlockingQueue<String> snapshots = new LinkedBlockingQueue<>(); transport(snapshots).connect(); take(snapshots);
        socket.sendCommand(CommandEnvelope.create("ABC234", "client", "r1", 1, new EndTurnCommand("P1")).toJson(), new Callback());
        Callback second = new Callback(); socket.sendCommand(CommandEnvelope.create("ABC234", "client", "r2", 1, new EndTurnCommand("P1")).toJson(), second);
        assertTrue(take(second.values) instanceof Throwable);
    }
    @Test public void invalidSnapshotDigestIsRejected() {
        CommandReceipt valid = CommandReceipt.fromJson(receipt(3, "SYNC"));
        assertEquals(3, RemoteBattleSnapshot.fromReceipt(valid).revision);
        try {
            RemoteBattleSnapshot.fromReceipt(new CommandReceipt(1, "SYNC", true, CommandErrorCode.OK, "", 3, valid.getSnapshotJson(), "bad")); fail();
        } catch (IllegalArgumentException expected) { }
    }
    @Test public void olderPushDoesNotRollBackDisplay() {
        AtomicReference<MultiplayerTransport.SnapshotListener> inbound = new AtomicReference<>();
        MultiplayerTransport fake = new MultiplayerTransport() {
            public void sendCommand(String json, Callback callback) { fail("not expected"); }
            public void requestSnapshot(String matchId, String clientId, Callback callback) {}
            public void setSnapshotListener(SnapshotListener listener) { inbound.set(listener); }
        };
        OnlineBattleSession session = new OnlineBattleSession(connection(), fake, new OnlineBattleSession.Listener() {
            public void updated(RemoteBattleSnapshot snapshot) {}
            public void failed(String message) { fail(message); }
            public void busyChanged(boolean busy) {}
        });
        session.connected(true); inbound.get().onSnapshot(receipt(5, "PUSH")); inbound.get().onSnapshot(receipt(3, "PUSH"));
        assertEquals(5, session.getSnapshot().revision);
    }
}
