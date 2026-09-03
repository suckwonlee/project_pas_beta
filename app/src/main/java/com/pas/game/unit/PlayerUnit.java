package com.pas.game.unit;

import com.pas.game.rune.RuneLoadout;
import com.pas.game.rune.RuneStats;

public final class PlayerUnit extends BattleUnit {
    private final int playerSlot;
    private final RuneLoadout runeLoadout;
    private final int markerResource;
    public PlayerUnit(String unitId,String name,int tile,int playerSlot){this(unitId,name,tile,playerSlot,RuneLoadout.none());}
    public PlayerUnit(String unitId,String name,int tile,int playerSlot,RuneLoadout runes){this(unitId,name,tile,playerSlot,runes,RuneStats.from(runes));}
    public PlayerUnit(String unitId,String name,int tile,int playerSlot,RuneLoadout runes,int maxHp,int attack,int defense,double criticalRate,double evasionRate,int markerResource){this(unitId,name,tile,playerSlot,runes,RuneStats.from(runes,maxHp,attack,defense,criticalRate,evasionRate),markerResource);}
    private PlayerUnit(String unitId,String name,int tile,int playerSlot,RuneLoadout runes,RuneStats stats){this(unitId,name,tile,playerSlot,runes,stats,0);}
    private PlayerUnit(String unitId,String name,int tile,int playerSlot,RuneLoadout runes,RuneStats stats,int markerResource){super(unitId,name,Team.PLAYER,stats.getMaxHp(),stats.getAttack(),stats.getDefense(),stats.getCriticalRate(),stats.getEvasionRate(),tile);this.playerSlot=playerSlot;this.runeLoadout=runes;this.markerResource=markerResource;}
    public int getPlayerSlot(){return playerSlot;}
    public RuneLoadout getRuneLoadout(){return runeLoadout;}
    public int getMarkerResource(){return markerResource;}
}
