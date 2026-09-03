package com.pas.game.skill.data;

import com.pas.game.skill.effect.SkillEffect;
import com.pas.game.unit.BattleUnit;

/** 스킬의 실제 계수와 현재 유닛 능력치를 사람이 읽는 전투 수치로 변환한다. */
public final class SkillDetailFormatter {
    private SkillDetailFormatter() {}

    public static String summary(SkillData skill,int upgrades,BattleUnit actor){
        return effectText(skill,skill.valueAt(upgrades),actor,null);
    }

    public static String detail(SkillData skill,int upgrades,BattleUnit actor,BattleUnit target){
        double value=skill.valueAt(upgrades);StringBuilder text=new StringBuilder();
        text.append(effectText(skill,value,actor,target));
        text.append("\n\n등급: ").append(skill.getStartingGrade().upgraded(upgrades).getLabel());
        if(skill.isUltimate())text.append(" · 궁극기");
        text.append("\n사용 횟수: ").append(uses(skill.usesAt(upgrades)));
        text.append("\n쿨타임: ").append(skill.getCooldown()==0?"없음":skill.getCooldown()+"턴");
        text.append("\n사거리: ").append(range(skill));
        text.append("\n대상: ").append(target(skill.getTargetType()));
        if(!skill.getFlavorText().isEmpty())text.append("\n\n").append(skill.getFlavorText());
        if(!skill.isImplemented())text.append("\n\n현재 사용할 수 없는 스킬입니다.");
        return text.toString();
    }

    public static String detail(SkillRuntime runtime,BattleUnit actor,BattleUnit target){
        SkillData skill=runtime.getData();StringBuilder text=new StringBuilder();
        text.append(effectText(skill,runtime.currentValue(),actor,target));
        text.append("\n\n등급: ").append(runtime.getCurrentGrade().getLabel());
        if(skill.isUltimate())text.append(" · 궁극기");
        text.append("\n사용 횟수: ").append(runtime.getRemainingUses()<0?"∞":runtime.getRemainingUses()+" / "+runtime.getMaxUses());
        text.append("\n현재 쿨타임: ").append(runtime.getCooldownRemaining()==0?"사용 가능":runtime.getCooldownRemaining()+"턴");
        text.append("\n기본 쿨타임: ").append(skill.getCooldown()==0?"없음":skill.getCooldown()+"턴");
        text.append("\n사거리: ").append(range(skill));
        text.append("\n대상: ").append(target(skill.getTargetType()));
        if(!skill.getFlavorText().isEmpty())text.append("\n\n").append(skill.getFlavorText());
        return text.toString();
    }

    private static String effectText(SkillData skill,double value,BattleUnit actor,BattleUnit target){
        if(actor==null)return skill.getDescription();StringBuilder text=new StringBuilder();
        for(SkillEffect effect:skill.getEffects()){String part=effect.describe(actor,target,value);if(part==null||part.isEmpty())continue;if(text.length()>0)text.append(" · ");text.append(part);}
        return text.length()==0?skill.getDescription():text.toString();
    }

    private static String range(SkillData skill){if(skill.getTargetType()==TargetType.ALL_ENEMIES)return "모든 적";return skill.getRange()==0?"같은 칸":skill.getRange()+"칸";}
    private static String target(TargetType type){switch(type){case ENEMY:return "적 1명";case ALLY:return "아군 1명";case SELF:return "자신";case TILE:return "칸";case ALL_ENEMIES:return "모든 적";case ALL_ENEMIES_IN_RANGE:return "사거리 안의 모든 적";default:return "대상 없음";}}
    private static String uses(int value){return value<0?"∞":value+"회";}
}
