package com.pas.game.passive;

import com.pas.game.unit.BattleUnit;

/** 패시브 효과 실행 단위. 소유자의 팀이나 구체 클래스에 의존하지 않는다. */
public interface PassiveEffect {
    void apply(PassiveContext context, BattleUnit owner, PassiveRuntime runtime);
}
