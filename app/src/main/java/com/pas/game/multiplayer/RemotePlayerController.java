package com.pas.game.multiplayer;

import com.pas.game.battle.command.BattleCommand;
import java.util.concurrent.atomic.AtomicLong;

/** 실제 소켓 전송기로 교체 가능한 원격 플레이어 명령 출처. */
public final class RemotePlayerController implements PlayerCommandSource {
    public interface Listener {void onReceipt(CommandReceipt receipt);void onTransportError(Throwable error);}
    private final String matchId,clientId;private final MultiplayerTransport transport;private final Listener listener;private final AtomicLong sequence=new AtomicLong();private volatile long revision;
    public RemotePlayerController(String matchId,String clientId,long initialRevision,MultiplayerTransport transport,Listener listener){this.matchId=matchId;this.clientId=clientId;this.revision=initialRevision;this.transport=transport;this.listener=listener;transport.setSnapshotListener(this::handle);}
    @Override public void submit(BattleCommand command){
        String requestId=clientId+"-"+sequence.incrementAndGet();CommandEnvelope envelope=CommandEnvelope.create(matchId,clientId,requestId,revision,command);
        transport.sendCommand(envelope.toJson(),new MultiplayerTransport.Callback(){@Override public void onResponse(String json){handle(json);}@Override public void onFailure(Throwable error){listener.onTransportError(error);}});
    }
    public void synchronize(){transport.requestSnapshot(matchId,clientId,new MultiplayerTransport.Callback(){@Override public void onResponse(String json){handle(json);}@Override public void onFailure(Throwable error){listener.onTransportError(error);}});}
    public long getRevision(){return revision;}
    private void handle(String json){try{CommandReceipt receipt=CommandReceipt.fromJson(json);if(receipt.getProtocolVersion()!=MultiplayerProtocol.VERSION)throw new IllegalStateException("server protocol version mismatch");if(receipt.getCode()!=CommandErrorCode.MATCH_MISMATCH&&receipt.getCode()!=CommandErrorCode.UNKNOWN_CLIENT)revision=receipt.getRevision();listener.onReceipt(receipt);}catch(RuntimeException e){listener.onTransportError(e);}}
}
