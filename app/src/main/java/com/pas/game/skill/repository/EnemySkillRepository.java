package com.pas.game.skill.repository;

import static com.pas.game.skill.effect.Effects.attackDamage;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.TargetType;

public final class EnemySkillRepository {
    private EnemySkillRepository() {}
    public static SkillData basicAttack(){return SkillData.builder("dummy_attack","기본 공격",SkillGrade.NORMAL).value(100,0).range(0).target(TargetType.ENEMY).describe("공격력 100% 피해").effect(attackDamage()).build();}
    public static SkillData slam(){return SkillData.builder("dummy_slam","강타",SkillGrade.UNCOMMON).value(150,0).range(0).target(TargetType.ENEMY).describe("3번째 자기 턴마다 공격력 150% 피해").effect(attackDamage()).build();}
}
