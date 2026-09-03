package com.pas.game.battle.state;

import com.pas.game.unit.Team;

/** 전장에 고정되어 플레이어 진영 턴 단위로 지속시간이 줄어드는 성역이다. */
public final class BattleSanctuary {
    private final String id;
    private final String ownerUnitId;
    private final Team ownerTeam;
    private final int tile;
    private final int range;
    private final int healing;
    private int remainingPlayerTurns;
    private int lastTickRound;

    public BattleSanctuary(String id,String ownerUnitId,Team ownerTeam,int tile,int range,int healing,int duration,int createdRound){
        this.id=id;this.ownerUnitId=ownerUnitId;this.ownerTeam=ownerTeam;this.tile=tile;
        this.range=Math.max(0,range);this.healing=Math.max(0,healing);
        this.remainingPlayerTurns=Math.max(1,duration);this.lastTickRound=createdRound;
    }
    public String getId(){return id;} public String getOwnerUnitId(){return ownerUnitId;} public Team getOwnerTeam(){return ownerTeam;}
    public int getTile(){return tile;} public int getRange(){return range;} public int getHealing(){return healing;}
    public int getRemainingPlayerTurns(){return remainingPlayerTurns;}
    public boolean tickPlayerTurn(int round){if(round==lastTickRound)return false;lastTickRound=round;remainingPlayerTurns--;return remainingPlayerTurns<=0;}
}
