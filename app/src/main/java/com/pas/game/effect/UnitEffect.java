package com.pas.game.effect;

/** 동일 효과라도 출처별로 독립 보관되며, 같은 출처의 재부여는 수치를 갱신한다. */
public final class UnitEffect {
    private final UnitEffectType type;
    private final String sourceKey;
    private final String sourceDisplayName;
    private final double magnitude;

    public UnitEffect(UnitEffectType type,String sourceKey,double magnitude){
        this(type,sourceKey,magnitude,"");
    }
    public UnitEffect(UnitEffectType type,String sourceKey,double magnitude,String sourceDisplayName){
        if(type==null)throw new IllegalArgumentException("effect type is required");
        this.type=type;
        this.sourceKey=sourceKey==null?"default":sourceKey;
        this.sourceDisplayName=sourceDisplayName==null?"":sourceDisplayName;
        this.magnitude=Math.max(0,magnitude);
    }
    public UnitEffectType getType(){return type;}
    public String getSourceKey(){return sourceKey;}
    public String getSourceDisplayName(){return sourceDisplayName;}
    public double getMagnitude(){return magnitude;}
}
