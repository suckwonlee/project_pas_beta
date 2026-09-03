package com.pas.game.rune;

public final class RuneLoadout {
    private final RuneData primary;
    private final int primaryLevel;
    private final RuneData secondary;
    private final int secondaryLevel;
    public RuneLoadout(RuneData primary,int primaryLevel,RuneData secondary,int secondaryLevel){this.primary=primary;this.primaryLevel=Math.max(1,primaryLevel);this.secondary=secondary;this.secondaryLevel=Math.max(1,secondaryLevel);}
    public static RuneLoadout none(){return new RuneLoadout(null,1,null,1);}
    public RuneData getPrimary(){return primary;} public int getPrimaryLevel(){return primaryLevel;}
    public RuneData getSecondary(){return secondary;} public int getSecondaryLevel(){return secondaryLevel;}
    public String summary(){String one=primary==null?"룬 1 없음":primary.getName()+" ("+primary.summary(primaryLevel)+")";String two=secondary==null?"룬 2 없음":secondary.getName()+" ("+secondary.summary(secondaryLevel)+")";return one+" / "+two;}
    public String battleEffectSummary(){return primary==null?"":primary.getName()+" ("+primary.passiveSummary(primaryLevel)+")";}
}
