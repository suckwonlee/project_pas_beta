package com.pas.game.skill.repository;

import static com.pas.game.skill.effect.Effects.*;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.TargetType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 성직자 스킬 20종. 복합 계수도 실행식과 상세 표시식이 같은 계산을 사용한다. */
public final class ClericSkillRepository implements SkillRepository {
    private final List<SkillData> skills;
    public ClericSkillRepository(){
        List<SkillData> s=new ArrayList<>();
        s.add(skill("banish_profane","불경 퇴치",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.ENEMY,"공격력 계수 단일 피해","\"참기 힘든 역겨움이네요, 이 불경한 자여.\"").effect(clericAttackDamage()).build());
        s.add(skill("holy_strike","거룩한 일격",SkillGrade.NORMAL,80,16,-1,0,0,0,TargetType.ENEMY,"공격력 피해와 방어력 회복","\"그녀에게 악의 정화는 봉사와도 같았고, 그녀는 봉사하며 지친 적이 없었다.\"").effect(holyStrike()).build());
        s.add(skill("purifying_flame","정화의 불길",SkillGrade.NORMAL,120,25,-1,0,0,1,TargetType.ENEMY,"피해 없이 정화를 얻고 지정 대상에게 이전 조건 확인","\"타락자들이 말하는 구원이 기다리던 자리에 그녀의 기도가 울려 퍼지자 정화의 불길이 피어났다.\"").effect(purifyingFlame()).build());

        s.add(skill("shield_of_faith","신앙의 방패",SkillGrade.NORMAL,300,0,-1,0,0,0,TargetType.SELF,"방어력 기반 피해 방어","\"빛이 절 보호할 것입니다. 무엇도 두려울 것은 없습니다.\"").effect(barrierFromDefense()).build());
        s.add(skill("healing_prayer","치유의 기도",SkillGrade.NORMAL,20,7,-1,0,0,0,TargetType.SELF,"방어력 기반 피해 감소와 잃은 HP 회복","\"추기경의 따님께선 정말 독실하신 분이지. 누가 부르기 전에 그분이 기도실에서 나오는 걸 본 사람이 없다네.\"").effect(healingPrayer()).build());
        s.add(skill("divine_intervention","신성 개입",SkillGrade.NORMAL,10,0,10,2,0,0,TargetType.SELF,"최대 HP 회복과 일반 피해 무적","\"필멸자 개인의 믿음으로 저만한 빛을 끌어낼 리가 없다. 천상이 그녀에게 걸어본 것일지도 모르겠군.\"").effect(divineIntervention()).build());

        s.add(skill("blessing_of_resolve","결의의 축복",SkillGrade.UNCOMMON,40,10,4,0,9,1,TargetType.ALLY,"방어력 증가","\"자신을 믿고 나아가세요. 제가 함께합니다!\"").effect(defenseBlessing()).build());
        s.add(skill("healing_touch","치유의 손길",SkillGrade.UNCOMMON,200,50,10,0,4,1,TargetType.ALLY,"방어력 기반 단일 회복","\"상처를 감추실 필요는 없습니다. 치유받는 것은 나약함의 증명이 아니니까요.\"").effect(healFromCasterDefense()).build());
        s.add(skill("absolution","면죄",SkillGrade.UNCOMMON,100,20,4,1,5,1,TargetType.ALLY,"부정적 상태 1개 해제와 회복","\"죄와 저주는 다릅니다. 적어도 후자는 제가 걷어드릴 수 있겠군요.\"").effect(absolution()).build());
        s.add(skill("blessing_of_life","생명의 축복",SkillGrade.UNCOMMON,20,5,4,0,9,1,TargetType.ALLY,"전투 종료까지 최대 HP와 현재 HP 증가","\"생명의 불씨가 남아 있는 한, 그것을 더 크게 피워내는 것은 어렵지 않습니다.\"").effect(lifeBlessing()).build());

        s.add(skill("blessing_of_courage","용맹의 축복",SkillGrade.MAGIC,40,10,4,0,9,1,TargetType.ALLY,"공격력 증가","\"당신이 공포를 느낀다 해도, 당신 안의 용기는 그 이상의 크기를 가지고 있습니다. 나아가십시오.\"").effect(attackBlessing()).build());
        s.add(skill("healing_wave","회복의 파동",SkillGrade.MAGIC,150,40,4,0,5,1,TargetType.SELF,"범위 우호적 대상 회복","\"천상은 누구부터 구해야 할지 고민하지 않습니다. 닿는 모두를 감싸안을 뿐이지요.\"").effect(healingWave()).build());
        s.add(skill("heavenly_passage","천상의 통로",SkillGrade.MAGIC,150,10,5,0,9,0,TargetType.SELF,"보유 정화 증폭","천상과 연결된 문과 같은 존재가 된 그녀의 몸에서 정화의 힘이 넘치기 시작했다.").effect(heavenlyPassage()).build());

        s.add(skill("baptism_of_purification","정화의 세례",SkillGrade.EPIC,300,100,3,0,0,0,TargetType.SELF,"방어력 기반 정화 획득","\"제 안의 빛이 부족하다면, 더욱 깊이 기도하면 될 일입니다.\"").effect(purificationBaptism()).build());
        s.add(skill("withdraw_grace","은총 철회",SkillGrade.EPIC,0,0,2,1,0,1,TargetType.SELF,"범위 내 해제 가능한 이로운 효과 제거","\"타인을 저주한다면 그 저주는 자신에게 돌아오는 법이지요.\"").effect(withdrawGrace()).build());
        s.add(skill("answered_prayer","응답된 기도",SkillGrade.EPIC,4,0,2,1,0,1,TargetType.ALLY,"모든 스킬 쿨타임 감소","\"이계의 침공에 고통받는 이들을 위한 기도는 평소 이상으로 간절했고, 그 기도는 천상에 닿았다.\"").effect(answeredPrayer()).build());

        s.add(skill("crusade","성전",SkillGrade.LEGENDARY,100,50,3,0,9,1,TargetType.SELF,"범위 아군 최대 HP와 공격력·방어력 증가","\"이 땅을 구하기 위한 싸움은 매 순간이 중요하지만, 이 전투야말로 판도를 뒤집을 전투가 될 것입니다.\"").effect(crusade()).build());
        s.add(skill("sanctuary","성역",SkillGrade.LEGENDARY,100,50,1,1,0,0,TargetType.SELF,"고정 범위 회복·정화 설치물","\"이곳에 머무르십시오. 적어도 이 땅 위에서는 그 누구도 당신을 앗아가지 못할 테니.\"").effect(sanctuary()).build());
        s.add(skill("divine_fragment","신성의 파편",SkillGrade.OTHERWORLD,5,0,1,0,0,0,TargetType.SELF,"싱글과 멀티플레이에 따라 효과 전환","\"제가 지금 짓는 것은 제 자의로 행하는 가장 큰 죄악입니다. 천상의 초대장을 이 땅의 구원에 필요한 힘으로 쓰겠나이다.\"").effect(divineFragment()).build());
        s.add(skill("divine_punishment","신벌",SkillGrade.OTHERWORLD,1,0,1,0,0,2,TargetType.ENEMY,"정화 획득을 대상의 현재·최대 HP 감소로 전환","\"그녀의 몸을 통해 뿜어져 나오는 빛에 닿자, 악의 존재가 서서히 흐려지기 시작했다.\"").effect(divinePunishment()).build());
        skills=Collections.unmodifiableList(s);
    }
    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description,String flavor){return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description).flavor(flavor);}
    @Override public List<SkillData> all(){return skills;}
    @Override public SkillData find(String id){for(SkillData skill:skills)if(skill.getId().equals(id))return skill;return null;}
}
