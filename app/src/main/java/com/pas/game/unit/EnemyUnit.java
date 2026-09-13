package com.pas.game.unit;

public final class EnemyUnit extends BattleUnit {
    private int ownTurnCount;
    private boolean struggleUsed;
    private final EnemyKind kind;
    private final String summonerId;
    private boolean gateOpened;
    private int summonSequence;
    public EnemyUnit(String unitId,String name,int tile){this(unitId,name,tile,EnemyKind.TRAINING_DUMMY,null,90,10,5);}
    private EnemyUnit(String id,String name,int tile,EnemyKind kind,String summonerId,int hp,int attack,int critical){
        super(id,name,Team.ENEMY,hp,attack,0,critical,0,tile);this.kind=kind;this.summonerId=summonerId;
    }
    public static EnemyUnit redAltarPriest(String id,int tile){return new EnemyUnit(id,"붉은 제단의 사제",tile,EnemyKind.RED_ALTAR_PRIEST,null,350,15,5);}
    public static EnemyUnit contractedDemon(String id,int tile,String summonerId){return new EnemyUnit(id,"계약된 하급악마",tile,EnemyKind.CONTRACTED_LESSER_DEMON,summonerId,100,12,0);}
    public EnemyKind getKind(){return kind;} public String getSummonerId(){return summonerId;}
    public boolean isGateOpened(){return gateOpened;} public void openGate(){gateOpened=true;}
    public int getSummonSequence(){return summonSequence;} public int nextSummonSequence(){return ++summonSequence;}
    public int beginOwnTurn(){return ++ownTurnCount;} public int getOwnTurnCount(){return ownTurnCount;}
    public boolean isStruggleUsed(){return struggleUsed;} public void markStruggleUsed(){struggleUsed=true;}
}
