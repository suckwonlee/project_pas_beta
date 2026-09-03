package com.pas.game.passive;

import com.pas.game.battle.damage.DamageType;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.unit.BattleUnit;

/** 한 번의 패시브 이벤트가 공유하는 문맥. 피해량은 피해 적용 전에 안전하게 조정할 수 있다. */
public final class PassiveContext {
    private final BattleEngine engine;
    private final PassiveTrigger trigger;
    private final BattleUnit actor;
    private final BattleUnit target;
    private final DamageType damageType;
    private final SkillRuntime skill;
    private double damage;
    private int finalDamage;

    private PassiveContext(BattleEngine engine,PassiveTrigger trigger,BattleUnit actor,BattleUnit target,DamageType damageType,SkillRuntime skill,double damage){this.engine=engine;this.trigger=trigger;this.actor=actor;this.target=target;this.damageType=damageType;this.skill=skill;this.damage=Math.max(0,damage);}
    public static PassiveContext event(BattleEngine engine,PassiveTrigger trigger,BattleUnit actor,BattleUnit target){return new PassiveContext(engine,trigger,actor,target,null,null,0);}
    public static PassiveContext skill(BattleEngine engine,BattleUnit actor,BattleUnit target,SkillRuntime skill){return new PassiveContext(engine,PassiveTrigger.AFTER_SKILL_USED,actor,target,null,skill,0);}
    public static PassiveContext damage(BattleEngine engine,PassiveTrigger trigger,BattleUnit actor,BattleUnit target,DamageType type,double amount){return new PassiveContext(engine,trigger,actor,target,type,null,amount);}
    public BattleEngine getEngine(){return engine;} public PassiveTrigger getTrigger(){return trigger;}
    public BattleUnit getActor(){return actor;} public BattleUnit getTarget(){return target;}
    public DamageType getDamageType(){return damageType;} public SkillRuntime getSkill(){return skill;}
    public double getDamage(){return damage;} public void setDamage(double value){damage=Math.max(0,value);}
    public double reduceDamage(double amount){double before=damage;damage=Math.max(0,damage-Math.max(0,amount));return before-damage;}
    public int getFinalDamage(){return finalDamage;} public void setFinalDamage(int value){finalDamage=Math.max(0,value);}
}
