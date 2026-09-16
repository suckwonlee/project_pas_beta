package com.pas.game.skill.repository;

import static com.pas.game.skill.effect.Effects.*;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.TargetType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 최신 전반 설계도의 마법사 스킬 20종. 전용 실행은 BattleEngine의 마법사 상태 모듈에 위임한다. */
public final class WizardSkillRepository implements SkillRepository {
    private final List<SkillData> skills;
    public WizardSkillRepository(){List<SkillData> s=new ArrayList<>();
        s.add(skill("mana_discharge","마력 방출",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.ENEMY,"일반 피해. 네 번째 사용마다 공격력 증가","”이렇게 넘치는 마력을 지배하면 결국 내 힘으로 돌아오지. 어때? 더 강렬한 마법을 느껴보겠어?”").effect(wizardManaDischarge()).build());
        s.add(skill("gluttonous_hand","탐식의 손",SkillGrade.NORMAL,80,20,-1,0,0,0,TargetType.ENEMY,"일반 피해. 가한 피해와 공격력에 비례해 회복","”탐식계의 마력이야. 이곳에 먹이를 나눠주면 나한테 그 이상 보답해주지”").effect(wizardGluttonousHand()).build());
        s.add(skill("nether_repulsion","명계의 척력",SkillGrade.NORMAL,80,20,-1,0,0,1,TargetType.ENEMY,"일반 피해. 자신에게 피해방어 부여","”이 바람을 뚫고도 나한테 공격이 닿았잖아? 칭찬해줄게”").effect(wizardNetherRepulsion()).build());

        s.add(skill("magic_ward","마력의 방호",SkillGrade.NORMAL,200,50,-1,0,0,0,TargetType.SELF,"피해방어와 누적 중갑","“주변에 마력을 두르면 그 자체로 준수한 갑옷이 돼.  어디 뚫어보겠어?”").effect(wizardMagicWard()).build());
        s.add(skill("chaos_distortion","혼돈의 왜곡",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.SELF,"적의 명중률 감소와 누적 치명타율 증가","“자 봐봐. 난 네 약점이 보이거든? 근데 넌 어떤데?”").effect(wizardChaosDistortion()).build());
        s.add(skill("devour","포식",SkillGrade.NORMAL,20,10,8,0,0,0,TargetType.SELF,"다음 자기 턴까지 일반 피해 무효화 및 회복","”자 탐식아. 서로 먹이를 나눠보자꾸나”").effect(wizardDevour()).build());

        s.add(skill("mana_disruption","마력 교란",SkillGrade.UNCOMMON,2,0,4,1,5,2,TargetType.ENEMY,"대상 스킬 하나의 현재 쿨타임 증가","“마법을 막을 필요가 있나? 술식에 조금 손대서 네가 다시 못 쓰게 만들면 되는데.”").effect(wizardManaDisruption()).build());
        s.add(skill("phantom_body","허수체",SkillGrade.UNCOMMON,3,0,4,0,5,2,TargetType.TILE,"피격과 상태효과를 대신 받는 허수체 설치","“가짜라고 얕보지 마. 적어도 저 녀석 눈에는 나랑 똑같이 보일 테니까.”").effect(wizardPhantom()).build());
        s.add(skill("mana_payment","마력 대납",SkillGrade.UNCOMMON,2,0,5,1,4,0,TargetType.SELF,"최대 HP 10%를 지불하고 다른 스킬 쿨타임 감소","“시간이 부족하면 다른 걸 내면 되지. 생명력 정도면 꽤 괜찮은 대가잖아?”").effect(wizardManaPayment()).build());
        s.add(skill("abyssal_mark","심연의 낙인",SkillGrade.UNCOMMON,20,5,4,0,6,2,TargetType.ENEMY,"아군의 일반 스킬 피해 시 공격자 회복","“심연은 틈만 있으면 파고들어. 네가 상처를 내줬으니, 흘러나오는 힘 정도는 내가 가져도 되겠지?”").effect(wizardAbyssalMark()).build());

        s.add(skill("dimensional_drift","차원 표류",SkillGrade.MAGIC,0,0,4,1,6,0,TargetType.SELF,"다음 자기 턴 시작까지 전장에서 이탈","“뿝! 나 보고싶었어?”").effect(wizardDimensionalDrift()).build());
        s.add(skill("otherworld_power","이계의 힘",SkillGrade.MAGIC,30,10,4,1,6,0,TargetType.SELF,"현재 공격력에 비례한 일시 공격력 증가","“물질계 안에서만 힘을 찾으니까 그 정도인 거야. 밖에는 넘쳐나는 게 힘인데.”").effect(wizardOtherworldPower()).build());
        s.add(skill("hungry_star","굶주린 별",SkillGrade.MAGIC,80,20,3,1,6,1,TargetType.TILE,"범위 안 유닛을 자동 공격하는 설치물","”편식을 안하는게 특징인 친구야. 그러니 조심해~.”").effect(wizardHungryStar()).build());

        s.add(skill("past_echo","과거의 잔상",SkillGrade.EPIC,0,0,1,0,0,0,TargetType.SELF,"세 번째 다음 자기 턴 시작에 위치와 HP 복원","“유감, 한계 시간내로 내 숨통을 끊지 못했어”").effect(wizardPastEcho()).build());
        s.add(skill("spell_theft","주문 탈취",SkillGrade.EPIC,1,0,3,0,0,1,TargetType.ENEMY,"가장 최근의 해제 가능한 이로운 효과 탈취","“지배력이 엉망이야. 네 힘이 나한테 꼬리치는 수준인걸?”").effect(wizardSpellTheft()).build());
        s.add(skill("existence_loan","존재 대여",SkillGrade.EPIC,3,0,2,1,0,0,TargetType.SELF,"비궁극기 스킬의 선택한 부가효과를 추가 적용","“존재한다는 건 의외로 단순해. 이쪽 세계가 있다고 받아들이게 만들면 그만이거든. 그러니 효과 하나쯤 더 존재한다고 해도 이상할 건 없지?”").effect(wizardExistenceLoan()).build());

        s.add(skill("infinite_fragment","무한의 편린",SkillGrade.LEGENDARY,50,30,1,0,0,0,TargetType.SELF,"현재 공격력에 비례한 전투 종료까지 공격력 증가","“그거 알아? 무한은 이론상 존재할 수 없어. 하지만 그 편린 정도라면 존재 가능하지. 내 마력이 그렇더라니까”").effect(wizardInfiniteFragment()).build());
        s.add(skill("double_cast","이중 영창",SkillGrade.LEGENDARY,1,0,1,1,9,0,TargetType.SELF,"다음 비궁극기 스킬을 스택 수만큼 추가 발동","\"한 번의 영창으로 한 번만 마법이 나가야 한다는 법칙은 없잖아? 아, 혹시 그런거 못한다고 마탑에서 배웠니?\"").effect(wizardDoubleCast()).build());

        s.add(skill("meteor_shower","유성우",SkillGrade.OTHERWORLD,500,0,1,0,0,2,TargetType.TILE,"선택 칸 주변 모든 유닛에게 진실 피해","” 마탑에서 쓰는 마법중 유일하게 쓸모있는 마법이야. 대마법의 영역까지 가야 통하는 마법쓰면서, 이길 생각은 있는거야?”").effect(wizardMeteor()).build());
        s.add(skill("otherworld_gate","이계의 문",SkillGrade.OTHERWORLD,100,0,1,0,0,0,TargetType.SELF,"적 턴 종료마다 진실 피해를 주는 영구 설치물","“완벽하게 통제된 이계의 문이야. 내가 개인이라는 약점을 완벽하게 지워주잖아?").effect(wizardOtherworldGate()).build());
        skills=Collections.unmodifiableList(s);
    }
    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description,String flavor){return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description).flavor(flavor);}
    @Override public List<SkillData> all(){return skills;}
    @Override public SkillData find(String id){for(SkillData skill:skills)if(skill.getId().equals(id))return skill;return null;}
}
