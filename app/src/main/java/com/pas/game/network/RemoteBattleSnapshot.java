package com.pas.game.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pas.game.multiplayer.CommandReceipt;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/** Network-only display DTO: never reconstruct an engine or reapply runes/passives here. */
public final class RemoteBattleSnapshot {
    public long revision;
    public int round;
    public String outcome, pendingWizardReturnOwnerId;
    public boolean networkCoop;
    public Turn turn;
    public List<Unit> units;
    public List<String> logs;
    public List<JsonObject> traps, decoys, sanctuaries, potions;
    public JsonObject wizard;
    public static final class Turn {
        public String activeUnitId;
        public int movesRemaining, skillsRemaining;
        public boolean blocked;
    }
    public static final class Unit {
        public String unitId, name, team;
        public int playerSlot, hp, maxHp, attack, defense, tile, barrier;
        public double criticalRate, criticalDamageMultiplier, evasionRate;
        public boolean dead, onField;
        public List<Skill> skills;
        public List<JsonObject> statuses, effects, passives;
    }
    public static final class Skill {
        public int slot, upgradeSteps, remainingUses, maxUses, cooldownRemaining;
        public String id;
    }
    public static RemoteBattleSnapshot fromReceipt(CommandReceipt receipt) {
        String json = receipt.getSnapshotJson();
        if (json == null || !digest(json).equals(receipt.getStateDigest())) {
            throw new IllegalArgumentException("전투 상태 검증에 실패했습니다.");
        }
        RemoteBattleSnapshot value = new Gson().fromJson(json, RemoteBattleSnapshot.class);
        if (value == null || value.revision != receipt.getRevision() || value.turn == null
                || value.units == null || value.logs == null || value.outcome == null) {
            throw new IllegalArgumentException("전투 상태 형식이 올바르지 않습니다.");
        }
        return value;
    }
    public Unit find(String id) {
        if (id == null) return null;
        for (Unit unit : units) if (id.equals(unit.unitId)) return unit;
        return null;
    }
    private static String digest(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
