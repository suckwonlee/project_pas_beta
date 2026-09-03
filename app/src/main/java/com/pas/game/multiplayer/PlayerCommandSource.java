package com.pas.game.multiplayer;

import com.pas.game.battle.command.BattleCommand;

/** 로컬 입력과 미래 RemotePlayerController가 구현할 공통 명령 출처다. */
public interface PlayerCommandSource { void submit(BattleCommand command); }
