package com.pas.game.battle.command;

public final class UseSkillCommand implements BattleCommand {
    private final String actorUnitId,skillId,targetUnitId,optionId; private final Integer targetTile;
    public UseSkillCommand(String actorUnitId,String skillId,String targetUnitId,Integer targetTile){this(actorUnitId,skillId,targetUnitId,targetTile,null);}
    public UseSkillCommand(String actorUnitId,String skillId,String targetUnitId,Integer targetTile,String optionId){this.actorUnitId=actorUnitId;this.skillId=skillId;this.targetUnitId=targetUnitId;this.targetTile=targetTile;this.optionId=optionId;}
    @Override public String getActorUnitId(){return actorUnitId;} @Override public String getType(){return "USE_SKILL";}
    public String getSkillId(){return skillId;} public String getTargetUnitId(){return targetUnitId;} public Integer getTargetTile(){return targetTile;}
    public String getOptionId(){return optionId;}
}
