package com.pas.game.rune;

import java.util.Arrays;

/** 구버전 룬 파일에서 추출한 이름과 수치만 보관한다. */
public final class RuneData {
    public enum Slot { PRIMARY, SECONDARY }
    public enum Stat { NONE, ATTACK, DEFENSE, MAX_HP, CRITICAL_RATE, EVASION_RATE }

    private final String id;
    private final String name;
    private final Slot slot;
    private final String passiveName;
    private final int[] passiveValues;
    private final Stat bonusStat;
    private final double bonusPerLevel;
    private final int bonusFromRuneLevel;

    private RuneData(String id,String name,Slot slot,String passiveName,int[] passiveValues,Stat bonusStat,double bonusPerLevel,int bonusFromRuneLevel){
        this.id=id;this.name=name;this.slot=slot;this.passiveName=passiveName;this.passiveValues=passiveValues==null?new int[0]:Arrays.copyOf(passiveValues,passiveValues.length);this.bonusStat=bonusStat;this.bonusPerLevel=bonusPerLevel;this.bonusFromRuneLevel=bonusFromRuneLevel;
    }

    public static RuneData primary(String id,String name,String passiveName,int[] passiveValues,Stat levelTwoBonus,double bonusValue){return new RuneData(id,name,Slot.PRIMARY,passiveName,passiveValues,levelTwoBonus,bonusValue,2);}
    public static RuneData secondary(String id,String name,Stat stat,double valuePerLevel){return new RuneData(id,name,Slot.SECONDARY,null,null,stat,valuePerLevel,1);}

    public String getId(){return id;} public String getName(){return name;} public Slot getSlot(){return slot;}
    public String getPassiveId(){return slot==Slot.PRIMARY?"rune_"+id:null;}
    public String getPassiveName(){return passiveName;} public int[] getPassiveValues(){return Arrays.copyOf(passiveValues,passiveValues.length);}
    public Stat getBonusStat(){return bonusStat;}
    public int passiveLevelForRuneLevel(int runeLevel){return runeLevel==3?2:1;}
    public int passiveValue(int passiveLevel){if(passiveValues.length==0)return 0;int index=Math.max(0,Math.min(passiveLevel-1,passiveValues.length-1));return passiveValues[index];}
    public double bonusValue(int runeLevel){if(runeLevel<bonusFromRuneLevel)return 0;return slot==Slot.SECONDARY?bonusPerLevel*Math.max(1,runeLevel):bonusPerLevel;}
    public String summary(int runeLevel){
        if(slot==Slot.PRIMARY){int passiveLevel=passiveLevelForRuneLevel(runeLevel);String text=passiveName+" "+passiveValue(passiveLevel)+passiveValueSuffix();double bonus=bonusValue(runeLevel);return bonus==0?text:text+" · "+statLabel(bonusStat)+" +"+format(bonus)+statValueSuffix(bonusStat);}
        return statLabel(bonusStat)+" +"+format(bonusValue(runeLevel))+statValueSuffix(bonusStat);
    }
    public String passiveSummary(int runeLevel){if(slot!=Slot.PRIMARY||passiveName==null)return "";int passiveLevel=passiveLevelForRuneLevel(runeLevel);return passiveName+" "+passiveValue(passiveLevel)+passiveValueSuffix();}
    private String passiveValueSuffix(){return "vampire".equals(id)?"%":"";}
    private String statValueSuffix(Stat stat){return stat==Stat.CRITICAL_RATE||stat==Stat.EVASION_RATE?"%p":"";}
    private String statLabel(Stat stat){switch(stat){case ATTACK:return "공격";case DEFENSE:return "방어";case MAX_HP:return "HP";case CRITICAL_RATE:return "치명타";case EVASION_RATE:return "회피";default:return "효과";}}
    private String format(double value){return value==(long)value?String.valueOf((long)value):String.valueOf(value);}
}
