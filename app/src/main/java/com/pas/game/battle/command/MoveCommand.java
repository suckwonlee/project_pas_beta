package com.pas.game.battle.command;

import com.pas.game.battle.turn.MovementType;

public final class MoveCommand implements BattleCommand {
    private final String actorUnitId; private final int destinationTile; private final MovementType movementType;
    public MoveCommand(String actorUnitId,int destinationTile,MovementType movementType){this.actorUnitId=actorUnitId;this.destinationTile=destinationTile;this.movementType=movementType;}
    @Override public String getActorUnitId(){return actorUnitId;} @Override public String getType(){return "MOVE";}
    public int getDestinationTile(){return destinationTile;} public MovementType getMovementType(){return movementType;}
}
