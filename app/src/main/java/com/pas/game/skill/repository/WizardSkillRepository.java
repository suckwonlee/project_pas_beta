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
        s.add(skill("mana_discharge","마력 방출",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.ENEMY,"일반 피해. 네 번째 사용마다 공격력 증가","\"모아둔 마력을 가장 단순하고 난폭한 형태로 쏟아낸다.\"").effect(wizardManaDischarge()).build());
        s.add(skill("gluttonous_hand","탐식의 손",SkillGrade.NORMAL,80,20,-1,0,0,0,TargetType.ENEMY,"일반 피해. 가한 피해와 공격력에 비례해 회복","\"잡아먹을 수 있는 힘이라면, 굳이 흘려보낼 이유가 없지.\"").effect(wizardGluttonousHand()).build());
        s.add(skill("nether_repulsion","명계의 척력",SkillGrade.NORMAL,80,20,-1,0,0,1,TargetType.ENEMY,"일반 피해. 자신에게 피해방어 부여","\"죽음의 세계조차 그녀의 손끝에서는 밀려난다.\"").effect(wizardNetherRepulsion()).build());

        s.add(skill("magic_ward","마력의 방호",SkillGrade.NORMAL,200,50,-1,0,0,0,TargetType.SELF,"피해방어와 누적 중갑","\"마법은 갑옷보다 가볍고, 성벽보다 단단하다.\"").effect(wizardMagicWard()).build());
        s.add(skill("chaos_distortion","혼돈의 왜곡",SkillGrade.NORMAL,100,20,-1,0,0,0,TargetType.SELF,"적의 명중률 감소와 누적 치명타율 증가","\"보이는 곳과 닿는 곳이 같으리란 보장은 없잖아?\"").effect(wizardChaosDistortion()).build());
        s.add(skill("devour","포식",SkillGrade.NORMAL,20,10,8,0,0,0,TargetType.SELF,"다음 자기 턴까지 일반 피해 무효화 및 회복","\"네가 던진 힘은 이제 내 것이야.\"").effect(wizardDevour()).build());

        s.add(skill("mana_disruption","마력 교란",SkillGrade.UNCOMMON,2,0,4,1,5,2,TargetType.ENEMY,"대상 스킬 하나의 현재 쿨타임 증가","\"주문을 막을 필요는 없어. 조금 늦추기만 하면 되니까.\"").effect(wizardManaDisruption()).build());
        s.add(skill("phantom_body","허수체",SkillGrade.UNCOMMON,3,0,4,0,5,2,TargetType.TILE,"피격과 상태효과를 대신 받는 허수체 설치","\"빈 껍데기도 믿게 만들면 훌륭한 방패가 된다.\"").effect(wizardPhantom()).build());
        s.add(skill("mana_payment","마력 대납",SkillGrade.UNCOMMON,2,0,5,1,4,0,TargetType.SELF,"최대 HP 10%를 지불하고 다른 스킬 쿨타임 감소","\"대가는 필요하지. 꼭 마력일 필요는 없지만.\"").effect(wizardManaPayment()).build());
        s.add(skill("abyssal_mark","심연의 낙인",SkillGrade.UNCOMMON,20,5,4,0,6,2,TargetType.ENEMY,"아군의 일반 스킬 피해 시 공격자 회복","\"심연은 한 번 바라본 것을 좀처럼 놓아주지 않는다.\"").effect(wizardAbyssalMark()).build());

        s.add(skill("dimensional_drift","차원 표류",SkillGrade.MAGIC,0,0,4,1,6,0,TargetType.SELF,"다음 자기 턴 시작까지 전장에서 이탈","\"잠시 다른 차원에 다녀올게. 여기선 눈 한 번 깜빡일 시간이겠지만.\"").effect(wizardDimensionalDrift()).build());
        s.add(skill("otherworld_power","이계의 힘",SkillGrade.MAGIC,30,10,4,1,6,0,TargetType.SELF,"현재 공격력에 비례한 일시 공격력 증가","\"빌려온 힘이라도 내 손에 들어오면 내 방식으로 움직여.\"").effect(wizardOtherworldPower()).build());
        s.add(skill("hungry_star","굶주린 별",SkillGrade.MAGIC,80,20,3,1,6,1,TargetType.TILE,"범위 안 유닛을 자동 공격하는 설치물","\"별은 아름답지. 가까이서 보면 굶주렸다는 것만 빼면.\"").effect(wizardHungryStar()).build());

        s.add(skill("past_echo","과거의 잔상",SkillGrade.EPIC,0,0,1,0,0,0,TargetType.SELF,"세 번째 다음 자기 턴 시작에 위치와 HP 복원","\"과거는 사라지는 게 아니야. 불러낼 방법을 잊을 뿐이지.\"").effect(wizardPastEcho()).build());
        s.add(skill("spell_theft","주문 탈취",SkillGrade.EPIC,1,0,3,0,0,1,TargetType.ENEMY,"가장 최근의 해제 가능한 이로운 효과 탈취","\"좋은 주문이네. 이제 내 거야.\"").effect(wizardSpellTheft()).build());
        s.add(skill("existence_loan","존재 대여",SkillGrade.EPIC,3,0,2,1,0,0,TargetType.SELF,"비궁극기 스킬의 선택한 부가효과를 추가 적용","\"존재를 하나 더 빌리면, 결과도 하나 더 남겠지.\"").effect(wizardExistenceLoan()).build());

        s.add(skill("infinite_fragment","무한의 편린",SkillGrade.LEGENDARY,50,30,1,0,0,0,TargetType.SELF,"현재 공격력에 비례한 전투 종료까지 공격력 증가","\"무한 전체는 필요 없어. 편린 하나면 충분해.\"").effect(wizardInfiniteFragment()).build());
        s.add(skill("double_cast","이중 영창",SkillGrade.LEGENDARY,1,0,1,1,9,0,TargetType.SELF,"다음 비궁극기 스킬을 스택 수만큼 추가 발동","\"한 번으로 부족하다면 두 번 외우면 될 일이야.\"").effect(wizardDoubleCast()).build());

        s.add(skill("meteor_shower","유성우",SkillGrade.OTHERWORLD,500,0,1,0,0,2,TargetType.TILE,"선택 칸 주변 모든 유닛에게 진실 피해","\"하늘이 무너지는 광경을 보고도 운석 하나하나를 셀 수 있을까?\"").effect(wizardMeteor()).build());
        s.add(skill("otherworld_gate","이계의 문",SkillGrade.OTHERWORLD,100,0,1,0,0,0,TargetType.SELF,"적 턴 종료마다 진실 피해를 주는 영구 설치물","\"문은 열렸고, 저편의 시선이 이곳을 향했다.\"").effect(wizardOtherworldGate()).build());
        skills=Collections.unmodifiableList(s);
    }
    private SkillData.Builder skill(String id,String name,SkillGrade grade,double base,double upgrade,int uses,int useUpgrade,int cooldown,int range,TargetType target,String description,String flavor){return SkillData.builder(id,name,grade).value(base,upgrade).uses(uses,useUpgrade).cooldown(cooldown).range(range).target(target).describe(description).flavor(flavor);}
    @Override public List<SkillData> all(){return skills;}
    @Override public SkillData find(String id){for(SkillData skill:skills)if(skill.getId().equals(id))return skill;return null;}
}
