package com.pas.game.status;

public enum StatusType {
    HUNT_TURN_SKIP(ValueKind.FLAT), SELF_STUN(ValueKind.FLAT),
    DAZED(ValueKind.DURATION), STIFF(ValueKind.DURATION), BIND(ValueKind.DURATION), UNSTOPPABLE(ValueKind.DURATION),
    POISON(ValueKind.FLAT), FIRE(ValueKind.FLAT), SHARP(ValueKind.PERCENT),
    DAMAGE_REDUCTION(ValueKind.PERCENT), CRIT_RATE_UP(ValueKind.PERCENTAGE_POINT), EVADE_UP(ValueKind.PERCENTAGE_POINT),
    TAUNT(ValueKind.DURATION), ATTACK_FLAT_UP(ValueKind.FLAT), ATTACK_UP(ValueKind.PERCENT), DAMAGE_TAKEN_UP(ValueKind.PERCENT),
    GUARDIAN_AURA(ValueKind.FLAT), JUSTICE_DOT(ValueKind.PERCENT), RECOIL_AT_TURN_END(ValueKind.PERCENT),
    MOVEMENT_DISABLED(ValueKind.DURATION), EVASION_DISABLED(ValueKind.DURATION), FLAT_DAMAGE_REDUCTION(ValueKind.FLAT),
    DOT_DAMAGE_TWICE(ValueKind.DURATION), EMERGENCY_ESCAPE_READY(ValueKind.FLAT),
    DEFENSE_FLAT_UP(ValueKind.FLAT), MAX_HP_UP(ValueKind.FLAT), INVINCIBLE_DIRECT(ValueKind.DURATION),
    PURIFICATION_STACK(ValueKind.FLAT), PURIFICATION_EXECUTION(ValueKind.FLAT), DIVINE_FRAGMENT(ValueKind.DURATION),
    DIVINE_PUNISHMENT(ValueKind.DURATION),
    MANA_ACTIVE(ValueKind.DURATION), HIT_CHANCE_REDUCTION(ValueKind.PERCENT), DEVOUR(ValueKind.PERCENT),
    SKILL_HEAVY_ARMOR(ValueKind.FLAT), ABYSSAL_MARK(ValueKind.PERCENT), DOUBLE_CAST(ValueKind.FLAT),
    EXISTENCE_LOAN(ValueKind.FLAT), DIMENSIONAL_DRIFT(ValueKind.DURATION), PAST_ECHO(ValueKind.DURATION);

    public enum ValueKind { DURATION, FLAT, PERCENT, PERCENTAGE_POINT }
    private final ValueKind valueKind;
    StatusType(ValueKind valueKind){this.valueKind=valueKind;}
    public ValueKind getValueKind(){return valueKind;}
    public String getValueSuffix(){if(valueKind==ValueKind.PERCENT)return "%";if(valueKind==ValueKind.PERCENTAGE_POINT)return "%p";return "";}
}
