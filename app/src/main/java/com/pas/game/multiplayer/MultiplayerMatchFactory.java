package com.pas.game.multiplayer;

import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.BattleFactory;
import com.pas.game.battle.engine.JavaRandomProvider;
import com.pas.game.ai.EncounterEnemyAI;
import com.pas.game.debug.DebugOptions;
import com.pas.game.item.potion.PotionInventory;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.PlayerUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 로비에서 확정된 참가자 구성으로 결정적 서버 매치를 만든다. */
public final class MultiplayerMatchFactory {
    private MultiplayerMatchFactory(){}
    public static AuthoritativeBattleSession create(String matchId,List<ControlledPlayerSetup> roster,long randomSeed,DebugOptions debug,PotionInventory potions){
        if(roster==null||roster.isEmpty())throw new IllegalArgumentException("roster is required");
        List<PlayerBattleSetup> players=new ArrayList<>();Set<Integer> slots=new HashSet<>();Set<String> controllingClients=new HashSet<>();
        for(ControlledPlayerSetup entry:roster){if(!slots.add(entry.getPlayer().getPlayerSlot()))throw new IllegalArgumentException("duplicate player slot: "+entry.getPlayer().getPlayerSlot());players.add(entry.getPlayer());controllingClients.add(entry.getClientId());}
        JavaRandomProvider random=new JavaRandomProvider(randomSeed);BattleEngine engine=BattleFactory.createParty(players,controllingClients.size()>1,random,debug==null?new DebugOptions():debug,potions==null?PotionInventory.empty():potions);
        Map<Integer,String> unitBySlot=new HashMap<>();for(BattleUnit unit:engine.getState().getUnits())if(unit instanceof PlayerUnit)unitBySlot.put(((PlayerUnit)unit).getPlayerSlot(),unit.getUnitId());
        Map<String,List<String>> ownership=new HashMap<>();for(ControlledPlayerSetup entry:roster)ownership.computeIfAbsent(entry.getClientId(),key->new ArrayList<>()).add(unitBySlot.get(entry.getPlayer().getPlayerSlot()));
        return new AuthoritativeBattleSession(matchId,engine,ownership,new EncounterEnemyAI(random));
    }
}
