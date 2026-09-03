package com.pas.game.unit;

/** 턴과 승패에는 참여하지 않지만 적의 공격/상태 대상이 되는 허수체. */
public final class WizardPhantom extends BattleUnit {
    private final String ownerId;
    private int remainingOwnerTurns;
    private int remainingHits;
    public WizardPhantom(String unitId,String ownerId,String name,Team team,int tile,int ownerTurns,int hits){super(unitId,name,team,1,0,0,0,0,tile);this.ownerId=ownerId;this.remainingOwnerTurns=ownerTurns;this.remainingHits=hits;}
    public String getOwnerId(){return ownerId;} public int getRemainingHits(){return remainingHits;} public int getRemainingOwnerTurns(){return remainingOwnerTurns;}
    public void addHits(int amount){remainingHits+=Math.max(0,amount);}
    public boolean consumeElement(){remainingHits--;if(remainingHits<=0){kill();return true;}return false;}
    public boolean tickOwnerTurn(){remainingOwnerTurns--;if(remainingOwnerTurns<=0){kill();return true;}return false;}
    @Override public boolean participatesInTurns(){return false;}
    @Override public boolean countsForOutcome(){return false;}
    @Override public boolean canReceiveSupport(){return false;}
}
