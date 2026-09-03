package com.pas.game.skill.data;

import com.pas.game.skill.effect.SkillEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 강화 전 원본 값과 실행 효과를 함께 가진 불변 스킬 설계도다. */
public final class SkillData {
    private final String id;
    private final String name;
    private final SkillGrade startingGrade;
    private final double baseValue;
    private final double upgradeValue;
    private final int baseUses;
    private final int upgradeUses;
    private final int cooldown;
    private final int range;
    private final int minimumRange;
    private final boolean cardinalTileOnly;
    private final TargetType targetType;
    private final String description;
    private final String flavorText;
    private final List<SkillEffect> effects;
    private final boolean implemented;

    private SkillData(Builder b) {
        id=b.id; name=b.name; startingGrade=b.grade; baseValue=b.baseValue;
        upgradeValue=b.upgradeValue; baseUses=b.baseUses; upgradeUses=b.upgradeUses;
        cooldown=b.cooldown; range=b.range; minimumRange=b.minimumRange; cardinalTileOnly=b.cardinalTileOnly; targetType=b.targetType;
        description=b.description; flavorText=b.flavorText; effects=Collections.unmodifiableList(new ArrayList<>(b.effects));
        implemented=b.implemented;
    }
    public String getId(){return id;} public String getName(){return name;}
    public SkillGrade getStartingGrade(){return startingGrade;}
    public double valueAt(int upgrades){return baseValue + upgradeValue * upgrades;}
    public int usesAt(int upgrades){return baseUses < 0 ? -1 : baseUses + upgradeUses * upgrades;}
    public int getCooldown(){return cooldown;} public int getRange(){return range;}
    public int getMinimumRange(){return minimumRange;} public boolean isCardinalTileOnly(){return cardinalTileOnly;}
    public TargetType getTargetType(){return targetType;} public String getDescription(){return description;}
    public String getFlavorText(){return flavorText;}
    public List<SkillEffect> getEffects(){return effects;} public boolean isImplemented(){return implemented;}
    public boolean isUltimate(){return startingGrade.ordinal() >= SkillGrade.LEGENDARY.ordinal();}

    public static Builder builder(String id, String name, SkillGrade grade){return new Builder(id,name,grade);}
    public static final class Builder {
        private final String id,name; private final SkillGrade grade;
        private double baseValue,upgradeValue; private int baseUses=-1,upgradeUses,cooldown,range,minimumRange;
        private boolean cardinalTileOnly;
        private TargetType targetType=TargetType.ENEMY; private String description="",flavorText="";
        private final List<SkillEffect> effects=new ArrayList<>(); private boolean implemented=true;
        private Builder(String id,String name,SkillGrade grade){this.id=id;this.name=name;this.grade=grade;}
        public Builder value(double base,double upgrade){baseValue=base;upgradeValue=upgrade;return this;}
        public Builder uses(int base,int upgrade){baseUses=base;upgradeUses=upgrade;return this;}
        public Builder cooldown(int value){cooldown=value;return this;}
        public Builder range(int value){range=value;return this;}
        public Builder minimumRange(int value){minimumRange=Math.max(0,value);return this;}
        public Builder cardinalTileOnly(){cardinalTileOnly=true;return this;}
        public Builder target(TargetType value){targetType=value;return this;}
        public Builder describe(String value){description=value;return this;}
        public Builder flavor(String value){flavorText=value==null?"":value;return this;}
        public Builder effect(SkillEffect value){effects.add(value);return this;}
        public Builder unavailable(){implemented=false;return this;}
        public SkillData build(){return new SkillData(this);}
    }
}
