package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.damage.DamageType;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.state.BattleState;
import com.pas.game.character.CharacterData;
import com.pas.game.character.CharacterRepository;
import com.pas.game.debug.DebugOptions;
import com.pas.game.passive.PassiveRepository;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.skill.repository.ClericSkillRepository;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import org.junit.Test;

public final class ClericBattleEngineTest {
    private static final RandomProvider ZERO=bound->0;

    @Test public void clericIsAvailableWithTwentySkillsAndLevelTwoPurification(){
        CharacterData cleric=null;for(CharacterData candidate:new CharacterRepository().all())if("CLERIC".equals(candidate.getId()))cleric=candidate;
        assertNotNull(cleric);assertTrue(cleric.isAvailable());assertEquals(160,cleric.getMaxHp());assertEquals(11,cleric.getAttack());assertEquals(18,cleric.getDefense());assertEquals(3,cleric.getCriticalRate(),0.001);assertEquals(4,cleric.getEvasionRate(),0.001);assertEquals(PassiveRepository.PURIFICATION,cleric.getStartingPassiveId());assertEquals(2,cleric.getStartingPassiveLevel());
        ClericSkillRepository skills=new ClericSkillRepository();assertEquals(20,skills.all().size());for(com.pas.game.skill.data.SkillData skill:skills.all()){assertTrue(skill.isImplemented());assertFalse(skill.getEffects().isEmpty());}
    }

    @Test public void purificationGainsEightAtEachClericTurnStart(){
        Fixture f=fixture();f.engine.start();assertEquals(8,f.player.sum(StatusType.PURIFICATION_STACK),0.001);
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));
        assertEquals(16,f.player.sum(StatusType.PURIFICATION_STACK),0.001);
    }

    @Test public void damagingAttackTransfersAllPurificationThenExecutesAtTargetTurnEnd(){
        Fixture f=fixture();f.player.equip(new SkillRuntime(new ClericSkillRepository().find("banish_profane"),0));f.enemy.setHpForDebug(20);f.engine.start();f.engine.gainPurification(f.player,5);
        assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"banish_profane",f.enemy.getUnitId(),1)).isSuccess());
        assertEquals(9,f.enemy.getHp());assertEquals(0,f.player.sum(StatusType.PURIFICATION_STACK),0.001);assertTrue(f.enemy.has(StatusType.PURIFICATION_EXECUTION));
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));assertFalse(f.enemy.isDead());
        f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));assertTrue(f.enemy.isDead());
    }

    @Test public void divinePunishmentImmediatelyAndContinuouslyReducesCurrentAndMaxHp(){
        Fixture f=fixture();f.engine.start();f.engine.castDivinePunishment(f.player,f.enemy);
        assertEquals(82,f.enemy.getHp());assertEquals(82,f.enemy.getMaxHp());assertEquals(0,f.player.sum(StatusType.PURIFICATION_STACK),0.001);
        f.engine.gainPurification(f.player,5);assertEquals(77,f.enemy.getHp());assertEquals(77,f.enemy.getMaxHp());assertEquals(0,f.player.sum(StatusType.PURIFICATION_STACK),0.001);
        f.enemy.kill();f.engine.gainPurification(f.player,7);assertEquals(7,f.player.sum(StatusType.PURIFICATION_STACK),0.001);
    }

    @Test public void healingPrayerUsesTwentySevenPercentAndExpiresAtNextOwnTurnStart(){
        Fixture f=fixture();f.player.equip(new SkillRuntime(new ClericSkillRepository().find("healing_prayer"),0));f.player.setHpForDebug(100);f.engine.start();
        assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"healing_prayer",f.player.getUnitId(),1)).isSuccess());
        assertEquals(27,f.player.sum(StatusType.DAMAGE_REDUCTION),0.001);assertEquals(112,f.player.getHp());
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));assertEquals(0,f.player.sum(StatusType.DAMAGE_REDUCTION),0.001);
    }

    @Test public void blessingsStackAndTemporaryMaxHpRestoresWithHpClamp(){
        Fixture f=fixture();ClericSkillRepository repo=new ClericSkillRepository();double value=repo.find("blessing_of_resolve").valueAt(0);
        repo.find("blessing_of_resolve").getEffects().get(0).apply(f.engine,f.player,f.player,value);repo.find("blessing_of_resolve").getEffects().get(0).apply(f.engine,f.player,f.player,value);
        assertEquals(37,f.player.getDefense());
        f.engine.applyMaxHpBuff(f.player,f.player,32,1,"TEST_MAX",true);assertEquals(192,f.player.getMaxHp());assertEquals(192,f.player.getHp());
        f.player.removeStatuses(f.player.tickStatuses());assertEquals(160,f.player.getMaxHp());assertEquals(160,f.player.getHp());
    }

    @Test public void lifeBlessingStacksAndLastsUntilBattleEnd(){
        Fixture f=fixture();com.pas.game.skill.data.SkillData skill=new ClericSkillRepository().find("blessing_of_life");
        skill.getEffects().get(0).apply(f.engine,f.player,f.player,skill.valueAt(0));skill.getEffects().get(0).apply(f.engine,f.player,f.player,skill.valueAt(0));
        assertEquals(231,f.player.getMaxHp());assertEquals(231,f.player.getHp());assertTrue(f.player.getStatuses().stream().filter(status->status.getType()==StatusType.MAX_HP_UP).allMatch(StatusEffect::isPermanent));
        f.player.removeStatuses(f.player.tickStatuses());assertEquals(231,f.player.getMaxHp());
    }

    @Test public void directInvincibilityDoesNotBlockDamageOverTime(){
        Fixture f=fixture();f.player.addStatus(new StatusEffect("INV",StatusType.INVINCIBLE_DIRECT,f.player.getUnitId(),-1,0,false,true));
        assertEquals(0,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.DIRECT));assertEquals(10,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.DAMAGE_OVER_TIME));
    }

    @Test public void sanctuaryHealsAndCleansesAtFriendlyTurnEndAndTicksOncePerPlayerRound(){
        Fixture f=fixture();f.engine.start();f.player.setHpForDebug(100);f.player.addStatus(new StatusEffect("POISON",StatusType.POISON,f.enemy.getUnitId(),-1,4,true,true));f.engine.installSanctuary(f.player,100,5);
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));assertEquals(160,f.player.getHp());assertFalse(f.player.has(StatusType.POISON));assertEquals(5,f.state.getSanctuaries().get(0).getRemainingPlayerTurns());
        f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));assertEquals(4,f.state.getSanctuaries().get(0).getRemainingPlayerTurns());
    }

    @Test public void multiplayerDivineFragmentHealsOnlyAlliesStartingInsideRange(){
        BattleState state=new BattleState();state.setMultiplayer(true);PlayerUnit caster=player("C",1,1);PlayerUnit ally=player("A",2,2);EnemyUnit enemy=new EnemyUnit("E","적",12);state.addUnit(caster);state.addUnit(ally);state.addUnit(enemy);BattleEngine engine=engine(state);engine.start();ally.setHpForDebug(10);engine.activateDivineFragment(caster,5);
        engine.execute(new EndTurnCommand(caster.getUnitId()));assertEquals(160,ally.getHp());
        ally.setHpForDebug(10);engine.execute(new EndTurnCommand(ally.getUnitId()));engine.execute(new EndTurnCommand(enemy.getUnitId()));engine.forceMove(ally.getUnitId(),12);engine.execute(new EndTurnCommand(caster.getUnitId()));assertEquals(10,ally.getHp());
    }

    private Fixture fixture(){BattleState state=new BattleState();PlayerUnit player=player("P",1,1);EnemyUnit enemy=new EnemyUnit("E","적",1);state.addUnit(player);state.addUnit(enemy);return new Fixture(state,player,enemy,engine(state));}
    private PlayerUnit player(String id,int tile,int slot){PlayerUnit player=new PlayerUnit(id,"성직자",tile,slot,RuneLoadout.none(),160,11,18,3,4,0);player.equipPassive(new PassiveRepository().create(PassiveRepository.PURIFICATION,2,"character:CLERIC"));return player;}
    private BattleEngine engine(BattleState state){DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);return new BattleEngine(state,ZERO,debug);}
    private static final class Fixture {final BattleState state;final PlayerUnit player;final EnemyUnit enemy;final BattleEngine engine;Fixture(BattleState state,PlayerUnit player,EnemyUnit enemy,BattleEngine engine){this.state=state;this.player=player;this.enemy=enemy;this.engine=engine;}}
}
