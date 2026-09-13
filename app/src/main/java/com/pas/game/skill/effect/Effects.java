package com.pas.game.skill.effect;

import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.effect.UnitEffect;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.BattleUnit;

/** 실행식과 표시식을 한 효과 객체에 묶는다. UI 수치는 실제 전투식에서 자동 계산된다. */
public final class Effects {
    private interface Executor { void apply(BattleEngine engine,BattleUnit caster,BattleUnit target,double value); }
    private interface TileExecutor { void apply(BattleEngine engine,BattleUnit caster,BattleUnit target,double value,Integer targetTile); }
    private interface Descriptor { String describe(BattleUnit caster,BattleUnit target,double value); }
    private Effects() {}

    private static SkillEffect described(Executor executor,Descriptor descriptor){return new SkillEffect(){
        @Override public void apply(BattleEngine engine,BattleUnit caster,BattleUnit target,double value){executor.apply(engine,caster,target,value);}
        @Override public String describe(BattleUnit caster,BattleUnit target,double value){return descriptor.describe(caster,target,value);}
    };}
    private static SkillEffect describedTile(TileExecutor executor,Descriptor descriptor){return new SkillEffect(){
        @Override public void apply(BattleEngine engine,BattleUnit caster,BattleUnit target,double value){executor.apply(engine,caster,target,value,caster.getTile());}
        @Override public void apply(BattleEngine engine,BattleUnit caster,BattleUnit target,double value,Integer targetTile){executor.apply(engine,caster,target,value,targetTile);}
        @Override public String describe(BattleUnit caster,BattleUnit target,double value){return descriptor.describe(caster,target,value);}
    };}

    public static SkillEffect attackDamage(){return described(
            (e,c,t,v)->e.dealDamage(c,t,c.getAttack()*v/100.0,true,false),
            (c,t,v)->ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%)");}
    public static SkillEffect defenseDamage(){return described(
            (e,c,t,v)->e.dealDamage(c,t,c.getDefense()*v/100.0,true,false),
            (c,t,v)->ceil(c.getDefense()*v/100.0)+"만큼 피해 (방어력의 "+number(v)+"%)");}
    public static SkillEffect maxHpFixedDamage(){return described(
            (e,c,t,v)->e.dealDamage(c,t,c.getMaxHp()*v/100.0,false,true),
            (c,t,v)->ceil(c.getMaxHp()*v/100.0)+"만큼 피해 (최대 HP의 "+number(v)+"%)");}
    public static SkillEffect targetMaxHpFixedDamage(){return described(
            (e,c,t,v)->e.dealDamage(c,t,t.getMaxHp()*v/100.0,false,true),
            (c,t,v)->t==null?"대상 최대 HP의 "+number(v)+"%만큼 피해":ceil(t.getMaxHp()*v/100.0)+"만큼 피해 (대상 최대 HP의 "+number(v)+"%)");}
    public static SkillEffect multiAttack(int hits){return described(
            (e,c,t,v)->{for(int i=0;i<hits&&!t.isDead();i++)e.dealDamage(c,t,c.getAttack()*v/100.0,true,false);},
            (c,t,v)->{int each=ceil(c.getAttack()*v/100.0);return "타격당 "+each+"만큼 "+hits+"회 피해 (공격력의 "+number(v)+"%, 최대 "+(each*hits)+")";});}
    public static SkillEffect criticalDamageAttack(){return described(
            (e,c,t,v)->e.dealCriticalDamage(c,t,c.getAttack()*v/100.0),
            (c,t,v)->"치명타 성공 시 "+ceil(ceil(c.getAttack()*v/100.0)*(c.getCriticalDamageMultiplier()+c.getCriticalRate()/100.0))+"만큼 피해 (공격력의 "+number(v)+"%, 실패 시 빗나감)");}
    public static SkillEffect permanentCriticalRate(double baseGain,double gainPerUpgrade,double baseCoefficient,double coefficientPerUpgrade){return described(
            (e,c,t,v)->{double steps=coefficientPerUpgrade==0?0:Math.max(0,(v-baseCoefficient)/coefficientPerUpgrade);double gain=baseGain+gainPerUpgrade*steps;e.applyStatus(c,new StatusEffect("PERMANENT_CRIT_"+c.getUnitId(),StatusType.CRIT_RATE_UP,c.getUnitId(),-1,gain,true,false));},
            (c,t,v)->{double steps=coefficientPerUpgrade==0?0:Math.max(0,(v-baseCoefficient)/coefficientPerUpgrade);return "사용할 때마다 치명타율 +"+number(baseGain+gainPerUpgrade*steps)+"%p";});}
    public static SkillEffect barrierFromDefense(){return described(
            (e,c,t,v)->e.addBarrier(t,ceil(c.getDefense()*v/100.0)),
            (c,t,v)->ceil(c.getDefense()*v/100.0)+"만큼 피해 방어 (방어력의 "+number(v)+"%, 1회 또는 다음 사용자 턴 시작까지)");}
    public static SkillEffect barrierFromAttack(){return described(
            (e,c,t,v)->e.addBarrier(t,ceil(c.getAttack()*v/100.0)),
            (c,t,v)->ceil(c.getAttack()*v/100.0)+"만큼 피해 방어 (공격력의 "+number(v)+"%, 1회 또는 다음 사용자 턴 시작까지)");}
    public static SkillEffect healMissing(){return described(
            (e,c,t,v)->e.heal(t,ceil((t.getMaxHp()-t.getHp())*v/100.0)),
            (c,t,v)->{if(t==null)return "잃은 HP의 "+number(v)+"% 회복 (100 손실 기준 "+ceil(v)+")";int missing=Math.max(0,t.getMaxHp()-t.getHp());return ceil(missing*v/100.0)+"만큼 회복 (대상의 잃은 HP "+missing+"의 "+number(v)+"%)";});}
    public static SkillEffect reducePoisonHalf(){return described(
            (e,c,t,v)->e.reduceStatusMagnitude(t,StatusType.POISON,0.5),(c,t,v)->"대상의 중독 수치 절반 감소");}
    public static SkillEffect status(StatusType type,int turns,double fixedMagnitude){return described(
            (e,c,t,v)->e.applyStatus(t,new StatusEffect(type.name()+"_"+c.getUnitId(),type,c.getUnitId(),turns,fixedMagnitude,false,true)),
            (c,t,v)->statusText(type,turns,fixedMagnitude));}
    public static SkillEffect statusUsingValue(StatusType type,int turns){return described(
            (e,c,t,v)->e.applyStatus(t,new StatusEffect(type.name()+"_"+c.getUnitId(),type,c.getUnitId(),turns,v,false,true)),
            (c,t,v)->statusText(type,turns,v));}
    public static SkillEffect selfStatus(StatusType type,int turns,double magnitude){return described(
            (e,c,t,v)->e.applyStatus(c,new StatusEffect(type.name()+"_"+c.getUnitId(),type,c.getUnitId(),turns,magnitude,false,true)),
            (c,t,v)->statusText(type,turns,magnitude));}
    public static SkillEffect selfStatusUsingValue(StatusType type,int turns){return described(
            (e,c,t,v)->e.applyStatus(c,new StatusEffect(type.name()+"_"+c.getUnitId(),type,c.getUnitId(),turns,v,false,true)),
            (c,t,v)->statusText(type,turns,v));}
    public static SkillEffect evasionFromDefense(int turns){return described(
            (e,c,t,v)->e.applyStatus(c,new StatusEffect("EVADE_FOCUS_"+c.getUnitId(),StatusType.EVADE_UP,c.getUnitId(),turns,ceil(c.getDefense()*v/100.0),false,true)),
            (c,t,v)->"회피율 +"+ceil(c.getDefense()*v/100.0)+"%p (방어력의 "+number(v)+"%) · "+turns+"턴");}
    public static SkillEffect poisonFromAttack(){return described(
            (e,c,t,v)->e.applyStatus(t,new StatusEffect("POISON_"+c.getUnitId(),StatusType.POISON,c.getUnitId(),-1,ceil(c.getAttack()*v/100.0),true,true)),
            (c,t,v)->"중독 "+ceil(c.getAttack()*v/100.0)+" 부여 (공격력의 "+number(v)+"%)");}
    public static SkillEffect criticalRateFromTargetPoison(){return described(
            (e,c,t,v)->{double poison=t==null?0:t.sum(StatusType.POISON);e.applyStatus(c,new StatusEffect("VENOM_CHASE_"+c.getUnitId(),StatusType.CRIT_RATE_UP,c.getUnitId(),-1,poison*v,true,false));},
            (c,t,v)->"대상의 중독 1당 치명타율 +"+number(v)+"%p"+(t==null?"":" (현재 +"+number(t.sum(StatusType.POISON)*v)+"%p)"));}
    public static SkillEffect dotDamageTwice(int turns){return described(
            (e,c,t,v)->e.applyStatus(t,new StatusEffect("DOT_TWICE_"+c.getUnitId(),StatusType.DOT_DAMAGE_TWICE,c.getUnitId(),turns,0,false,true)),
            (c,t,v)->"턴 종료 시 지속 피해가 2회 적용 · "+turns+"턴");}
    public static SkillEffect installBindingTrap(int bindTurns){return describedTile(
            (e,c,t,v,tile)->e.installBindingTrap(c,tile,bindTurns),
            (c,t,v)->"선택한 칸에 적이 밟으면 한 번 발동하는 덫 설치 · 구속 "+bindTurns+"턴");}
    public static SkillEffect skillMove(){return describedTile(
            (e,c,t,v,tile)->e.moveBySkill(c,tile),
            (c,t,v)->"선택한 칸으로 스킬 이동");}
    public static SkillEffect piercingLineAttack(){return describedTile(
            (e,c,t,v,tile)->e.dealPiercingLine(c,tile,v),
            (c,t,v)->"선택 방향 2칸 직선상의 모든 적에게 "+ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%)");}
    public static SkillEffect armEmergencyEscape(){return describedTile(
            (e,c,t,v,tile)->e.armEmergencyEscape(c,tile,v),
            (c,t,v)->"인접 칸을 비상 탈출 지점으로 지정 · 피격 직전 즉시 이동 · 발동 후 회피율 +"+number(v)+"%p");}
    public static SkillEffect sharpTraining(){return described(
            (e,c,t,v)->e.grantEffect(c,new UnitEffect(UnitEffectType.SHARP_GAIN,"skill:cold_aim",v)),
            (c,t,v)->"일반 공격 치명타 시 예리함 "+number(v)+"% 획득 · 비치명타 시 보유한 예리함 소멸");}
    public static SkillEffect venomAttackTraining(){return described(
            (e,c,t,v)->e.grantStackingEffect(c,new UnitEffect(UnitEffectType.VENOM_COATING,"skill:venom_injection",v)),
            (c,t,v)->"독공격 "+number(v)+" 획득 · 즉발 공격의 각 타수마다 중독 "+number(v)+" 부여");}
    public static SkillEffect installDecoy(int ownerTurns){return describedTile(
            (e,c,t,v,tile)->e.installDecoy(c,tile,ownerTurns),
            (c,t,v)->"선택한 칸에 미끼 설치 · 적이 해당 칸에 진입하면 소멸 · "+ownerTurns+"턴");}
    public static SkillEffect extraSkillAction(){return described((e,c,t,v)->e.grantSkillAction(c),(c,t,v)->"스킬 행동 +1");}
    public static SkillEffect restoreNonUltimateUses(int amount){return described(
            (e,c,t,v)->{for(SkillRuntime s:c.getEquippedSkills())if(s!=null&&!s.getData().isUltimate())s.restoreUses(amount);e.log(c.getName()+"의 다른 비궁극기 스킬 사용 횟수가 각각 "+amount+"회 충전되었습니다.");},
            (c,t,v)->"다른 비궁극기 스킬의 사용 횟수를 각각 "+amount+"회 충전");}
    public static SkillEffect fireFromAttack(double attackPercent){return described(
            (e,c,t,v)->e.applyStatus(t,new StatusEffect("FIRE",StatusType.FIRE,c.getUnitId(),-1,ceil(Math.max(1,c.getAttack()*attackPercent/100.0)),true,true)),
            (c,t,v)->"화염 "+ceil(Math.max(1,c.getAttack()*attackPercent/100.0))+" 부여 (공격력의 "+number(attackPercent)+"%)");}
    public static SkillEffect guardianAura(){return described(
            (e,c,t,v)->{double reduction=ceil(c.getDefense()*v/100.0);e.applyStatus(c,new StatusEffect("AURA_"+c.getUnitId(),StatusType.GUARDIAN_AURA,c.getUnitId(),-1,reduction,false,false));},
            (c,t,v)->"자신과 1칸 안의 아군이 받는 피해를 "+ceil(c.getDefense()*v/100.0)+"만큼 감소 (방어력의 "+number(v)+"%)");}
    public static SkillEffect battleCry(){return described(
            (e,c,t,v)->{e.applyStatus(c,new StatusEffect("CRY_"+c.getUnitId(),StatusType.ATTACK_UP,c.getUnitId(),2,v,false,true));if(t!=c)e.applyStatus(t,new StatusEffect("CRY_"+c.getUnitId(),StatusType.ATTACK_UP,c.getUnitId(),1,v,false,true));},
            (c,t,v)->"공격력 +"+ceil(c.getAttack()*v/100.0)+" (현재 공격력의 "+number(v)+"%) · 다음 자기 턴까지");}
    public static SkillEffect tauntAllEnemies(int turns){return described(
            (e,c,t,v)->{for(BattleUnit enemy:e.enemiesOf(c))e.applyStatus(enemy,new StatusEffect("TAUNT_"+c.getUnitId(),StatusType.TAUNT,c.getUnitId(),turns,0,false,true));},
            (c,t,v)->"모든 적 도발 "+turns+"턴");}
    public static SkillEffect damageTakenAllAndSelf(){return described(
            (e,c,t,v)->{e.applyStatus(c,new StatusEffect("RESOLVE_SELF_"+c.getUnitId(),StatusType.DAMAGE_TAKEN_UP,c.getUnitId(),2,v,false,true));for(BattleUnit enemy:e.enemiesOf(c))e.applyStatus(enemy,new StatusEffect("RESOLVE_"+c.getUnitId(),StatusType.DAMAGE_TAKEN_UP,c.getUnitId(),1,v,false,true));},
            (c,t,v)->"자신이 받는 피해 +"+number(v)+"% (2턴) · 모든 적이 받는 피해 +"+number(v)+"% (1턴)");}
    public static SkillEffect justiceDot(){return described(
            (e,c,t,v)->e.applyStatus(t,new StatusEffect("JUSTICE_"+c.getUnitId(),StatusType.JUSTICE_DOT,c.getUnitId(),-1,v,false,false)),
            (c,t,v)->t==null?"대상 턴마다 최대 HP의 "+number(v)+"%만큼 피해 (최대 HP 100 기준 "+ceil(v)+")":"대상 턴마다 "+ceil(t.getMaxHp()*v/100.0)+"만큼 피해 (대상 최대 HP의 "+number(v)+"%)");}
    public static SkillEffect defenseFocusRestrictions(){return described(
            (e,c,t,v)->{e.applyStatus(c,new StatusEffect("FOCUS_EVADE_"+c.getUnitId(),StatusType.EVASION_DISABLED,c.getUnitId(),2,0,false,true));if(e.getState().isNetworkCoop())e.applyStatus(c,new StatusEffect("FOCUS_MOVE_"+c.getUnitId(),StatusType.MOVEMENT_DISABLED,c.getUnitId(),2,0,false,true));},
            (c,t,v)->"회피 불가 2턴 · 멀티플레이 시 이동 불가 2턴");}
    public static SkillEffect permanentMaxHpFlatReduction(){return described(
            (e,c,t,v)->e.applyStatus(c,new StatusEffect("SALVATION_"+c.getUnitId(),StatusType.FLAT_DAMAGE_REDUCTION,c.getUnitId(),-1,ceil(c.getMaxHp()*v/100.0),false,false)),
            (c,t,v)->ceil(c.getMaxHp()*v/100.0)+"만큼 피해 감소 (최대 HP의 "+number(v)+"%)");}

    public static SkillEffect clericAttackDamage(){return described(
            (e,c,t,v)->{e.dealDamage(c,t,c.getAttack()*v/100.0,true,false);e.tryTransferPurification(c,t);},
            (c,t,v)->ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%) · 피해 적용 후 정화 이전 조건 확인");}
    public static SkillEffect holyStrike(){return described(
            (e,c,t,v)->{e.dealDamage(c,t,c.getAttack()*v/100.0,true,false);e.tryTransferPurification(c,t);double healPercent=20+steps(v,80,16)*4;e.heal(c,ceil(c.getDefense()*healPercent/100.0));},
            (c,t,v)->{double healPercent=20+steps(v,80,16)*4;return ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%) · "+ceil(c.getDefense()*healPercent/100.0)+"만큼 회복 (방어력의 "+number(healPercent)+"%)";});}
    public static SkillEffect purifyingFlame(){return described(
            (e,c,t,v)->{e.gainPurification(c,ceil(c.getDefense()*v/100.0));e.tryTransferPurification(c,t);},
            (c,t,v)->"정화 "+ceil(c.getDefense()*v/100.0)+" 획득 (방어력의 "+number(v)+"%) · 피해와 피격/공격 발동 효과 없이 대상에게 이전 조건 확인");}
    public static SkillEffect healingPrayer(){return described(
            (e,c,t,v)->{int reduction=ceil(c.getDefense()*1.5);e.applyStatus(c,new StatusEffect("HEALING_PRAYER@"+c.getUnitId(),StatusType.DAMAGE_REDUCTION,c.getUnitId(),-1,reduction,true,true));e.heal(c,ceil((c.getMaxHp()-c.getHp())*v/100.0));},
            (c,t,v)->"받는 피해 "+ceil(c.getDefense()*1.5)+"% 감소 (방어력의 150%) · 잃은 HP의 "+number(v)+"%인 "+ceil((c.getMaxHp()-c.getHp())*v/100.0)+" 회복 · 다음 자신의 턴 시작 직전까지");}
    public static SkillEffect divineIntervention(){return described(
            (e,c,t,v)->{e.heal(c,ceil(c.getMaxHp()*v/100.0));e.applyStatus(c,new StatusEffect("DIVINE_INTERVENTION@"+c.getUnitId(),StatusType.INVINCIBLE_DIRECT,c.getUnitId(),-1,0,false,true));},
            (c,t,v)->ceil(c.getMaxHp()*v/100.0)+"만큼 회복 (최대 HP의 "+number(v)+"%) · 일반 피해 무적 · 다음 자신의 턴 시작 직전까지");}
    public static SkillEffect defenseBlessing(){return described(
            (e,c,t,v)->{int gain=ceil(c.getDefense()*v/100.0);e.applyStatus(t,new StatusEffect("DEFENSE_BLESSING@"+c.getUnitId(),StatusType.DEFENSE_FLAT_UP,c.getUnitId(),-1,gain,true,true));},
            (c,t,v)->"대상 방어력 +"+ceil(c.getDefense()*v/100.0)+" (시전자 방어력의 "+number(v)+"%) · 중첩 · 전투 종료 시까지");}
    public static SkillEffect healFromCasterDefense(){return described(
            (e,c,t,v)->e.heal(t,ceil(c.getDefense()*v/100.0)),
            (c,t,v)->ceil(c.getDefense()*v/100.0)+"만큼 회복 (시전자 방어력의 "+number(v)+"%)");}
    public static SkillEffect absolution(){return described(
            (e,c,t,v)->{e.removeHighestStackDispellableDebuff(t);e.heal(t,ceil(c.getDefense()*v/100.0));},
            (c,t,v)->"해제 가능한 부정적 상태 중 스택이 가장 높은 1개 제거 · "+ceil(c.getDefense()*v/100.0)+"만큼 회복 (시전자 방어력의 "+number(v)+"%)");}
    public static SkillEffect lifeBlessing(){return described(
            (e,c,t,v)->{int amount=ceil(c.getMaxHp()*v/100.0);e.applyMaxHpBuff(c,t,amount,-1,"LIFE_BLESSING@"+c.getUnitId(),true);},
            (c,t,v)->"대상 최대 HP와 현재 HP +"+ceil(c.getMaxHp()*v/100.0)+" (시전자 최대 HP의 "+number(v)+"%) · 중첩 · 전투 종료 시까지");}
    public static SkillEffect attackBlessing(){return described(
            (e,c,t,v)->{int gain=ceil(c.getAttack()*v/100.0);e.applyStatus(t,new StatusEffect("ATTACK_BLESSING@"+c.getUnitId(),StatusType.ATTACK_FLAT_UP,c.getUnitId(),-1,gain,true,true));},
            (c,t,v)->"대상 공격력 +"+ceil(c.getAttack()*v/100.0)+" (시전자 공격력의 "+number(v)+"%) · 중첩 · 전투 종료 시까지");}
    public static SkillEffect healingWave(){return described(
            (e,c,t,v)->{int amount=ceil(c.getDefense()*v/100.0);for(BattleUnit ally:e.alliesInRange(c,1))e.heal(ally,amount);},
            (c,t,v)->"자신과 1칸 안의 모든 아군 HP "+ceil(c.getDefense()*v/100.0)+" 회복 (방어력의 "+number(v)+"%)");}
    public static SkillEffect heavenlyPassage(){return described(
            (e,c,t,v)->e.amplifyPurification(c,v),
            (c,t,v)->"현재 정화를 "+number(v)+"%로 증폭"+(c.sum(StatusType.PURIFICATION_STACK)<=0?" (현재 증폭할 정화 없음)":" (현재 "+number(c.sum(StatusType.PURIFICATION_STACK))+" → "+ceil(c.sum(StatusType.PURIFICATION_STACK)*v/100.0)+")"));}
    public static SkillEffect purificationBaptism(){return described(
            (e,c,t,v)->e.gainPurification(c,ceil(c.getDefense()*v/100.0)),
            (c,t,v)->"정화 "+ceil(c.getDefense()*v/100.0)+" 획득 (방어력의 "+number(v)+"%) · 피해 없음");}
    public static SkillEffect withdrawGrace(){return described(
            (e,c,t,v)->{e.removeAllDispellableBenefits(c);for(BattleUnit enemy:e.enemiesOf(c))if(com.pas.game.map.BattleGrid.distance(c.getTile(),enemy.getTile())<=1)e.removeAllDispellableBenefits(enemy);},
            (c,t,v)->"자신과 1칸 안의 모든 적에게서 해제 가능한 이로운 효과를 모두 제거 · 패시브 효과 유지");}
    public static SkillEffect answeredPrayer(){return described(
            (e,c,t,v)->e.reduceAllCooldowns(t,4),(c,t,v)->"대상의 모든 스킬 쿨타임 4턴 감소");}
    public static SkillEffect crusade(){return described(
            (e,c,t,v)->{int hp=ceil(v),step=(int)steps(v,100,50),stat=10+step*5;for(BattleUnit ally:e.alliesInRange(c,1)){String source=c.getUnitId()+"@CRUSADE";ally.addStatus(new StatusEffect("CRUSADE_HP@"+source,StatusType.MAX_HP_UP,source,-1,hp,true,true,"성전"));ally.increaseCurrentHpWithMax(hp);e.applyStatus(ally,new StatusEffect("CRUSADE_ATK@"+source,StatusType.ATTACK_FLAT_UP,source,-1,stat,true,true));e.applyStatus(ally,new StatusEffect("CRUSADE_DEF@"+source,StatusType.DEFENSE_FLAT_UP,source,-1,stat,true,true));}},
            (c,t,v)->{int step=(int)steps(v,100,50),stat=10+step*5;return "자신과 1칸 안의 모든 아군 최대 HP·현재 HP +"+ceil(v)+", 공격력·방어력 +"+stat+" · 중첩 · 전투 종료 시까지";});}
    public static SkillEffect sanctuary(){return described(
            (e,c,t,v)->e.installSanctuary(c,ceil(v),5),
            (c,t,v)->"현재 칸 중심 1칸 범위에 성역 설치 · 아군 턴 종료 시 HP "+ceil(v)+" 회복 및 해제 가능한 부정적 상태 1개 제거 · 플레이어 진영 턴 기준 5턴");}
    public static SkillEffect divineFragment(){return described(
            (e,c,t,v)->e.activateDivineFragment(c,5),
            (c,t,v)->"싱글: 일반 피해 무적과 저지불가 5턴 · 멀티: 2칸 안에서 턴을 시작한 모든 아군 HP 300 회복 · 성직자 턴 기준 5턴");}
    public static SkillEffect divinePunishment(){return described(
            (e,c,t,v)->e.castDivinePunishment(c,t),
            (c,t,v)->"보유한 정화를 즉시 전환하고, 이후 획득할 정화 1당 대상의 현재 HP와 최대 HP를 각각 1 감소 · 대상 사망 시 종료");}

    public static SkillEffect wizardManaDischarge(){return described((e,c,t,v)->e.castWizardManaDischarge(c,t,v),(c,t,v)->ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%) · 4회 사용마다 공격력 +2");}
    public static SkillEffect wizardGluttonousHand(){return described((e,c,t,v)->e.castWizardGluttonousHand(c,t,v),(c,t,v)->ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%) · 실제 피해의 10%와 공격력의 20% 회복");}
    public static SkillEffect wizardNetherRepulsion(){return described((e,c,t,v)->e.castWizardNetherRepulsion(c,t,v),(c,t,v)->{double barrier=100+steps(v,80,20)*50;return ceil(c.getAttack()*v/100.0)+"만큼 피해 (공격력의 "+number(v)+"%) · 피해방어 "+ceil(c.getDefense()*barrier/100.0)+" (방어력의 "+number(barrier)+"%)";});}
    public static SkillEffect wizardMagicWard(){return described((e,c,t,v)->e.castWizardMagicWard(c,v),(c,t,v)->ceil(c.getDefense()*v/100.0)+"만큼 피해 방어 (방어력의 "+number(v)+"%) · 중갑 "+number(1+steps(v,200,50))+" 추가");}
    public static SkillEffect wizardChaosDistortion(){return described((e,c,t,v)->e.castWizardChaosDistortion(c,v),(c,t,v)->"적의 명중률 -"+ceil(c.getDefense()*v/100.0)+"%p (방어력의 "+number(v)+"%) · 치명타율 +5%p");}
    public static SkillEffect wizardDevour(){return described((e,c,t,v)->e.castWizardDevour(c,v),(c,t,v)->"다음 자기 턴 시작 직전까지 일반 피해 무효화 · 무효화 직전 피해의 "+number(v)+"% 회복");}
    public static SkillEffect wizardManaDisruption(){return described((e,c,t,v)->e.castWizardManaDisruption(c,t),(c,t,v)->"대상 스킬의 현재 쿨타임 +2 · 마력 활성 시 +4");}
    public static SkillEffect wizardPhantom(){return describedTile((e,c,t,v,tile)->e.castWizardPhantom(c,tile),(c,t,v)->"선택 칸에 3턴간 허수체 설치 · 기본 1회, 마력 활성 시 2회 효과를 대신 받음");}
    public static SkillEffect wizardManaPayment(){return described((e,c,t,v)->e.castWizardManaPayment(c),(c,t,v)->"최대 HP의 10%인 "+ceil(c.getMaxHp()*.1)+" HP 지불 · 다른 모든 스킬 쿨타임 2턴 감소 (마력 4턴)");}
    public static SkillEffect wizardAbyssalMark(){return described((e,c,t,v)->e.castWizardAbyssalMark(c,t,v),(c,t,v)->"3턴간 일반 스킬 피해를 준 아군이 공격력의 "+number(v)+"% 회복 · 한 스킬당 1회");}
    public static SkillEffect wizardDimensionalDrift(){return described((e,c,t,v)->e.castWizardDimensionalDrift(c),(c,t,v)->"다음 자기 턴 시작까지 전장 이탈 · 마력 활성 시 복귀할 인접 칸을 그때 선택");}
    public static SkillEffect wizardOtherworldPower(){return described((e,c,t,v)->e.castWizardOtherworldPower(c,v),(c,t,v)->"공격력 +"+ceil(c.getAttack()*v/100.0)+" (현재 공격력의 "+number(v)+"%) · 다음 자기 턴 종료까지");}
    public static SkillEffect wizardHungryStar(){return describedTile((e,c,t,v,tile)->e.castWizardHungryStar(c,tile,v),(c,t,v)->"공격력의 "+number(v)+"% 피해를 총 4회 가하는 굶주린 별 설치 · 시전자 능력치 저장");}
    public static SkillEffect wizardPastEcho(){return described((e,c,t,v)->e.castWizardPastEcho(c),(c,t,v)->"현재 위치와 HP 저장 · 세 번째 다음 자기 턴 시작에 복원");}
    public static SkillEffect wizardSpellTheft(){return described((e,c,t,v)->e.castWizardSpellTheft(c,t),(c,t,v)->"대상의 가장 최근 해제 가능한 이로운 효과 1개 탈취 · 마력 활성 시 최대 2개");}
    public static SkillEffect wizardExistenceLoan(){return described((e,c,t,v)->e.castWizardExistenceLoan(c),(c,t,v)->"3턴간 비궁극기 스킬 사용 시 선택한 부가효과를 추가 적용 · 중첩 가능");}
    public static SkillEffect wizardInfiniteFragment(){return described((e,c,t,v)->e.castWizardInfiniteFragment(c,v),(c,t,v)->"공격력 +"+ceil(c.getAttack()*v/100.0)+" (현재 공격력의 "+number(v)+"%) · 전투 종료까지");}
    public static SkillEffect wizardDoubleCast(){return described((e,c,t,v)->e.castWizardDoubleCast(c),(c,t,v)->"다음 비궁극기 스킬을 스택 수만큼 추가 발동");}
    public static SkillEffect wizardMeteor(){return describedTile((e,c,t,v,tile)->e.castWizardMeteor(c,tile,v),(c,t,v)->ceil(c.getAttack()*v/100.0)+"만큼 범위 진실 피해 (공격력의 "+number(v)+"%) · 즉발 패시브 미발동");}
    public static SkillEffect wizardOtherworldGate(){return described((e,c,t,v)->e.castWizardOtherworldGate(c,v),(c,t,v)->"현재 칸에 이계의 문 설치 · 적 턴 종료마다 2칸 안에 "+ceil(c.getAttack()*v/100.0)+" 진실 피해 · 시전자 사망 시 즉시 패배");}

    private static String statusText(StatusType type,int turns,double magnitude){String duration=turns<0?"":" · "+turns+"턴";switch(type){case DAMAGE_REDUCTION:return "받는 피해 "+number(magnitude)+"% 감소"+duration;case UNSTOPPABLE:return "저지불가"+duration;case DAZED:return "멍해짐"+duration;case STIFF:return "경직"+duration;case BIND:return "구속"+duration;case CRIT_RATE_UP:return "치명타율 +"+number(magnitude)+"%p"+duration;case ATTACK_FLAT_UP:return "공격력 +"+number(magnitude)+duration;case ATTACK_UP:return "공격력 +"+number(magnitude)+"%"+duration;case RECOIL_AT_TURN_END:return "턴 종료 시 최대 HP의 "+number(magnitude)+"%만큼 피해";case MOVEMENT_DISABLED:return "이동 불가"+duration;case EVASION_DISABLED:return "회피 불가"+duration;default:return com.pas.game.status.StatusDisplay.label(type)+" "+number(magnitude)+duration;}}
    private static double steps(double value,double base,double increment){return increment==0?0:Math.max(0,Math.round((value-base)/increment));}
    private static int ceil(double value){return (int)Math.ceil(value);}
    private static String number(double value){return value==(long)value?String.valueOf((long)value):String.valueOf(value);}
}
