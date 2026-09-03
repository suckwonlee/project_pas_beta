package com.pas.game;

import static org.junit.Assert.assertEquals;

import com.pas.game.battle.damage.DamageType;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.state.BattleState;
import com.pas.game.debug.DebugOptions;
import com.pas.game.effect.UnitEffect;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.passive.PassiveRepository;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import org.junit.Test;

public class PassiveEffectEngineTest {
    private static final RandomProvider ZERO=bound->0;

    @Test public void heavyArmorPassiveGrantsNamedEffectAtBattleStart(){
        Fixture f=fixture(true,false);
        f.engine.start();
        assertEquals(2,f.player.sumEffect(UnitEffectType.HEAVY_ARMOR),0.001);
        assertEquals(1,f.player.getEffects().size());
        org.junit.Assert.assertTrue(String.join("\n",f.engine.getState().getLogs()).contains("중갑 1 효과로 중갑 2를 얻었습니다"));
    }

    @Test public void heavyArmorReducesOnlyDirectDamage(){
        Fixture f=fixture(true,false);
        f.engine.start();
        assertEquals(8,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.DIRECT));
        assertEquals(10,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.DAMAGE_OVER_TIME));
        assertEquals(10,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.TRUE_DAMAGE));
    }

    @Test public void differentSourcesStackAndSameSourceRefreshes(){
        Fixture f=fixture(false,false);
        f.player.grantEffect(new UnitEffect(UnitEffectType.HEAVY_ARMOR,"character",2));
        f.player.grantEffect(new UnitEffect(UnitEffectType.HEAVY_ARMOR,"rune",4));
        f.player.grantEffect(new UnitEffect(UnitEffectType.HEAVY_ARMOR,"character",3));
        assertEquals(7,f.player.sumEffect(UnitEffectType.HEAVY_ARMOR),0.001);
        assertEquals(2,f.player.getEffects().size());
        assertEquals(3,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.DIRECT));
    }

    @Test public void monsterUsesTheSamePassiveAndEffectPipeline(){
        Fixture f=fixture(false,true);
        f.engine.start();
        assertEquals(2,f.enemy.sumEffect(UnitEffectType.HEAVY_ARMOR),0.001);
        assertEquals(8,f.engine.dealDamage(f.player,f.enemy,10,false,DamageType.DIRECT));
    }

    @Test public void passiveDescriptionsInsertEachLevelValue(){
        PassiveRepository passives=new PassiveRepository();
        assertEquals("받는 일반 피해를 2만큼 감소시킵니다.",passives.find(PassiveRepository.HEAVY_ARMOR).describeAt(1));
        assertEquals("가한 즉발형 최종 피해의 3%만큼 HP를 회복합니다.",passives.find(PassiveRepository.RUNE_VAMPIRE).describeAt(1));
        assertEquals("가한 즉발형 최종 피해의 20%만큼 HP를 회복합니다.",passives.find(PassiveRepository.RUNE_VAMPIRE).describeAt(5));
        assertEquals("전투 시작 시 공격력 7 증가 버프를 얻습니다.",passives.find(PassiveRepository.RUNE_FIGHTING).describeAt(4));
        assertEquals("치명타 피해가 10% 증가합니다. (치명타 배율 160%)",passives.find(PassiveRepository.CRITICAL_POWER).describeAt(1));
        assertEquals("치명타 피해가 60% 증가합니다. (치명타 배율 210%)",passives.find(PassiveRepository.CRITICAL_POWER).describeAt(5));
    }

    private Fixture fixture(boolean playerPassive,boolean enemyPassive){
        BattleState state=new BattleState();
        PlayerUnit player=new PlayerUnit("P1","용사후보",1,1);
        EnemyUnit enemy=new EnemyUnit("E1","허수아비",12);
        PassiveRepository passives=new PassiveRepository();
        if(playerPassive)player.equipPassive(passives.create(PassiveRepository.HEAVY_ARMOR,1,"character:HERO"));
        if(enemyPassive)enemy.equipPassive(passives.create(PassiveRepository.HEAVY_ARMOR,1,"monster:E1"));
        state.addUnit(player);state.addUnit(enemy);
        DebugOptions debug=new DebugOptions();
        debug.setCritical(DebugOptions.ForcedRoll.FAILURE);
        debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);
        return new Fixture(player,enemy,new BattleEngine(state,ZERO,debug));
    }

    private static final class Fixture {
        final PlayerUnit player;final EnemyUnit enemy;final BattleEngine engine;
        Fixture(PlayerUnit player,EnemyUnit enemy,BattleEngine engine){this.player=player;this.enemy=enemy;this.engine=engine;}
    }
}
