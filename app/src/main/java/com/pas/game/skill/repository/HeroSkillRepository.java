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
        s.add(skill("slash","참격",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.ENEMY,"공격력 계수 단일 피해").effect(attackDamage()).flavor("“그가 검을 다루는 솜씨는 그 누구도 뛰어나다고 평가하지 않았다. 그러나 그 위력만은 누구도 무시할 수 없었지.”").build());
        s.add(skill("shield_art","방패술",SkillGrade.NORMAL,95,18,-1,0,0,0,TargetType.ENEMY,"방어력 계수 단일 피해").effect(defenseDamage()).flavor("“그가 방패를 들어올렸다고 방어태세라 착각하지 말거라. 방패도 훌륭한 둔기란걸 보여주는 녀석이니까”").build());
        s.add(skill("sword_faith","신념의 검",SkillGrade.NORMAL,20,4,-1,0,0,0,TargetType.ENEMY,"피해를 가하고 화염을 부여").effect(attackDamage()).effect(fireFromAttack(20)).flavor("“성검은 수세기동안 그 누구도 주인으로 인정하지 않았다. 허나 그 사내가 악이라 생각하는 자를 베자 악은 타오르기 시작했다”").build());
        s.add(skill("defend","방어",SkillGrade.NORMAL,250,50,-1,0,0,0,TargetType.SELF,"방어력 기반 피해 방어").effect(barrierFromDefense()).flavor("“적이 누군지 잘 모르겠다면 일단 방어태세를 취하라. 적의 패턴을 다 읽었을 때가 승리의 시점이다.’").build());
        s.add(skill("weapon_guard","무기방어술",SkillGrade.NORMAL,200,40,-1,0,0,0,TargetType.SELF,"공격력 기반 피해 방어").effect(barrierFromAttack()).flavor("“최선의 방어는 공격이라고는 하지만, 오래 살아남아야 적을 더 처치할 수 있다”").build());
        s.add(skill("unyielding_faith","불굴의 신념",SkillGrade.NORMAL,50,0,18,2,0,0,TargetType.SELF,"피해감소 50%, 저지불가·도발 2턴").effect(selfStatus(StatusType.DAMAGE_REDUCTION,2,50)).effect(selfStatus(StatusType.UNSTOPPABLE,2,0)).effect(tauntAllEnemies(2)).flavor("“내 길이 꺾이지 않기를… 이 세상이 다시 평화를 찾을 때까지”").build());

        s.add(skill("head_bash","머리 강타",SkillGrade.UNCOMMON,50,0,4,1,0,0,TargetType.ENEMY,"피해 및 멍해짐 1").effect(attackDamage()).effect(status(StatusType.DAZED,1,0)).flavor("“머리 방어구의 발전으로 그다지 위력적인 공격은 아니지. 하지만 적어도 어지럽게 만들기는 충분한 공격이다”").build());
        s.add(skill("first_aid","응급 처치",SkillGrade.UNCOMMON,32,8,6,0,0,1,TargetType.ALLY,"잃은 HP 회복 및 중독 절반 감소").effect(healMissing()).effect(reducePoisonHalf()).flavor("“이 약초는 상처에 좋고 이 약초는 독에 좋지. 세상을 구하는게 하루이틀일은 아닐거 아냐”").build());
        s.add(skill("certain_strike","확신의 검격",SkillGrade.UNCOMMON,20,4,6,0,0,0,TargetType.SELF,"치명타율 증가 2턴").effect(selfStatusUsingValue(StatusType.CRIT_RATE_UP,2)).flavor("“그의 검은 조금도 흔들리지 않았다. 이 길이 틀렸다는 의심 자체가 없는것처럼”").build());
        s.add(skill("defense_focus","방어전념",SkillGrade.UNCOMMON,30,0,3,1,1,0,TargetType.SELF,"피해감소·회피불가 2턴, 멀티에서는 이동불가, 스킬 행동 +1").effect(selfStatus(StatusType.DAMAGE_REDUCTION,2,30)).effect(defenseFocusRestrictions()).effect(extraSkillAction()).flavor("“피하지 않는다. 내가 옳다면 저 공격을 버텨낼 수 있을것이다”").build());

        s.add(skill("guardian_aura","수호의 오라",SkillGrade.MAGIC,20,5,3,0,6,1,TargetType.SELF,"자신을 중심으로 1칸 안의 아군이 받는 피해 감소").effect(guardianAura()).flavor("“기원은 다르나 그의 의지는 성기사들의 오오라와 비슷한 결과를 가저왔다..”").build());
        s.add(skill("battle_cry","전투의 함성",SkillGrade.MAGIC,60,20,4,0,0,1,TargetType.ALLY,"다음 자기 턴까지 공격력 증가").effect(battleCry()).flavor("“기세에서 절대 밀리지 마라. 전투는 기세가 절반이다”").build());
        s.add(skill("stone_throw","돌팔매",SkillGrade.MAGIC,120,25,8,0,0,2,TargetType.ENEMY,"사거리 2 원거리 피해").effect(attackDamage()).flavor("“과거 중앙대륙을 통일한 왕은 돌 하나를 날려 거인을 해치웠다고 한다”").build());

        s.add(skill("mangle","난도질",SkillGrade.EPIC,40,10,4,0,0,0,TargetType.ALL_ENEMIES,"모든 적에게 4회 피해 후 다음 턴 멍해짐").effect(multiAttack(4)).effect(selfStatus(StatusType.DAZED,2,0)).flavor("“원래 얌전한 사람이 분노한게 더 무섭다고들 하지. 저 친구도 비슷하니 조심하라고”").build());
        s.add(skill("reckless_charge","저돌맹진",SkillGrade.EPIC,20,8,2,0,0,0,TargetType.ENEMY,"최대 HP 계수 피해, 저지불가, 턴 종료 반동").effect(maxHpFixedDamage()).effect(selfStatus(StatusType.UNSTOPPABLE,3,0)).effect(selfStatus(StatusType.RECOIL_AT_TURN_END,1,10)).flavor("“전투에서 가장 먼저 죽는 자들이 보통 저렇게 돌진하곤 한다. 허나 전투에서 필요한것이 저런 용기고, 저런 용기를 가진 자들이 승전을 가져오곤 한다”").build());
        s.add(skill("resolve","결단의 각오",SkillGrade.EPIC,30,10,3,0,0,0,TargetType.SELF,"적과 자신이 받는 피해 증가, 스킬 행동 +1").effect(damageTakenAllAndSelf()).effect(extraSkillAction()).flavor("“정의를 가로막는 너를 반드시 베어내겠다. 설령 내가 쓰러질지라도”").build());

        s.add(skill("full_will","의지충만",SkillGrade.LEGENDARY,0,0,1,1,0,0,TargetType.SELF,"다른 비궁극기 스킬의 사용 횟수를 각각 2회 충전").effect(restoreNonUltimateUses(2)).flavor("“아직이다. 아직 내 마음은 꺾이지 않았어”").build());
        s.add(skill("hero_swordsmanship","용사 검법",SkillGrade.LEGENDARY,500,200,1,0,0,0,TargetType.ENEMY,"궁극 단일 피해").effect(attackDamage()).flavor("“이 일격에 모든걸 걸겠어”").build());
        s.add(skill("judgment","정의의 심판",SkillGrade.OTHERWORLD,4,0,1,0,0,2,TargetType.ENEMY,"대상의 턴마다 최대 HP의 4%만큼 피해").effect(justiceDot()).flavor("“그가 정의를 구현하겠다고 결의하며 적을 베어내자, 이 세상이 악을 단죄하기 시작했다.”").build());
        s.add(skill("salvation_vow","구원의 맹세",SkillGrade.OTHERWORLD,10,0,1,0,0,0,TargetType.SELF,"최대 HP의 10%만큼 피해 감소").effect(permanentMaxHpFlatReduction()).flavor("“아직, 난 이 세계를 구하지 못했다”").build());
        skills=Collections.unmodifiableList(s);
    }
    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description){
        return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description);
    }
    public List<SkillData> all(){return skills;}
    public SkillData find(String id){for(SkillData s:skills)if(s.getId().equals(id))return s;return null;}
}
