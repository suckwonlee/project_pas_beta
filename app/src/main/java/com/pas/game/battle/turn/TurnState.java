package com.pas.game.battle.turn;

/** 현재 유닛의 이동/스킬 행동권을 추적한다. */
public final class TurnState {
    private String activeUnitId;
    private int movesRemaining;
    private int skillsRemaining;
    private boolean blocked;
    public void begin(String unitId,boolean blocked){activeUnitId=unitId;movesRemaining=blocked?0:1;skillsRemaining=blocked?0:1;this.blocked=blocked;}
    public String getActiveUnitId(){return activeUnitId;} public int getMovesRemaining(){return movesRemaining;}
    public int getSkillsRemaining(){return skillsRemaining;} public boolean isBlocked(){return blocked;}
    public boolean canMove(){return movesRemaining>0||skillsRemaining>0;}
    /** 기본 이동권이 없으면 스킬권 하나를 추가 이동으로 변환한다. */
    public boolean consumeMove(){if(movesRemaining>0){movesRemaining--;return true;}if(skillsRemaining>0){skillsRemaining--;return true;}return false;}
    public boolean consumeSkill(){if(skillsRemaining<=0)return false;skillsRemaining--;return true;}
    public void addSkillAction(){skillsRemaining++;}
    public void clear(){activeUnitId=null;movesRemaining=skillsRemaining=0;blocked=false;}
}
