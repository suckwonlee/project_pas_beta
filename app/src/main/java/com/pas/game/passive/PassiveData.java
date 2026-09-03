package com.pas.game.passive;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 패시브 정의는 불변이며 플레이어·몬스터 런타임에서 함께 재사용한다. */
public final class PassiveData {
    public interface LevelDescription { String describe(double value,String formattedValue); }
    private final String id,name,description,valueSuffix;
    private final Set<PassiveTrigger> triggers;
    private final double[] levelValues;
    private final PassiveEffect effect;
    private final LevelDescription levelDescription;
    public PassiveData(String id,String name,String description,Set<PassiveTrigger> triggers,double[] levelValues,PassiveEffect effect){this(id,name,description,triggers,levelValues,"",effect);}
    public PassiveData(String id,String name,String description,Set<PassiveTrigger> triggers,double[] levelValues,String valueSuffix,PassiveEffect effect){this(id,name,description,triggers,levelValues,valueSuffix,effect,null);}
    public PassiveData(String id,String name,String description,Set<PassiveTrigger> triggers,double[] levelValues,String valueSuffix,PassiveEffect effect,LevelDescription levelDescription){this.id=id;this.name=name;this.description=description;this.triggers=Collections.unmodifiableSet(triggers.isEmpty()?EnumSet.noneOf(PassiveTrigger.class):EnumSet.copyOf(triggers));this.levelValues=levelValues.clone();this.valueSuffix=valueSuffix==null?"":valueSuffix;this.effect=effect;this.levelDescription=levelDescription;}
    public String getId(){return id;} public String getName(){return name;} public String getDescription(){return description;}
    public Set<PassiveTrigger> getTriggers(){return triggers;} public boolean respondsTo(PassiveTrigger trigger){return triggers.contains(trigger);}
    public double valueAt(int level){if(levelValues.length==0)return 0;return levelValues[Math.max(0,Math.min(level-1,levelValues.length-1))];}
    public int getLevelCount(){return levelValues.length;} public String getValueSuffix(){return valueSuffix;}
    public String describeAt(int level){double value=valueAt(level);String formatted=format(value)+valueSuffix;return levelDescription==null?description.replace("%s",formatted):levelDescription.describe(value,formatted);}
    public PassiveEffect getEffect(){return effect;}
    private String format(double value){return value==(long)value?String.valueOf((long)value):String.valueOf(value);}
}
