package com.pas.game.battle.state;

import com.pas.game.unit.Team;

/** 적 이동을 유도하고 적이 해당 칸에 들어오면 사라지는 미끼다. */
public final class BattleDecoy {
    private final String id,ownerUnitId;
    private final Team ownerTeam;
    private final int tile;
    private int remainingOwnerTurns;
    public BattleDecoy(String id,String ownerUnitId,Team ownerTeam,int tile,int remainingOwnerTurns){this.id=id;this.ownerUnitId=ownerUnitId;this.ownerTeam=ownerTeam;this.tile=tile;this.remainingOwnerTurns=Math.max(1,remainingOwnerTurns);}
    public String getId(){return id;} public String getOwnerUnitId(){return ownerUnitId;} public Team getOwnerTeam(){return ownerTeam;} public int getTile(){return tile;}
    public boolean tickOwnerTurn(){remainingOwnerTurns--;return remainingOwnerTurns<=0;}
}
