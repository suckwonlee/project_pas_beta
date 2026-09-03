package com.pas.game.battle.damage;

/**
 * 피해 계산 경로를 구분한다.
 * 모든 피해는 비율 피해 감소의 영향을 받지만, 수치 차감·중갑·피해방어는 일반 피해에만 적용된다.
 */
public enum DamageType {
    DIRECT(true),
    DAMAGE_OVER_TIME(false),
    TRUE_DAMAGE(false);

    private final boolean affectedByDirectMitigation;
    DamageType(boolean affectedByDirectMitigation){this.affectedByDirectMitigation=affectedByDirectMitigation;}
    public boolean isAffectedByDirectMitigation(){return affectedByDirectMitigation;}
}
