package com.pas.game.skill.repository;

import static com.pas.game.skill.effect.Effects.*;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.TargetType;
import com.pas.game.status.StatusType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 용사후보의 현재 스킬 설계도. 스킬 이름 분기 없이 각 효과 객체를 조합한다. */
public final class HeroSkillRepository implements SkillRepository {
    private final List<SkillData> skills;
    public HeroSkillRepository(){
        List<SkillData> s=new ArrayList<>();
        s.add(skill("slash","참격",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.ENEMY,"공격력 계수 단일 피해").effect(attackDamage()).build());
        s.add(skill("shield_art","방패술",SkillGrade.NORMAL,95,18,-1,0,0,0,TargetType.ENEMY,"방어력 계수 단일 피해").effect(defenseDamage()).build());
        s.add(skill("sword_faith","신념의 검",SkillGrade.NORMAL,20,4,-1,0,0,0,TargetType.ENEMY,"피해를 가하고 화염을 부여").effect(attackDamage()).effect(fireFromAttack(20)).build());
        s.add(skill("defend","방어",SkillGrade.NORMAL,250,50,-1,0,0,0,TargetType.SELF,"방어력 기반 피해 방어").effect(barrierFromDefense()).build());
        s.add(skill("weapon_guard","무기방어술",SkillGrade.NORMAL,200,40,-1,0,0,0,TargetType.SELF,"공격력 기반 피해 방어").effect(barrierFromAttack()).build());
        s.add(skill("unyielding_faith","불굴의 신념",SkillGrade.NORMAL,50,0,18,2,0,0,TargetType.SELF,"피해감소 50%, 저지불가·도발 2턴").effect(selfStatus(StatusType.DAMAGE_REDUCTION,2,50)).effect(selfStatus(StatusType.UNSTOPPABLE,2,0)).effect(tauntAllEnemies(2)).build());

        s.add(skill("head_bash","머리 강타",SkillGrade.UNCOMMON,50,0,4,1,0,0,TargetType.ENEMY,"피해 및 멍해짐 1").effect(attackDamage()).effect(status(StatusType.DAZED,1,0)).build());
        s.add(skill("first_aid","응급 처치",SkillGrade.UNCOMMON,32,8,6,0,0,1,TargetType.ALLY,"잃은 HP 회복 및 중독 절반 감소").effect(healMissing()).effect(reducePoisonHalf()).build());
        s.add(skill("certain_strike","확신의 검격",SkillGrade.UNCOMMON,20,4,6,0,0,0,TargetType.SELF,"치명타율 증가 2턴").effect(selfStatusUsingValue(StatusType.CRIT_RATE_UP,2)).build());
        s.add(skill("defense_focus","방어전념",SkillGrade.UNCOMMON,30,0,3,1,1,0,TargetType.SELF,"피해감소·회피불가 2턴, 멀티에서는 이동불가, 스킬 행동 +1").effect(selfStatus(StatusType.DAMAGE_REDUCTION,2,30)).effect(defenseFocusRestrictions()).effect(extraSkillAction()).build());

        s.add(skill("guardian_aura","수호의 오라",SkillGrade.MAGIC,20,5,3,0,6,1,TargetType.SELF,"자신을 중심으로 1칸 안의 아군이 받는 피해 감소").effect(guardianAura()).build());
        s.add(skill("battle_cry","전투의 함성",SkillGrade.MAGIC,60,20,4,0,0,1,TargetType.ALLY,"다음 자기 턴까지 공격력 증가").effect(battleCry()).build());
        s.add(skill("stone_throw","돌팔매",SkillGrade.MAGIC,120,25,8,0,0,2,TargetType.ENEMY,"사거리 2 원거리 피해").effect(attackDamage()).build());

        s.add(skill("mangle","난도질",SkillGrade.EPIC,40,10,4,0,0,0,TargetType.ALL_ENEMIES,"모든 적에게 4회 피해 후 다음 턴 멍해짐").effect(multiAttack(4)).effect(selfStatus(StatusType.DAZED,2,0)).build());
        s.add(skill("reckless_charge","저돌맹진",SkillGrade.EPIC,20,8,2,0,0,0,TargetType.ENEMY,"최대 HP 계수 피해, 저지불가, 턴 종료 반동").effect(maxHpFixedDamage()).effect(selfStatus(StatusType.UNSTOPPABLE,3,0)).effect(selfStatus(StatusType.RECOIL_AT_TURN_END,1,10)).build());
        s.add(skill("resolve","결단의 각오",SkillGrade.EPIC,30,10,3,0,0,0,TargetType.SELF,"적과 자신이 받는 피해 증가, 스킬 행동 +1").effect(damageTakenAllAndSelf()).effect(extraSkillAction()).build());

        s.add(skill("full_will","의지충만",SkillGrade.LEGENDARY,0,0,1,1,0,0,TargetType.SELF,"다른 비궁극기 스킬의 사용 횟수를 각각 2회 충전").effect(restoreNonUltimateUses(2)).build());
        s.add(skill("hero_swordsmanship","용사 검법",SkillGrade.LEGENDARY,500,200,-1,0,0,0,TargetType.ENEMY,"궁극 단일 피해").effect(attackDamage()).build());
        s.add(skill("judgment","정의의 심판",SkillGrade.OTHERWORLD,4,0,-1,0,0,2,TargetType.ENEMY,"대상의 턴마다 최대 HP의 4%만큼 피해").effect(justiceDot()).build());
        s.add(skill("salvation_vow","구원의 맹세",SkillGrade.OTHERWORLD,10,0,-1,0,0,0,TargetType.SELF,"최대 HP의 10%만큼 피해 감소").effect(permanentMaxHpFlatReduction()).build());
        skills=Collections.unmodifiableList(s);
    }
    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description){
        return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description);
    }
    public List<SkillData> all(){return skills;}
    public SkillData find(String id){for(SkillData s:skills)if(s.getId().equals(id))return s;return null;}
}
