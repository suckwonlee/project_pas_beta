package com.pas.game.passive;

import com.pas.game.effect.UnitEffect;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.BattleUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** 공용 패시브 정의 저장소. 몬스터도 같은 ID와 런타임을 장착한다. */
public final class PassiveRepository {
    public static final String HEAVY_ARMOR="heavy_armor";
    public static final String RUNE_FIRE="rune_fire";
    public static final String RUNE_FROST="rune_frost";
    public static final String RUNE_GUARDIAN="rune_guardian";
    public static final String RUNE_VENOM="rune_venom";
    public static final String RUNE_FIGHTING="rune_fighting";
    public static final String RUNE_VAMPIRE="rune_vampire";
    public static final String RUNE_MUST_LIVE="rune_must_live";
    public static final String RUNE_CHARGE="rune_charge";
    public static final String CRITICAL_POWER="critical_power";
    public static final String KILLING_INTENT="killing_intent";
    public static final String DETERMINATION="determination";
    public static final String SURPRISE_ATTACK="surprise_attack";
    public static final String PURIFICATION="purification";
    public static final String JUDGMENT="cleric_judgment";
    public static final String MANA="wizard_mana";
    private final Map<String,PassiveData> definitions=new LinkedHashMap<>();
    public PassiveRepository(){
        register(new PassiveData(HEAVY_ARMOR,"중갑","받는 일반 피해를 %s만큼 감소시킵니다.",battleStart(),new double[]{2,4,7,11,16},(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.HEAVY_ARMOR)));
        register(new PassiveData(RUNE_FIRE,"방화","전투 시작 시 모든 적에게 화염 %s를 부여합니다.",battleStart(),new double[]{5,7,10,18,25},(context,owner,runtime)->{for(BattleUnit enemy:context.getEngine().enemiesOf(owner))context.getEngine().applyStatus(enemy,new StatusEffect(runtime.getRuntimeKey()+"@"+enemy.getUnitId(),StatusType.FIRE,owner.getUnitId(),-1,runtime.currentValue(),true,true,runtime.getDisplayName()));}));
        register(new PassiveData(RUNE_FROST,"중갑","받는 일반 피해를 %s만큼 감소시킵니다.",battleStart(),new double[]{2,4,7,11,16},(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.HEAVY_ARMOR)));
        register(new PassiveData(RUNE_GUARDIAN,"의지","전투 시작 시 저지불가를 %s만큼 얻습니다.",battleStart(),new double[]{5,10,15,20,30},(context,owner,runtime)->context.getEngine().applyStatus(owner,new StatusEffect(runtime.getRuntimeKey(),StatusType.UNSTOPPABLE,owner.getUnitId(),(int)Math.ceil(runtime.currentValue()),0,false,false,runtime.getDisplayName()))));
        register(new PassiveData(RUNE_VENOM,"침독","피해를 준 즉발 공격의 각 타수마다 대상에게 중독 %s를 부여합니다.",battleStart(),new double[]{1,1,2,3,4},(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.VENOM_COATING)));
        register(new PassiveData(RUNE_FIGHTING,"투쟁심","전투 시작 시 공격력 %s 증가 버프를 얻습니다.",battleStart(),new double[]{1,2,4,7,10},(context,owner,runtime)->context.getEngine().applyStatus(owner,new StatusEffect(runtime.getRuntimeKey(),StatusType.ATTACK_FLAT_UP,owner.getUnitId(),-1,runtime.currentValue(),false,false,runtime.getDisplayName()))));
        register(new PassiveData(RUNE_VAMPIRE,"흡혈","가한 즉발형 최종 피해의 %s만큼 HP를 회복합니다.",battleStart(),new double[]{3,5,8,12,20},"%",(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.LIFESTEAL)));
        register(new PassiveData(RUNE_MUST_LIVE,"생존력","자신의 3번째 턴마다 HP를 %s만큼 회복합니다.",battleStart(),new double[]{3,5,8,12,20},(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.SURVIVAL)));
        register(new PassiveData(RUNE_CHARGE,"출격","전투 시작 시 HP를 %s만큼 회복합니다.",battleStart(),new double[]{10,20,40,70,100},(context,owner,runtime)->context.getEngine().heal(owner,(int)Math.ceil(runtime.currentValue()),runtime.getDisplayName())));
        register(new PassiveData(CRITICAL_POWER,"치명","치명타 피해가 %s 증가합니다.",battleStart(),new double[]{10,20,30,45,60},"%",(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.CRITICAL_DAMAGE_UP),(value,formatted)->"치명타 피해가 "+formatted+" 증가합니다. (치명타 배율 "+format(150+value)+"%)"));
        register(new PassiveData(KILLING_INTENT,"살의","치명타율이 %s 증가합니다.",battleStart(),new double[]{7,12,18,25,35},"%p",(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.CRITICAL_RATE_UP)));
        register(new PassiveData(DETERMINATION,"결단","전투 시작 시 방어력을 %s만큼 증가시킵니다.",battleStart(),new double[]{2,4,7,11,16},(context,owner,runtime)->grant(context,owner,runtime,UnitEffectType.DETERMINATION)));
        register(new PassiveData(SURPRISE_ATTACK,"기습","각 대상에게 전투당 한 번, 즉발 공격으로 피해를 주면 이동 불가를 %s턴 부여합니다. 저지불가로 무시할 수 없습니다.",EnumSet.of(PassiveTrigger.AFTER_DAMAGE_DEALT),new double[]{1,1,2,2,3},(context,owner,runtime)->{
            BattleUnit target=context.getTarget();
            if(target==null||target.getTeam()==owner.getTeam()||context.getDamageType()!=com.pas.game.battle.damage.DamageType.DIRECT||context.getFinalDamage()<=0)return;
            if(!runtime.triggerOncePerTarget(target.getUnitId()))return;
            int turns=(int)Math.ceil(runtime.currentValue());
            context.getEngine().applyStatus(target,new StatusEffect(runtime.getRuntimeKey()+"@"+target.getUnitId(),StatusType.MOVEMENT_DISABLED,owner.getUnitId(),turns,0,false,false));
        }));
        register(new PassiveData(PURIFICATION,"정화","자신의 턴 시작마다 정화 %s를 얻습니다.",Collections.singleton(PassiveTrigger.TURN_START),new double[]{5,8,14,26,50},(context,owner,runtime)->context.getEngine().gainPurification(owner,(int)Math.ceil(runtime.currentValue()))));
        register(new PassiveData(JUDGMENT,"심판","자신의 턴 시작마다 1칸 안의 적에게 피해 50을 가하고 아군의 HP를 20 회복합니다.",Collections.singleton(PassiveTrigger.TURN_START),new double[]{50,50,50,50,50},(context,owner,runtime)->context.getEngine().triggerClericJudgment(owner)));
        register(new PassiveData(MANA,"마력","자신의 매 %s번째 턴에 마력이 활성화되어, 그 턴에 사용하는 스킬의 명시된 부가효과가 강화됩니다.",Collections.singleton(PassiveTrigger.TURN_START),new double[]{6,5,4,3,2},(context,owner,runtime)->context.getEngine().activateWizardMana(owner,(int)Math.ceil(runtime.currentValue()))));
    }
    private java.util.Set<PassiveTrigger> battleStart(){return Collections.singleton(PassiveTrigger.BATTLE_START);}
    private void register(PassiveData data){definitions.put(data.getId(),data);}
    private static void grant(PassiveContext context,BattleUnit owner,PassiveRuntime runtime,UnitEffectType type){context.getEngine().grantEffect(owner,new UnitEffect(type,runtime.getRuntimeKey(),runtime.currentValue(),runtime.getDisplayName()));}
    private static String format(double value){return value==(long)value?String.valueOf((long)value):String.valueOf(value);}
    public PassiveData find(String id){return definitions.get(id);} public Map<String,PassiveData> all(){return Collections.unmodifiableMap(definitions);}
    public PassiveRuntime create(String id,int level,String sourceKey){PassiveData data=find(id);return data==null?null:new PassiveRuntime(data,level,sourceKey);}
}
