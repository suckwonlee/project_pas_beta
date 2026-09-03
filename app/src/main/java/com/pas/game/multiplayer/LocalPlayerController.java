package com.pas.game.multiplayer;

import com.pas.game.battle.command.BattleCommand;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.result.BattleResult;

public final class LocalPlayerController implements PlayerCommandSource {
    public interface Listener { void onCommandResolved(BattleResult result); }
    private final BattleEngine engine; private final Listener listener;
    public LocalPlayerController(BattleEngine engine,Listener listener){this.engine=engine;this.listener=listener;}
    @Override public void submit(BattleCommand command){listener.onCommandResolved(engine.execute(command));}
}
