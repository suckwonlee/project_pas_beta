package com.pas.game.item.potion;

/** 챕터 진행과 무관하게 변하지 않는 포션 원본 데이터다. */
public final class PotionData {
    private final int tier;
    private final String name;
    private final int healing;
    private final int price;
    private final String description;

    public PotionData(int tier,String name,int healing,int price,String description){
        this.tier=tier;this.name=name;this.healing=healing;this.price=price;this.description=description;
    }
    public int getTier(){return tier;}
    public String getName(){return name;}
    public int getHealing(){return healing;}
    public int getPrice(){return price;}
    public String getDescription(){return description;}
}
