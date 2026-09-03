package com.pas.game.ai;

import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.result.BattleResult;

public interface EnemyAI { BattleResult takeTurn(BattleEngine engine); }
