package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.ai.TrainingDummyAI;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.command.UsePotionCommand;
import com.pas.game.battle.damage.DamageType;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.state.BattleOutcome;
import com.pas.game.battle.state.BattleState;
import com.pas.game.battle.turn.MovementType;
import com.pas.game.debug.DebugOptions;
import com.pas.game.effect.UnitEffect;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.map.BattleGrid;
import com.pas.game.item.potion.PotionInventory;
import com.pas.game.item.potion.PotionRepository;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.repository.EnemySkillRepository;
import com.pas.game.skill.repository.HeroSkillRepository;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import org.junit.Test;

public class BattleEngineTest {
    private static final RandomProvider ZERO=bound->0;

    @Test public void gridUsesManhattanDistance(){assertEquals(1,BattleGrid.distance(1,2));assertEquals(1,BattleGrid.distance(1,5));assertEquals(2,BattleGrid.distance(1,6));}

    @Test public void secondMoveConsumesSkillOpportunity(){
        Fixture f=fixture(1,12);f.engine.start();
        assertTrue(f.engine.execute(new MoveCommand(f.player.getUnitId(),5,MovementType.VOLUNTARY)).isSuccess());
        assertTrue(f.engine.execute(new MoveCommand(f.player.getUnitId(),6,MovementType.VOLUNTARY)).isSuccess());
        assertEquals(0,f.state.getTurn().getMovesRemaining());assertEquals(0,f.state.getTurn().getSkillsRemaining());
    }

    @Test public void unusedBarrierExpiresAtOwnersNextTurnStart(){
        Fixture f=fixture(1,12);f.engine.start();f.player.addBarrier(35);
        assertEquals(35,f.player.getBarrier());
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));
        assertEquals(35,f.player.getBarrier());
        f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));
        assertEquals(0,f.player.getBarrier());
        assertTrue(f.state.getLogs().get(f.state.getLogs().size()-2).contains("피해방어 35 소멸")||f.state.getLogs().get(f.state.getLogs().size()-1).contains("피해방어 35 소멸"));
    }

    @Test public void criticalSkipsForcedSuccessfulEvasion(){
        Fixture f=fixture(1,2);f.debug.setCritical(DebugOptions.ForcedRoll.SUCCESS);f.debug.setEvasion(DebugOptions.ForcedRoll.SUCCESS);
        int damage=f.engine.dealDamage(f.player,f.enemy,10,true,false);
        assertEquals(15,damage);
        assertFalse(f.state.getLogs().stream().anyMatch(line->line.contains("치명타 판정")||line.contains("회피 판정")));
    }

    @Test public void coefficientDamageIsRoundedUpBeforeCriticalMultiplier(){
        BattleState state=new BattleState();PlayerUnit player=new PlayerUnit("P1","용사",1,1);EnemyUnit enemy=new EnemyUnit("E1","허수아비",1);
        player.equip(new SkillRuntime(new HeroSkillRepository().find("shield_art"),0));state.addUnit(player);state.addUnit(enemy);
        DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.SUCCESS);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);BattleEngine engine=new BattleEngine(state,ZERO,debug);engine.start();
        int roundedBase=(int)Math.ceil(player.getDefense()*0.95);
        assertEquals(14,roundedBase);
        assertTrue(engine.execute(new UseSkillCommand(player.getUnitId(),"shield_art",enemy.getUnitId(),1)).isSuccess());
        assertEquals(21,enemy.getMaxHp()-enemy.getHp());
    }

    @Test public void guardianAuraStacksAsFlatReductionAndFollowsRange(){
        BattleState state=new BattleState();PlayerUnit caster=new PlayerUnit("P1","수호자",1,1);PlayerUnit ally=new PlayerUnit("P2","아군",2,2);EnemyUnit enemy=new EnemyUnit("E1","적",12);
        state.addUnit(caster);state.addUnit(ally);state.addUnit(enemy);DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);BattleEngine engine=new BattleEngine(state,ZERO,debug);
        com.pas.game.skill.data.SkillData aura=new HeroSkillRepository().find("guardian_aura");double value=aura.valueAt(0);int each=(int)Math.ceil(caster.getDefense()*value/100.0);
        aura.getEffects().get(0).apply(engine,caster,caster,value);aura.getEffects().get(0).apply(engine,caster,caster,value);
        assertEquals(each*2,caster.sum(StatusType.GUARDIAN_AURA),0.001);
        assertEquals(20-each*2,engine.dealDamage(enemy,ally,20,false,DamageType.DIRECT));
        engine.forceMove(ally.getUnitId(),3);assertEquals(20,engine.dealDamage(enemy,ally,20,false,DamageType.DIRECT));
        engine.forceMove(ally.getUnitId(),2);assertEquals(20-each*2,engine.dealDamage(enemy,ally,20,false,DamageType.DIRECT));
        for(String log:state.getLogs())assertFalse(log.contains("TileEnterEvent"));
    }

    @Test public void reductionsAreAdditiveAndClamped(){
        Fixture f=fixture(1,2);f.enemy.addStatus(status(StatusType.DAMAGE_REDUCTION,"A",50));f.enemy.addStatus(status(StatusType.DAMAGE_REDUCTION,"B",30));
        assertEquals(2,f.engine.dealDamage(f.player,f.enemy,10,false,false));
        f.enemy.addStatus(status(StatusType.DAMAGE_REDUCTION,"C",40));assertEquals(0,f.engine.dealDamage(f.player,f.enemy,10,false,false));
    }

    @Test public void percentModifiersAndFlatReductionUseDifferentStages(){
        Fixture f=fixture(1,2);f.enemy.addStatus(status(StatusType.DAMAGE_REDUCTION,"PERCENT",20));f.enemy.addStatus(status(StatusType.DAMAGE_TAKEN_UP,"TAKEN",50));f.enemy.addStatus(status(StatusType.FLAT_DAMAGE_REDUCTION,"FLAT",3));
        assertEquals(9,f.engine.dealDamage(f.player,f.enemy,10,false,false));
    }

    @Test public void percentageReductionFromAnySourceReducesDamageOverTimeAndSpecialDamage(){
        Fixture f=fixture(1,2);
        f.enemy.addStatus(status(StatusType.DAMAGE_REDUCTION,"DEFENSE_SKILL",10));
        f.enemy.addStatus(status(StatusType.DAMAGE_REDUCTION,"PASSIVE_OR_OTHER_SOURCE",15));
        assertEquals(8,f.engine.dealDamage(f.player,f.enemy,10,false,DamageType.DAMAGE_OVER_TIME));
        assertEquals(8,f.engine.dealDamage(f.player,f.enemy,10,false,DamageType.TRUE_DAMAGE));
    }

    @Test public void barrierOnlyBlocksDirectDamage(){
        Fixture f=fixture(1,2);f.enemy.addBarrier(20);
        assertEquals(10,f.engine.dealDamage(f.player,f.enemy,10,false,DamageType.DAMAGE_OVER_TIME));
        assertEquals(20,f.enemy.getBarrier());
        assertEquals(10,f.engine.dealDamage(f.player,f.enemy,10,false,DamageType.TRUE_DAMAGE));
        assertEquals(20,f.enemy.getBarrier());
        assertEquals(0,f.engine.dealDamage(f.player,f.enemy,10,false,DamageType.DIRECT));
        assertEquals(0,f.enemy.getBarrier());
    }

    @Test public void battleCryRaisesCurrentFinalAttackBySixtyPercent(){
        BattleState state=new BattleState();PlayerUnit player=new PlayerUnit("P1","용사",1,1);EnemyUnit enemy=new EnemyUnit("E1","허수아비",1);
        player.addStatus(new StatusEffect("attack",StatusType.ATTACK_FLAT_UP,"test",-1,3,false,false));player.equip(new SkillRuntime(new HeroSkillRepository().find("battle_cry"),0));state.addUnit(player);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,new DebugOptions());engine.start();
        assertEquals(15,player.getAttack());assertTrue(engine.execute(new UseSkillCommand(player.getUnitId(),"battle_cry",player.getUnitId(),1)).isSuccess());
        assertEquals(60,player.sum(StatusType.ATTACK_UP),0.001);assertEquals(24,player.getAttack());
    }

    @Test public void deadUnitImmediatelyProducesVictory(){
        Fixture f=fixture(1,1);f.enemy.setHpForDebug(1);f.engine.start();
        assertTrue(f.engine.execute(new UseSkillCommand(f.player.getUnitId(),"slash",f.enemy.getUnitId(),1)).isSuccess());
        assertTrue(f.enemy.isDead());assertEquals(BattleOutcome.VICTORY,f.state.getOutcome());
    }

    @Test public void dummyMovesOneTileAndAttacksWhenSharingTile(){
        Fixture f=fixture(1,1);f.engine.start();f.engine.execute(new EndTurnCommand(f.player.getUnitId()));int hp=f.player.getHp();
        new TrainingDummyAI(ZERO).takeTurn(f.engine);assertTrue(f.player.getHp()<hp);
    }

    @Test public void repositoryRegistersAllCurrentSkillsAndUltimateDependsOnStartingGrade(){
        HeroSkillRepository repo=new HeroSkillRepository();assertEquals(20,repo.all().size());assertTrue(repo.find("salvation_vow").isImplemented());assertTrue(repo.find("hero_swordsmanship").isUltimate());assertFalse(repo.find("slash").isUltimate());SkillRuntime upgradedEpic=new SkillRuntime(repo.find("mangle"),1);assertEquals(com.pas.game.skill.data.SkillGrade.LEGENDARY,upgradedEpic.getCurrentGrade());assertFalse(upgradedEpic.getData().isUltimate());
    }

    @Test public void fixedSkillSlotsKeepUltimateAtBottomRight(){
        PlayerUnit player=new PlayerUnit("P1","용사",1,1);HeroSkillRepository repo=new HeroSkillRepository();player.replaceSkill(5,new SkillRuntime(repo.find("hero_swordsmanship"),0));
        assertEquals(6,player.getEquippedSkills().size());assertNull(player.getEquippedSkills().get(2));assertEquals("hero_swordsmanship",player.getEquippedSkills().get(5).getData().getId());
    }

    @Test public void limitedSkillExposesRemainingMaximumAndCooldownTurns(){
        SkillRuntime skill=new SkillRuntime(new HeroSkillRepository().find("guardian_aura"),0);assertEquals(3,skill.getRemainingUses());assertEquals(3,skill.getMaxUses());assertEquals(0,skill.getCooldownRemaining());skill.consume();assertEquals(2,skill.getRemainingUses());assertEquals(6,skill.getCooldownRemaining());skill.onOwnerTurnStart();assertEquals(5,skill.getCooldownRemaining());
    }

    @Test public void potionHealsConsumesOneItemAndOneAction(){
        Fixture f=fixture(1,12);PotionInventory inventory=new PotionInventory(new PotionRepository().forChapter(1),3);f.state.setPotionInventory(inventory);f.engine.start();f.player.setHpForDebug(100);
        assertTrue(f.engine.execute(new UsePotionCommand(f.player.getUnitId())).isSuccess());assertEquals(150,f.player.getHp());assertEquals(2,inventory.getCount());assertEquals(0,f.state.getTurn().getSkillsRemaining());
    }

    @Test public void potionAtFullHpDoesNotConsumeItemOrAction(){
        Fixture f=fixture(1,12);PotionInventory inventory=new PotionInventory(new PotionRepository().forChapter(1),3);f.state.setPotionInventory(inventory);f.engine.start();
        assertFalse(f.engine.execute(new UsePotionCommand(f.player.getUnitId())).isSuccess());assertEquals(3,inventory.getCount());assertEquals(1,f.state.getTurn().getSkillsRemaining());
    }

    @Test public void selectedPotionTypeControlsHealingAndOnlyItsCount(){
        Fixture f=fixture(1,12);PotionRepository repository=new PotionRepository();PotionInventory inventory=new PotionInventory(repository.forChapter(1),1);inventory.add(repository.forChapter(2),1);f.state.setPotionInventory(inventory);f.engine.start();f.player.setHpForDebug(50);
        assertTrue(f.engine.execute(new UsePotionCommand(f.player.getUnitId(),2)).isSuccess());assertEquals(130,f.player.getHp());assertEquals(1,inventory.getCount(repository.forChapter(1)));assertEquals(0,inventory.getCount(repository.forChapter(2)));
    }

    @Test public void baseCriticalDamageIsOnePointFiveAndBaseSkillsAreNormal(){
        PlayerUnit player=new PlayerUnit("P","용사후보",1,1);HeroSkillRepository repository=new HeroSkillRepository();
        assertEquals(1.5,player.getCriticalDamageMultiplier(),0.001);assertEquals(SkillGrade.NORMAL,repository.find("slash").getStartingGrade());assertEquals(SkillGrade.NORMAL,repository.find("defend").getStartingGrade());assertEquals(SkillGrade.UNCOMMON,repository.find("head_bash").getStartingGrade());
    }

    @Test public void fireAndPoisonResolveAtOwnerTurnEndAsStacks(){
        Fixture f=fixture(1,12);f.engine.start();
        StatusEffect fire=new StatusEffect("FIRE",StatusType.FIRE,f.enemy.getUnitId(),-1,10,true,true);
        StatusEffect poison=new StatusEffect("POISON",StatusType.POISON,f.enemy.getUnitId(),-1,5,true,true);
        f.player.addStatus(fire);f.player.addStatus(poison);
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));
        assertEquals(165,f.player.getHp());assertEquals(10,fire.getMagnitude(),0.001);assertEquals(4,poison.getMagnitude(),0.001);
        f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));f.engine.execute(new EndTurnCommand(f.player.getUnitId()));
        assertEquals(151,f.player.getHp());assertEquals(9,fire.getMagnitude(),0.001);assertEquals(3,poison.getMagnitude(),0.001);
        assertTrue(f.state.getLogs().stream().anyMatch(line->line.contains("화염 피해 10을 가했습니다.")));
        assertTrue(f.state.getLogs().stream().anyMatch(line->line.contains("중독 피해 5를 가했습니다.")));
        assertFalse(f.state.getLogs().stream().anyMatch(line->line.contains("기본 피해")||line.contains("최종 피해")));
        assertTrue(f.state.getLogs().contains(""));
    }

    @Test public void dazedBlocksThenDecrementsAtAffectedTurnEnd(){
        Fixture f=fixture(1,12);f.enemy.addStatus(new StatusEffect("D",StatusType.DAZED,f.player.getUnitId(),1,0,false,true));f.engine.start();
        f.engine.execute(new EndTurnCommand(f.player.getUnitId()));assertTrue(f.state.getTurn().isBlocked());assertTrue(f.enemy.has(StatusType.DAZED));
        f.engine.execute(new EndTurnCommand(f.enemy.getUnitId()));assertFalse(f.enemy.has(StatusType.DAZED));
    }

    @Test public void salvationVowGrantsMaxHpBasedFlatReductionButNotAgainstFixedDamage(){
        BattleState state=new BattleState();PlayerUnit p=new PlayerUnit("P1","용사",1,1);EnemyUnit e=new EnemyUnit("E1","허수아비",1);
        p.equip(new SkillRuntime(new HeroSkillRepository().find("salvation_vow"),0));e.equip(new SkillRuntime(EnemySkillRepository.basicAttack(),0));state.addUnit(p);state.addUnit(e);
        DebugOptions d=new DebugOptions();d.setCritical(DebugOptions.ForcedRoll.FAILURE);d.setEvasion(DebugOptions.ForcedRoll.FAILURE);BattleEngine engine=new BattleEngine(state,ZERO,d);engine.start();
        engine.execute(new UseSkillCommand(p.getUnitId(),"salvation_vow",p.getUnitId(),1));
        assertEquals(2,engine.dealDamage(e,p,20,false,false));assertEquals(20,engine.dealDamage(e,p,20,false,true));
    }

    @Test public void multiplayerDefenseFocusDisablesMovementAndEvasion(){
        BattleState state=new BattleState();state.setMultiplayer(true);PlayerUnit p=new PlayerUnit("P1","용사",1,1);EnemyUnit e=new EnemyUnit("E1","허수아비",12);
        p.equip(new SkillRuntime(new HeroSkillRepository().find("defense_focus"),0));state.addUnit(p);state.addUnit(e);BattleEngine engine=new BattleEngine(state,ZERO,new DebugOptions());engine.start();
        engine.execute(new UseSkillCommand(p.getUnitId(),"defense_focus",p.getUnitId(),1));
        assertTrue(p.has(StatusType.MOVEMENT_DISABLED));assertEquals(0,p.getEvasionRate(),0.001);assertFalse(engine.execute(new MoveCommand(p.getUnitId(),5,MovementType.VOLUNTARY)).isSuccess());
    }

    @Test public void mangleHitsEveryEnemyFourTimes(){
        BattleState state=new BattleState();PlayerUnit p=new PlayerUnit("P1","용사",1,1);EnemyUnit e1=new EnemyUnit("E1","허수아비1",12);EnemyUnit e2=new EnemyUnit("E2","허수아비2",8);
        p.equip(new SkillRuntime(new HeroSkillRepository().find("mangle"),0));state.addUnit(p);state.addUnit(e1);state.addUnit(e2);DebugOptions d=new DebugOptions();d.setCritical(DebugOptions.ForcedRoll.FAILURE);d.setEvasion(DebugOptions.ForcedRoll.FAILURE);BattleEngine engine=new BattleEngine(state,ZERO,d);engine.start();
        assertTrue(engine.execute(new UseSkillCommand(p.getUnitId(),"mangle",p.getUnitId(),1)).isSuccess());
        assertEquals(70,e1.getHp());assertEquals(70,e2.getHp());assertTrue(p.has(StatusType.DAZED));
    }

    @Test public void recklessChargeUsesNaturalStatusAndRecoilLogs(){
        BattleState state=new BattleState();PlayerUnit player=new PlayerUnit("P1","용사",1,1);EnemyUnit enemy=new EnemyUnit("E1","허수아비",1);
        player.equip(new SkillRuntime(new HeroSkillRepository().find("reckless_charge"),0));state.addUnit(player);state.addUnit(enemy);DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);BattleEngine engine=new BattleEngine(state,ZERO,debug);engine.start();
        assertTrue(engine.execute(new UseSkillCommand(player.getUnitId(),"reckless_charge",enemy.getUnitId(),1)).isSuccess());
        engine.execute(new EndTurnCommand(player.getUnitId()));String all=String.join("\n",state.getLogs());
        assertFalse(all.contains("RECOIL_AT_TURN_END"));assertTrue(all.contains("반동 효과가 적용되었습니다"));assertTrue(all.contains("반동 피해 18을 가했습니다"));
    }

    private StatusEffect status(StatusType type,String source,double amount){return new StatusEffect(type.name()+source,type,source,2,amount,true,true);}
    private Fixture fixture(int playerTile,int enemyTile){
        BattleState state=new BattleState();PlayerUnit p=new PlayerUnit("P1","용사",playerTile,1);EnemyUnit e=new EnemyUnit("E1","허수아비",enemyTile);
        p.equip(new SkillRuntime(new HeroSkillRepository().find("slash"),0));e.equip(new SkillRuntime(EnemySkillRepository.basicAttack(),0));e.equip(new SkillRuntime(EnemySkillRepository.slam(),0));state.addUnit(p);state.addUnit(e);DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);return new Fixture(state,p,e,debug,new BattleEngine(state,ZERO,debug));
    }
    private static final class Fixture {final BattleState state;final PlayerUnit player;final EnemyUnit enemy;final DebugOptions debug;final BattleEngine engine;Fixture(BattleState s,PlayerUnit p,EnemyUnit e,DebugOptions d,BattleEngine b){state=s;player=p;enemy=e;debug=d;engine=b;}}
}
