package com.pas.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pas.game.battle.damage.DamageType;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.state.BattleState;
import com.pas.game.debug.DebugOptions;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.passive.PassiveRepository;
import com.pas.game.rune.RuneData;
import com.pas.game.rune.RuneRepository;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import org.junit.Test;

public class RunePassiveEngineTest {
    private static final RandomProvider ZERO=bound->0;
    private final PassiveRepository passives=new PassiveRepository();
    private final RuneRepository runes=new RuneRepository();

    @Test public void arsonAppliesFireToEveryEnemyAtBattleStart(){
        Fixture f=fixture("fire",1,2);f.engine.start();
        for(EnemyUnit enemy:f.enemies){assertTrue(enemy.has(StatusType.FIRE));assertEquals(5,enemy.sum(StatusType.FIRE),0.001);}
    }

    @Test public void willExpiresAfterFiveOwnerTurnEndsNotEnemyTurnEnds(){
        Fixture f=fixture("guardian",1,1);f.engine.start();
        assertTrue(f.player.has(StatusType.UNSTOPPABLE));
        StatusEffect effect=findStatus(f.player,StatusType.UNSTOPPABLE);
        assertTrue(effect!=null&&!effect.isPermanent()&&!effect.isDispellable());
        assertEquals(5,effect.getRemainingTurns());
        for(int remaining=4;remaining>=0;remaining--){
            f.engine.advanceTurn();
            assertEquals(remaining,effect.getRemainingTurns());
            assertEquals(remaining>0,f.player.has(StatusType.UNSTOPPABLE));
            f.engine.advanceTurn();
            assertEquals(remaining,effect.getRemainingTurns());
        }
    }

    @Test public void frostGrantsNamedEffectAndFightingGrantsOrdinaryAttackBuff(){
        Fixture frost=fixture("frost",1,1);frost.engine.start();
        assertEquals(2,frost.player.sumEffect(UnitEffectType.HEAVY_ARMOR),0.001);
        Fixture fighting=fixture("fighting",1,1);fighting.engine.start();
        assertEquals(1,fighting.player.sum(StatusType.ATTACK_FLAT_UP),0.001);
        assertEquals(13,fighting.player.getAttack());
        assertTrue(String.join("\n",fighting.engine.getState().getLogs()).contains("투쟁심 1 효과로 공격력 증가 1을 얻었습니다"));
    }

    @Test public void venomAppliesPoisonForEveryDirectHit(){
        Fixture f=fixture("venom",1,1);f.engine.start();EnemyUnit enemy=f.enemies[0];
        f.engine.dealDamage(f.player,enemy,5,false,DamageType.DIRECT);
        f.engine.dealDamage(f.player,enemy,5,false,DamageType.DIRECT);
        assertEquals(2,enemy.sum(StatusType.POISON),0.001);
        f.engine.dealDamage(f.player,enemy,5,false,DamageType.DAMAGE_OVER_TIME);
        assertEquals(2,enemy.sum(StatusType.POISON),0.001);
    }

    @Test public void lifestealUsesFinalDamageAndRoundsHealingUp(){
        Fixture f=fixture("vampire",1,1);f.player.setHpForDebug(100);f.engine.start();
        assertEquals(1,f.engine.dealDamage(f.player,f.enemies[0],1,false,DamageType.DIRECT));
        assertEquals(101,f.player.getHp());
        f.engine.dealDamage(f.player,f.enemies[0],10,false,DamageType.DAMAGE_OVER_TIME);
        assertEquals(101,f.player.getHp());
    }

    @Test public void survivalHealsEveryThirdOwnerTurn(){
        Fixture f=fixture("must_live",1,1);f.player.setHpForDebug(100);f.engine.start();
        f.engine.advanceTurn();f.engine.advanceTurn();
        assertEquals(100,f.player.getHp());
        f.engine.advanceTurn();f.engine.advanceTurn();
        assertEquals(103,f.player.getHp());
    }

    @Test public void sortieHealsAtBattleStart(){
        Fixture f=fixture("charge",1,1);f.player.setHpForDebug(100);f.engine.start();
        assertEquals(110,f.player.getHp());
    }

    @Test public void fractionalFireDamageRoundsUpWhenAppliedToHp(){
        Fixture f=fixture(null,1,1);f.player.setHpForDebug(100);
        assertEquals(5,f.engine.dealDamage(f.enemies[0],f.player,4.01,false,DamageType.DAMAGE_OVER_TIME));
        assertEquals(95,f.player.getHp());
    }

    @Test public void correctedHeavyArmorTableUsesSevenAtLevelThree(){
        Fixture f=fixture(null,1,1);f.player.equipPassive(passives.create(PassiveRepository.HEAVY_ARMOR,3,"character"));f.engine.start();
        assertEquals(7,f.player.sumEffect(UnitEffectType.HEAVY_ARMOR),0.001);
    }

    @Test public void criticalPowerKillingIntentAndDeterminationUseSharedEffects(){
        Fixture critical=fixture(null,1,1);critical.player.equipPassive(passives.create(PassiveRepository.CRITICAL_POWER,1,"test"));critical.engine.getDebug().setCritical(DebugOptions.ForcedRoll.SUCCESS);critical.engine.start();
        assertEquals(16,critical.engine.dealDamage(critical.player,critical.enemies[0],10,true,DamageType.DIRECT));
        Fixture stats=fixture(null,1,1);stats.player.equipPassive(passives.create(PassiveRepository.KILLING_INTENT,1,"test"));stats.player.equipPassive(passives.create(PassiveRepository.DETERMINATION,1,"test"));stats.engine.start();
        assertEquals(13,stats.player.getCriticalRate(),0.001);assertEquals(16,stats.player.getDefense());
    }

    private Fixture fixture(String runeId,int runeLevel,int enemyCount){
        BattleState state=new BattleState();PlayerUnit player=new PlayerUnit("P1","용사",1,1);
        if(runeId!=null){RuneData rune=runes.primary(runeId);player.equipPassive(passives.create(rune.getPassiveId(),rune.passiveLevelForRuneLevel(runeLevel),"rune:"+runeId));}
        state.addUnit(player);EnemyUnit[] enemies=new EnemyUnit[enemyCount];for(int i=0;i<enemyCount;i++){enemies[i]=new EnemyUnit("E"+(i+1),"적"+(i+1),8+i);state.addUnit(enemies[i]);}
        DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);
        return new Fixture(player,enemies,new BattleEngine(state,ZERO,debug));
    }

    private StatusEffect findStatus(PlayerUnit player,StatusType type){for(StatusEffect effect:player.getStatuses())if(effect.getType()==type)return effect;return null;}
    private static final class Fixture {final PlayerUnit player;final EnemyUnit[] enemies;final BattleEngine engine;Fixture(PlayerUnit player,EnemyUnit[] enemies,BattleEngine engine){this.player=player;this.enemies=enemies;this.engine=engine;}}
}
