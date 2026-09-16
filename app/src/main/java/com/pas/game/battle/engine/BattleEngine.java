package com.pas.game.battle.engine;

import com.pas.game.battle.command.BattleCommand;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.ResolveWizardReturnCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.command.UsePotionCommand;
import com.pas.game.battle.damage.DamageType;
import com.pas.game.battle.result.BattleEvent;
import com.pas.game.battle.result.BattleResult;
import com.pas.game.battle.state.BattleOutcome;
import com.pas.game.battle.state.BattleDecoy;
import com.pas.game.battle.state.BattleState;
import com.pas.game.battle.state.BattleTrap;
import com.pas.game.battle.state.BattleSanctuary;
import com.pas.game.battle.state.WizardBattleState;
import com.pas.game.battle.turn.MovementType;
import com.pas.game.debug.DebugOptions;
import com.pas.game.effect.UnitEffect;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.map.BattleGrid;
import com.pas.game.passive.PassiveContext;
import com.pas.game.passive.PassiveRuntime;
import com.pas.game.passive.PassiveTrigger;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.skill.data.TargetType;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import com.pas.game.unit.Team;
import com.pas.game.unit.WizardPhantom;
import com.pas.game.skill.repository.EnemySkillRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** 모든 명령 검증과 상태 변경을 담당하는 결정적 전투 엔진 진입점이다. */
public final class BattleEngine {
    private final BattleState state;
    private final RandomProvider random;
    private final DebugOptions debug;
    private int passiveDispatchDepth;
    private SkillRuntime executingSkill;
    private String executingOptionId;
    private long skillUseSequence;
    private String executingUseToken;
    private final Set<String> abyssalMarkUseKeys=new HashSet<>();
    private int wizardLastSpecialDamage;
    private static final int MAX_PASSIVE_DISPATCH_DEPTH=24;

    public BattleEngine(BattleState state,RandomProvider random,DebugOptions debug){this.state=state;this.random=random;this.debug=debug;}
    public BattleState getState(){return state;} public DebugOptions getDebug(){return debug;}

    public BattleResult start(){
        if(state.getUnits().isEmpty())return BattleResult.error("전투 유닛이 없습니다.");
        for(BattleUnit unit:new ArrayList<>(state.getUnits()))dispatchPassives(unit,PassiveTrigger.BATTLE_START,PassiveContext.event(this,PassiveTrigger.BATTLE_START,unit,unit));
        evaluateOutcome();if(state.getOutcome()!=BattleOutcome.ONGOING)return outcomeResult();
        buildRoundOrder(); return advanceTurn();
    }

    public BattleResult execute(BattleCommand command){
        if(command==null)return BattleResult.error("명령이 없습니다.");
        if(state.getOutcome()!=BattleOutcome.ONGOING)return BattleResult.error("이미 전투가 종료되었습니다.");
        BattleUnit active=activeUnit();
        if(active==null||!active.getUnitId().equals(command.getActorUnitId()))return BattleResult.error("현재 행동 유닛의 명령이 아닙니다.");
        if(command instanceof ResolveWizardReturnCommand)return resolveWizardReturn(active,(ResolveWizardReturnCommand)command);
        if(state.getWizardState().getPendingReturnOwnerId()!=null)return BattleResult.error("차원 표류의 복귀 위치를 먼저 선택하세요.");
        if(command instanceof MoveCommand)return move(active,(MoveCommand)command);
        if(command instanceof UseSkillCommand)return useSkill(active,(UseSkillCommand)command);
        if(command instanceof UsePotionCommand)return usePotion(active,(UsePotionCommand)command);
        if(command instanceof EndTurnCommand)return advanceTurn();
        return BattleResult.error("지원하지 않는 명령입니다.");
    }

    public BattleUnit activeUnit(){return state.find(state.getTurn().getActiveUnitId());}

    public BattleResult advanceTurn(){
        finishActiveTurn();
        evaluateOutcome();
        if(state.getOutcome()!=BattleOutcome.ONGOING)return outcomeResult();
        while(true){
            int index=state.nextTurnIndex();
            if(index>=state.getRoundOrder().size()){
                state.nextRound(); buildRoundOrder(); index=state.nextTurnIndex();
            }
            if(state.getRoundOrder().isEmpty())return BattleResult.error("행동 가능한 유닛이 없습니다.");
            BattleUnit unit=state.getRoundOrder().get(index);
            if(state.find(unit.getUnitId())!=unit||unit.isDead()||unit.getEligibleRound()>state.getRound())continue;
            return beginTurn(unit);
        }
    }

    private void buildRoundOrder(){
        List<BattleUnit> order=new ArrayList<>();for(BattleUnit unit:state.getUnits())if(unit.participatesInTurns())order.add(unit);
        order.sort(Comparator.comparing((BattleUnit u)->u.getTeam()==Team.PLAYER?0:1)
                .thenComparingInt(u->u instanceof PlayerUnit?((PlayerUnit)u).getPlayerSlot():0));
        state.setRoundOrder(order); state.setTurnIndex(-1);
    }

    private BattleResult beginTurn(BattleUnit unit){
        WizardBattleState.Drift drift=state.getWizardState().getDrifts().get(unit.getUnitId());
        if(drift!=null&&drift.chooseAdjacent){state.getTurn().begin(unit.getUnitId(),true);state.getWizardState().setPendingReturnOwnerId(unit.getUnitId());log(unit.getName()+"의 차원 표류 복귀 위치를 선택합니다.");return BattleResult.ok().add(BattleEvent.Type.TURN_STARTED,"차원 표류 복귀 위치 선택");}
        if(drift!=null){unit.setTile(drift.originTile);unit.setOnField(true);state.getWizardState().getDrifts().remove(unit.getUnitId());unit.removeStatus(StatusType.DIMENSIONAL_DRIFT);log(unit.getName()+"이(가) 차원 표류에서 원래 위치로 복귀했습니다.");}
        return continueBeginTurn(unit);
    }

    private BattleResult continueBeginTurn(BattleUnit unit){
        processWizardPreTurnStart(unit);
        unit.removeStatus(StatusType.EMERGENCY_ESCAPE_READY);tickOwnedDecoys(unit);tickSanctuariesAtPlayerTurn(unit);
        unit.removeStatusById("DIVINE_INTERVENTION@"+unit.getUnitId());
        unit.removeStatusById("HEALING_PRAYER@"+unit.getUnitId());
        int expiredBarrier=unit.clearBarrier();if(expiredBarrier>0)log(unit.getName()+" 턴 시작: 피해방어 "+expiredBarrier+" 소멸");
        unit.beginTurnCycle();
        for(SkillRuntime skill:unit.getEquippedSkills())if(skill!=null)skill.onOwnerTurnStart();
        processDivineFragmentTurnStart(unit);
        dispatchPassives(unit,PassiveTrigger.TURN_START,PassiveContext.event(this,PassiveTrigger.TURN_START,unit,unit));
        processPersistentTurnEffects(unit);
        processTurnStartEffects(unit);
        if(state.getOutcome()!=BattleOutcome.ONGOING)return outcomeResult();
        if(unit.isDead()){evaluateOutcome();return advanceTurn();}
        boolean controlled=unit.has(StatusType.DAZED)||unit.has(StatusType.STIFF);
        boolean blocked=controlled&&!hasUnstoppable(unit);
        state.getTurn().begin(unit.getUnitId(),blocked);
        if(!blocked&&unit.getTurnsStarted()==10){
            for(PassiveRuntime passive:unit.getPassives())if(passive.isEnabled()&&com.pas.game.passive.PassiveRepository.GRAND_MAGIC.equals(passive.getData().getId())){
                state.getTurn().addSkillAction();break;
            }
        }
        if(unit instanceof EnemyUnit)((EnemyUnit)unit).beginOwnTurn();
        String text="라운드 "+state.getRound()+" - "+unit.getName()+"의 턴"+(blocked?" (행동 불가)":"");
        log(text);
        for(StatusEffect status:new ArrayList<>(unit.getStatuses())){
            if(status.getType()==StatusType.HUNT_TURN_SKIP&&status.getMagnitude()>0){
                status.decreaseMagnitude(1);
                if(status.getMagnitude()<=0)unit.removeStatusEffect(status);
                log(unit.getName()+"의 턴을 사냥 효과로 건너뜁니다.");
                return advanceTurn();
            }
        }
        return BattleResult.ok().add(BattleEvent.Type.TURN_STARTED,text);
    }

    public boolean hasPendingWizardReturn(){return state.getWizardState().getPendingReturnOwnerId()!=null;}
    public List<Integer> validWizardReturnTiles(){List<Integer> result=new ArrayList<>();String id=state.getWizardState().getPendingReturnOwnerId();WizardBattleState.Drift drift=id==null?null:state.getWizardState().getDrifts().get(id);if(drift!=null)for(int tile:BattleGrid.adjacent(drift.originTile))result.add(tile);return result;}
    /** @deprecated 복귀도 네트워크 명령으로 제출해야 한다. */
    @Deprecated public BattleResult resolveWizardReturn(int tile){String id=state.getWizardState().getPendingReturnOwnerId();return execute(new ResolveWizardReturnCommand(id,tile));}

    private BattleResult resolveWizardReturn(BattleUnit unit,ResolveWizardReturnCommand command){
        String id=state.getWizardState().getPendingReturnOwnerId();
        WizardBattleState.Drift drift=id==null?null:state.getWizardState().getDrifts().get(id);
        if(drift==null||unit==null||!unit.getUnitId().equals(id))return BattleResult.error("복귀 대기 중인 마법사가 아닙니다.");
        int tile=command.getDestinationTile();
        if(!BattleGrid.isValid(tile)||BattleGrid.distance(drift.originTile,tile)!=1)return BattleResult.error("원래 위치에서 정확히 1칸 떨어진 칸을 선택하세요.");
        unit.setTile(tile);unit.setOnField(true);unit.removeStatus(StatusType.DIMENSIONAL_DRIFT);
        state.getWizardState().getDrifts().remove(id);state.getWizardState().setPendingReturnOwnerId(null);
        log(unit.getName()+"이(가) 차원 표류에서 "+tile+"번 칸으로 복귀했습니다.");
        return continueBeginTurn(unit);
    }

    private void processTurnStartEffects(BattleUnit unit){
        List<StatusEffect> copy=new ArrayList<>(unit.getStatuses());
        for(StatusEffect status:copy){
            if(status.getType()==StatusType.JUSTICE_DOT){
                dealDamage(state.find(status.getSourceUnitId()),unit,unit.getMaxHp()*status.getMagnitude()/100.0,false,DamageType.DAMAGE_OVER_TIME,"심판");
            }
        }
    }

    private void processPersistentTurnEffects(BattleUnit unit){
        if(unit.getTurnsStarted()%3==0){
            double survival=unit.sumEffect(UnitEffectType.SURVIVAL);
            if(survival>0)heal(unit,ceilToInt(survival));
        }
    }

    private void finishActiveTurn(){
        BattleUnit active=activeUnit(); if(active==null)return;
        List<StatusEffect> consumedAtEnd=new ArrayList<>();
        for(StatusEffect status:new ArrayList<>(active.getStatuses())){
            if(status.getType()==StatusType.FIRE){
                int hits=active.has(StatusType.DOT_DAMAGE_TWICE)?2:1;
                for(int i=0;i<hits&&!active.isDead();i++)dealDamage(state.find(status.getSourceUnitId()),active,status.getMagnitude(),false,DamageType.DAMAGE_OVER_TIME,"화염");
                if(!status.consumeReinforcedFlag()){
                    status.scaleMagnitudeAndFloor(0.95);
                    if(status.getMagnitude()<=0)consumedAtEnd.add(status);
                }
            }else if(status.getType()==StatusType.POISON){
                int hits=active.has(StatusType.DOT_DAMAGE_TWICE)?2:1;
                for(int i=0;i<hits&&!active.isDead();i++)dealDamage(state.find(status.getSourceUnitId()),active,status.getMagnitude(),false,DamageType.DAMAGE_OVER_TIME,"중독");
                status.decreaseMagnitude(1);
                if(status.getMagnitude()<=0)consumedAtEnd.add(status);
            }else if(status.getType()==StatusType.RECOIL_AT_TURN_END){
                dealDamage(active,active,active.getMaxHp()*status.getMagnitude()/100.0,false,DamageType.TRUE_DAMAGE,"반동");
                consumedAtEnd.add(status);
            }
        }
        processPurificationExecution(active);
        processSanctuaryTurnEnd(active);
        processPhantomTurnEnd(active);
        processHungryStars(active);
        processOtherworldGates(active);
        active.removeStatuses(consumedAtEnd);
        active.removeStatuses(active.tickStatuses());
        dispatchPassives(active,PassiveTrigger.TURN_END,PassiveContext.event(this,PassiveTrigger.TURN_END,active,active));
        active.removeStatus(StatusType.MANA_ACTIVE);
        state.getTurn().clear(); evaluateOutcome(); log("");
    }

    private String format(double value){return value==(long)value?String.valueOf((long)value):String.format(java.util.Locale.US,"%.2f",value);}

    private BattleResult move(BattleUnit actor,MoveCommand command){
        if(!actor.isOnField())return BattleResult.error("전장을 이탈한 동안에는 이동할 수 없습니다.");
        int destination=command.getDestinationTile();
        if(!BattleGrid.isValid(destination)||BattleGrid.distance(actor.getTile(),destination)!=1)return BattleResult.error("인접한 한 칸만 이동할 수 있습니다.");
        if(command.getMovementType()!=MovementType.FORCED){
            if(actor.has(StatusType.MOVEMENT_DISABLED))return BattleResult.error("이동 불가 상태입니다.");
            if(actor.has(StatusType.BIND)&&!actor.has(StatusType.UNSTOPPABLE))return BattleResult.error("구속 상태에서는 자발적으로 이동할 수 없습니다.");
            if(!state.getTurn().canMove())return BattleResult.error("남은 이동 기회가 없습니다.");
            if(!state.getTurn().consumeMove())return BattleResult.error("이동 기회를 소비할 수 없습니다.");
        }
        int from=actor.getTile(); actor.setTile(destination);
        String text=actor.getName()+" 이동: "+from+" → "+destination;
        log(text); onEnterTile(actor,destination);
        return BattleResult.ok().add(BattleEvent.Type.MOVED,text);
    }

    private void onEnterTile(BattleUnit actor,int tile){
        log(actor.getName()+"이(가) "+tile+"번 칸에 진입했습니다.");
        for(BattleTrap trap:new ArrayList<>(state.getTraps()))if(trap.getTile()==tile&&trap.getOwnerTeam()!=actor.getTeam()){
            state.removeTrap(trap);log(actor.getName()+"이(가) 고정덫을 밟았습니다.");
            applyStatus(actor,new StatusEffect(trap.getId(),StatusType.BIND,trap.getOwnerUnitId(),trap.getBindTurns(),0,false,true));
        }
        for(BattleDecoy decoy:new ArrayList<>(state.getDecoys()))if(decoy.getTile()==tile&&decoy.getOwnerTeam()!=actor.getTeam()){
            state.removeDecoy(decoy);log(actor.getName()+"이(가) 미끼가 설치된 칸에 진입해 미끼가 사라졌습니다.");
        }
    }

    private BattleResult useSkill(BattleUnit actor,UseSkillCommand command){
        if(!actor.isOnField())return BattleResult.error("전장을 이탈한 동안에는 스킬을 사용할 수 없습니다.");
        if(state.getTurn().getSkillsRemaining()<=0)return BattleResult.error("남은 스킬 사용 기회가 없습니다.");
        SkillRuntime skill=actor.findSkill(command.getSkillId());
        if(skill==null)return BattleResult.error("장착하지 않은 스킬입니다.");
        if(!debug.isAllSkillsAvailable()&&!skill.canUse())return BattleResult.error("사용횟수 또는 쿨타임을 확인하세요.");
        BattleUnit target=resolveTarget(actor,skill,command.getTargetUnitId());
        String invalid=validateTarget(actor,skill,target,command.getTargetTile());
        if(invalid!=null)return BattleResult.error(invalid);
        String optionError=validateWizardOption(actor,skill,target,command.getOptionId());if(optionError!=null)return BattleResult.error(optionError);
        if(!state.getTurn().consumeSkill())return BattleResult.error("스킬 기회를 소비할 수 없습니다.");
        if(!debug.isAllSkillsAvailable())skill.consume();
        log("["+actor.getName()+"] "+skill.getData().getName()+" 사용");
        int repeats=1;if(!skill.getData().isUltimate()){int extra=actor.has(StatusType.DOUBLE_CAST)?ceilToInt(actor.sum(StatusType.DOUBLE_CAST)):0;state.getWizardState().consumeDoubleCast(actor.getUnitId());repeats+=extra;if(extra>0)actor.removeStatus(StatusType.DOUBLE_CAST);}
        SkillRuntime previous=executingSkill;String previousOption=executingOptionId,previousToken=executingUseToken;executingSkill=skill;executingOptionId=command.getOptionId();executingUseToken=actor.getUnitId()+"#"+(++skillUseSequence);abyssalMarkUseKeys.clear();
        try{for(int activation=0;activation<repeats&&state.getOutcome()==BattleOutcome.ONGOING;activation++){
            if(activation>0)log(skill.getData().getName()+"이(가) 이중 영창으로 추가 발동합니다. ("+(activation+1)+"/"+repeats+")");
            List<BattleUnit> targets=skillTargets(actor,skill,target);
            for(BattleUnit one:targets)if(one!=null&&!one.isDead())for(com.pas.game.skill.effect.SkillEffect effect:skill.getData().getEffects())effect.apply(this,actor,one,skill.currentValue(),command.getTargetTile());
        }
        applyExistenceLoanCopy(actor,skill,target,command.getTargetTile(),command.getOptionId());
        }finally{executingSkill=previous;executingOptionId=previousOption;executingUseToken=previousToken;abyssalMarkUseKeys.clear();}
        dispatchPassives(actor,PassiveTrigger.AFTER_SKILL_USED,PassiveContext.skill(this,actor,target,skill));
        evaluateOutcome();
        return BattleResult.ok().add(BattleEvent.Type.SKILL_USED,skill.getData().getName()+" 실행");
    }

    private List<BattleUnit> skillTargets(BattleUnit actor,SkillRuntime skill,BattleUnit target){List<BattleUnit> targets=new ArrayList<>();if(skill.getData().getTargetType()==TargetType.ALL_ENEMIES)targets.addAll(enemiesOf(actor));else if(skill.getData().getTargetType()==TargetType.ALL_ENEMIES_IN_RANGE){for(BattleUnit enemy:enemiesOf(actor))if(BattleGrid.distance(actor.getTile(),enemy.getTile())<=skill.getData().getRange())targets.add(enemy);}else targets.add(target==null?actor:target);return targets;}

    private BattleResult usePotion(BattleUnit actor,UsePotionCommand command){
        if(actor.getTeam()!=Team.PLAYER)return BattleResult.error("플레이어만 포션을 사용할 수 있습니다.");
        if(state.getTurn().getSkillsRemaining()<=0)return BattleResult.error("남은 행동 기회가 없습니다.");
        if(actor.getHp()>=actor.getMaxHp())return BattleResult.error("HP가 가득 차 있습니다.");
        com.pas.game.item.potion.PotionInventory inventory=state.getPotionInventory();
        if(inventory==null||inventory.isEmpty())return BattleResult.error("사용할 포션이 없습니다.");
        com.pas.game.item.potion.PotionData potion=command.getPotionTier()>0?inventory.find(command.getPotionTier()):inventory.getPotion();
        if(potion==null||inventory.getCount(potion)<=0)return BattleResult.error("선택한 포션이 없습니다.");
        if(!state.getTurn().consumeSkill())return BattleResult.error("행동 기회를 소비할 수 없습니다.");
        inventory.consumeOne(potion);
        int before=actor.getHp();heal(actor,potion.getHealing());int healed=actor.getHp()-before;
        String text=actor.getName()+"이(가) "+potion.getName()+" 사용 · HP "+healed+" 회복";
        log(text);return BattleResult.ok().add(BattleEvent.Type.POTION_USED,text);
    }

    private BattleUnit resolveTarget(BattleUnit actor,SkillRuntime skill,String targetId){
        TargetType type=skill.getData().getTargetType();
        if(type==TargetType.SELF||type==TargetType.NONE||type==TargetType.TILE||type==TargetType.ALL_ENEMIES||type==TargetType.ALL_ENEMIES_IN_RANGE)return actor;
        return targetId==null?null:state.find(targetId);
    }
    private String validateTarget(BattleUnit actor,SkillRuntime skill,BattleUnit target,Integer targetTile){
        if(!skill.getData().isImplemented())return "최신 설계 문구가 없어 아직 사용할 수 없습니다.";
        TargetType type=skill.getData().getTargetType();
        if(type==TargetType.TILE){
            if(targetTile==null||!BattleGrid.isValid(targetTile))return "대상 칸이 필요합니다.";
            int distance=BattleGrid.distance(actor.getTile(),targetTile);
            if(distance<skill.getData().getMinimumRange()||distance>skill.getData().getRange())return "선택한 칸이 스킬 사거리 밖에 있습니다.";
            if(skill.getData().isCardinalTileOnly()&&BattleGrid.column(actor.getTile())!=BattleGrid.column(targetTile)&&BattleGrid.row(actor.getTile())!=BattleGrid.row(targetTile))return "상하좌우 직선 방향의 칸을 선택하세요.";
            return null;
        }
        if(type==TargetType.NONE||type==TargetType.ALL_ENEMIES)return null;
        if(type==TargetType.ALL_ENEMIES_IN_RANGE){for(BattleUnit enemy:enemiesOf(actor))if(BattleGrid.distance(actor.getTile(),enemy.getTile())<=skill.getData().getRange())return null;return "사거리 안에 적이 없습니다.";}
        if(target==null||target.isDead()||!target.isOnField())return "전장에 있는 살아있는 대상을 선택하세요.";
        if(type==TargetType.ENEMY&&target.getTeam()==actor.getTeam())return "아군은 공격할 수 없습니다.";
        if(type==TargetType.ALLY&&(target.getTeam()!=actor.getTeam()||!target.canReceiveSupport()))return "효과를 받을 수 있는 우호적 대상만 선택할 수 있습니다.";
        if(BattleGrid.distance(actor.getTile(),target.getTile())>skill.getData().getRange())return "대상이 사거리 밖에 있습니다.";
        return null;
    }

    /** 치명타 → (비치명타일 때만) 회피 → 피해감소/피해증가 → 방어량 순으로 처리한다. */
    public int dealDamage(BattleUnit attacker,BattleUnit target,double raw,boolean rollHit,boolean fixed){
        return dealDamage(attacker,target,raw,rollHit,fixed?DamageType.TRUE_DAMAGE:DamageType.DIRECT);
    }
    public int dealDamage(BattleUnit attacker,BattleUnit target,double raw,boolean rollHit,DamageType damageType){
        return dealDamage(attacker,target,raw,rollHit,damageType,null);
    }
    /** 치명 피해: 치명타 판정 실패 시 빗나가며, 성공 시 올림한 기본 피해에 치명타 배율과 치명타율을 더해 계산한다. */
    public int dealCriticalDamage(BattleUnit attacker,BattleUnit target,double raw){
        int critRoll=random.nextInt(100)+1;
        boolean critical=debug.getCritical()==DebugOptions.ForcedRoll.SUCCESS||(debug.getCritical()==DebugOptions.ForcedRoll.NORMAL&&critRoll<=(attacker==null?0:attacker.getCriticalRate()));
        if(debug.getCritical()==DebugOptions.ForcedRoll.FAILURE)critical=false;
        if(!critical){processSharpOnNormalAttack(attacker,false);return 0;}
        int attackRange=executingSkill==null?BattleGrid.distance(attacker.getTile(),target.getTile()):executingSkill.getData().getRange();
        if(tryEmergencyEscape(target,attacker,attackRange)){processSharpOnNormalAttack(attacker,true);return 0;}
        double multiplier=(attacker==null?1.5:attacker.getCriticalDamageMultiplier()+(attacker.getCriticalRate()/100.0));
        int dealt=dealDamage(attacker,target,ceilToInt(raw)*multiplier,false,DamageType.DIRECT,null);processSharpOnNormalAttack(attacker,true);return dealt;
    }
    private int dealDamage(BattleUnit attacker,BattleUnit target,double raw,boolean rollHit,DamageType damageType,String cause){
        if(target==null||target.isDead())return 0;
        if(target instanceof WizardPhantom){WizardPhantom phantom=(WizardPhantom)target;consumePhantomElement(phantom,"피해");if(!phantom.isDead()){PassiveContext afterTaken=PassiveContext.damage(this,PassiveTrigger.AFTER_DAMAGE_TAKEN,attacker,target,damageType,1);afterTaken.setFinalDamage(1);dispatchPassives(target,PassiveTrigger.AFTER_DAMAGE_TAKEN,afterTaken);PassiveContext afterDealt=PassiveContext.damage(this,PassiveTrigger.AFTER_DAMAGE_DEALT,attacker,target,damageType,1);afterDealt.setFinalDamage(1);dispatchPassives(attacker,PassiveTrigger.AFTER_DAMAGE_DEALT,afterDealt);double venom=attacker==null?0:attacker.sumEffect(UnitEffectType.VENOM_COATING);if(damageType==DamageType.DIRECT&&venom>0)applyStatus(phantom,new StatusEffect("venom@"+attacker.getUnitId(),StatusType.POISON,attacker.getUnitId(),-1,venom,true,true));}return 0;}
        if(debug.isInvinciblePlayers()&&target.getTeam()==Team.PLAYER){log("디버그 무적: 피해 0");return 0;}
        if(damageType==DamageType.DIRECT&&(target.has(StatusType.INVINCIBLE_DIRECT)||(!state.isNetworkCoop()&&target.has(StatusType.DIVINE_FRAGMENT)))){log(target.getName()+"이(가) 무적으로 일반 피해를 막았습니다.");return 0;}
        boolean critical=false,evaded=false;
        if(rollHit&&damageType==DamageType.DIRECT){
            int hitRoll=random.nextInt(100)+1;double hitChance=100-target.sum(StatusType.HIT_CHANCE_REDUCTION);if(hitRoll>hitChance)return 0;
            int critRoll=random.nextInt(100)+1;
            critical=debug.getCritical()==DebugOptions.ForcedRoll.SUCCESS||(debug.getCritical()==DebugOptions.ForcedRoll.NORMAL&&critRoll<=(attacker==null?0:attacker.getCriticalRate()));
            if(debug.getCritical()==DebugOptions.ForcedRoll.FAILURE)critical=false;
            if(!critical){
                int evadeRoll=random.nextInt(100)+1;
                evaded=debug.getEvasion()==DebugOptions.ForcedRoll.SUCCESS||(debug.getEvasion()==DebugOptions.ForcedRoll.NORMAL&&evadeRoll<=target.getEvasionRate());
                if(debug.getEvasion()==DebugOptions.ForcedRoll.FAILURE)evaded=false;
            }
        }
        if(!evaded&&rollHit&&damageType==DamageType.DIRECT){int attackRange=executingSkill==null?(attacker==null?0:BattleGrid.distance(attacker.getTile(),target.getTile())):executingSkill.getData().getRange();if(tryEmergencyEscape(target,attacker,attackRange)){processSharpOnNormalAttack(attacker,critical);return 0;}}
        if(evaded){processSharpOnNormalAttack(attacker,false);return 0;}
        double roundedBase=ceilToInt(raw);
        double amount=roundedBase*(critical?(attacker==null?1.5:attacker.getCriticalDamageMultiplier()):1.0);
        double reduction=target.sum(StatusType.DAMAGE_REDUCTION);
        amount=amount*Math.max(0,1-reduction/100.0);
        if(damageType.isAffectedByDirectMitigation()){
            double increased=target.sum(StatusType.DAMAGE_TAKEN_UP);
            amount=amount*(1+increased/100.0);
        }
        PassiveContext damageContext=PassiveContext.damage(this,PassiveTrigger.BEFORE_DAMAGE_DEALT,attacker,target,damageType,amount);
        dispatchPassives(attacker,PassiveTrigger.BEFORE_DAMAGE_DEALT,damageContext);
        damageContext=PassiveContext.damage(this,PassiveTrigger.BEFORE_DAMAGE_TAKEN,attacker,target,damageType,damageContext.getDamage());
        dispatchPassives(target,PassiveTrigger.BEFORE_DAMAGE_TAKEN,damageContext);
        amount=damageContext.getDamage();
        if(damageType==DamageType.DIRECT&&target.has(StatusType.DEVOUR)){
            int nullified=ceilToInt(amount);int healed=ceilToInt(nullified*target.sum(StatusType.DEVOUR)/100.0);log(target.getName()+"이(가) 포식으로 일반 피해 "+nullified+"을 무효화했습니다.");if(healed>0)heal(target,healed,"포식");return 0;
        }
        if(damageType.isAffectedByDirectMitigation()){
            amount=Math.max(0,amount-auraReduction(target)-target.sum(StatusType.FLAT_DAMAGE_REDUCTION));
            double heavyArmor=target.sumEffect(UnitEffectType.HEAVY_ARMOR)+target.sum(StatusType.SKILL_HEAVY_ARMOR);
            double reduced=Math.min(amount,heavyArmor);
            amount=Math.max(0,amount-heavyArmor);
            if(reduced>0)log(target.getName()+" [중갑 "+format(heavyArmor)+"] 피해 "+format(reduced)+" 감소");
        }
        int rounded=ceilToInt(amount);
        int afterBarrier=damageType.isAffectedByDirectMitigation()?target.absorbWithBarrier(rounded):rounded;
        int before=target.getHp(); int dealt=target.damage(afterBarrier);
        logDamageResult(target,cause,dealt,before);
        if(target.isDead())log(target.getName()+" 사망");
        RedAltarEncounter.synchronize(this);
        PassiveContext afterTaken=PassiveContext.damage(this,PassiveTrigger.AFTER_DAMAGE_TAKEN,attacker,target,damageType,amount);afterTaken.setFinalDamage(dealt);
        dispatchPassives(target,PassiveTrigger.AFTER_DAMAGE_TAKEN,afterTaken);
        PassiveContext afterDealt=PassiveContext.damage(this,PassiveTrigger.AFTER_DAMAGE_DEALT,attacker,target,damageType,amount);afterDealt.setFinalDamage(dealt);
        dispatchPassives(attacker,PassiveTrigger.AFTER_DAMAGE_DEALT,afterDealt);
        processDamageDealtEffects(attacker,target,damageType,dealt);
        processAbyssalMarks(attacker,target,damageType,dealt);
        if(rollHit&&damageType==DamageType.DIRECT)processSharpOnNormalAttack(attacker,critical);
        evaluateOutcome(); return dealt;
    }

    private void logDamageResult(BattleUnit target,String cause,int dealt,int before){
        String damageName=cause==null?"":cause+" ";
        log(target.getName()+"에게 "+damageName+"피해 "+dealt+objectParticle(dealt,"")+" 가했습니다. HP "+before+" → "+target.getHp());
    }

    private void processDamageDealtEffects(BattleUnit attacker,BattleUnit target,DamageType damageType,int dealt){
        if(attacker==null||target==null||dealt<=0||attacker.getTeam()==target.getTeam())return;
        double lifesteal=attacker.sumEffect(UnitEffectType.LIFESTEAL);
        if(damageType==DamageType.DIRECT&&lifesteal>0)heal(attacker,ceilToInt(dealt*lifesteal/100.0));
        double venom=attacker.sumEffect(UnitEffectType.VENOM_COATING);
        if(damageType==DamageType.DIRECT&&venom>0&&!target.isDead())applyStatus(target,new StatusEffect("venom@"+attacker.getUnitId(),StatusType.POISON,attacker.getUnitId(),-1,venom,true,true));
    }

    private void processSharpOnNormalAttack(BattleUnit attacker,boolean critical){
        if(attacker==null||executingSkill==null||executingSkill.getData().getStartingGrade()!=com.pas.game.skill.data.SkillGrade.NORMAL||executingSkill.getData().getTargetType()!=TargetType.ENEMY)return;
        if(!critical){attacker.removeStatus(StatusType.SHARP);return;}
        double gain=attacker.sumEffect(UnitEffectType.SHARP_GAIN);if(gain>0)applyStatus(attacker,new StatusEffect("SHARP_"+attacker.getUnitId(),StatusType.SHARP,attacker.getUnitId(),-1,gain,true,false));
    }

    private boolean tryEmergencyEscape(BattleUnit target,BattleUnit attacker,int attackRange){
        StatusEffect ready=null;for(StatusEffect effect:target.getStatuses())if(effect.getType()==StatusType.EMERGENCY_ESCAPE_READY){ready=effect;break;}if(ready==null)return false;
        if(target.has(StatusType.MOVEMENT_DISABLED)||(target.has(StatusType.BIND)&&!target.has(StatusType.UNSTOPPABLE)))return false;
        int destination=(int)ready.getMagnitude();if(!BattleGrid.isValid(destination))return false;int from=target.getTile();target.removeStatus(StatusType.EMERGENCY_ESCAPE_READY);target.setTile(destination);log(target.getName()+" 비상 탈출: "+from+" → "+destination);onEnterTile(target,destination);applyStatus(target,new StatusEffect("EMERGENCY_EVADE_"+target.getUnitId(),StatusType.EVADE_UP,target.getUnitId(),1,50,false,true));
        return attacker!=null&&BattleGrid.distance(attacker.getTile(),target.getTile())>attackRange;
    }

    private int ceilToInt(double value){return Math.max(0,(int)Math.ceil(Math.max(0,value)));}

    private void dispatchPassives(BattleUnit owner,PassiveTrigger trigger,PassiveContext context){
        if(owner==null||passiveDispatchDepth>=MAX_PASSIVE_DISPATCH_DEPTH)return;
        passiveDispatchDepth++;
        try{for(PassiveRuntime runtime:new ArrayList<>(owner.getPassives()))if(runtime.isEnabled()&&runtime.getData().respondsTo(trigger))runtime.getData().getEffect().apply(context,owner,runtime);}finally{passiveDispatchDepth--;}
    }

    private double auraReduction(BattleUnit target){
        double result=0;
        for(BattleUnit unit:state.getUnits())if(!unit.isDead()&&unit.getTeam()==target.getTeam()&&BattleGrid.distance(unit.getTile(),target.getTile())<=1)result+=unit.sum(StatusType.GUARDIAN_AURA);
        return result;
    }
    public void addBarrier(BattleUnit target,int amount){target.addBarrier(amount);log(target.getName()+"이(가) 피해방어 "+amount+"을 얻었습니다.");}
    public void grantEffect(BattleUnit target,UnitEffect effect){if(target==null||effect==null)return;target.grantEffect(effect);String suffix=effect.getType().getValueSuffix();String gained=effect.getType().getDisplayName()+" "+format(effect.getMagnitude())+suffix;String particle=objectParticle(effect.getMagnitude(),suffix);if(effect.getSourceDisplayName().isEmpty())log(target.getName()+"이(가) "+gained+particle+" 얻었습니다.");else log(target.getName()+"이(가) "+effect.getSourceDisplayName()+" 효과로 "+gained+particle+" 얻었습니다.");}
    public void grantStackingEffect(BattleUnit target,UnitEffect effect){if(target==null||effect==null)return;target.stackEffect(effect);String suffix=effect.getType().getValueSuffix();log(target.getName()+"이(가) "+effect.getType().getDisplayName()+" "+format(effect.getMagnitude())+suffix+objectParticle(effect.getMagnitude(),suffix)+" 추가로 얻었습니다.");}
    public void heal(BattleUnit target,int amount){int before=target.getHp();int healed=target.heal(amount);log(target.getName()+" 회복 "+healed+" ("+before+" → "+target.getHp()+")");}
    public void heal(BattleUnit target,int amount,String sourceDisplayName){int before=target.getHp();int healed=target.heal(amount);if(sourceDisplayName==null||sourceDisplayName.isEmpty())log(target.getName()+" 회복 "+healed+" ("+before+" → "+target.getHp()+")");else log(target.getName()+"이(가) "+sourceDisplayName+" 효과로 HP "+healed+"을 회복했습니다. ("+before+" → "+target.getHp()+")");}
    public void applyStatus(BattleUnit target,StatusEffect effect){
        if(executingSkill!=null)effect.identifyDisplaySource(executingSkill.getData().getName());
        if(target instanceof WizardPhantom){if(target.isDead()||state.find(target.getUnitId())==null)return;target.addStatus(effect);consumePhantomElement((WizardPhantom)target,com.pas.game.status.StatusDisplay.label(effect.getType()));return;}
        String statusName=com.pas.game.status.StatusDisplay.label(effect.getType());
        if((effect.getType()==StatusType.DAZED||effect.getType()==StatusType.STIFF||effect.getType()==StatusType.BIND)&&hasUnstoppable(target)){log(target.getName()+": 저지불가로 "+statusName+" 효과를 막았습니다.");return;}
        target.addStatus(effect);String duration=effect.isPermanent()?"":" ("+effect.getRemainingTurns()+"턴)";if(effect.getType()==StatusType.FIRE){String source=effect.getSourceDisplayName().isEmpty()?"":effect.getSourceDisplayName()+" 효과로 ";log(target.getName()+"에게 "+source+"화염 "+format(effect.getMagnitude())+objectParticle(effect.getMagnitude(),"")+" 부여했습니다."+duration);}else if(effect.getSourceDisplayName().isEmpty())log(target.getName()+"에게 "+statusName+" 효과가 적용되었습니다."+duration);else log(target.getName()+"이(가) "+effect.getSourceDisplayName()+" 효과로 "+com.pas.game.status.StatusDisplay.gainText(effect)+objectParticle(effect.getMagnitude(),effect.getType().getValueSuffix())+" 얻었습니다."+duration);
    }
    private String objectParticle(double value,String suffix){if(suffix!=null&&!suffix.isEmpty())return "를";String digits=format(Math.abs(value)).replace(".","");char last=digits.charAt(digits.length()-1);return last=='0'||last=='1'||last=='3'||last=='6'||last=='7'||last=='8'?"을":"를";}
    public void reduceStatusMagnitude(BattleUnit target,StatusType type,double factor){for(StatusEffect s:target.getStatuses())if(s.getType()==type)s.scaleMagnitude(factor);log(target.getName()+"의 "+com.pas.game.status.StatusDisplay.label(type)+" 수치가 감소했습니다.");}
    public void grantSkillAction(BattleUnit unit){if(activeUnit()==unit){state.getTurn().addSkillAction();log(unit.getName()+" 스킬 사용 기회 +1");}}
    public void installBindingTrap(BattleUnit owner,Integer tile,int bindTurns){if(owner==null||tile==null||!BattleGrid.isValid(tile))return;BattleTrap trap=new BattleTrap("TRAP_"+owner.getUnitId()+"_"+state.getRound()+"_"+state.getTraps().size(),owner.getUnitId(),owner.getTeam(),tile,Math.max(1,bindTurns));state.addTrap(trap);log(owner.getName()+"이(가) "+tile+"번 칸에 고정덫을 설치했습니다.");}
    public void armEmergencyEscape(BattleUnit owner,Integer tile,double evasionBonus){if(owner==null||tile==null||!BattleGrid.isValid(tile))return;owner.removeStatus(StatusType.EMERGENCY_ESCAPE_READY);applyStatus(owner,new StatusEffect("EMERGENCY_READY_"+owner.getUnitId(),StatusType.EMERGENCY_ESCAPE_READY,owner.getUnitId(),-1,tile,false,false));log(owner.getName()+"이(가) 비상 탈출 지점을 "+tile+"번 칸으로 지정했습니다.");}
    public void installDecoy(BattleUnit owner,Integer tile,int ownerTurns){if(owner==null||tile==null||!BattleGrid.isValid(tile))return;BattleDecoy decoy=new BattleDecoy("DECOY_"+owner.getUnitId()+"_"+state.getRound()+"_"+state.getDecoys().size(),owner.getUnitId(),owner.getTeam(),tile,ownerTurns);state.addDecoy(decoy);log(owner.getName()+"이(가) "+tile+"번 칸에 미끼를 설치했습니다.");}
    private void tickOwnedDecoys(BattleUnit owner){for(BattleDecoy decoy:new ArrayList<>(state.getDecoys()))if(decoy.getOwnerUnitId().equals(owner.getUnitId())&&decoy.tickOwnerTurn()){state.removeDecoy(decoy);log(owner.getName()+"의 미끼가 지속시간 종료로 사라졌습니다.");}}
    public Integer preferredDecoyTile(BattleUnit unit,int attackRange){for(BattleUnit enemy:enemiesOf(unit))if(BattleGrid.distance(unit.getTile(),enemy.getTile())<=attackRange)return null;List<BattleDecoy> candidates=new ArrayList<>();for(BattleDecoy decoy:state.getDecoys())if(decoy.getOwnerTeam()!=unit.getTeam()&&BattleGrid.distance(unit.getTile(),decoy.getTile())<=1)candidates.add(decoy);candidates.sort(Comparator.comparing(BattleDecoy::getId));return candidates.isEmpty()?null:candidates.get(0).getTile();}
    public void moveBySkill(BattleUnit actor,Integer destination){
        if(actor==null||destination==null||!BattleGrid.isValid(destination))return;
        if(actor.has(StatusType.MOVEMENT_DISABLED)||(actor.has(StatusType.BIND)&&!hasUnstoppable(actor))){log(actor.getName()+"은(는) 이동 불가 상태라 스킬 이동에 실패했습니다.");return;}
        while(actor.getTile()!=destination&&!actor.isDead()){
            int from=actor.getTile(),x=BattleGrid.column(from),y=BattleGrid.row(from),targetX=BattleGrid.column(destination),targetY=BattleGrid.row(destination);
            if(x!=targetX)x+=Integer.compare(targetX,x);else y+=Integer.compare(targetY,y);
            int next=BattleGrid.tile(x,y);actor.setTile(next);log(actor.getName()+" 스킬 이동: "+from+" → "+next);onEnterTile(actor,next);
            if(actor.has(StatusType.MOVEMENT_DISABLED)||(actor.has(StatusType.BIND)&&!hasUnstoppable(actor))){if(next!=destination)log(actor.getName()+"의 스킬 이동이 "+next+"번 칸에서 중단되었습니다.");break;}
        }
    }
    public void dealPiercingLine(BattleUnit caster,Integer directionTile,double attackPercent){
        if(caster==null||directionTile==null)return;int dx=Integer.compare(BattleGrid.column(directionTile),BattleGrid.column(caster.getTile()));int dy=Integer.compare(BattleGrid.row(directionTile),BattleGrid.row(caster.getTile()));if(dx!=0&&dy!=0)return;
        int x=BattleGrid.column(caster.getTile()),y=BattleGrid.row(caster.getTile());
        for(int step=1;step<=2;step++){int nx=x+dx*step,ny=y+dy*step;if(nx<0||nx>=BattleGrid.COLUMNS||ny<0||ny>=BattleGrid.ROWS)break;for(BattleUnit unit:new ArrayList<>(state.atTile(BattleGrid.tile(nx,ny),true)))if(unit.getTeam()!=caster.getTeam())dealDamage(caster,unit,caster.getAttack()*attackPercent/100.0,true,false);}
    }
    public List<BattleUnit> alliesInRange(BattleUnit caster,int range){List<BattleUnit> result=new ArrayList<>();if(caster==null)return result;for(BattleUnit unit:state.living(caster.getTeam()))if(unit.canReceiveSupport()&&BattleGrid.distance(caster.getTile(),unit.getTile())<=range)result.add(unit);return result;}

    public void gainPurification(BattleUnit owner,int amount){
        if(owner==null||owner.isDead()||amount<=0)return;
        BattleUnit punished=findDivinePunishmentTarget(owner);
        if(punished!=null){applyDivinePunishmentReduction(owner,punished,amount);return;}
        StatusEffect current=findStatus(owner,StatusType.PURIFICATION_STACK,null);
        if(current==null)owner.addStatus(new StatusEffect("PURIFICATION@"+owner.getUnitId(),StatusType.PURIFICATION_STACK,owner.getUnitId(),-1,amount,true,false));
        else current.addMagnitude(amount);
        log(owner.getName()+"이(가) 정화 "+amount+"을 얻었습니다. (현재 "+format(owner.sum(StatusType.PURIFICATION_STACK))+")");
    }

    public void amplifyPurification(BattleUnit owner,double percent){
        StatusEffect current=findStatus(owner,StatusType.PURIFICATION_STACK,null);double before=current==null?0:current.getMagnitude();
        if(current!=null)current.addMagnitude(Math.max(0,ceilToInt(before*percent/100.0)-before));
        log(owner.getName()+"의 정화가 "+format(before)+" → "+format(owner.sum(StatusType.PURIFICATION_STACK))+"로 증폭되었습니다.");
    }

    public void tryTransferPurification(BattleUnit owner,BattleUnit target){
        if(owner==null||target==null||target.isDead()||owner.getTeam()==target.getTeam())return;
        double amount=owner.sum(StatusType.PURIFICATION_STACK);if(amount<target.getHp()||amount<=0)return;
        owner.removeStatus(StatusType.PURIFICATION_STACK);
        applyStatus(target,new StatusEffect("PURIFICATION_EXECUTION@"+owner.getUnitId(),StatusType.PURIFICATION_EXECUTION,owner.getUnitId(),-1,amount,false,false));
        log(owner.getName()+"의 정화 "+format(amount)+"이(가) "+target.getName()+"에게 전부 옮겨졌습니다.");
    }

    public void castDivinePunishment(BattleUnit owner,BattleUnit target){
        if(owner==null||target==null||owner.getTeam()==target.getTeam())return;
        for(BattleUnit unit:state.getUnits())unit.removeStatusFromSource(StatusType.DIVINE_PUNISHMENT,owner.getUnitId());
        target.addStatus(new StatusEffect("DIVINE_PUNISHMENT@"+owner.getUnitId(),StatusType.DIVINE_PUNISHMENT,owner.getUnitId(),-1,0,false,false));
        int held=ceilToInt(owner.sum(StatusType.PURIFICATION_STACK));owner.removeStatus(StatusType.PURIFICATION_STACK);
        log(owner.getName()+"이(가) "+target.getName()+"에게 신벌을 지정했습니다.");
        if(held>0)applyDivinePunishmentReduction(owner,target,held);
    }

    private BattleUnit findDivinePunishmentTarget(BattleUnit owner){for(BattleUnit unit:state.getUnits())if(!unit.isDead()&&findStatus(unit,StatusType.DIVINE_PUNISHMENT,owner.getUnitId())!=null)return unit;return null;}
    private void applyDivinePunishmentReduction(BattleUnit owner,BattleUnit target,int amount){
        int beforeHp=target.getHp(),beforeMax=target.getMaxHp();target.damage(amount);target.reduceMaxHpPermanently(amount);
        log(target.getName()+" 신벌: 현재 HP "+beforeHp+" → "+target.getHp()+", 최대 HP "+beforeMax+" → "+target.getMaxHp());
        if(target.isDead())log(target.getName()+" 사망");evaluateOutcome();
    }

    private StatusEffect findStatus(BattleUnit unit,StatusType type,String source){for(StatusEffect status:unit.getStatuses())if(status.getType()==type&&(source==null||source.equals(status.getSourceUnitId())))return status;return null;}

    private void processPurificationExecution(BattleUnit unit){StatusEffect execution=findStatus(unit,StatusType.PURIFICATION_EXECUTION,null);if(execution==null||unit.isDead())return;unit.kill();log(unit.getName()+"이(가) 정화되어 즉사했습니다.");evaluateOutcome();}

    public void applyMaxHpBuff(BattleUnit caster,BattleUnit target,int amount,int turns,String id,boolean dispellable){
        if(target==null||amount<=0)return;StatusEffect effect=new StatusEffect(id,StatusType.MAX_HP_UP,caster==null?null:caster.getUnitId(),turns,amount,true,dispellable);if(executingSkill!=null)effect.identifyDisplaySource(executingSkill.getData().getName());target.addStatus(effect);target.increaseCurrentHpWithMax(amount);log(target.getName()+" 최대 HP 및 현재 HP +"+amount);
    }

    public void removeHighestStackDispellableDebuff(BattleUnit target){
        StatusEffect selected=null;double score=-1;for(StatusEffect status:target.getStatuses())if(status.isDispellable()&&com.pas.game.status.StatusDisplay.isDebuff(status.getType())){double candidate=Math.max(status.getStackCount(),status.getMagnitude());if(candidate>score){score=candidate;selected=status;}}
        if(selected!=null){String name=com.pas.game.status.StatusDisplay.label(selected.getType());target.removeStatusEffect(selected);log(target.getName()+"의 "+name+" 효과가 제거되었습니다.");}
    }

    public void removeAllDispellableBenefits(BattleUnit target){
        List<StatusEffect> removed=new ArrayList<>();for(StatusEffect status:target.getStatuses())if(status.isDispellable()&&!com.pas.game.status.StatusDisplay.isDebuff(status.getType()))removed.add(status);
        if(!removed.isEmpty()){for(StatusEffect status:removed)onWizardStatusRemoved(target,status);target.removeStatuses(removed);log(target.getName()+"의 해제 가능한 이로운 효과가 모두 제거되었습니다.");}
    }

    public void reduceAllCooldowns(BattleUnit target,int amount){for(SkillRuntime skill:target.getEquippedSkills())if(skill!=null)skill.reduceCooldown(amount);log(target.getName()+"의 모든 스킬 쿨타임이 "+amount+"턴 감소했습니다.");}

    public void installSanctuary(BattleUnit owner,int healing,int duration){BattleSanctuary sanctuary=new BattleSanctuary("SANCTUARY_"+owner.getUnitId()+"_"+state.getRound()+"_"+state.getSanctuaries().size(),owner.getUnitId(),owner.getTeam(),owner.getTile(),1,healing,duration,state.getRound());state.addSanctuary(sanctuary);log(owner.getName()+"이(가) 현재 위치에 성역을 설치했습니다. ("+duration+"턴)");}
    private void tickSanctuariesAtPlayerTurn(BattleUnit unit){if(unit.getTeam()!=Team.PLAYER)return;for(BattleSanctuary sanctuary:new ArrayList<>(state.getSanctuaries()))if(sanctuary.tickPlayerTurn(state.getRound())){state.removeSanctuary(sanctuary);log("성역의 지속시간이 끝났습니다.");}}
    private void processSanctuaryTurnEnd(BattleUnit unit){for(BattleSanctuary sanctuary:new ArrayList<>(state.getSanctuaries()))if(sanctuary.getOwnerTeam()==unit.getTeam()&&BattleGrid.distance(sanctuary.getTile(),unit.getTile())<=sanctuary.getRange()){heal(unit,sanctuary.getHealing(),"성역");removeHighestStackDispellableDebuff(unit);}}

    public void activateDivineFragment(BattleUnit owner,int turns){owner.removeStatus(StatusType.DIVINE_FRAGMENT);owner.addStatus(new StatusEffect("DIVINE_FRAGMENT@"+owner.getUnitId(),StatusType.DIVINE_FRAGMENT,owner.getUnitId(),turns,0,false,false));log(owner.getName()+"이(가) 신성의 파편을 "+turns+"턴 동안 발동했습니다.");}
    private void processDivineFragmentTurnStart(BattleUnit unit){
        if(state.isNetworkCoop())for(BattleUnit caster:state.living(unit.getTeam()))if(caster.has(StatusType.DIVINE_FRAGMENT)&&BattleGrid.distance(caster.getTile(),unit.getTile())<=2){heal(unit,300,"신성의 파편");break;}
        if(unit.has(StatusType.DIVINE_FRAGMENT))unit.removeStatuses(unit.tickStatus(StatusType.DIVINE_FRAGMENT));
    }
    public void triggerClericJudgment(BattleUnit owner){for(BattleUnit enemy:enemiesOf(owner))if(BattleGrid.distance(owner.getTile(),enemy.getTile())<=1)dealDamage(owner,enemy,50,false,DamageType.TRUE_DAMAGE,"심판");for(BattleUnit ally:alliesInRange(owner,1))heal(ally,20,"심판");}

    public void activateWizardMana(BattleUnit owner,int period){if(owner!=null&&period>0&&owner.getTurnsStarted()%period==0){owner.removeStatus(StatusType.MANA_ACTIVE);owner.addStatus(new StatusEffect("MANA_ACTIVE@"+owner.getUnitId(),StatusType.MANA_ACTIVE,owner.getUnitId(),-1,0,false,false));log(owner.getName()+"의 마력이 활성화되었습니다.");}}
    private boolean wizardMana(BattleUnit owner){return owner!=null&&owner.has(StatusType.MANA_ACTIVE);}

    public void castWizardManaDischarge(BattleUnit caster,BattleUnit target,double coefficient){dealDamage(caster,target,caster.getAttack()*coefficient/100.0,true,DamageType.DIRECT);int count=wizardMana(caster)?2:1;addManaDischargeCount(caster,count);}
    private void addManaDischargeCount(BattleUnit caster,int count){WizardBattleState ws=state.getWizardState();int total=ws.getDischargeUses(caster.getUnitId())+count;if(total>=4){int gain=count>=2?4:2;total=0;applyStatus(caster,new StatusEffect("MANA_DISCHARGE_ATTACK@"+caster.getUnitId(),StatusType.ATTACK_FLAT_UP,caster.getUnitId(),-1,gain,true,true));log(caster.getName()+"의 마력 방출 누적이 완성되어 공격력 "+gain+"을 얻었습니다.");}ws.setDischargeUses(caster.getUnitId(),total);}
    public void castWizardGluttonousHand(BattleUnit caster,BattleUnit target,double coefficient){int dealt=dealDamage(caster,target,caster.getAttack()*coefficient/100.0,true,DamageType.DIRECT);wizardLastSpecialDamage=dealt;double factor=wizardMana(caster)?2:1;heal(caster,ceilToInt(dealt*.10*factor+caster.getAttack()*.20*factor),"탐식의 손");}
    public void castWizardNetherRepulsion(BattleUnit caster,BattleUnit target,double coefficient){dealDamage(caster,target,caster.getAttack()*coefficient/100.0,true,DamageType.DIRECT);double barrierPercent=100+Math.max(0,Math.round((coefficient-80)/20.0))*50;int barrier=ceilToInt(caster.getDefense()*barrierPercent/100.0)*(wizardMana(caster)?2:1);addBarrier(caster,barrier);}
    public void castWizardMagicWard(BattleUnit caster,double coefficient){addBarrier(caster,ceilToInt(caster.getDefense()*coefficient/100.0));double armor=1+Math.max(0,Math.round((coefficient-200)/50.0));if(wizardMana(caster))armor*=2;applyStatus(caster,new StatusEffect("MAGIC_WARD_ARMOR@"+caster.getUnitId(),StatusType.SKILL_HEAVY_ARMOR,caster.getUnitId(),-1,armor,true,true));}
    public void castWizardChaosDistortion(BattleUnit caster,double coefficient){int reduction=ceilToInt(caster.getDefense()*coefficient/100.0);applyStatus(caster,new StatusEffect("CHAOS_HIT@"+caster.getUnitId(),StatusType.HIT_CHANCE_REDUCTION,caster.getUnitId(),-1,reduction,false,true));double crit=wizardMana(caster)?10:5;applyStatus(caster,new StatusEffect("CHAOS_CRIT@"+caster.getUnitId(),StatusType.CRIT_RATE_UP,caster.getUnitId(),-1,crit,true,true));}
    public void castWizardDevour(BattleUnit caster,double healPercent){if(wizardMana(caster))healPercent*=2;applyStatus(caster,new StatusEffect("DEVOUR@"+caster.getUnitId(),StatusType.DEVOUR,caster.getUnitId(),-1,healPercent,false,true));}
    public void castWizardManaDisruption(BattleUnit caster,BattleUnit target){if(target==null)return;SkillRuntime selected=target.findSkill(executingOptionId);if(selected==null||selected.getData().getCooldown()<=0)return;int amount=wizardMana(caster)?4:2;selected.increaseCooldown(amount);log(target.getName()+"의 "+selected.getData().getName()+" 쿨타임이 "+amount+"턴 증가했습니다.");}
    public void castWizardPhantom(BattleUnit caster,Integer tile){if(tile==null)return;int hits=wizardMana(caster)?2:1;WizardPhantom phantom=new WizardPhantom("PHANTOM@"+caster.getUnitId()+"#"+(++skillUseSequence),caster.getUnitId(),caster.getName()+"의 허수체",caster.getTeam(),tile,3,hits);state.addUnit(phantom);log(tile+"번 칸에 허수체가 생성되었습니다. ("+hits+"회)");}
    public void castWizardManaPayment(BattleUnit caster){int cost=ceilToInt(caster.getMaxHp()*.10);int before=caster.getHp();caster.damage(cost);log(caster.getName()+"이(가) 마력 대납으로 HP "+cost+"을 지불했습니다. ("+before+" → "+caster.getHp()+")");int amount=wizardMana(caster)?4:2;reduceOtherCooldowns(caster,amount);if(caster.isDead())log(caster.getName()+" 사망");evaluateOutcome();}
    private void reduceOtherCooldowns(BattleUnit caster,int amount){for(SkillRuntime runtime:caster.getEquippedSkills())if(runtime!=null&&runtime!=executingSkill)runtime.reduceCooldown(amount);log(caster.getName()+"의 다른 모든 스킬 쿨타임이 "+amount+"턴 감소했습니다.");}
    public void castWizardAbyssalMark(BattleUnit caster,BattleUnit target,double healPercent){if(wizardMana(caster))healPercent*=2;StatusEffect mark=new StatusEffect("ABYSSAL_MARK@"+caster.getUnitId()+"#"+(++skillUseSequence),StatusType.ABYSSAL_MARK,caster.getUnitId(),3,healPercent,false,true);applyStatus(target,mark);}
    public void castWizardDimensionalDrift(BattleUnit caster){WizardBattleState ws=state.getWizardState();ws.getDrifts().put(caster.getUnitId(),new WizardBattleState.Drift(caster.getUnitId(),caster.getTile(),wizardMana(caster)));caster.setOnField(false);caster.addStatus(new StatusEffect("DRIFT@"+caster.getUnitId(),StatusType.DIMENSIONAL_DRIFT,caster.getUnitId(),-1,0,false,true));log(caster.getName()+"이(가) 차원 표류로 전장을 이탈했습니다.");}
    public void castWizardOtherworldPower(BattleUnit caster,double percent){int gain=ceilToInt(caster.getAttack()*percent/100.0)*(wizardMana(caster)?2:1);applyStatus(caster,new StatusEffect("OTHERWORLD_POWER@"+caster.getUnitId()+"#"+(++skillUseSequence),StatusType.ATTACK_FLAT_UP,caster.getUnitId()+"@otherworld#"+skillUseSequence,2,gain,false,true));}
    public void castWizardHungryStar(BattleUnit caster,Integer tile,double coefficient){if(tile==null)return;WizardBattleState.Star star=new WizardBattleState.Star("HUNGRY_STAR@"+caster.getUnitId()+"#"+(++skillUseSequence),caster.getUnitId(),caster.getTeam(),tile,caster.getAttack(),caster.getCriticalRate(),caster.getCriticalDamageMultiplier(),coefficient);state.getWizardState().getStars().add(star);log(tile+"번 칸에 굶주린 별이 설치되었습니다.");}
    public void castWizardPastEcho(BattleUnit caster){state.getWizardState().getEchoes().put(caster.getUnitId(),new WizardBattleState.Echo(caster.getUnitId(),caster.getTile(),caster.getHp()));caster.removeStatus(StatusType.PAST_ECHO);caster.addStatus(new StatusEffect("PAST_ECHO@"+caster.getUnitId(),StatusType.PAST_ECHO,caster.getUnitId(),3,0,false,true));log(caster.getName()+"의 현재 위치와 HP가 과거의 잔상에 저장되었습니다.");}
    public void castWizardSpellTheft(BattleUnit caster,BattleUnit target){stealWizardBenefits(caster,target,wizardMana(caster)?2:1);}
    private void stealWizardBenefits(BattleUnit caster,BattleUnit target,int count){for(int i=0;i<count;i++){StatusEffect selected=null;for(StatusEffect status:target.getStatuses())if(status.isDispellable()&&!com.pas.game.status.StatusDisplay.isDebuff(status.getType())&&(selected==null||status.getApplicationOrder()>selected.getApplicationOrder()))selected=status;if(selected==null){if(i==0)log(target.getName()+"에게 탈취할 이로운 효과가 없습니다.");break;}onWizardStatusRemoved(target,selected);target.removeStatusEffect(selected);StatusEffect copy=selected.copy("STOLEN@"+caster.getUnitId()+"#"+(++skillUseSequence),caster.getUnitId());caster.addStatus(copy);onWizardStatusCopied(caster,copy);if(copy.getType()==StatusType.MAX_HP_UP)caster.increaseCurrentHpWithMax(ceilToInt(copy.getMagnitude()*copy.getStackCount()));log(caster.getName()+"이(가) "+target.getName()+"의 "+com.pas.game.status.StatusDisplay.label(copy.getType())+" 효과를 탈취했습니다.");}}
    public void castWizardExistenceLoan(BattleUnit caster){state.getWizardState().addLoan(caster.getUnitId(),1);caster.addStatus(new StatusEffect("EXISTENCE_LOAN@"+caster.getUnitId(),StatusType.EXISTENCE_LOAN,caster.getUnitId(),3,1,true,true));log(caster.getName()+"이(가) 존재 대여 1스택을 얻었습니다.");}
    public void castWizardInfiniteFragment(BattleUnit caster,double percent){int gain=ceilToInt(caster.getAttack()*percent/100.0);applyStatus(caster,new StatusEffect("INFINITE_FRAGMENT@"+caster.getUnitId()+"#"+(++skillUseSequence),StatusType.ATTACK_FLAT_UP,caster.getUnitId()+"@infinite#"+skillUseSequence,-1,gain,false,true));}
    public void castWizardDoubleCast(BattleUnit caster){state.getWizardState().addDoubleCast(caster.getUnitId(),1);caster.addStatus(new StatusEffect("DOUBLE_CAST@"+caster.getUnitId(),StatusType.DOUBLE_CAST,caster.getUnitId(),-1,1,true,true));log(caster.getName()+"이(가) 이중 영창 1스택을 얻었습니다.");}
    public void castWizardMeteor(BattleUnit caster,Integer tile,double coefficient){if(tile==null)return;int damage=ceilToInt(caster.getAttack()*coefficient/100.0);for(BattleUnit unit:new ArrayList<>(state.getUnits()))if(unit.isOnField()&&!unit.isDead()&&BattleGrid.distance(tile,unit.getTile())<=1)dealDamage(caster,unit,damage,false,DamageType.TRUE_DAMAGE,"유성우");}
    public void castWizardOtherworldGate(BattleUnit caster,double coefficient){int damage=ceilToInt(caster.getAttack()*coefficient/100.0);WizardBattleState.Gate gate=new WizardBattleState.Gate("OTHERWORLD_GATE@"+caster.getUnitId()+"#"+(++skillUseSequence),caster.getUnitId(),caster.getTeam(),caster.getTile(),damage);state.getWizardState().getGates().add(gate);log(caster.getTile()+"번 칸에 이계의 문이 설치되었습니다. (피해 "+damage+")");}

    public List<String> wizardOptionIds(BattleUnit caster,SkillRuntime skill,BattleUnit target){List<String> ids=new ArrayList<>();if(skill==null)return ids;if("mana_disruption".equals(skill.getData().getId())&&target!=null){for(SkillRuntime runtime:target.getEquippedSkills())if(runtime!=null&&runtime.getData().getCooldown()>0)ids.add(runtime.getData().getId());return ids;}WizardBattleState.Loan loan=caster.has(StatusType.EXISTENCE_LOAN)?state.getWizardState().getLoan(caster.getUnitId()):null;if(loan!=null&&!skill.getData().isUltimate()&&hasLoanSecondary(skill.getData().getId()))ids.add("secondary");return ids;}
    public List<String> wizardOptionLabels(BattleUnit caster,SkillRuntime skill,BattleUnit target){List<String> labels=new ArrayList<>();for(String id:wizardOptionIds(caster,skill,target)){if("secondary".equals(id))labels.add("부가효과 추가 적용 · "+loanSecondaryLabel(skill.getData().getId()));else{SkillRuntime runtime=target.findSkill(id);labels.add(runtime.getData().getName()+" · 현재 "+runtime.getCooldownRemaining()+"턴 / 기본 "+runtime.getData().getCooldown()+"턴");}}return labels;}
    private String validateWizardOption(BattleUnit caster,SkillRuntime skill,BattleUnit target,String option){List<String> ids=wizardOptionIds(caster,skill,target);if(ids.isEmpty())return "mana_disruption".equals(skill.getData().getId())?"쿨타임이 있는 대상 스킬이 없습니다.":null;if(option==null||!ids.contains(option))return "적용할 스킬 또는 부가효과를 선택하세요.";return null;}
    private boolean hasLoanSecondary(String id){switch(id){case "mana_discharge":case "gluttonous_hand":case "nether_repulsion":case "magic_ward":case "chaos_distortion":case "devour":case "mana_disruption":case "phantom_body":case "mana_payment":case "abyssal_mark":case "dimensional_drift":case "otherworld_power":case "hungry_star":case "spell_theft":return true;default:return false;}}
    private String loanSecondaryLabel(String id){switch(id){case "mana_discharge":return "마력 방출 누적 +1";case "gluttonous_hand":return "회복 1회";case "nether_repulsion":return "피해방어 1회";case "magic_ward":return "중갑 추가";case "chaos_distortion":return "치명타율 추가";case "devour":return "포식 회복 비율 추가";case "mana_disruption":return "쿨타임 추가 증가";case "phantom_body":return "허수체 내구 +1";case "mana_payment":return "쿨타임 추가 감소";case "abyssal_mark":return "낙인 회복 비율 추가";case "dimensional_drift":return "인접 칸 복귀 선택";case "otherworld_power":return "공격력 증가 추가";case "hungry_star":return "턴 종료 공격 추가";case "spell_theft":return "효과 추가 탈취";default:return "부가효과";}}

    private void applyExistenceLoanCopy(BattleUnit caster,SkillRuntime skill,BattleUnit target,Integer tile,String option){WizardBattleState.Loan loan=caster.has(StatusType.EXISTENCE_LOAN)?state.getWizardState().getLoan(caster.getUnitId()):null;String skillId=skill.getData().getId();boolean selected="secondary".equals(option)||"mana_disruption".equals(skillId);if(loan==null||!selected||skill.getData().isUltimate())return;for(int i=0;i<loan.stacks;i++){String id=skillId;double v=skill.currentValue();if("mana_discharge".equals(id))addManaDischargeCount(caster,1);else if("gluttonous_hand".equals(id))heal(caster,ceilToInt(wizardLastSpecialDamage*.10+caster.getAttack()*.20),"존재 대여 · 탐식의 손");else if("nether_repulsion".equals(id)){double p=100+Math.max(0,Math.round((v-80)/20.0))*50;addBarrier(caster,ceilToInt(caster.getDefense()*p/100.0));}else if("magic_ward".equals(id))applyStatus(caster,new StatusEffect("MAGIC_WARD_ARMOR@"+caster.getUnitId(),StatusType.SKILL_HEAVY_ARMOR,caster.getUnitId(),-1,1+Math.max(0,Math.round((v-200)/50.0)),true,true));else if("chaos_distortion".equals(id))applyStatus(caster,new StatusEffect("CHAOS_CRIT@"+caster.getUnitId(),StatusType.CRIT_RATE_UP,caster.getUnitId(),-1,5,true,true));else if("devour".equals(id)){StatusEffect s=findStatus(caster,StatusType.DEVOUR,null);if(s!=null)s.addMagnitude(v);}else if("mana_disruption".equals(id)){SkillRuntime r=target==null?null:target.findSkill(executingOptionId);if(r!=null)r.increaseCooldown(2);}else if("phantom_body".equals(id)){for(int n=state.getUnits().size()-1;n>=0;n--){BattleUnit u=state.getUnits().get(n);if(u instanceof WizardPhantom&&((WizardPhantom)u).getOwnerId().equals(caster.getUnitId())){((WizardPhantom)u).addHits(1);break;}}}else if("mana_payment".equals(id))reduceOtherCooldowns(caster,2);else if("abyssal_mark".equals(id)){StatusEffect newest=null;for(StatusEffect s:target.getStatuses())if(s.getType()==StatusType.ABYSSAL_MARK&&s.getSourceUnitId().equals(caster.getUnitId())&&(newest==null||s.getApplicationOrder()>newest.getApplicationOrder()))newest=s;if(newest!=null)newest.addMagnitude(v);}else if("dimensional_drift".equals(id)){WizardBattleState.Drift d=state.getWizardState().getDrifts().get(caster.getUnitId());if(d!=null)state.getWizardState().getDrifts().put(caster.getUnitId(),new WizardBattleState.Drift(d.ownerId,d.originTile,true));}else if("otherworld_power".equals(id))castWizardOtherworldPower(caster,v);else if("hungry_star".equals(id)){for(int n=state.getWizardState().getStars().size()-1;n>=0;n--){WizardBattleState.Star star=state.getWizardState().getStars().get(n);if(star.ownerId.equals(caster.getUnitId())){star.extraAttacksPerTick++;break;}}}else if("spell_theft".equals(id))stealWizardBenefits(caster,target,1);}}

    private void onWizardStatusRemoved(BattleUnit owner,StatusEffect status){if(status.getType()==StatusType.DOUBLE_CAST)state.getWizardState().consumeDoubleCast(owner.getUnitId());else if(status.getType()==StatusType.EXISTENCE_LOAN)state.getWizardState().removeLoan(owner.getUnitId());else if(status.getType()==StatusType.PAST_ECHO)state.getWizardState().getEchoes().remove(owner.getUnitId());}
    private void onWizardStatusCopied(BattleUnit owner,StatusEffect status){int amount=ceilToInt(status.getMagnitude()*status.getStackCount());if(status.getType()==StatusType.DOUBLE_CAST)state.getWizardState().addDoubleCast(owner.getUnitId(),amount);else if(status.getType()==StatusType.EXISTENCE_LOAN)state.getWizardState().addLoan(owner.getUnitId(),amount);}

    private void processWizardPreTurnStart(BattleUnit owner){WizardBattleState ws=state.getWizardState();WizardBattleState.Echo echo=ws.getEchoes().get(owner.getUnitId());if(echo!=null){owner.removeStatuses(owner.tickStatus(StatusType.PAST_ECHO));if(--echo.remainingStarts<=0){if(!owner.isDead()){owner.setTile(echo.tile);owner.setHpForDebug(echo.hp);log(owner.getName()+"이(가) 과거의 잔상으로 위치와 HP를 복원했습니다.");}ws.getEchoes().remove(owner.getUnitId());owner.removeStatus(StatusType.PAST_ECHO);}}for(BattleUnit u:new ArrayList<>(state.getUnits()))if(u instanceof WizardPhantom&&((WizardPhantom)u).getOwnerId().equals(owner.getUnitId())&&((WizardPhantom)u).tickOwnerTurn()){state.removeUnit(u);log(u.getName()+"가 지속시간 종료로 사라졌습니다.");}for(BattleUnit unit:state.getUnits())for(StatusEffect status:new ArrayList<>(unit.getStatuses()))if(status.getType()==StatusType.ABYSSAL_MARK&&owner.getUnitId().equals(status.getSourceUnitId())&&status.tick())unit.removeStatusEffect(status);WizardBattleState.Loan loan=ws.getLoan(owner.getUnitId());if(loan!=null){owner.removeStatuses(owner.tickStatus(StatusType.EXISTENCE_LOAN));if(--loan.remainingStarts<=0){ws.removeLoan(owner.getUnitId());owner.removeStatus(StatusType.EXISTENCE_LOAN);}}owner.removeStatus(StatusType.HIT_CHANCE_REDUCTION);owner.removeStatus(StatusType.DEVOUR);}

    private void processHungryStars(BattleUnit owner){for(WizardBattleState.Star star:new ArrayList<>(state.getWizardState().getStars()))if(star.ownerId.equals(owner.getUnitId())){int attacks=1+(wizardMana(owner)?1:0)+star.extraAttacksPerTick;for(int i=0;i<attacks;i++){BattleUnit target=chooseStarTarget(star);if(target!=null)dealStarDamage(star,target);}if(--star.attacksRemaining<=0){state.getWizardState().getStars().remove(star);log("굶주린 별이 모든 공격을 마치고 사라졌습니다.");}}}
    private void processPhantomTurnEnd(BattleUnit owner){for(BattleUnit unit:new ArrayList<>(state.getUnits()))if(unit instanceof WizardPhantom&&((WizardPhantom)unit).getOwnerId().equals(owner.getUnitId())){WizardPhantom phantom=(WizardPhantom)unit;List<StatusEffect> consumed=new ArrayList<>();for(StatusEffect status:new ArrayList<>(phantom.getStatuses())){if(status.getType()==StatusType.FIRE||status.getType()==StatusType.POISON){int hits=phantom.has(StatusType.DOT_DAMAGE_TWICE)?2:1;for(int i=0;i<hits&&!phantom.isDead();i++)consumePhantomElement(phantom,com.pas.game.status.StatusDisplay.label(status.getType())+" 피해");if(phantom.isDead())break;if(status.getType()==StatusType.FIRE){if(!status.consumeReinforcedFlag()){status.scaleMagnitudeAndFloor(.95);if(status.getMagnitude()<=0)consumed.add(status);}}else{status.decreaseMagnitude(1);if(status.getMagnitude()<=0)consumed.add(status);}}else if(status.getType()==StatusType.RECOIL_AT_TURN_END){consumePhantomElement(phantom,"반동 피해");consumed.add(status);if(phantom.isDead())break;}}if(!phantom.isDead()){phantom.removeStatuses(consumed);phantom.removeStatuses(phantom.tickStatuses());}}}
    private BattleUnit chooseStarTarget(WizardBattleState.Star star){List<BattleUnit> all=new ArrayList<>();int max=-1;for(BattleUnit unit:state.getUnits())if(unit.isOnField()&&!unit.isDead()&&BattleGrid.distance(star.tile,unit.getTile())<=1){if(unit.getHp()>max){all.clear();max=unit.getHp();}if(unit.getHp()==max)all.add(unit);}if(all.isEmpty())return null;List<BattleUnit> enemies=new ArrayList<>();for(BattleUnit u:all)if(u.getTeam()!=star.team)enemies.add(u);return random.choose(enemies.isEmpty()?all:enemies);}
    private void dealStarDamage(WizardBattleState.Star star,BattleUnit target){if(target instanceof WizardPhantom){consumePhantomElement((WizardPhantom)target,"굶주린 별");return;}int hit=random.nextInt(100)+1;if(hit>100-target.sum(StatusType.HIT_CHANCE_REDUCTION))return;int crit=random.nextInt(100)+1;boolean critical=crit<=star.critRate;if(!critical&&random.nextInt(100)+1<=target.getEvasionRate())return;double raw=ceilToInt(star.attack*star.coefficient/100.0)*(critical?star.critMultiplier:1);dealDamage(null,target,raw,false,DamageType.DIRECT,"굶주린 별");}
    private void processOtherworldGates(BattleUnit active){for(WizardBattleState.Gate gate:new ArrayList<>(state.getWizardState().getGates()))if(gate.team!=active.getTeam()&&active.isOnField()&&!active.isDead()&&BattleGrid.distance(gate.tile,active.getTile())<=2)dealDamage(state.find(gate.ownerId),active,gate.damage,false,DamageType.TRUE_DAMAGE,"이계의 문");}
    private void processAbyssalMarks(BattleUnit attacker,BattleUnit target,DamageType type,int dealt){if(attacker==null||target==null||dealt<=0||type!=DamageType.DIRECT||executingSkill==null)return;for(StatusEffect mark:new ArrayList<>(target.getStatuses()))if(mark.getType()==StatusType.ABYSSAL_MARK){BattleUnit caster=state.find(mark.getSourceUnitId());if(caster==null||caster.getTeam()!=attacker.getTeam())continue;String key=mark.getId()+"@"+executingUseToken;if(!abyssalMarkUseKeys.add(key))continue;heal(attacker,ceilToInt(attacker.getAttack()*mark.getMagnitude()/100.0),"심연의 낙인");}}
    private void consumePhantomElement(WizardPhantom phantom,String element){boolean gone=phantom.consumeElement();log(phantom.getName()+"가 "+element+" 효과를 대신 받았습니다. (남은 내구 "+Math.max(0,phantom.getRemainingHits())+")");if(gone)state.removeUnit(phantom);}
    private boolean hasUnstoppable(BattleUnit unit){return unit.has(StatusType.UNSTOPPABLE)||(!state.isNetworkCoop()&&unit.has(StatusType.DIVINE_FRAGMENT));}
    public List<BattleUnit> enemiesOf(BattleUnit unit){return state.living(unit.getTeam()==Team.PLAYER?Team.ENEMY:Team.PLAYER);}
    public void log(String line){state.log(line);}

    private void evaluateOutcome(){
        if(state.getOutcome()!=BattleOutcome.ONGOING)return;
        cleanupWizardDeaths();if(state.getOutcome()!=BattleOutcome.ONGOING)return;
        RedAltarEncounter.synchronize(this);
        boolean enemyAlive=false;for(BattleUnit u:state.getUnits())if(u.getTeam()==Team.ENEMY&&u.countsForOutcome()&&!u.isDead()){enemyAlive=true;break;}if(!enemyAlive){state.setOutcome(BattleOutcome.VICTORY);log("전투 승리");return;}
        List<BattleUnit> players=new ArrayList<>();for(BattleUnit u:state.getUnits())if(u.getTeam()==Team.PLAYER&&u.countsForOutcome())players.add(u);
        boolean anyDead=false,allDead=!players.isEmpty();for(BattleUnit p:players){anyDead|=p.isDead();allDead&=p.isDead();}
        if((state.getReviveResourceCount()==0&&anyDead)||(state.getReviveResourceCount()>0&&allDead)){state.setOutcome(BattleOutcome.DEFEAT);log("전투 패배");}
    }
    private void cleanupWizardDeaths(){for(BattleUnit owner:new ArrayList<>(state.getUnits()))if(owner.countsForOutcome()&&owner.isDead()){boolean hasGate=false;for(WizardBattleState.Gate gate:state.getWizardState().getGates())if(gate.ownerId.equals(owner.getUnitId())){hasGate=true;break;}if(hasGate){state.setOutcome(owner.getTeam()==Team.PLAYER?BattleOutcome.DEFEAT:BattleOutcome.VICTORY);log("이계의 문 시전자가 사망해 전투가 즉시 종료되었습니다.");return;}for(BattleUnit unit:new ArrayList<>(state.getUnits()))if(unit instanceof WizardPhantom&&((WizardPhantom)unit).getOwnerId().equals(owner.getUnitId()))state.removeUnit(unit);state.getWizardState().getStars().removeIf(star->star.ownerId.equals(owner.getUnitId()));state.getWizardState().getDrifts().remove(owner.getUnitId());state.getWizardState().getEchoes().remove(owner.getUnitId());}}
    private BattleResult outcomeResult(){return BattleResult.ok().add(state.getOutcome()==BattleOutcome.VICTORY?BattleEvent.Type.VICTORY:BattleEvent.Type.DEFEAT,state.getOutcome()==BattleOutcome.VICTORY?"승리":"패배");}

    public void restoreAll(){for(BattleUnit u:state.getUnits()){u.setHpForDebug(u.getMaxHp());for(SkillRuntime s:u.getEquippedSkills())if(s!=null){s.restoreAllUses();s.resetCooldown();}}}
    public void setHp(String unitId,int hp){BattleUnit u=state.find(unitId);if(u!=null)u.setHpForDebug(hp);}
    public void forceMove(String unitId,int tile){BattleUnit u=state.find(unitId);if(u!=null&&BattleGrid.isValid(tile)){u.setTile(tile);onEnterTile(u,tile);}}
    public void clearStatuses(String unitId){BattleUnit u=state.find(unitId);if(u!=null){for(StatusEffect status:new ArrayList<>(u.getStatuses()))onWizardStatusRemoved(u,status);for(StatusType t:StatusType.values())u.removeStatus(t);}}
    public void addDebugEnemy(int tile){
        int index=1;while(state.find("ENEMY_DUMMY_"+index)!=null)index++;
        EnemyUnit enemy=new EnemyUnit("ENEMY_DUMMY_"+index,"살아있는 허수아비 "+index,tile);
        enemy.setEligibleRound(state.getRound()+1);enemy.equip(new SkillRuntime(EnemySkillRepository.basicAttack(),0));enemy.equip(new SkillRuntime(EnemySkillRepository.slam(),0));state.addUnit(enemy);log(enemy.getName()+" 추가 (다음 라운드부터 행동)");
    }
    public void removeDebugEnemy(){List<BattleUnit> enemies=state.living(Team.ENEMY);if(!enemies.isEmpty()){BattleUnit target=enemies.get(enemies.size()-1);if(target!=activeUnit()){state.removeUnit(target);log(target.getName()+" 제거");evaluateOutcome();}}}
}
