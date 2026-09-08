package com.pas.game.battle.command;

/** 차원 표류 복귀 위치 선택도 일반 전투 명령과 같은 검증 경로를 거친다. */
public final class ResolveWizardReturnCommand implements BattleCommand {
    private final String actorUnitId;
    private final int destinationTile;

    public ResolveWizardReturnCommand(String actorUnitId,int destinationTile){
        this.actorUnitId=actorUnitId;
        this.destinationTile=destinationTile;
    }

    @Override public String getActorUnitId(){return actorUnitId;}
    @Override public String getType(){return "RESOLVE_WIZARD_RETURN";}
    public int getDestinationTile(){return destinationTile;}
}
