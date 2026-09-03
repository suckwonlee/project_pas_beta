package com.pas.game.battle.result;

public final class BattleEvent {
    public enum Type { TURN_STARTED, MOVED, SKILL_USED, POTION_USED, DAMAGE, HEAL, STATUS, DEATH, VICTORY, DEFEAT, ERROR }
    private final Type type; private final String message;
    public BattleEvent(Type type,String message){this.type=type;this.message=message;}
    public Type getType(){return type;} public String getMessage(){return message;}
}
