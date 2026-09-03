package com.pas.game.character;

public final class CharacterData {
    private final String id,name,flavorText,startingPassiveId;
    private final int portraitResource;
    private final int startingPassiveLevel;
    private final int maxHp,attack,defense;
    private final double criticalRate,evasionRate;
    private final boolean available;
    public CharacterData(String id,String name,String flavorText,int portraitResource,boolean available){this(id,name,flavorText,portraitResource,available,180,12,14,6,6,null,0);}
    public CharacterData(String id,String name,String flavorText,int portraitResource,boolean available,String startingPassiveId,int startingPassiveLevel){this(id,name,flavorText,portraitResource,available,180,12,14,6,6,startingPassiveId,startingPassiveLevel);}
    public CharacterData(String id,String name,String flavorText,int portraitResource,boolean available,int maxHp,int attack,int defense,double criticalRate,double evasionRate,String startingPassiveId,int startingPassiveLevel){this.id=id;this.name=name;this.flavorText=flavorText;this.portraitResource=portraitResource;this.available=available;this.maxHp=maxHp;this.attack=attack;this.defense=defense;this.criticalRate=criticalRate;this.evasionRate=evasionRate;this.startingPassiveId=startingPassiveId;this.startingPassiveLevel=Math.max(0,startingPassiveLevel);}
    public String getId(){return id;} public String getName(){return name;} public String getFlavorText(){return flavorText;}
    public int getPortraitResource(){return portraitResource;} public boolean isAvailable(){return available;}
    public int getMaxHp(){return maxHp;} public int getAttack(){return attack;} public int getDefense(){return defense;}
    public double getCriticalRate(){return criticalRate;} public double getEvasionRate(){return evasionRate;}
    public String getStartingPassiveId(){return startingPassiveId;} public int getStartingPassiveLevel(){return startingPassiveLevel;}
}
