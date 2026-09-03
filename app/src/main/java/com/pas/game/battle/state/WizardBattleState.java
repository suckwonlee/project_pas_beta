package com.pas.game.battle.state;

import com.pas.game.unit.Team;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 마법사 고유 카운터, 예약 복귀, 설치물을 BattleState 안에서 직렬화 가능한 값으로 보관한다. */
public final class WizardBattleState {
    public static final class Drift {
        public final String ownerId; public final int originTile; public final boolean chooseAdjacent;
        public Drift(String ownerId,int originTile,boolean chooseAdjacent){this.ownerId=ownerId;this.originTile=originTile;this.chooseAdjacent=chooseAdjacent;}
    }
    public static final class Echo {
        public final String ownerId; public final int tile,hp; public int remainingStarts=3;
        public Echo(String ownerId,int tile,int hp){this.ownerId=ownerId;this.tile=tile;this.hp=hp;}
    }
    public static final class Star {
        public final String id,ownerId; public final Team team; public final int tile,attack; public final double critRate,critMultiplier,coefficient;
        public int attacksRemaining=4;
        public int extraAttacksPerTick;
        public Star(String id,String ownerId,Team team,int tile,int attack,double critRate,double critMultiplier,double coefficient){this.id=id;this.ownerId=ownerId;this.team=team;this.tile=tile;this.attack=attack;this.critRate=critRate;this.critMultiplier=critMultiplier;this.coefficient=coefficient;}
    }
    public static final class Gate {
        public final String id,ownerId; public final Team team; public final int tile,damage;
        public Gate(String id,String ownerId,Team team,int tile,int damage){this.id=id;this.ownerId=ownerId;this.team=team;this.tile=tile;this.damage=damage;}
    }
    public static final class Loan {
        public int stacks; public int remainingStarts;
        public Loan(int stacks,int remainingStarts){this.stacks=stacks;this.remainingStarts=remainingStarts;}
    }

    private final Map<String,Integer> dischargeUses=new HashMap<>();
    private final Map<String,Integer> doubleCastStacks=new HashMap<>();
    private final Map<String,Loan> loans=new HashMap<>();
    private final Map<String,Drift> drifts=new HashMap<>();
    private final Map<String,Echo> echoes=new HashMap<>();
    private final List<Star> stars=new ArrayList<>();
    private final List<Gate> gates=new ArrayList<>();
    private String pendingReturnOwnerId;

    public int getDischargeUses(String id){return dischargeUses.getOrDefault(id,0);}
    public void setDischargeUses(String id,int value){dischargeUses.put(id,Math.max(0,value));}
    public int getDoubleCastStacks(String id){return doubleCastStacks.getOrDefault(id,0);}
    public void addDoubleCast(String id,int amount){doubleCastStacks.put(id,getDoubleCastStacks(id)+Math.max(0,amount));}
    public int consumeDoubleCast(String id){int value=getDoubleCastStacks(id);doubleCastStacks.remove(id);return value;}
    public Loan getLoan(String id){return loans.get(id);}
    public void addLoan(String id,int amount){Loan loan=loans.get(id);if(loan==null)loans.put(id,new Loan(amount,3));else{loan.stacks+=amount;loan.remainingStarts=3;}}
    public void removeLoan(String id){loans.remove(id);}
    public Map<String,Drift> getDrifts(){return drifts;} public Map<String,Echo> getEchoes(){return echoes;}
    public List<Star> getStars(){return stars;} public List<Gate> getGates(){return gates;}
    public String getPendingReturnOwnerId(){return pendingReturnOwnerId;} public void setPendingReturnOwnerId(String value){pendingReturnOwnerId=value;}
}
