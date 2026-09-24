package com.pas.game.skill.repository;

import static com.pas.game.skill.effect.Effects.attackDamage;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.TargetType;
import com.pas.game.battle.engine.RedAltarEncounter;
import com.pas.game.unit.EnemyUnit;

public final class EnemySkillRepository {
    private EnemySkillRepository() {}
    public static SkillData basicAttack(){return SkillData.builder("dummy_attack","기본 공격",SkillGrade.NORMAL).value(100,0).range(0).target(TargetType.ENEMY).describe("공격력 100% 피해").effect(attackDamage()).build();}
    public static SkillData slam(){return SkillData.builder("dummy_slam","강타",SkillGrade.UNCOMMON).value(150,0).range(0).target(TargetType.ENEMY).describe("3번째 자기 턴마다 공격력 150% 피해").effect(attackDamage()).build();}
    public static SkillData unholyFlame(){return SkillData.builder("altar_unholy_flame","불경한 화염",SkillGrade.NORMAL).value(100,0).range(2).target(TargetType.ENEMY).effect(attackDamage()).build();}
    public static SkillData summon(){return SkillData.builder("altar_summon","악마 소환",SkillGrade.UNCOMMON).cooldown(4).target(TargetType.SELF).describe("계약된 하급악마 1체 소환. 다음 라운드부터 행동. 싱글 최대 2체, 온라인 협동은 제한 없음.").effect((engine,caster,target,value)->RedAltarEncounter.summon(engine,(EnemyUnit)caster)).build();}
    public static SkillData bloodHymn(){return SkillData.builder("altar_blood_hymn","핏빛 찬가",SkillGrade.UNCOMMON).cooldown(5).target(TargetType.SELF).describe("자신과 모든 소환 악마의 공격력 +5, 2턴. 해제 가능.").effect((engine,caster,target,value)->RedAltarEncounter.hymn(engine,(EnemyUnit)caster)).build();}
    public static SkillData claw(){return SkillData.builder("demon_claw","할퀴기",SkillGrade.NORMAL).value(100,0).range(0).target(TargetType.ENEMY).effect(attackDamage()).build();}
    public static SkillData hellfireFangs(){return SkillData.builder("demon_hellfire_fangs","지옥불 이빨",SkillGrade.UNCOMMON).value(70,0).range(0).cooldown(3).target(TargetType.ENEMY).effect(attackDamage()).build();}
    public static SkillData find(String id){for(SkillData data:new SkillData[]{basicAttack(),slam(),unholyFlame(),summon(),bloodHymn(),claw(),hellfireFangs()})if(data.getId().equals(id))return data;return ChapterOneEnemySkills.find(id);}
}
