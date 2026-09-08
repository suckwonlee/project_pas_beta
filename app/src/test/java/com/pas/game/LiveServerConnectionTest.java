package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.multiplayer.*;
import com.pas.game.network.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/** Opt-in tests against a locally running PAS_server, never a production endpoint. */
public class LiveServerConnectionTest {
    private OkHttpClient http;
    private RoomApiClient api;
    private ServerEndpoint endpoint;
    private final List<WebSocketBattleTransport> sockets = new ArrayList<>();
    @Before public void setup() {
        String address = System.getProperty("pasTestServerUrl", "");
        Assume.assumeTrue("Run local server and pass -PpasTestServerUrl=http://127.0.0.1:8090", !address.isEmpty());
        assertTrue("Only loopback servers are allowed", address.matches("http://(127\\.0\\.0\\.1|localhost):[0-9]+/?"));
        endpoint = new ServerEndpoint(address, true); http = new OkHttpClient();
        api = new RoomApiClient(endpoint, System.getenv().getOrDefault("PAS_TEST_BETA_KEY", ""), http, Runnable::run);
    }
    @After public void close() {
        for (WebSocketBattleTransport socket : sockets) socket.close();
        if (api != null) api.close();
        if (http != null) { http.dispatcher().cancelAll(); http.connectionPool().evictAll(); http.dispatcher().executorService().shutdown(); }
    }
    private static class Result<T> implements RoomApiClient.Result<T> {
        final BlockingQueue<Object> events = new LinkedBlockingQueue<>();
        public void success(T value) { events.add(value); }
        public void failure(Throwable error) { events.add(error); }
        @SuppressWarnings("unchecked") T get() throws Exception {
            Object result = events.poll(15, TimeUnit.SECONDS); assertNotNull("REST timeout", result);
            if (result instanceof Throwable) throw new AssertionError(result); return (T) result;
        }
    }
    private RoomApiClient.Connection create(RoomApiClient.Mode mode) throws Exception {
        Result<RoomApiClient.Connection> r = new Result<>(); api.create("자동검증", mode, r); return r.get();
    }
    private void ready(RoomApiClient.Connection c) throws Exception {
        List<RoomApiClient.CharacterLoadout> loadouts = new ArrayList<>();
        for (int slot : c.playerSlots) loadouts.add(new RoomApiClient.CharacterLoadout(slot, slot == 1 ? "HERO" : "HUNTER"));
        Result<RoomApiClient.RoomStatus> configured = new Result<>(); api.configure(c, loadouts, configured); configured.get();
        Result<RoomApiClient.RoomStatus> result = new Result<>(); api.ready(c, result); result.get();
    }
    private WebSocketBattleTransport connect(RoomApiClient.Connection c, BlockingQueue<String> inbound) {
        WebSocketBattleTransport socket = new WebSocketBattleTransport(endpoint, c, http, Runnable::run, (state, message) -> {});
        socket.setSnapshotListener(inbound::add); sockets.add(socket); socket.connect(); return socket;
    }
    private CommandReceipt take(BlockingQueue<String> inbound) throws Exception {
        String value = inbound.poll(15, TimeUnit.SECONDS); assertNotNull("WS timeout", value);
        CommandReceipt receipt = CommandReceipt.fromJson(value); assertTrue(receipt.getMessage(), receipt.isAccepted());
        RemoteBattleSnapshot.fromReceipt(receipt); return receipt;
    }
    private CommandReceipt endTurn(WebSocketBattleTransport socket, RoomApiClient.Connection c, CommandReceipt prior) throws Exception {
        RemoteBattleSnapshot snapshot = RemoteBattleSnapshot.fromReceipt(prior); BlockingQueue<String> result = new LinkedBlockingQueue<>();
        socket.sendCommand(CommandEnvelope.create(c.roomCode, c.clientId, java.util.UUID.randomUUID().toString(), prior.getRevision(),
                new EndTurnCommand(snapshot.turn.activeUnitId)).toJson(), new MultiplayerTransport.Callback() {
            public void onResponse(String json) { result.add(json); }
            public void onFailure(Throwable error) { result.add("FAILED"); }
        });
        return take(result);
    }
    @Test public void oneCharacterServerBattleAdvancesAndReconnects() throws Exception {
        RoomApiClient.Connection c = create(RoomApiClient.Mode.SOLO_ONE); ready(c);
        BlockingQueue<String> inbound = new LinkedBlockingQueue<>(); WebSocketBattleTransport socket = connect(c, inbound);
        CommandReceipt result = endTurn(socket, c, take(inbound));
        RemoteBattleSnapshot state = RemoteBattleSnapshot.fromReceipt(result);
        assertFalse(state.networkCoop); assertEquals(2, state.round);
        BlockingQueue<String> reconnect = new LinkedBlockingQueue<>();
        socket.requestSnapshot(c.roomCode, c.clientId, new MultiplayerTransport.Callback() {
            public void onResponse(String json) { reconnect.add(json); }
            public void onFailure(Throwable error) { reconnect.add("FAILED"); }
        });
        assertEquals(result.getStateDigest(), take(reconnect).getStateDigest());
    }
    @Test public void configuredBetaServerRejectsMissingAndWrongKey() throws Exception {
        Assume.assumeTrue("This check requires a local server with a beta key", !System.getenv().getOrDefault("PAS_TEST_BETA_KEY", "").isEmpty());
        for (String key : new String[]{"", "wrong-test-key"}) {
            try (RoomApiClient unauthenticated = new RoomApiClient(endpoint, key, http, Runnable::run)) {
                Result<RoomApiClient.Connection> result = new Result<>();
                unauthenticated.create("접속거부검증", RoomApiClient.Mode.SOLO_ONE, result);
                Object outcome = result.events.poll(15, TimeUnit.SECONDS);
                assertTrue(outcome instanceof RoomApiClient.ApiException);
                assertEquals(403, ((RoomApiClient.ApiException) outcome).statusCode);
            }
        }
    }
    @Test public void soloPartyCanControlBothUnits() throws Exception {
        RoomApiClient.Connection c = create(RoomApiClient.Mode.SOLO_PARTY); ready(c);
        BlockingQueue<String> inbound = new LinkedBlockingQueue<>(); WebSocketBattleTransport socket = connect(c, inbound);
        CommandReceipt afterFirst = endTurn(socket, c, take(inbound));
        RemoteBattleSnapshot state = RemoteBattleSnapshot.fromReceipt(afterFirst);
        assertFalse(state.networkCoop); assertEquals(2, state.find(state.turn.activeUnitId).playerSlot);
        assertTrue(endTurn(socket, c, afterFirst).isAccepted());
    }
    @Test public void coopPushesToOtherPlayerAndAcceptsTheirTurn() throws Exception {
        RoomApiClient.Connection host = create(RoomApiClient.Mode.COOP);
        Result<RoomApiClient.Connection> joined = new Result<>(); api.join(host.roomCode, "동료", joined); RoomApiClient.Connection guest = joined.get();
        ready(host); ready(guest);
        BlockingQueue<String> hostEvents = new LinkedBlockingQueue<>(), guestEvents = new LinkedBlockingQueue<>();
        WebSocketBattleTransport hostSocket = connect(host, hostEvents), guestSocket = connect(guest, guestEvents);
        CommandReceipt before = take(hostEvents); take(guestEvents);
        CommandReceipt after = endTurn(hostSocket, host, before), push = take(guestEvents);
        assertEquals("PUSH", push.getRequestId()); assertEquals(after.getStateDigest(), push.getStateDigest());
        RemoteBattleSnapshot state = RemoteBattleSnapshot.fromReceipt(push);
        assertTrue(state.networkCoop); assertEquals(2, state.find(state.turn.activeUnitId).playerSlot);
        CommandReceipt afterGuest = endTurn(guestSocket, guest, push);
        assertEquals(afterGuest.getStateDigest(), take(hostEvents).getStateDigest());
    }
}
