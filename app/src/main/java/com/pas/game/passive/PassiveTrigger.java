package com.pas.game.passive;

/** 플레이어와 몬스터가 함께 사용하는 결정적 패시브 발동 지점. */
public enum PassiveTrigger {
    BATTLE_START,
    TURN_START,
    TURN_END,
    BEFORE_DAMAGE_DEALT,
    BEFORE_DAMAGE_TAKEN,
    AFTER_DAMAGE_DEALT,
    AFTER_DAMAGE_TAKEN,
    AFTER_SKILL_USED
}
