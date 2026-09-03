package com.pas.game.skill.data;

/** 전투/챕터 중 변하는 사용횟수, 쿨타임, 강화 단계를 설계도와 분리한다. */
public final class SkillRuntime {
    private final SkillData data;
    private int upgradeSteps;
    private int remainingUses;
    private int cooldownRemaining;

    public SkillRuntime(SkillData data, int upgradeSteps) {
        this.data=data; setUpgradeSteps(upgradeSteps); remainingUses=data.usesAt(this.upgradeSteps);
    }
    public SkillData getData(){return data;} public int getUpgradeSteps(){return upgradeSteps;}
    public int getRemainingUses(){return remainingUses;} public int getMaxUses(){return data.usesAt(upgradeSteps);} public int getCooldownRemaining(){return cooldownRemaining;}
    public SkillGrade getCurrentGrade(){return data.getStartingGrade().upgraded(upgradeSteps);}
    public double currentValue(){return data.valueAt(upgradeSteps);}
    public boolean canUse(){return data.isImplemented() && cooldownRemaining==0 && remainingUses!=0;}
    public void consume(){if(remainingUses>0)remainingUses--; cooldownRemaining=data.getCooldown();}
    public void onOwnerTurnStart(){if(cooldownRemaining>0)cooldownRemaining--;}
    public void restoreUses(int amount){if(remainingUses>=0)remainingUses=Math.min(data.usesAt(upgradeSteps),remainingUses+amount);}
    public void restoreAllUses(){remainingUses=data.usesAt(upgradeSteps);}
    public void resetCooldown(){cooldownRemaining=0;}
    public void reduceCooldown(int amount){cooldownRemaining=Math.max(0,cooldownRemaining-Math.max(0,amount));}
    public void increaseCooldown(int amount){cooldownRemaining=Math.max(0,cooldownRemaining+Math.max(0,amount));}
    public void setUpgradeSteps(int steps){upgradeSteps=Math.max(0,Math.min(steps,SkillGrade.values().length-1-data.getStartingGrade().ordinal()));}
}
