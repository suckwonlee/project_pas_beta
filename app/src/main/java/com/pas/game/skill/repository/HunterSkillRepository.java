package com.pas.game.skill.repository;

import static com.pas.game.skill.effect.Effects.*;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.TargetType;
import com.pas.game.status.StatusType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 사냥꾼 스킬 설계도. 전장 설치물/반응 이동은 전용 런타임이 준비되기 전까지 명시적으로 잠근다. */
public final class HunterSkillRepository implements SkillRepository {
    private final List<SkillData> skills;

    public HunterSkillRepository(){
        List<SkillData> s=new ArrayList<>();
        s.add(skill("archery","궁술",SkillGrade.NORMAL,90,20,-1,0,0,1,TargetType.ENEMY,"공격력 계수 단일 피해").effect(attackDamage()).build());
        s.add(skill("rapid_fire","속사",SkillGrade.NORMAL,40,8,-1,0,0,1,TargetType.ENEMY,"공격력 계수 2회 피해").effect(multiAttack(2)).build());
        s.add(skill("brow_shot","미간 사격",SkillGrade.NORMAL,200,40,-1,0,0,2,TargetType.ENEMY,"치명 피해와 영구 치명타율 증가").effect(criticalDamageAttack()).effect(permanentCriticalRate(5,1,200,40)).build());

        s.add(skill("hunter_defend","방어",SkillGrade.NORMAL,250,50,-1,0,0,0,TargetType.SELF,"방어력 기반 피해 방어").effect(barrierFromDefense()).build());
        s.add(skill("evasion_focus","회피 전념",SkillGrade.NORMAL,100,10,-1,0,0,0,TargetType.SELF,"다음 자기 턴까지 방어력 기반 회피율 증가").effect(evasionFromDefense(2)).build());
        s.add(skill("emergency_escape","비상 탈출",SkillGrade.NORMAL,50,0,8,1,0,1,TargetType.TILE,"인접 칸을 미리 지정하고 피격 직전에 이동해 회피").minimumRange(1).effect(armEmergencyEscape()).build());

        s.add(skill("fixed_trap","고정덫",SkillGrade.UNCOMMON,1,0,3,1,4,0,TargetType.TILE,"밟은 적에게 다음 턴 종료까지 구속을 거는 덫 설치").effect(installBindingTrap(2)).build());
        s.add(skill("stealth_movement","은밀기동",SkillGrade.UNCOMMON,30,5,6,0,2,1,TargetType.TILE,"스킬 이동 1칸 및 회피율 증가").minimumRange(1).effect(skillMove()).effect(selfStatusUsingValue(StatusType.EVADE_UP,2)).build());
        s.add(skill("cold_aim","냉혈한 조준",SkillGrade.UNCOMMON,20,10,3,0,12,0,TargetType.SELF,"일반 공격 치명타 시 예리함 획득, 비치명타 시 예리함 소멸").effect(sharpTraining()).build());
        s.add(skill("venom_injection","독액주입",SkillGrade.UNCOMMON,1,2,3,0,9,0,TargetType.SELF,"침독 패시브와 같은 독공격 효과 획득").effect(venomAttackTraining()).build());

        s.add(skill("piercing_shot","관통사",SkillGrade.MAGIC,110,10,8,0,0,2,TargetType.TILE,"선택 방향의 직선상 모든 적 관통").minimumRange(1).cardinalTileOnly().effect(piercingLineAttack()).build());
        s.add(skill("install_decoy","미끼 설치",SkillGrade.MAGIC,2,0,3,1,5,1,TargetType.TILE,"공격할 대상이 없는 인접 적의 이동을 유도하고 적 진입 시 소멸").effect(installDecoy(2)).build());
        s.add(skill("swift_movement","신속기동",SkillGrade.MAGIC,2,0,8,2,0,2,TargetType.TILE,"실제 경로를 따라 최대 2칸 스킬 이동. 꺾이는 경로는 가로 칸을 먼저 통과").minimumRange(1).effect(skillMove()).build());

        s.add(skill("venom_chase","추격의 맹독",SkillGrade.EPIC,1,.5,2,0,0,2,TargetType.ENEMY,"대상의 중독에 비례해 영구 치명타율 증가").effect(criticalRateFromTargetPoison()).build());
        s.add(skill("execution_shot","처형 사격",SkillGrade.EPIC,320,80,3,0,0,2,TargetType.ENEMY,"강력한 공격력 계수 피해").effect(attackDamage()).build());
        s.add(skill("joint_shot","관절 사격",SkillGrade.EPIC,120,0,3,2,0,2,TargetType.ENEMY,"피해와 몸이 굳음 1").effect(attackDamage()).effect(status(StatusType.STIFF,1,0)).build());

        s.add(skill("solitude","고독",SkillGrade.LEGENDARY,300,100,2,0,9,2,TargetType.ENEMY,"공격력에 비례한 중독 부여").effect(poisonFromAttack()).build());
        s.add(skill("arrow_rain","화살비",SkillGrade.LEGENDARY,35,10,5,0,0,2,TargetType.ALL_ENEMIES_IN_RANGE,"사거리 안의 모든 적에게 4회 피해").effect(multiAttack(4)).build());
        s.add(skill("deadly_poison_curse","극독의 저주",SkillGrade.OTHERWORLD,3,0,1,0,0,2,TargetType.ENEMY,"3턴 동안 지속 피해가 턴 종료마다 2회 적용").effect(dotDamageTwice(3)).build());
        s.add(skill("shadow_chase","그림자 추격",SkillGrade.OTHERWORLD,60,0,2,0,5,0,TargetType.SELF,"치명타율과 회피율 증가 3턴").effect(selfStatusUsingValue(StatusType.CRIT_RATE_UP,3)).effect(selfStatusUsingValue(StatusType.EVADE_UP,3)).build());
        skills=Collections.unmodifiableList(s);
    }

    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description){
        return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description);
    }
    @Override public List<SkillData> all(){return skills;}
    @Override public SkillData find(String id){for(SkillData skill:skills)if(skill.getId().equals(id))return skill;return null;}
}
