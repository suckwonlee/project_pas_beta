package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.ai.TrainingDummyAI;
import com.pas.game.battle.state.BattleState;
import com.pas.game.character.CharacterData;
import com.pas.game.character.CharacterRepository;
import com.pas.game.debug.DebugOptions;
import com.pas.game.passive.PassiveData;
import com.pas.game.passive.PassiveRepository;
import com.pas.game.skill.repository.HunterSkillRepository;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.skill.data.SkillDetailFormatter;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import org.junit.Test;

public class HunterImplementationTest {
    private static final RandomProvider ZERO=bound->0;

    @Test public void hunterIsAvailableWithDesignedBaseStatsAndTwentySkills(){
        CharacterData hunter=new CharacterRepository().all().get(1);
        assertTrue(hunter.isAvailable());assertEquals(160,hunter.getMaxHp());assertEquals(14,hunter.getAttack());assertEquals(11,hunter.getDefense());
        assertEquals(9,hunter.getCriticalRate(),0.001);assertEquals(8,hunter.getEvasionRate(),0.001);assertEquals(PassiveRepository.SURPRISE_ATTACK,hunter.getStartingPassiveId());
        HunterSkillRepository skills=new HunterSkillRepository();assertEquals(20,skills.all().size());assertEquals("archery",skills.all().get(0).getId());assertEquals("hunter_defend",skills.all().get(3).getId());
        assertEquals(0,skills.all().stream().filter(skill->!skill.isImplemented()).count());
    }

    @Test public void surpriseAttackDurationGrowsAtLevelsThreeAndFive(){
        PassiveData passive=new PassiveRepository().find(PassiveRepository.SURPRISE_ATTACK);
        assertTrue(passive.describeAt(1).contains("1턴"));assertTrue(passive.describeAt(2).contains("1턴"));assertTrue(passive.describeAt(3).contains("2턴"));assertTrue(passive.describeAt(4).contains("2턴"));assertTrue(passive.describeAt(5).contains("3턴"));
        assertTrue(passive.describeAt(5).contains("대상에게 전투당 한 번"));assertTrue(passive.describeAt(5).contains("저지불가"));
    }

    @Test public void surpriseAttackTriggersOnlyOnceForEachTargetPerBattleAndIgnoresUnstoppable(){
        BattleState state=new BattleState();PlayerUnit hunter=hunterWithSurprise(3);EnemyUnit first=new EnemyUnit("E1","첫 적",1);EnemyUnit second=new EnemyUnit("E2","둘째 적",1);
        first.addStatus(new StatusEffect("WILL",StatusType.UNSTOPPABLE,first.getUnitId(),-1,10,false,false));state.addUnit(hunter);state.addUnit(first);state.addUnit(second);
        DebugOptions debug=noRolls();BattleEngine engine=new BattleEngine(state,ZERO,debug);engine.start();
        engine.dealDamage(hunter,first,1,false,false);StatusEffect applied=status(first,StatusType.MOVEMENT_DISABLED);assertNotNull(applied);assertEquals(2,applied.getRemainingTurns());
        applied.tick();engine.dealDamage(hunter,first,1,false,false);assertEquals(1,status(first,StatusType.MOVEMENT_DISABLED).getRemainingTurns());
        engine.dealDamage(hunter,second,1,false,false);assertNotNull(status(second,StatusType.MOVEMENT_DISABLED));assertEquals(2,status(second,StatusType.MOVEMENT_DISABLED).getRemainingTurns());
    }

    @Test public void damageOverTimeDoesNotConsumeSurpriseAttackForThatTarget(){
        BattleState state=new BattleState();PlayerUnit hunter=hunterWithSurprise(1);EnemyUnit enemy=new EnemyUnit("E1","적",1);state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());engine.start();
        engine.dealDamage(hunter,enemy,3,false,com.pas.game.battle.damage.DamageType.DAMAGE_OVER_TIME);assertFalse(enemy.has(StatusType.MOVEMENT_DISABLED));
        engine.dealDamage(hunter,enemy,3,false,false);assertTrue(enemy.has(StatusType.MOVEMENT_DISABLED));
    }

    @Test public void fixedTrapTriggersOnceWhenEnemyEntersItsTile(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=hunterWithSurprise(1);EnemyUnit enemy=new EnemyUnit("E1","적",12);hunter.equip(new SkillRuntime(skills.find("fixed_trap"),0));state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());engine.start();
        assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"fixed_trap",hunter.getUnitId(),1)).isSuccess());assertEquals(1,state.getTraps().size());engine.forceMove(enemy.getUnitId(),1);assertTrue(enemy.has(StatusType.BIND));assertTrue(state.getTraps().isEmpty());
    }

    @Test public void piercingShotHitsEveryEnemyInTheChosenLine(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=new PlayerUnit("P1","사냥꾼",1,1,com.pas.game.rune.RuneLoadout.none(),160,14,11,9,8,0);EnemyUnit first=new EnemyUnit("E1","앞 적",2);EnemyUnit second=new EnemyUnit("E2","뒤 적",3);hunter.equip(new SkillRuntime(skills.find("piercing_shot"),0));state.addUnit(hunter);state.addUnit(first);state.addUnit(second);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());engine.start();
        assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"piercing_shot",hunter.getUnitId(),2)).isSuccess());assertEquals(16,first.getMaxHp()-first.getHp());assertEquals(16,second.getMaxHp()-second.getHp());
    }

    @Test public void emergencyEscapeUsesThePreselectedTileBeforeIncomingDamage(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=new PlayerUnit("P1","사냥꾼",1,1,com.pas.game.rune.RuneLoadout.none(),160,14,11,9,8,0);EnemyUnit enemy=new EnemyUnit("E1","적",1);hunter.equip(new SkillRuntime(skills.find("emergency_escape"),0));enemy.equip(new SkillRuntime(com.pas.game.skill.repository.EnemySkillRepository.basicAttack(),0));state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());engine.start();
        assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"emergency_escape",hunter.getUnitId(),2)).isSuccess());assertTrue(hunter.has(StatusType.EMERGENCY_ESCAPE_READY));engine.execute(new EndTurnCommand(hunter.getUnitId()));
        assertTrue(engine.execute(new UseSkillCommand(enemy.getUnitId(),"dummy_attack",hunter.getUnitId(),1)).isSuccess());assertEquals(2,hunter.getTile());assertEquals(160,hunter.getHp());assertFalse(hunter.has(StatusType.EMERGENCY_ESCAPE_READY));assertEquals(58,hunter.getEvasionRate(),0.001);
    }

    @Test public void sharpRaisesCriticalDamageAndDisappearsOnNonCriticalNormalAttack(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=new PlayerUnit("P1","사냥꾼",1,1,com.pas.game.rune.RuneLoadout.none(),160,14,11,9,8,0);EnemyUnit enemy=new EnemyUnit("E1","적",2);hunter.equip(new SkillRuntime(skills.find("archery"),0));state.addUnit(hunter);state.addUnit(enemy);DebugOptions debug=noRolls();BattleEngine engine=new BattleEngine(state,ZERO,debug);skills.find("cold_aim").getEffects().get(0).apply(engine,hunter,hunter,20);engine.start();
        debug.setCritical(DebugOptions.ForcedRoll.SUCCESS);assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"archery",enemy.getUnitId(),2)).isSuccess());assertEquals(20,hunter.sum(StatusType.SHARP),0.001);assertEquals(1.7,hunter.getCriticalDamageMultiplier(),0.001);
        state.getTurn().addSkillAction();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"archery",enemy.getUnitId(),2)).isSuccess());assertFalse(hunter.has(StatusType.SHARP));assertEquals(1.5,hunter.getCriticalDamageMultiplier(),0.001);
    }

    @Test public void venomInjectionGrantsTheSamePerHitPoisonAttackEffectAsVenomPassive(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=new PlayerUnit("P1","사냥꾼",1,1);EnemyUnit enemy=new EnemyUnit("E1","적",2);hunter.equip(new SkillRuntime(skills.find("rapid_fire"),0));state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());skills.find("venom_injection").getEffects().get(0).apply(engine,hunter,hunter,1);engine.start();
        assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"rapid_fire",enemy.getUnitId(),2)).isSuccess());assertEquals(2,enemy.sum(StatusType.POISON),0.001);assertTrue(state.getLogs().stream().anyMatch(line->line.contains("독공격 1")));
    }

    @Test public void decoyDisappearsAsSoonAsEnemyMovesOntoItsTile(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=new PlayerUnit("P1","사냥꾼",1,1);EnemyUnit enemy=new EnemyUnit("E1","적",3);hunter.equip(new SkillRuntime(skills.find("install_decoy"),0));state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());engine.start();assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"install_decoy",hunter.getUnitId(),2)).isSuccess());assertEquals(1,state.getDecoys().size());engine.execute(new EndTurnCommand(hunter.getUnitId()));new TrainingDummyAI(ZERO).takeTurn(engine);assertEquals(2,enemy.getTile());assertTrue(state.getDecoys().isEmpty());
    }

    @Test public void allThreeNormalAttacksUseCurrentHunterStatsAndRoundedDamage(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=hunter();EnemyUnit enemy=new EnemyUnit("E1","적",1);state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());
        skills.find("archery").getEffects().get(0).apply(engine,hunter,enemy,skills.find("archery").valueAt(0));assertEquals(13,90-enemy.getHp());
        enemy.setHpForDebug(90);skills.find("rapid_fire").getEffects().get(0).apply(engine,hunter,enemy,skills.find("rapid_fire").valueAt(0));assertEquals(12,90-enemy.getHp());
        enemy.setHpForDebug(90);DebugOptions critical=noRolls();critical.setCritical(DebugOptions.ForcedRoll.SUCCESS);BattleEngine criticalEngine=new BattleEngine(state,ZERO,critical);for(com.pas.game.skill.effect.SkillEffect effect:skills.find("brow_shot").getEffects())effect.apply(criticalEngine,hunter,enemy,skills.find("brow_shot").valueAt(0));assertEquals(45,90-enemy.getHp());assertEquals(14,hunter.getCriticalRate(),0.001);
    }

    @Test public void hunterDefensesProduceBarrierEvasionAndPreselectedEscape(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=hunter();EnemyUnit enemy=new EnemyUnit("E1","적",1);state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());
        skills.find("hunter_defend").getEffects().get(0).apply(engine,hunter,hunter,skills.find("hunter_defend").valueAt(0));assertEquals(28,hunter.getBarrier());
        skills.find("evasion_focus").getEffects().get(0).apply(engine,hunter,hunter,skills.find("evasion_focus").valueAt(0));assertEquals(19,hunter.getEvasionRate(),0.001);
        skills.find("emergency_escape").getEffects().get(0).apply(engine,hunter,hunter,skills.find("emergency_escape").valueAt(0),2);assertTrue(hunter.has(StatusType.EMERGENCY_ESCAPE_READY));
    }

    @Test public void stealthAndSwiftMovementReachSelectedTilesAndStealthAddsEvasion(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState firstState=new BattleState();PlayerUnit stealth=hunter();EnemyUnit enemy=new EnemyUnit("E1","적",12);stealth.equip(new SkillRuntime(skills.find("stealth_movement"),0));firstState.addUnit(stealth);firstState.addUnit(enemy);BattleEngine first=new BattleEngine(firstState,ZERO,noRolls());first.start();
        assertTrue(first.execute(new UseSkillCommand(stealth.getUnitId(),"stealth_movement",stealth.getUnitId(),2)).isSuccess());assertEquals(2,stealth.getTile());assertEquals(38,stealth.getEvasionRate(),0.001);
        BattleState secondState=new BattleState();PlayerUnit swift=hunter();EnemyUnit secondEnemy=new EnemyUnit("E2","적",12);swift.equip(new SkillRuntime(skills.find("swift_movement"),0));secondState.addUnit(swift);secondState.addUnit(secondEnemy);BattleEngine second=new BattleEngine(secondState,ZERO,noRolls());second.start();assertTrue(second.execute(new UseSkillCommand(swift.getUnitId(),"swift_movement",swift.getUnitId(),3)).isSuccess());assertEquals(3,swift.getTile());
    }

    @Test public void swiftMovementProcessesEachPathTileAndStopsWhenIntermediateTrapBinds(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=hunter();EnemyUnit trapOwner=new EnemyUnit("E1","덫 주인",12);hunter.equip(new SkillRuntime(skills.find("swift_movement"),0));state.addUnit(hunter);state.addUnit(trapOwner);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());engine.installBindingTrap(trapOwner,2,2);engine.start();
        assertTrue(engine.execute(new UseSkillCommand(hunter.getUnitId(),"swift_movement",hunter.getUnitId(),6)).isSuccess());assertEquals(2,hunter.getTile());assertTrue(hunter.has(StatusType.BIND));assertTrue(state.getTraps().isEmpty());assertTrue(state.getLogs().stream().anyMatch(line->line.contains("2번 칸에서 중단")));
    }

    @Test public void epicSkillsApplyPoisonScalingExecutionDamageAndStiffness(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=hunter();EnemyUnit enemy=new EnemyUnit("E1","적",1);state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());enemy.addStatus(new StatusEffect("P",StatusType.POISON,hunter.getUnitId(),-1,7,true,true));
        skills.find("venom_chase").getEffects().get(0).apply(engine,hunter,enemy,skills.find("venom_chase").valueAt(0));assertEquals(16,hunter.getCriticalRate(),0.001);
        skills.find("execution_shot").getEffects().get(0).apply(engine,hunter,enemy,skills.find("execution_shot").valueAt(0));assertEquals(45,90-enemy.getHp());
        enemy.setHpForDebug(90);for(com.pas.game.skill.effect.SkillEffect effect:skills.find("joint_shot").getEffects())effect.apply(engine,hunter,enemy,skills.find("joint_shot").valueAt(0));assertEquals(17,90-enemy.getHp());assertTrue(enemy.has(StatusType.STIFF));
    }

    @Test public void legendarySkillsApplyLargePoisonAndFourHitsToEveryEnemyInRange(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState poisonState=new BattleState();PlayerUnit hunter=hunter();EnemyUnit target=new EnemyUnit("E0","적",1);poisonState.addUnit(hunter);poisonState.addUnit(target);BattleEngine poisonEngine=new BattleEngine(poisonState,ZERO,noRolls());skills.find("solitude").getEffects().get(0).apply(poisonEngine,hunter,target,skills.find("solitude").valueAt(0));assertEquals(42,target.sum(StatusType.POISON),0.001);
        BattleState rainState=new BattleState();PlayerUnit rainHunter=hunter();EnemyUnit nearOne=new EnemyUnit("E1","근거리1",2);EnemyUnit nearTwo=new EnemyUnit("E2","근거리2",6);EnemyUnit far=new EnemyUnit("E3","원거리",12);rainHunter.equip(new SkillRuntime(skills.find("arrow_rain"),0));rainState.addUnit(rainHunter);rainState.addUnit(nearOne);rainState.addUnit(nearTwo);rainState.addUnit(far);BattleEngine rainEngine=new BattleEngine(rainState,ZERO,noRolls());rainEngine.start();assertTrue(rainEngine.execute(new UseSkillCommand(rainHunter.getUnitId(),"arrow_rain",rainHunter.getUnitId(),1)).isSuccess());assertEquals(20,90-nearOne.getHp());assertEquals(20,90-nearTwo.getHp());assertEquals(90,far.getHp());
    }

    @Test public void deadlyPoisonCurseDoublesEachTurnEndDotTickAndShadowChaseRaisesBothRates(){
        HunterSkillRepository skills=new HunterSkillRepository();BattleState state=new BattleState();PlayerUnit hunter=hunter();EnemyUnit enemy=new EnemyUnit("E1","적",1);state.addUnit(hunter);state.addUnit(enemy);BattleEngine engine=new BattleEngine(state,ZERO,noRolls());enemy.addStatus(new StatusEffect("P",StatusType.POISON,hunter.getUnitId(),-1,10,true,true));skills.find("deadly_poison_curse").getEffects().get(0).apply(engine,hunter,enemy,3);engine.start();engine.execute(new EndTurnCommand(hunter.getUnitId()));engine.execute(new EndTurnCommand(enemy.getUnitId()));assertEquals(70,enemy.getHp());assertEquals(9,enemy.sum(StatusType.POISON),0.001);
        BattleState chaseState=new BattleState();PlayerUnit chaseHunter=hunter();EnemyUnit chaseEnemy=new EnemyUnit("E2","적",12);chaseState.addUnit(chaseHunter);chaseState.addUnit(chaseEnemy);BattleEngine chaseEngine=new BattleEngine(chaseState,ZERO,noRolls());for(com.pas.game.skill.effect.SkillEffect effect:skills.find("shadow_chase").getEffects())effect.apply(chaseEngine,chaseHunter,chaseHunter,60);assertEquals(69,chaseHunter.getCriticalRate(),0.001);assertEquals(68,chaseHunter.getEvasionRate(),0.001);
    }

    @Test public void hunterUpgradeUseCountsCooldownsAndDescriptionsMatchRuntimeValues(){
        HunterSkillRepository skills=new HunterSkillRepository();SkillRuntime escape=new SkillRuntime(skills.find("emergency_escape"),1);assertEquals(9,escape.getMaxUses());SkillRuntime trap=new SkillRuntime(skills.find("fixed_trap"),1);assertEquals(4,trap.getMaxUses());trap.consume();assertEquals(4,trap.getCooldownRemaining());trap.onOwnerTurnStart();assertEquals(3,trap.getCooldownRemaining());
        PlayerUnit hunter=hunter();String archery=SkillDetailFormatter.summary(skills.find("archery"),0,hunter);String defense=SkillDetailFormatter.summary(skills.find("hunter_defend"),0,hunter);assertTrue(archery.contains("13만큼 피해"));assertTrue(defense.contains("28만큼 피해 방어"));
    }

    private PlayerUnit hunterWithSurprise(int level){PlayerUnit hunter=new PlayerUnit("P1","사냥꾼",1,1);hunter.equipPassive(new PassiveRepository().create(PassiveRepository.SURPRISE_ATTACK,level,"character:HUNTER"));return hunter;}
    private PlayerUnit hunter(){return new PlayerUnit("P1","사냥꾼",1,1,RuneLoadout.none(),160,14,11,9,8,0);}
    private DebugOptions noRolls(){DebugOptions debug=new DebugOptions();debug.setCritical(DebugOptions.ForcedRoll.FAILURE);debug.setEvasion(DebugOptions.ForcedRoll.FAILURE);return debug;}
    private StatusEffect status(EnemyUnit unit,StatusType type){for(StatusEffect effect:unit.getStatuses())if(effect.getType()==type)return effect;return null;}
}
