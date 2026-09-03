package com.pas.game.battle.command;

/** 로컬 UI와 미래 서버 입력이 동일한 형태로 엔진에 전달되는 명령 계약이다. */
public interface BattleCommand {
    String getActorUnitId();
    String getType();
}
