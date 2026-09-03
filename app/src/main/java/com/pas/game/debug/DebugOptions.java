package com.pas.game.debug;

/** 설정 쿠폰으로 열린 디버그 패널의 전투 강제 옵션. */
public final class DebugOptions {
    public enum ForcedRoll { NORMAL, SUCCESS, FAILURE }
    public static final String DEVELOPER_COUPON = "PAS-DEV-2026";
    private ForcedRoll critical=ForcedRoll.NORMAL, evasion=ForcedRoll.NORMAL;
    private boolean skipEnemyAi, invinciblePlayers, allSkillsAvailable, detailedLogs=true;
    public ForcedRoll getCritical(){return critical;} public void setCritical(ForcedRoll v){critical=v;}
    public ForcedRoll getEvasion(){return evasion;} public void setEvasion(ForcedRoll v){evasion=v;}
    public boolean isSkipEnemyAi(){return skipEnemyAi;} public void setSkipEnemyAi(boolean v){skipEnemyAi=v;}
    public boolean isInvinciblePlayers(){return invinciblePlayers;} public void setInvinciblePlayers(boolean v){invinciblePlayers=v;}
    public boolean isAllSkillsAvailable(){return allSkillsAvailable;} public void setAllSkillsAvailable(boolean v){allSkillsAvailable=v;}
    public boolean isDetailedLogs(){return detailedLogs;} public void setDetailedLogs(boolean v){detailedLogs=v;}
}
