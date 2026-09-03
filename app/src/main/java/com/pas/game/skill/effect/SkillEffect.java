package com.pas.game.skill.effect;

import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.unit.BattleUnit;

/** 스킬 효과의 실행 단위. 스킬 추가 시 엔진의 이름 분기 대신 효과를 조합한다. */
public interface SkillEffect {
    void apply(BattleEngine engine, BattleUnit caster, BattleUnit target, double value);
    default void apply(BattleEngine engine,BattleUnit caster,BattleUnit target,double value,Integer targetTile){apply(engine,caster,target,value);}
    default String describe(BattleUnit caster,BattleUnit target,double value){return "";}
}
