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
import com.pas.game.skill.repository.EnemySkillRepository;
import com.pas.game.skill.repository.WizardSkillRepository;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import com.pas.game.unit.WizardPhantom;
import org.junit.Test;

public final class WizardImplementationTest {
    private static final RandomProvider ZERO=bound->0;

    @Test public void wizardIsAvailableWithDesignedStatsPassiveAndTwentySkills(){CharacterData wizard=null;for(CharacterData c:new CharacterRepository().all())if("WIZARD".equals(c.getId()))wizard=c;assertNotNull(wizard);assertTrue(wizard.isAvailable());assertEquals(150,wizard.getMaxHp());assertEquals(16,wizard.getAttack());assertEquals(10,wizard.getDefense());assertEquals(9,wizard.getCriticalRate(),.001);assertEquals(6,wizard.getEvasionRate(),.001);assertEquals(PassiveRepository.MANA,wizard.getStartingPassiveId());WizardSkillRepository repo=new WizardSkillRepository();assertEquals(20,repo.all().size());for(com.pas.game.skill.data.SkillData skill:repo.all()){assertTrue(skill.isImplemented());assertFalse(skill.getEffects().isEmpty());}}

    @Test public void levelTwoManaActivatesOnFifthOwnTurnOnly(){Fixture f=fixture("mana_discharge");f.player.equipPassive(new PassiveRepository().create(PassiveRepository.MANA,2,"character:WIZARD"));f.engine.start();assertFalse(f.player.has(StatusType.MANA_ACTIVE));for(int i=0;i<4;i++){f.engine.execute(new EndTurnCommand(f.player.getUnitId()));f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));}assertTrue(f.player.has(StatusType.MANA_ACTIVE));}

    @Test public void magicWardStacksDispellableHeavyArmorAndBarrier(){Fixture f=fixture("magic_ward");f.engine.start();assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"magic_ward",f.player.getUnitId(),1)).isSuccess());f.state.getTurn().addSkillAction();assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"magic_ward",f.player.getUnitId(),1)).isSuccess());assertEquals(40,f.player.getBarrier());assertEquals(2,f.player.sum(StatusType.SKILL_HEAVY_ARMOR),.001);assertTrue(f.player.getStatuses().stream().filter(s->s.getType()==StatusType.SKILL_HEAVY_ARMOR).allMatch(StatusEffect::isDispellable));}

    @Test public void devourNullifiesOnlyDirectDamageAndKeepsBarrier(){Fixture f=fixture("devour");f.player.setHpForDebug(100);f.engine.start();f.player.addBarrier(20);assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"devour",f.player.getUnitId(),1)).isSuccess());assertEquals(0,f.engine.dealDamage(f.enemy,f.player,30,false,DamageType.DIRECT));assertEquals(106,f.player.getHp());assertEquals(20,f.player.getBarrier());assertEquals(10,f.engine.dealDamage(f.enemy,f.player,10,false,DamageType.DAMAGE_OVER_TIME));assertEquals(96,f.player.getHp());}

    @Test public void manaDisruptionTargetsAChosenCooldownSkill(){Fixture f=fixture("mana_disruption");f.enemy.equip(new SkillRuntime(new WizardSkillRepository().find("hungry_star"),0));f.engine.start();assertTrue(f.engine.wizardOptionIds(f.player,f.player.findSkill("mana_disruption"),f.enemy).contains("hungry_star"));assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"mana_disruption",f.enemy.getUnitId(),1,"hungry_star")).isSuccess());assertEquals(2,f.enemy.findSkill("hungry_star").getCooldownRemaining());}

    @Test public void phantomIsTargetableButDoesNotJoinTurnOrderOrVictoryCheck(){Fixture f=fixture("phantom_body");f.engine.start();assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"phantom_body",f.player.getUnitId(),2)).isSuccess());BattleUnit phantom=null;for(BattleUnit unit:f.state.getUnits())if(unit instanceof WizardPhantom)phantom=unit;assertNotNull(phantom);assertFalse(phantom.participatesInTurns());f.engine.dealDamage(f.enemy,phantom,99,true,DamageType.DIRECT);assertNull(f.state.find(phantom.getUnitId()));}

    @Test public void manaPhantomStoresFirstStatusThenItsTickConsumesSecondHit(){Fixture f=fixture("phantom_body");f.engine.start();f.player.addStatus(new StatusEffect("M",StatusType.MANA_ACTIVE,f.player.getUnitId(),-1,0,false,false));assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"phantom_body",f.player.getUnitId(),2)).isSuccess());WizardPhantom phantom=null;for(BattleUnit unit:f.state.getUnits())if(unit instanceof WizardPhantom)phantom=(WizardPhantom)unit;assertNotNull(phantom);f.engine.applyStatus(phantom,new StatusEffect("P",StatusType.POISON,f.enemy.getUnitId(),-1,3,true,true));assertNotNull(f.state.find(phantom.getUnitId()));assertTrue(phantom.has(StatusType.POISON));f.engine.execute(new EndTurnCommand(f.player.getUnitId()));assertNull(f.state.find(phantom.getUnitId()));}

    @Test public void manaDimensionalDriftWaitsForReturnTileChoice(){Fixture f=fixture("dimensional_drift");f.engine.start();f.player.addStatus(new StatusEffect("M",StatusType.MANA_ACTIVE,f.player.getUnitId(),-1,0,false,false));assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"dimensional_drift",f.player.getUnitId(),1)).isSuccess());assertFalse(f.player.isOnField());f.engine.execute(new EndTurnCommand(f.player.getUnitId()));f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));assertTrue(f.engine.hasPendingWizardReturn());assertTrue(f.engine.validWizardReturnTiles().contains(2));assertTrue(f.engine.resolveWizardReturn(2).isSuccess());assertTrue(f.player.isOnField());assertEquals(2,f.player.getTile());}

    @Test public void doubleCastRepeatsNextNonUltimateWithoutExtraUseConsumption(){Fixture f=fixture("mana_discharge");f.engine.start();f.engine.castWizardDoubleCast(f.player);assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"mana_discharge",f.enemy.getUnitId(),1)).isSuccess());assertEquals(32,90-f.enemy.getHp());assertEquals(0,f.state.getWizardState().getDoubleCastStacks(f.player.getUnitId()));}

    @Test public void hungryStarAttacksAtCasterTurnEndAndGateAtEnemyTurnEnd(){Fixture star=fixture("hungry_star");star.engine.start();star.player.setHpForDebug(10);assertTrue(star.engine.execute(new UseSkillCommand(star.player.getUnitId(),"hungry_star",star.player.getUnitId(),1)).isSuccess());star.engine.execute(new EndTurnCommand(star.player.getUnitId()));assertEquals(20,90-star.enemy.getHp());Fixture gate=fixture("otherworld_gate");gate.engine.start();assertTrue(gate.engine.execute(new UseSkillCommand(gate.player.getUnitId(),"otherworld_gate",gate.player.getUnitId(),1)).isSuccess());gate.engine.execute(new EndTurnCommand(gate.player.getUnitId()));gate.engine.execute(new EndTurnCommand(gate.enemy.getUnitId()));assertEquals(16,90-gate.enemy.getHp());}

    @Test public void meteorDealsTrueNonCriticalAreaDamageToFriendAndEnemy(){Fixture f=fixture("meteor_shower");f.engine.start();assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"meteor_shower",f.player.getUnitId(),1)).isSuccess());assertEquals(70,f.player.getHp());assertEquals(10,f.enemy.getHp());}

    private Fixture fixture(String skillId){WizardSkillRepository repo=new WizardSkillRepository();BattleState state=new BattleState();PlayerUnit player=new PlayerUnit("P1_WIZARD","마법사",1,1,RuneLoadout.none(),150,16,10,9,6,0);EnemyUnit enemy=new EnemyUnit("E1","허수아비",1);player.equip(new SkillRuntime(repo.find(skillId),0));state.addUnit(player);state.addUnit(enemy);DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);return new Fixture(state,player,enemy,new BattleEngine(state,ZERO,debug));}
    private static final class Fixture {final BattleState state;final PlayerUnit player;final EnemyUnit enemy;final BattleEngine engine;Fixture(BattleState state,PlayerUnit player,EnemyUnit enemy,BattleEngine engine){this.state=state;this.player=player;this.enemy=enemy;this.engine=engine;}}
}
