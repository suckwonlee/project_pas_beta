package com.pas.game.multiplayer;

import com.pas.game.battle.command.BattleCommand;

/** 재전송·순서 역전·다른 플레이어 명령을 서버에서 검증하기 위한 요청 봉투. */
public final class CommandEnvelope {
    private final int protocolVersion;
    private final String matchId,clientId,requestId;
    private final long expectedRevision;
    private final BattleCommand command;

    public CommandEnvelope(int protocolVersion,String matchId,String clientId,String requestId,long expectedRevision,BattleCommand command){
        this.protocolVersion=protocolVersion;this.matchId=matchId;this.clientId=clientId;this.requestId=requestId;this.expectedRevision=expectedRevision;this.command=command;
    }
    public static CommandEnvelope create(String matchId,String clientId,String requestId,long expectedRevision,BattleCommand command){return new CommandEnvelope(MultiplayerProtocol.VERSION,matchId,clientId,requestId,expectedRevision,command);}
    public int getProtocolVersion(){return protocolVersion;}public String getMatchId(){return matchId;}public String getClientId(){return clientId;}public String getRequestId(){return requestId;}public long getExpectedRevision(){return expectedRevision;}public BattleCommand getCommand(){return command;}
    public String toJson(){return BattleCommandWireCodec.encode(this);}
    public static CommandEnvelope fromJson(String json){return BattleCommandWireCodec.decode(json);}
}
