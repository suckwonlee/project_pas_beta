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
        s.add(skill("archery","궁술",SkillGrade.NORMAL,95,20,-1,0,0,1,TargetType.ENEMY,"공격력 계수 단일 피해").effect(attackDamage()).flavor("“다루기 쉽지 않지만, 그렇기에 감시단의 일원들은 모두 활을 다뤘다. 이조차 하지 못하는 이들은 살아남을 수 없다는 의미였지”").build());
        s.add(skill("rapid_fire","속사",SkillGrade.NORMAL,45,8,-1,0,0,1,TargetType.ENEMY,"공격력 계수 2회 피해").effect(multiAttack(2)).flavor("“단궁의 장점은 위력보단 빠른 속사에 있다. 장궁이 더 유리한 시점이 오기전에 승리하지 않으면 죽는건 이쪽이니까”").build());
        s.add(skill("brow_shot","미간 사격",SkillGrade.NORMAL,200,40,-1,0,0,2,TargetType.ENEMY,"치명 피해와 영구 치명타율 증가").effect(criticalDamageAttack()).effect(permanentCriticalRate(5,1,200,40)).flavor("“빗나갈지라도, 놈을 서서히 궁지로 몰아라. 마침내 구석에 몰린 놈의 숨통을 끊도록”").build());

        s.add(skill("hunter_defend","방어",SkillGrade.NORMAL,250,50,-1,0,0,0,TargetType.SELF,"방어력 기반 피해 방어").effect(barrierFromDefense()).flavor("“모든걸 피할수는 없다. 그렇다면 적어도 덜아프게 맞거라”").build());
        s.add(skill("evasion_focus","회피 전념",SkillGrade.NORMAL,100,10,-1,0,0,0,TargetType.SELF,"다음 자기 턴까지 방어력 기반 회피율 증가").effect(evasionFromDefense(2)).flavor("“살아남아라. 살아서 끝까지 저 끔찍한 놈들을 처단하라.").build());
        s.add(skill("emergency_escape","비상 탈출",SkillGrade.NORMAL,50,0,8,1,0,1,TargetType.TILE,"인접 칸을 미리 지정하고 피격 직전에 이동해 회피").minimumRange(1).effect(armEmergencyEscape()).flavor("“적에게 등을 보이지 마라. 보일 틈도 없이 피해내는것이다”").build());

        s.add(skill("fixed_trap","고정덫",SkillGrade.UNCOMMON,1,0,3,1,4,0,TargetType.TILE,"밟은 적에게 다음 턴 종료까지 구속을 거는 덫 설치").effect(installBindingTrap(2)).flavor("“공포를 가르처주마 괴물들아. 피하지 못하는 죽음이라는”").build());
        s.add(skill("stealth_movement","은밀기동",SkillGrade.UNCOMMON,30,5,6,0,2,1,TargetType.TILE,"스킬 이동 1칸 및 회피율 증가").minimumRange(1).effect(skillMove()).effect(selfStatusUsingValue(StatusType.EVADE_UP,2)).flavor("“도망치는 적들이 가장 성가신것은, 언제 습격할지 모르는 추적이다”").build());
        s.add(skill("cold_aim","냉혈한 조준",SkillGrade.UNCOMMON,20,10,3,0,12,0,TargetType.SELF,"일반 공격 치명타 시 예리함 획득, 비치명타 시 예리함 소멸").effect(sharpTraining()).flavor("“이걸로 죽음의 흐름은 네쪽에 흐르기 시작했다”").build());
        s.add(skill("venom_injection","독액주입",SkillGrade.UNCOMMON,2,2,3,0,9,0,TargetType.SELF,"침독 패시브와 같은 독공격 효과 획득").effect(venomAttackTraining()).flavor("“피를 흘리게 하고 그 자리를 독으로 채워넣는다면 괴물들도 버텨내지 못하더군”").build());

        s.add(skill("piercing_shot","관통사",SkillGrade.MAGIC,110,10,8,0,0,2,TargetType.TILE,"선택 방향의 직선상 모든 적 관통").minimumRange(1).cardinalTileOnly().effect(piercingLineAttack()).flavor("“엄폐물에서 나오도록 유도하는게 최고지만, 그게 힘들다면 꿰뚫어라”").build());
        s.add(skill("install_decoy","미끼 설치",SkillGrade.MAGIC,2,0,3,1,5,1,TargetType.TILE,"공격할 대상이 없는 인접 적의 이동을 유도하고 적 진입 시 소멸").effect(installDecoy(2)).flavor("“전장을 택하는 것도 전투에서 중요한 우위를 차지하게 한다.”").build());
        s.add(skill("swift_movement","신속기동",SkillGrade.MAGIC,2,0,8,2,0,2,TargetType.TILE,"실제 경로를 따라 최대 2칸 스킬 이동. 꺾이는 경로는 가로 칸을 먼저 통과").minimumRange(1).effect(skillMove()).flavor("“추격할때도, 피할때도, 후퇴할때도, 기동력은 언제나 중요하다”").build());

        s.add(skill("venom_chase","추격의 맹독",SkillGrade.EPIC,1,.5,2,0,0,2,TargetType.ENEMY,"대상의 중독에 비례해 영구 치명타율 증가").effect(criticalRateFromTargetPoison()).flavor("“독에 의해 몸이 굳어간다는것은, 내가 너의 목에 칼을 들이밀때란 거다”").build());
        s.add(skill("execution_shot","처형 사격",SkillGrade.EPIC,320,80,3,0,0,2,TargetType.ENEMY,"강력한 공격력 계수 피해").effect(attackDamage()).flavor("“확실하게 죽일 수 있는 순간이 왔다면 망설이지 마라. 두 번째 기회는 없을지도 모르니까.”").build());
        s.add(skill("joint_shot","관절 사격",SkillGrade.EPIC,120,0,3,2,0,2,TargetType.ENEMY,"피해와 몸이 굳음 1").effect(attackDamage()).effect(status(StatusType.STIFF,1,0)).flavor("“죽이는 것만이 사냥은 아니다. 도망칠 수 없게 만들면 그다음은 어렵지 않다.”").build());

        s.add(skill("solitude","고독",SkillGrade.LEGENDARY,300,100,2,0,9,2,TargetType.ENEMY,"공격력에 비례한 중독 부여").effect(poisonFromAttack()).flavor("“마지막에 살아남은 독충보다 독한 종은 바로 인간이다.”").build());
        s.add(skill("arrow_rain","화살비",SkillGrade.LEGENDARY,35,10,5,0,0,2,TargetType.ALL_ENEMIES_IN_RANGE,"사거리 안의 모든 적에게 4회 피해").effect(multiAttack(4)).flavor("“숨을곳도, 도망칠 곳도 없다. 끝까지 너희를 쫓겠다”").build());
        s.add(skill("deadly_poison_curse","극독의 저주",SkillGrade.OTHERWORLD,3,0,1,0,0,2,TargetType.ENEMY,"3턴 동안 지속 피해가 턴 종료마다 2회 적용").effect(dotDamageTwice(3)).flavor("“상대가 신이라 해도 죽여주겠다. 이 세계를 넘보지 마라”").build());
        s.add(skill("shadow_chase","그림자 추격",SkillGrade.OTHERWORLD,60,0,2,0,5,0,TargetType.SELF,"치명타율과 회피율 증가 3턴").effect(selfStatusUsingValue(StatusType.CRIT_RATE_UP,3)).effect(selfStatusUsingValue(StatusType.EVADE_UP,3)).flavor("“보이지 않는 것으로부터 도망칠수도, 숨을수도 없다”").build());
        skills=Collections.unmodifiableList(s);
    }

    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description){
        return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description);
    }
    @Override public List<SkillData> all(){return skills;}
    @Override public SkillData find(String id){for(SkillData skill:skills)if(skill.getId().equals(id))return skill;return null;}
}
