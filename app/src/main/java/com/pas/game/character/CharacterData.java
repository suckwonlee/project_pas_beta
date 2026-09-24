package com.pas.game.character;

public final class CharacterData {
    private final String id,name,flavorText,startingPassiveId;
    private final int portraitResource;
    private final int startingPassiveLevel;
    private final int maxHp,attack,defense;
    private final double criticalRate,evasionRate;
    private final boolean available;
    private String skinId;
    public String getSkinId(){return skinId==null?"default":skinId;}
    public CharacterData withSkin(String requested){
        CharacterData copy=new CharacterData(id,name,flavorText,portraitResource,available,maxHp,attack,defense,criticalRate,evasionRate,startingPassiveId,startingPassiveLevel);
        copy.skinId=com.pas.game.character.skin.SkinRepository.BUILT_IN.resolve(this,requested).getId();
        return copy;
    }
    public CharacterData(String id,String name,String flavorText,int portraitResource,boolean available){this(id,name,flavorText,portraitResource,available,180,12,14,6,6,null,0);}
    public CharacterData(String id,String name,String flavorText,int portraitResource,boolean available,String startingPassiveId,int startingPassiveLevel){this(id,name,flavorText,portraitResource,available,180,12,14,6,6,startingPassiveId,startingPassiveLevel);}
    public CharacterData(String id,String name,String flavorText,int portraitResource,boolean available,int maxHp,int attack,int defense,double criticalRate,double evasionRate,String startingPassiveId,int startingPassiveLevel){this.id=id;this.name=name;this.flavorText=flavorText;this.portraitResource=portraitResource;this.available=available;this.maxHp=maxHp;this.attack=attack;this.defense=defense;this.criticalRate=criticalRate;this.evasionRate=evasionRate;this.startingPassiveId=startingPassiveId;this.startingPassiveLevel=Math.max(0,startingPassiveLevel);}
    public String getId(){return id;} public String getName(){return name;} public String getFlavorText(){return flavorText;}
    public int getDefaultPortraitResource(){return portraitResource;}
    public int getPortraitResource(){return getPortraitResource(getSkinId());}
    public int getPortraitResource(String skinId){return com.pas.game.character.skin.SkinRepository.BUILT_IN.resolve(this,skinId).portraitResource(portraitResource);}
    public int getSelectionResource(String skinId){return com.pas.game.character.skin.SkinRepository.BUILT_IN.resolve(this,skinId).selectionResource(portraitResource);}
    public int getMarkerResource(String skinId){return com.pas.game.character.skin.SkinRepository.BUILT_IN.resolve(this,skinId).markerResource(portraitResource);}
    public boolean isAvailable(){return available;}
    public int getMaxHp(){return maxHp;} public int getAttack(){return attack;} public int getDefense(){return defense;}
    public double getCriticalRate(){return criticalRate;} public double getEvasionRate(){return evasionRate;}
    public String getStartingPassiveId(){return startingPassiveId;} public int getStartingPassiveLevel(){return startingPassiveLevel;}
}
