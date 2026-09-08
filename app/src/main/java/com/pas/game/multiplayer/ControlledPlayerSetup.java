package com.pas.game.multiplayer;

/** 매치 참가 클라이언트와 그 클라이언트가 조종할 캐릭터 슬롯을 연결한다. */
public final class ControlledPlayerSetup {
    private final String clientId;
    private final PlayerBattleSetup player;
    public ControlledPlayerSetup(String clientId,PlayerBattleSetup player){if(clientId==null||clientId.trim().isEmpty()||player==null)throw new IllegalArgumentException("clientId and player setup are required");this.clientId=clientId;this.player=player;}
    public String getClientId(){return clientId;}public PlayerBattleSetup getPlayer(){return player;}
}
