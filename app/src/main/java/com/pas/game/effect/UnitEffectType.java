package com.pas.game.effect;

/**
 * 전투 유닛에게 부여되는 이름 있는 효과의 종류다.
 * 패시브, 룬, 장비, 스킬은 효과를 부여할 뿐 실제 규칙은 BattleEngine이 처리한다.
 */
public enum UnitEffectType {
    HEAVY_ARMOR("중갑",""),
    VENOM_COATING("독공격",""),
    LIFESTEAL("흡혈","%"),
    SURVIVAL("생존력",""),
    CRITICAL_DAMAGE_UP("치명","%"),
    CRITICAL_RATE_UP("살의","%p"),
    DETERMINATION("결단",""),
    SHARP_GAIN("예리함 획득","%");

    private final String displayName;
    private final String valueSuffix;

    UnitEffectType(String displayName,String valueSuffix){this.displayName=displayName;this.valueSuffix=valueSuffix;}
    public String getDisplayName(){return displayName;}
    public String getValueSuffix(){return valueSuffix;}
}
