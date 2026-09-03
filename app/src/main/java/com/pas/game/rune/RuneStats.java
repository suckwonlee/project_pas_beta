package com.pas.game.rune;

/** 룬의 명시적 능력치 증가만 전투 유닛 생성 수치로 변환한다. */
public final class RuneStats {
    private int maxHp,attack,defense;
    private double criticalRate,evasionRate;
    private RuneStats(int maxHp,int attack,int defense,double criticalRate,double evasionRate){this.maxHp=maxHp;this.attack=attack;this.defense=defense;this.criticalRate=criticalRate;this.evasionRate=evasionRate;}
    public static RuneStats from(RuneLoadout loadout){RuneStats stats=new RuneStats();if(loadout!=null){stats.apply(loadout.getPrimary(),loadout.getPrimaryLevel());stats.apply(loadout.getSecondary(),loadout.getSecondaryLevel());}return stats;}
    public static RuneStats from(RuneLoadout loadout,int maxHp,int attack,int defense,double criticalRate,double evasionRate){RuneStats stats=new RuneStats(maxHp,attack,defense,criticalRate,evasionRate);if(loadout!=null){stats.apply(loadout.getPrimary(),loadout.getPrimaryLevel());stats.apply(loadout.getSecondary(),loadout.getSecondaryLevel());}return stats;}
    private RuneStats(){this(180,12,14,6,6);}
    private void apply(RuneData rune,int level){if(rune==null)return;double value=rune.bonusValue(level);switch(rune.getBonusStat()){case ATTACK:attack+=(int)Math.ceil(value);break;case DEFENSE:defense+=(int)Math.ceil(value);break;case MAX_HP:maxHp+=(int)Math.ceil(value);break;case CRITICAL_RATE:criticalRate+=value;break;case EVASION_RATE:evasionRate+=value;break;default:break;}}
    public int getMaxHp(){return maxHp;} public int getAttack(){return attack;} public int getDefense(){return defense;} public double getCriticalRate(){return criticalRate;} public double getEvasionRate(){return evasionRate;}
}
