package com.pas.game.passive;

import java.util.HashSet;
import java.util.Set;

/** 동일 패시브라도 캐릭터·룬·몬스터 고유 출처별로 독립 장착·중첩할 수 있다. */
public final class PassiveRuntime {
    private final PassiveData data;
    private final String sourceKey;
    private int level;
    private boolean enabled=true;
    private final Set<String> triggeredTargetIds=new HashSet<>();
    private final java.util.Map<String,Integer> targetHitCounts=new java.util.HashMap<>();
    /** Per-owner, per-battle and per-target counter. Counts wrap at the trigger interval. */
    public boolean countTargetHit(String targetId,int interval){
        int count=targetHitCounts.getOrDefault(targetId,0)+1;
        targetHitCounts.put(targetId,count%interval);return count>=interval;
    }
    public PassiveRuntime(PassiveData data,int level,String sourceKey){this.data=data;this.level=Math.max(1,level);this.sourceKey=sourceKey==null?"default":sourceKey;}
    public PassiveData getData(){return data;} public int getLevel(){return level;} public void setLevel(int value){level=Math.max(1,value);}
    public String getSourceKey(){return sourceKey;} public String getRuntimeKey(){return data.getId()+"@"+sourceKey;}
    public String getDisplayName(){return data.getName()+" "+level;}
    public double currentValue(){return data.valueAt(level);} public boolean isEnabled(){return enabled;} public void setEnabled(boolean value){enabled=value;}
    public boolean triggerOncePerTarget(String unitId){return unitId!=null&&triggeredTargetIds.add(unitId);}
}
