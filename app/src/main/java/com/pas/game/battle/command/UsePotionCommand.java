package com.pas.game.battle.command;

/** 현재 행동 유닛이 전투 인벤토리의 포션 한 개를 사용한다. */
public final class UsePotionCommand implements BattleCommand {
    private final String actorUnitId;
    private final int potionTier;
    public UsePotionCommand(String actorUnitId){this(actorUnitId,0);}
    public UsePotionCommand(String actorUnitId,int potionTier){this.actorUnitId=actorUnitId;this.potionTier=potionTier;}
    public int getPotionTier(){return potionTier;}
    @Override public String getActorUnitId(){return actorUnitId;}
    @Override public String getType(){return "USE_POTION";}
}
