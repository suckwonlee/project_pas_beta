package com.pas.game.item.potion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 서로 다른 포션을 함께 보관하는 전투 공용 인벤토리. 전체 소지 한도는 3개다. */
public final class PotionInventory {
    public static final int MAX_COUNT=3;
    private final Map<Integer,PotionData> potions=new LinkedHashMap<>();
    private final Map<Integer,Integer> counts=new LinkedHashMap<>();
    private PotionData defaultPotion;

    public PotionInventory(PotionData potion,int count){if(potion!=null){defaultPotion=potion;potions.put(potion.getTier(),potion);counts.put(potion.getTier(),0);add(potion,count);}}
    public static PotionInventory empty(){return new PotionInventory(null,0);}

    /** 기존 단일 포션 호출은 현재 보유 중인 첫 포션으로 안전하게 연결한다. */
    public PotionData getPotion(){if(defaultPotion!=null&&getCount(defaultPotion)>0)return defaultPotion;for(PotionData potion:potions.values())if(getCount(potion)>0)return potion;return null;}
    public PotionData find(int tier){PotionData potion=potions.get(tier);return potion!=null&&getCount(potion)>0?potion:null;}
    public List<PotionData> getOwnedPotions(){List<PotionData> owned=new ArrayList<>();for(PotionData potion:potions.values())if(getCount(potion)>0)owned.add(potion);return Collections.unmodifiableList(owned);}
    public int getCount(){int total=0;for(int count:counts.values())total+=count;return total;}
    public int getCount(PotionData potion){return potion==null?0:counts.containsKey(potion.getTier())?counts.get(potion.getTier()):0;}
    public boolean isEmpty(){return getCount()<=0;}
    public boolean consumeOne(){PotionData potion=getPotion();return potion!=null&&consumeOne(potion);}
    public boolean consumeOne(PotionData potion){int count=getCount(potion);if(count<=0)return false;counts.put(potion.getTier(),count-1);return true;}
    public int add(int amount){return defaultPotion==null?0:add(defaultPotion,amount);}
    public int add(PotionData potion,int amount){if(potion==null||amount<=0)return 0;potions.put(potion.getTier(),potion);if(defaultPotion==null)defaultPotion=potion;int before=getCount(potion);int added=Math.min(Math.max(0,MAX_COUNT-getCount()),amount);counts.put(potion.getTier(),before+added);return added;}
}
