package com.pas.game.multiplayer;

import com.pas.game.battle.state.BattleDecoy;
import com.pas.game.battle.state.BattleSanctuary;
import com.pas.game.battle.state.BattleState;
import com.pas.game.battle.state.BattleTrap;
import com.pas.game.battle.state.WizardBattleState;
import com.pas.game.effect.UnitEffect;
import com.pas.game.item.potion.PotionData;
import com.pas.game.passive.PassiveRuntime;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.status.StatusEffect;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.PlayerUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 서버 상태에서 만든 읽기 전용 전체 화면 스냅샷. 클라이언트는 이를 렌더링하며 전투를 재계산하지 않는다. */
public final class BattleSnapshot {
    private final long revision;
    private final String json;
    private final String digest;

    private BattleSnapshot(long revision,String json){this.revision=revision;this.json=json;this.digest=sha256(json);}
    public static BattleSnapshot capture(BattleState state,long revision){return new BattleSnapshot(revision,buildJson(state,revision));}
    public long getRevision(){return revision;}public String toJson(){return json;}public String getDigest(){return digest;}

    private static String buildJson(BattleState state,long revision){
        StringBuilder out=new StringBuilder("{");number(out,"revision",revision);number(out,"round",state.getRound());text(out,"outcome",state.getOutcome().name());bool(out,"networkCoop",state.isNetworkCoop());
        out.append(",\"turn\":{");text(out,"activeUnitId",state.getTurn().getActiveUnitId());number(out,"movesRemaining",state.getTurn().getMovesRemaining());number(out,"skillsRemaining",state.getTurn().getSkillsRemaining());bool(out,"blocked",state.getTurn().isBlocked());out.append('}');
        text(out,"pendingWizardReturnOwnerId",state.getWizardState().getPendingReturnOwnerId());
        out.append(",\"units\":[");List<BattleUnit> units=new ArrayList<>(state.getUnits());units.sort(Comparator.comparing(BattleUnit::getUnitId));for(int i=0;i<units.size();i++){if(i>0)out.append(',');unit(out,units.get(i),state);}out.append(']');
        out.append(",\"traps\":[");for(int i=0;i<state.getTraps().size();i++){if(i>0)out.append(',');BattleTrap v=state.getTraps().get(i);out.append('{');text(out,"id",v.getId());text(out,"ownerUnitId",v.getOwnerUnitId());text(out,"ownerTeam",v.getOwnerTeam().name());number(out,"tile",v.getTile());number(out,"bindTurns",v.getBindTurns());out.append('}');}out.append(']');
        out.append(",\"decoys\":[");for(int i=0;i<state.getDecoys().size();i++){if(i>0)out.append(',');BattleDecoy v=state.getDecoys().get(i);out.append('{');text(out,"id",v.getId());text(out,"ownerUnitId",v.getOwnerUnitId());text(out,"ownerTeam",v.getOwnerTeam().name());number(out,"tile",v.getTile());number(out,"remainingOwnerTurns",v.getRemainingOwnerTurns());out.append('}');}out.append(']');
        out.append(",\"sanctuaries\":[");for(int i=0;i<state.getSanctuaries().size();i++){if(i>0)out.append(',');BattleSanctuary v=state.getSanctuaries().get(i);out.append('{');text(out,"id",v.getId());text(out,"ownerUnitId",v.getOwnerUnitId());text(out,"ownerTeam",v.getOwnerTeam().name());number(out,"tile",v.getTile());number(out,"range",v.getRange());number(out,"healing",v.getHealing());number(out,"remainingPlayerTurns",v.getRemainingPlayerTurns());out.append('}');}out.append(']');
        wizard(out,state.getWizardState());
        out.append(",\"potions\":[");List<PotionData> potions=state.getPotionInventory().getOwnedPotions();for(int i=0;i<potions.size();i++){if(i>0)out.append(',');PotionData p=potions.get(i);out.append('{');number(out,"tier",p.getTier());text(out,"name",p.getName());number(out,"count",state.getPotionInventory().getCount(p));out.append('}');}out.append(']');
        out.append(",\"logs\":[");List<String> logs=state.getLogs();for(int i=0;i<logs.size();i++){if(i>0)out.append(',');out.append(WireJson.quote(logs.get(i)));}out.append("]}");return out.toString();
    }

    private static void unit(StringBuilder out,BattleUnit unit,BattleState state){
        out.append('{');text(out,"unitId",unit.getUnitId());text(out,"name",unit.getName());text(out,"team",unit.getTeam().name());number(out,"playerSlot",unit instanceof PlayerUnit?((PlayerUnit)unit).getPlayerSlot():0);
        if(unit instanceof PlayerUnit){PlayerUnit player=(PlayerUnit)unit;text(out,"characterId",player.getCharacterId());text(out,"skinId",player.getSkinId());}
        if(unit instanceof com.pas.game.unit.EnemyUnit){
            com.pas.game.unit.EnemyUnit enemy=(com.pas.game.unit.EnemyUnit)unit;
            text(out,"enemyKind",enemy.getKind().name());text(out,"summonerId",enemy.getSummonerId());bool(out,"gateOpened",enemy.isGateOpened());number(out,"summonSequence",enemy.getSummonSequence());
        }
        number(out,"hp",unit.getHp());number(out,"maxHp",unit.getMaxHp());number(out,"attack",unit.getAttack());number(out,"defense",unit.getDefense());decimal(out,"criticalRate",unit.getCriticalRate());decimal(out,"criticalDamageMultiplier",unit.getCriticalDamageMultiplier());decimal(out,"evasionRate",unit.getEvasionRate());number(out,"tile",unit.getTile());number(out,"barrier",unit.getBarrier());number(out,"eligibleRound",unit.getEligibleRound());number(out,"turnsStarted",unit.getTurnsStarted());bool(out,"dead",unit.isDead());bool(out,"onField",unit.isOnField());
        text(out,"intent",unit instanceof com.pas.game.unit.EnemyUnit?com.pas.game.battle.ai.EnemyIntent.describe(unit,state):null);
        out.append(",\"statusGroups\":[");List<com.pas.game.status.StatusSummary.Group> groups=com.pas.game.status.StatusSummary.of(unit,state);
        for(int i=0;i<groups.size();i++){if(i>0)out.append(',');com.pas.game.status.StatusSummary.Group g=groups.get(i);out.append('{');text(out,"key",g.key);text(out,"label",g.label);text(out,"value",g.value);text(out,"description",g.description);bool(out,"debuff",g.debuff);out.append('}');}out.append(']');
        out.append(",\"statuses\":[");List<StatusEffect> statuses=new ArrayList<>(unit.getStatuses());statuses.sort(Comparator.comparingLong(StatusEffect::getApplicationOrder).thenComparing(StatusEffect::getId));for(int i=0;i<statuses.size();i++){if(i>0)out.append(',');StatusEffect s=statuses.get(i);out.append('{');text(out,"id",s.getId());text(out,"type",s.getType().name());text(out,"sourceUnitId",s.getSourceUnitId());text(out,"sourceDisplayName",s.getDisplaySourceName());number(out,"remainingTurns",s.getRemainingTurns());decimal(out,"magnitude",s.getMagnitude());number(out,"stackCount",s.getStackCount());bool(out,"dispellable",s.isDispellable());out.append('}');}out.append(']');
        out.append(",\"effects\":[");for(int i=0;i<unit.getEffects().size();i++){if(i>0)out.append(',');UnitEffect e=unit.getEffects().get(i);out.append('{');text(out,"type",e.getType().name());text(out,"sourceKey",e.getSourceKey());text(out,"sourceDisplayName",e.getSourceDisplayName());decimal(out,"magnitude",e.getMagnitude());out.append('}');}out.append(']');
        out.append(",\"passives\":[");for(int i=0;i<unit.getPassives().size();i++){if(i>0)out.append(',');PassiveRuntime p=unit.getPassives().get(i);out.append('{');text(out,"runtimeKey",p.getRuntimeKey());text(out,"id",p.getData().getId());number(out,"level",p.getLevel());bool(out,"enabled",p.isEnabled());out.append('}');}out.append(']');
        out.append(",\"skills\":[");for(int i=0;i<unit.getEquippedSkills().size();i++){if(i>0)out.append(',');SkillRuntime s=unit.getEquippedSkills().get(i);if(s==null){out.append("null");continue;}out.append('{');number(out,"slot",i);text(out,"id",s.getData().getId());number(out,"upgradeSteps",s.getUpgradeSteps());number(out,"remainingUses",s.getRemainingUses());number(out,"maxUses",s.getMaxUses());number(out,"cooldownRemaining",s.getCooldownRemaining());out.append('}');}out.append("]}");
    }

    private static void wizard(StringBuilder out,WizardBattleState state){
        out.append(",\"wizard\":{");sortedIntMap(out,"dischargeUses",state.getDischargeUses());sortedIntMap(out,"doubleCastStacks",state.getDoubleCastStacks());
        out.append(",\"loans\":[");List<String> loanIds=new ArrayList<>(state.getLoans().keySet());loanIds.sort(String::compareTo);for(int i=0;i<loanIds.size();i++){if(i>0)out.append(',');String id=loanIds.get(i);WizardBattleState.Loan loan=state.getLoans().get(id);out.append('{');text(out,"ownerUnitId",id);number(out,"stacks",loan.stacks);number(out,"remainingStarts",loan.remainingStarts);out.append('}');}out.append(']');
        out.append(",\"drifts\":[");List<String> driftIds=new ArrayList<>(state.getDrifts().keySet());driftIds.sort(String::compareTo);for(int i=0;i<driftIds.size();i++){if(i>0)out.append(',');WizardBattleState.Drift d=state.getDrifts().get(driftIds.get(i));out.append('{');text(out,"ownerUnitId",d.ownerId);number(out,"originTile",d.originTile);bool(out,"chooseAdjacent",d.chooseAdjacent);out.append('}');}out.append(']');
        out.append(",\"echoes\":[");List<String> echoIds=new ArrayList<>(state.getEchoes().keySet());echoIds.sort(String::compareTo);for(int i=0;i<echoIds.size();i++){if(i>0)out.append(',');WizardBattleState.Echo e=state.getEchoes().get(echoIds.get(i));out.append('{');text(out,"ownerUnitId",e.ownerId);number(out,"tile",e.tile);number(out,"hp",e.hp);number(out,"remainingStarts",e.remainingStarts);out.append('}');}out.append(']');
        out.append(",\"stars\":[");for(int i=0;i<state.getStars().size();i++){if(i>0)out.append(',');WizardBattleState.Star s=state.getStars().get(i);out.append('{');text(out,"id",s.id);text(out,"ownerUnitId",s.ownerId);text(out,"team",s.team.name());number(out,"tile",s.tile);number(out,"attack",s.attack);decimal(out,"criticalRate",s.critRate);decimal(out,"criticalMultiplier",s.critMultiplier);decimal(out,"coefficient",s.coefficient);number(out,"attacksRemaining",s.attacksRemaining);number(out,"extraAttacksPerTick",s.extraAttacksPerTick);out.append('}');}out.append(']');
        out.append(",\"gates\":[");for(int i=0;i<state.getGates().size();i++){if(i>0)out.append(',');WizardBattleState.Gate g=state.getGates().get(i);out.append('{');text(out,"id",g.id);text(out,"ownerUnitId",g.ownerId);text(out,"team",g.team.name());number(out,"tile",g.tile);number(out,"damage",g.damage);out.append('}');}out.append("]}");
    }

    private static void sortedIntMap(StringBuilder out,String name,Map<String,Integer> values){out.append(out.charAt(out.length()-1)=='{'?"":",").append(WireJson.quote(name)).append(":[");List<String> ids=new ArrayList<>(values.keySet());ids.sort(String::compareTo);for(int i=0;i<ids.size();i++){if(i>0)out.append(',');out.append('{');text(out,"unitId",ids.get(i));number(out,"value",values.get(ids.get(i)));out.append('}');}out.append(']');}
    private static void text(StringBuilder out,String name,String value){comma(out);out.append(WireJson.quote(name)).append(':').append(WireJson.quote(value));}
    private static void number(StringBuilder out,String name,long value){comma(out);out.append(WireJson.quote(name)).append(':').append(value);}
    private static void decimal(StringBuilder out,String name,double value){comma(out);out.append(WireJson.quote(name)).append(':').append(Double.toString(value));}
    private static void bool(StringBuilder out,String name,boolean value){comma(out);out.append(WireJson.quote(name)).append(':').append(value);}
    private static void comma(StringBuilder out){char c=out.charAt(out.length()-1);if(c!='{'&&c!='['&&c!=',')out.append(',');}
    private static String sha256(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format("%02x",b&0xff));return out.toString();}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
