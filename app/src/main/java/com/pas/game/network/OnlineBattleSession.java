package com.pas.game.network;

import com.pas.game.battle.command.BattleCommand;
import com.pas.game.multiplayer.CommandEnvelope;
import com.pas.game.multiplayer.CommandReceipt;
import com.pas.game.multiplayer.MultiplayerProtocol;
import com.pas.game.multiplayer.MultiplayerTransport;
import com.pas.game.multiplayer.PlayerCommandSource;
import java.util.UUID;

/** Client-side input gate and display state; damage and turn simulation stay on the server. */
public final class OnlineBattleSession implements PlayerCommandSource {
    public interface Listener {
        void updated(RemoteBattleSnapshot snapshot);
        void failed(String message);
        void busyChanged(boolean busy);
    }
    private final RoomApiClient.Connection connection;
    private final MultiplayerTransport transport;
    private final String matchId;
    private final Listener listener;
    private RemoteBattleSnapshot snapshot;
    private boolean connected, pending;
    public OnlineBattleSession(RoomApiClient.Connection connection, MultiplayerTransport transport, Listener listener) {
        this(connection,transport,listener,connection.roomCode);
    }
    public OnlineBattleSession(RoomApiClient.Connection connection, MultiplayerTransport transport, Listener listener,String matchId) {
        this.connection = connection; this.transport = transport; this.listener = listener;this.matchId=matchId;
        transport.setSnapshotListener(this::receive);
    }
    public void connected(boolean value) { connected = value; listener.busyChanged(isBusy()); }
    public boolean isBusy() { return !connected || pending || snapshot == null; }
    public boolean owns(RemoteBattleSnapshot.Unit unit) { return unit != null && connection.playerSlots.contains(unit.playerSlot); }
    public RemoteBattleSnapshot getSnapshot() { return snapshot; }
    public void acceptSnapshot(String json) { receive(json); }
    @Override public void submit(BattleCommand command) {
        if (isBusy()) { listener.failed("서버 연결과 이전 행동 결과를 기다리세요."); return; }
        if (!owns(snapshot.find(command.getActorUnitId()))) { listener.failed("내 캐릭터만 조종할 수 있습니다."); return; }
        pending = true; listener.busyChanged(true);
        String json = CommandEnvelope.create(matchId, connection.clientId,
                UUID.randomUUID().toString(), snapshot.revision, command).toJson();
        transport.sendCommand(json, new MultiplayerTransport.Callback() {
            @Override public void onResponse(String json) { pending = false; receive(json); listener.busyChanged(isBusy()); }
            @Override public void onFailure(Throwable error) { pending = false; listener.failed(error.getMessage()); listener.busyChanged(isBusy()); }
        });
    }
    private void receive(String json) {
        try {
            CommandReceipt receipt = CommandReceipt.fromJson(json);
            if (receipt.getProtocolVersion() != MultiplayerProtocol.VERSION) throw new IllegalArgumentException("서버와 앱 버전이 다릅니다.");
            if (receipt.getSnapshotJson() != null) {
                RemoteBattleSnapshot next = RemoteBattleSnapshot.fromReceipt(receipt);
                if (snapshot == null || next.revision >= snapshot.revision) {
                    snapshot = next; listener.updated(snapshot);
                }
            }
            if (!receipt.isAccepted()) listener.failed(receipt.getMessage());
            listener.busyChanged(isBusy());
        } catch (RuntimeException e) { connected = false; listener.failed("전투 상태를 읽지 못했습니다. 다시 연결하세요."); listener.busyChanged(true); }
    }
}
