package com.pas.game;

import static org.junit.Assert.assertTrue;

import com.pas.game.skill.data.SkillDetailFormatter;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.skill.repository.HeroSkillRepository;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.PlayerUnit;
import org.junit.Test;

public final class SkillDetailFormatterTest {
    private final HeroSkillRepository skills=new HeroSkillRepository();

    @Test public void attackScalingUsesCurrentFinalAttackAndCeiling(){
        PlayerUnit hero=heroWithAttack13();
        assertTrue(SkillDetailFormatter.summary(skills.find("slash"),0,hero).contains("13만큼 피해 (공격력의 100%)"));
        assertTrue(SkillDetailFormatter.summary(skills.find("hero_swordsmanship"),0,hero).contains("65만큼 피해 (공격력의 500%)"));
        assertTrue(SkillDetailFormatter.summary(skills.find("mangle"),0,hero).contains("타격당 6만큼 4회 피해"));
        assertTrue(SkillDetailFormatter.summary(skills.find("mangle"),0,hero).contains("최대 24"));
    }

    @Test public void preBattlePreviewDoesNotApplyBattleStartPassive(){
        PlayerUnit hero=new PlayerUnit("PREVIEW","용사후보",1,1);
        assertTrue(SkillDetailFormatter.summary(skills.find("slash"),0,hero).contains("12만큼 피해 (공격력의 100%)"));
    }

    @Test public void nonDamageSkillsExposeConcreteValuesAndRuntimeRules(){
        PlayerUnit hero=heroWithAttack13();
        assertTrue(SkillDetailFormatter.summary(skills.find("certain_strike"),0,hero).contains("치명타율 +20%p"));
        assertTrue(SkillDetailFormatter.summary(skills.find("salvation_vow"),0,hero).contains("18만큼 피해 감소"));
        SkillRuntime aura=new SkillRuntime(skills.find("guardian_aura"),0);aura.consume();
        String detail=SkillDetailFormatter.detail(aura,hero,hero);
        assertTrue(detail.contains("사용 횟수: 2 / 3"));
        assertTrue(detail.contains("현재 쿨타임: 6턴"));
        assertTrue(detail.contains("기본 쿨타임: 6턴"));
    }

    @Test public void recklessChargeUsesNaturalKoreanEffectText(){
        String detail=SkillDetailFormatter.detail(skills.find("reckless_charge"),0,new PlayerUnit("P1","용사후보",1,1),null);
        assertTrue(detail.contains("턴 종료 시 최대 HP의 10%만큼 피해"));
        assertTrue(!detail.contains("RECOIL_AT_TURN_END"));
        assertTrue(!detail.contains("고정 피해"));
    }

    private PlayerUnit heroWithAttack13(){PlayerUnit hero=new PlayerUnit("P1","용사후보",1,1);hero.addStatus(new StatusEffect("attack",StatusType.ATTACK_FLAT_UP,"test",-1,1,false,false));return hero;}
}
