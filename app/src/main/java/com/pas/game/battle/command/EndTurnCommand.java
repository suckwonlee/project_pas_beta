package com.pas.game.battle.command;

public final class EndTurnCommand implements BattleCommand {
    private final String actorUnitId; public EndTurnCommand(String actorUnitId){this.actorUnitId=actorUnitId;}
    @Override public String getActorUnitId(){return actorUnitId;} @Override public String getType(){return "END_TURN";}
}
