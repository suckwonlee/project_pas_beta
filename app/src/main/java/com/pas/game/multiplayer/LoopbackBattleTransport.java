package com.pas.game.multiplayer;

/** 네트워크 없이도 직렬화 경계와 서버 검증을 그대로 통과시키는 멀티 테스트 전송기. */
public final class LoopbackBattleTransport implements MultiplayerTransport {
    private final AuthoritativeBattleSession session;private final String clientId;private final AuthoritativeBattleSession.Observer observer;private SnapshotListener snapshotListener;
    public LoopbackBattleTransport(AuthoritativeBattleSession session){this(session,null);}
    public LoopbackBattleTransport(AuthoritativeBattleSession session,String clientId){if(session==null)throw new IllegalArgumentException("session is required");this.session=session;this.clientId=clientId;this.observer=(source,snapshot)->{if(snapshotListener!=null&&(this.clientId==null||!this.clientId.equals(source)))snapshotListener.onSnapshot(new CommandReceipt(MultiplayerProtocol.VERSION,"PUSH",true,CommandErrorCode.OK,"",snapshot.getRevision(),snapshot.toJson(),snapshot.getDigest()).toJson());};session.addObserver(observer);}
    @Override public void sendCommand(String requestJson,Callback callback){
        try{callback.onResponse(session.submit(CommandEnvelope.fromJson(requestJson)).toJson());}catch(RuntimeException e){callback.onFailure(e);}
    }
    @Override public void requestSnapshot(String matchId,String clientId,Callback callback){
        try{if(!session.getMatchId().equals(matchId))callback.onResponse(new CommandReceipt(MultiplayerProtocol.VERSION,"SYNC",false,CommandErrorCode.MATCH_MISMATCH,"다른 매치입니다.",session.getRevision(),session.currentSnapshot().toJson(),session.currentSnapshot().getDigest()).toJson());else callback.onResponse(session.synchronize(clientId).toJson());}catch(RuntimeException e){callback.onFailure(e);}
    }
    @Override public void setSnapshotListener(SnapshotListener listener){this.snapshotListener=listener;}
}
