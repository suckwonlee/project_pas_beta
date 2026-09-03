package com.pas.game.battle.state;

import com.pas.game.unit.Team;

/** 전투판에 남아 적이 밟을 때 한 번 발동하는 덫 상태다. */
public final class BattleTrap {
    private final String id,ownerUnitId;
    private final Team ownerTeam;
    private final int tile,bindTurns;
    public BattleTrap(String id,String ownerUnitId,Team ownerTeam,int tile,int bindTurns){this.id=id;this.ownerUnitId=ownerUnitId;this.ownerTeam=ownerTeam;this.tile=tile;this.bindTurns=bindTurns;}
    public String getId(){return id;} public String getOwnerUnitId(){return ownerUnitId;} public Team getOwnerTeam(){return ownerTeam;} public int getTile(){return tile;} public int getBindTurns(){return bindTurns;}
}
