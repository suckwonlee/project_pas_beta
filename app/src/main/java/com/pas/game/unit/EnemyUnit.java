package com.pas.game.unit;

public final class EnemyUnit extends BattleUnit {
    private int ownTurnCount;
    private boolean struggleUsed;
    public EnemyUnit(String unitId,String name,int tile){super(unitId,name,Team.ENEMY,90,10,0,5,0,tile);}
    public int beginOwnTurn(){return ++ownTurnCount;} public int getOwnTurnCount(){return ownTurnCount;}
    public boolean isStruggleUsed(){return struggleUsed;} public void markStruggleUsed(){struggleUsed=true;}
}
