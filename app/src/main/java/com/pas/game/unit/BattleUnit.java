package com.pas.game.unit;

import com.pas.game.effect.UnitEffect;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.passive.PassiveRuntime;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** unitId가 전투 개체의 유일한 식별자이며 이름/캐릭터 타입 중복을 허용한다. */
public abstract class BattleUnit {
    private final String unitId;
    private final String name;
    private final Team team;
    private final int baseMaxHp;
    private int permanentMaxHpReduction;
    private final int baseAttack;
    private final int baseDefense;
    private final double baseCriticalRate;
    private final double baseEvasionRate;
    private int hp;
    private int tile;
    private boolean dead;
    private int barrier;
    private int eligibleRound = 1;
    private int turnsStarted;
    private boolean onField=true;
    private final List<StatusEffect> statuses = new ArrayList<>();
    private final List<SkillRuntime> equippedSkills = new ArrayList<>(Collections.nCopies(6,null));
    private final List<PassiveRuntime> passives = new ArrayList<>();
    private final List<UnitEffect> effects = new ArrayList<>();
    private long statusSequence;

    protected BattleUnit(String unitId, String name, Team team, int maxHp, int attack, int defense,
                         double criticalRate, double evasionRate, int tile) {
        this.unitId=unitId; this.name=name; this.team=team; this.baseMaxHp=maxHp;
        this.baseAttack=attack; this.baseDefense=defense; this.baseCriticalRate=criticalRate; this.baseEvasionRate=evasionRate;
        this.hp=maxHp; this.tile=tile;
    }
    public String getUnitId(){return unitId;} public String getName(){return name;}
    public Team getTeam(){return team;} public int getMaxHp(){return Math.max(0,(int)Math.ceil(baseMaxHp+sum(StatusType.MAX_HP_UP)-permanentMaxHpReduction));}
    public int getHp(){return hp;} public int getTile(){return tile;} public boolean isDead(){return dead;}
    public int getBarrier(){return barrier;} public int getEligibleRound(){return eligibleRound;}
    public int getTurnsStarted(){return turnsStarted;} public int beginTurnCycle(){return ++turnsStarted;}
    public boolean isOnField(){return onField;} public void setOnField(boolean value){onField=value;}
    public boolean participatesInTurns(){return true;} public boolean countsForOutcome(){return true;} public boolean canReceiveSupport(){return true;}
    public void setEligibleRound(int value){eligibleRound=value;} public void setTile(int value){tile=value;}
    public int getBaseAttack(){return baseAttack;}
    public int getAttack(){double beforePercent=baseAttack+sum(StatusType.ATTACK_FLAT_UP);return Math.max(0,(int)Math.ceil(beforePercent*(1+sum(StatusType.ATTACK_UP)/100.0)));}
    public int getDefense(){return Math.max(0,(int)Math.ceil(baseDefense+sumEffect(UnitEffectType.DETERMINATION)+sum(StatusType.DEFENSE_FLAT_UP)));}
    public double getCriticalRate(){return baseCriticalRate+sum(StatusType.CRIT_RATE_UP)+sumEffect(UnitEffectType.CRITICAL_RATE_UP);}
    public double getCriticalDamageMultiplier(){return 1.5+(sumEffect(UnitEffectType.CRITICAL_DAMAGE_UP)+sum(StatusType.SHARP))/100.0;}
    public double getEvasionRate(){
        boolean stiffBlocked=has(StatusType.STIFF)&&!has(StatusType.UNSTOPPABLE);
        if(stiffBlocked||has(StatusType.EVASION_DISABLED))return 0;
        return baseEvasionRate+sum(StatusType.EVADE_UP);
    }
    public double sum(StatusType type){double total=0;for(StatusEffect s:statuses)if(s.getType()==type)total+=s.getMagnitude()*s.getStackCount();return total;}
    public boolean has(StatusType type){for(StatusEffect s:statuses)if(s.getType()==type)return true;return false;}
    public List<StatusEffect> getStatuses(){return Collections.unmodifiableList(statuses);}
    public List<SkillRuntime> getEquippedSkills(){return Collections.unmodifiableList(equippedSkills);}
    public List<PassiveRuntime> getPassives(){return Collections.unmodifiableList(passives);}
    public List<UnitEffect> getEffects(){return Collections.unmodifiableList(effects);}
    public void equipPassive(PassiveRuntime passive){if(passive==null)return;for(int i=0;i<passives.size();i++)if(passives.get(i).getRuntimeKey().equals(passive.getRuntimeKey())){passives.set(i,passive);return;}passives.add(passive);}
    public PassiveRuntime findPassive(String runtimeKey){for(PassiveRuntime passive:passives)if(passive.getRuntimeKey().equals(runtimeKey))return passive;return null;}
    public void removePassive(String runtimeKey){for(Iterator<PassiveRuntime> i=passives.iterator();i.hasNext();)if(i.next().getRuntimeKey().equals(runtimeKey)){i.remove();return;}}
    public void grantEffect(UnitEffect effect){if(effect==null)return;for(int i=0;i<effects.size();i++){UnitEffect current=effects.get(i);if(current.getType()==effect.getType()&&same(current.getSourceKey(),effect.getSourceKey())){effects.set(i,effect);return;}}effects.add(effect);}
    public void stackEffect(UnitEffect effect){if(effect==null)return;for(int i=0;i<effects.size();i++){UnitEffect current=effects.get(i);if(current.getType()==effect.getType()&&same(current.getSourceKey(),effect.getSourceKey())){effects.set(i,new UnitEffect(effect.getType(),effect.getSourceKey(),current.getMagnitude()+effect.getMagnitude(),effect.getSourceDisplayName()));return;}}effects.add(effect);}
    public void removeEffect(UnitEffectType type,String sourceKey){for(Iterator<UnitEffect> i=effects.iterator();i.hasNext();){UnitEffect effect=i.next();if(effect.getType()==type&&same(effect.getSourceKey(),sourceKey))i.remove();}}
    public double sumEffect(UnitEffectType type){double total=0;for(UnitEffect effect:effects)if(effect.getType()==type)total+=effect.getMagnitude();return total;}
    public void equip(SkillRuntime skill){if(skill==null)return;for(int i=0;i<equippedSkills.size();i++)if(equippedSkills.get(i)==null){equippedSkills.set(i,skill);return;}}
    public void replaceSkill(int slot,SkillRuntime skill){if(slot>=0&&slot<equippedSkills.size())equippedSkills.set(slot,skill);}
    public void clearSkills(){for(int i=0;i<equippedSkills.size();i++)equippedSkills.set(i,null);}
    public SkillRuntime findSkill(String id){for(SkillRuntime s:equippedSkills)if(s!=null&&s.getData().getId().equals(id))return s;return null;}
    public void addStatus(StatusEffect effect){
        if(effect.getType()==StatusType.FIRE||effect.getType()==StatusType.POISON){
            for(StatusEffect current:statuses)if(current.getType()==effect.getType()){current.addMagnitude(effect.getMagnitude());current.setApplicationOrder(++statusSequence);return;}
        }
        if(effect.getType()==StatusType.GUARDIAN_AURA){
            for(StatusEffect current:statuses)if(current.getType()==effect.getType()&&same(current.getSourceUnitId(),effect.getSourceUnitId())){current.addMagnitude(effect.getMagnitude());current.setApplicationOrder(++statusSequence);return;}
        }
        if(effect.getType()==StatusType.ABYSSAL_MARK){effect.setApplicationOrder(++statusSequence);statuses.add(effect);return;}
        for(StatusEffect current:statuses){
            if(current.getType()==effect.getType() && same(current.getSourceUnitId(),effect.getSourceUnitId())
                    && (!separateStatSources(effect.getType()) || same(current.getId(),effect.getId()))){
                if(effect.isStackable()){current.addMagnitude(effect.getMagnitude());current.setApplicationOrder(++statusSequence);return;}
                current.refresh(effect.getRemainingTurns(),effect.getMagnitude());current.setApplicationOrder(++statusSequence); return;
            }
        }
        effect.setApplicationOrder(++statusSequence);statuses.add(effect);
    }
    private static boolean separateStatSources(StatusType type) {
        switch (type) {
            case ATTACK_FLAT_UP: case ATTACK_UP: case DEFENSE_FLAT_UP: case MAX_HP_UP:
            case CRIT_RATE_UP: case EVADE_UP: case SHARP: case SKILL_HEAVY_ARMOR:
                return true;
            default: return false;
        }
    }
    private boolean same(String a,String b){return a==null?b==null:a.equals(b);}
    public void removeStatus(StatusType type){for(Iterator<StatusEffect> i=statuses.iterator();i.hasNext();)if(i.next().getType()==type)i.remove();clampHpToMax();}
    public void removeStatusFromSource(StatusType type,String source){for(Iterator<StatusEffect> i=statuses.iterator();i.hasNext();){StatusEffect s=i.next();if(s.getType()==type&&same(s.getSourceUnitId(),source))i.remove();}clampHpToMax();}
    public void removeStatusById(String id){for(Iterator<StatusEffect> i=statuses.iterator();i.hasNext();)if(same(i.next().getId(),id))i.remove();clampHpToMax();}
    public void removeStatusEffect(StatusEffect effect){statuses.remove(effect);clampHpToMax();}
    public List<StatusEffect> tickStatuses(){List<StatusEffect> expiring=new ArrayList<>();for(StatusEffect s:statuses)if(s.getType()!=StatusType.DIVINE_FRAGMENT&&s.getType()!=StatusType.ABYSSAL_MARK&&s.getType()!=StatusType.EXISTENCE_LOAN&&s.getType()!=StatusType.PAST_ECHO&&s.tick())expiring.add(s);return expiring;}
    public List<StatusEffect> tickStatus(StatusType type){List<StatusEffect> expiring=new ArrayList<>();for(StatusEffect s:statuses)if(s.getType()==type&&s.tick())expiring.add(s);return expiring;}
    public void removeStatuses(List<StatusEffect> effects){statuses.removeAll(effects);clampHpToMax();}
    public int absorbWithBarrier(int amount){if(amount<=0||barrier<=0)return Math.max(0,amount);int current=barrier;barrier=0;return Math.max(0,amount-current);}
    public void addBarrier(int amount){barrier=Math.max(0,barrier+amount);}
    public int clearBarrier(){int removed=barrier;barrier=0;return removed;}
    public int damage(int amount){int before=hp;hp=Math.max(0,hp-Math.max(0,amount));if(hp==0)dead=true;return before-hp;}
    public int heal(int amount){if(dead)return 0;int before=hp;hp=Math.min(getMaxHp(),hp+Math.max(0,amount));return hp-before;}
    public void increaseCurrentHpWithMax(int amount){if(dead)return;hp=Math.min(getMaxHp(),hp+Math.max(0,amount));}
    public int reduceMaxHpPermanently(int amount){int before=getMaxHp();permanentMaxHpReduction+=Math.max(0,amount);clampHpToMax();return Math.max(0,before-getMaxHp());}
    public void kill(){hp=0;dead=true;}
    private void clampHpToMax(){hp=Math.min(hp,getMaxHp());if(hp<=0){hp=0;dead=true;}}
    public void setHpForDebug(int value){hp=Math.max(0,Math.min(getMaxHp(),value));dead=hp==0;}
}
