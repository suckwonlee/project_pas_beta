package com.pas.game.battle.state;

import com.pas.game.battle.turn.TurnState;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.Team;
import com.pas.game.item.potion.PotionInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 화면과 네트워크가 공유할 수 있는 전투의 단일 상태 원본이다. */
public final class BattleState {
    private final List<BattleUnit> units=new ArrayList<>();
    private final TurnState turn=new TurnState();
    private final List<String> logs=new ArrayList<>();
    private final List<BattleTrap> traps=new ArrayList<>();
    private final List<BattleDecoy> decoys=new ArrayList<>();
    private final List<BattleSanctuary> sanctuaries=new ArrayList<>();
    private final WizardBattleState wizardState=new WizardBattleState();
    private int round=1,turnIndex=-1,reviveResourceCount;
    private boolean networkCoop;
    private PotionInventory potionInventory=PotionInventory.empty();
    private BattleOutcome outcome=BattleOutcome.ONGOING;
    private List<BattleUnit> roundOrder=new ArrayList<>();
    public List<BattleUnit> getUnits(){return Collections.unmodifiableList(units);} public void addUnit(BattleUnit u){units.add(u);}
    public void removeUnit(BattleUnit u){units.remove(u);} public TurnState getTurn(){return turn;}
    public int getRound(){return round;} public void nextRound(){round++;turnIndex=-1;}
    public int nextTurnIndex(){return ++turnIndex;} public void setTurnIndex(int v){turnIndex=v;}
    public List<BattleUnit> getRoundOrder(){return roundOrder;} public void setRoundOrder(List<BattleUnit> value){roundOrder=new ArrayList<>(value);}
    public BattleOutcome getOutcome(){return outcome;} public void setOutcome(BattleOutcome v){outcome=v;}
    public int getReviveResourceCount(){return reviveResourceCount;} public void setReviveResourceCount(int v){reviveResourceCount=Math.max(0,v);}
    public boolean isNetworkCoop(){return networkCoop;} public void setNetworkCoop(boolean value){networkCoop=value;}
    /** @deprecated 캐릭터 수가 아니라 네트워크 협동 여부를 뜻한다. */
    @Deprecated public boolean isMultiplayer(){return isNetworkCoop();}
    /** @deprecated 새 코드는 setNetworkCoop을 사용한다. */
    @Deprecated public void setMultiplayer(boolean value){setNetworkCoop(value);}
    private final java.util.Map<Integer,PotionInventory> playerPotions=new java.util.HashMap<>();
    public void setPlayerPotionInventory(int slot,PotionInventory inventory){playerPotions.put(slot,inventory);}
    public PotionInventory getPotionInventory(int slot){PotionInventory bag=playerPotions.get(slot);return bag==null?potionInventory:bag;}
    public PotionInventory getPotionInventory(){
        BattleUnit active=find(turn.getActiveUnitId());
        if(active instanceof com.pas.game.unit.PlayerUnit){PotionInventory owned=playerPotions.get(((com.pas.game.unit.PlayerUnit)active).getPlayerSlot());if(owned!=null)return owned;}
        return potionInventory;
    }
    public void setPotionInventory(PotionInventory value){potionInventory=value==null?PotionInventory.empty():value;}
    public void log(String line){logs.add(line);} public List<String> getLogs(){return Collections.unmodifiableList(logs);}
    public List<BattleTrap> getTraps(){return Collections.unmodifiableList(traps);} public void addTrap(BattleTrap trap){if(trap!=null)traps.add(trap);} public void removeTrap(BattleTrap trap){traps.remove(trap);}
    public List<BattleDecoy> getDecoys(){return Collections.unmodifiableList(decoys);} public void addDecoy(BattleDecoy decoy){if(decoy!=null)decoys.add(decoy);} public void removeDecoy(BattleDecoy decoy){decoys.remove(decoy);}
    public List<BattleSanctuary> getSanctuaries(){return Collections.unmodifiableList(sanctuaries);} public void addSanctuary(BattleSanctuary sanctuary){if(sanctuary!=null)sanctuaries.add(sanctuary);} public void removeSanctuary(BattleSanctuary sanctuary){sanctuaries.remove(sanctuary);}
    public WizardBattleState getWizardState(){return wizardState;}
    public BattleUnit find(String id){for(BattleUnit u:units)if(u.getUnitId().equals(id))return u;return null;}
    public List<BattleUnit> living(Team team){List<BattleUnit> r=new ArrayList<>();for(BattleUnit u:units)if(!u.isDead()&&u.isOnField()&&u.getTeam()==team)r.add(u);return r;}
    public List<BattleUnit> atTile(int tile,boolean livingOnly){List<BattleUnit> r=new ArrayList<>();for(BattleUnit u:units)if(u.isOnField()&&u.getTile()==tile&&(!livingOnly||!u.isDead()))r.add(u);return r;}
}
