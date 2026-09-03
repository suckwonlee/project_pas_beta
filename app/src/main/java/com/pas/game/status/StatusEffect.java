package com.pas.game.status;

import java.util.HashSet;
import java.util.Set;

/** 공통 상태 인스턴스. magnitude는 비율/수치이며 sourceUnitId로 중복 출처를 구분한다. */
public final class StatusEffect {
    private final String id;
    private final StatusType type;
    private final String sourceUnitId;
    private final String sourceDisplayName;
    private int remainingTurns;
    private double magnitude;
    private int stackCount;
    private final boolean stackable;
    private final boolean dispellable;
    private final Set<String> consumedTurnTokens = new HashSet<>();
    private boolean reinforcedSinceOwnerTurn = true;
    private long applicationOrder;

    public StatusEffect(String id, StatusType type, String sourceUnitId, int remainingTurns,
                        double magnitude, boolean stackable, boolean dispellable) {
        this(id,type,sourceUnitId,remainingTurns,magnitude,stackable,dispellable,"");
    }
    public StatusEffect(String id, StatusType type, String sourceUnitId, int remainingTurns,
                        double magnitude, boolean stackable, boolean dispellable, String sourceDisplayName) {
        this.id = id;
        this.type = type;
        this.sourceUnitId = sourceUnitId;
        this.sourceDisplayName = sourceDisplayName == null ? "" : sourceDisplayName;
        this.remainingTurns = remainingTurns;
        this.magnitude = magnitude;
        this.stackable = stackable;
        this.dispellable = dispellable;
        this.stackCount = 1;
    }

    public String getId() { return id; }
    public StatusType getType() { return type; }
    public String getSourceUnitId() { return sourceUnitId; }
    public String getSourceDisplayName() { return sourceDisplayName; }
    public int getRemainingTurns() { return remainingTurns; }
    public double getMagnitude() { return magnitude; }
    public int getStackCount() { return stackCount; }
    public boolean isStackable() { return stackable; }
    public boolean isDispellable() { return dispellable; }
    public boolean isPermanent() { return remainingTurns < 0; }
    public long getApplicationOrder(){return applicationOrder;} public void setApplicationOrder(long value){applicationOrder=value;}
    public boolean tick() { if (remainingTurns > 0) remainingTurns--; return remainingTurns == 0; }
    public void refresh(int turns, double newMagnitude) {
        remainingTurns = Math.max(remainingTurns, turns);
        magnitude = Math.max(magnitude, newMagnitude);
        if (stackable) stackCount++;
    }
    public void scaleMagnitude(double factor) { magnitude *= Math.max(0, factor); }
    public void scaleMagnitudeAndFloor(double factor) { magnitude = Math.floor(magnitude * Math.max(0, factor)); }
    public void addMagnitude(double amount) { magnitude = Math.max(0, magnitude + amount); reinforcedSinceOwnerTurn = true; }
    public void decreaseMagnitude(double amount) { magnitude = Math.max(0, magnitude - Math.max(0, amount)); }
    public boolean consumeReinforcedFlag() { boolean value=reinforcedSinceOwnerTurn; reinforcedSinceOwnerTurn=false; return value; }
    public boolean consumeOncePerActorTurn(String token) { return consumedTurnTokens.add(token); }
    public StatusEffect copy(String newId,String newSource){StatusEffect copy=new StatusEffect(newId,type,newSource,remainingTurns,magnitude,stackable,dispellable,sourceDisplayName);copy.stackCount=stackCount;return copy;}
}
