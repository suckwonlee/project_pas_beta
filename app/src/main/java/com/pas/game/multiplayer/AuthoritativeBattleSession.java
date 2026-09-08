package com.pas.game.multiplayer;

import com.pas.game.battle.command.BattleCommand;
import com.pas.game.ai.EnemyAI;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.result.BattleResult;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.Team;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 서버 권위형 매치. 클라이언트는 명령만 보내고 이 객체만 엔진을 변경한다.
 * 한 clientId에 여러 unitId를 묶을 수 있어 1인 2캐릭터와 2인 협동을 모두 지원한다.
 */
public final class AuthoritativeBattleSession {
    public interface Observer {void onStateChanged(String sourceClientId,BattleSnapshot snapshot);}
    private static final class Cached {final String requestJson;final CommandReceipt receipt;Cached(String requestJson,CommandReceipt receipt){this.requestJson=requestJson;this.receipt=receipt;}}
    private final String matchId;
    private final BattleEngine engine;
    private final EnemyAI enemyAI;
    private final Map<String,Set<String>> ownership=new HashMap<>();
    private final Map<String,Cached> processed=new HashMap<>();
    private final List<Observer> observers=new CopyOnWriteArrayList<>();
    private long revision;
    private boolean started;

    public AuthoritativeBattleSession(String matchId,BattleEngine engine,Map<String,? extends Iterable<String>> clientOwnership){
        this(matchId,engine,clientOwnership,null);
    }

    public AuthoritativeBattleSession(String matchId,BattleEngine engine,Map<String,? extends Iterable<String>> clientOwnership,EnemyAI enemyAI){
        if(blank(matchId)||engine==null)throw new IllegalArgumentException("matchId and engine are required");
        this.matchId=matchId;this.engine=engine;this.enemyAI=enemyAI;
        if(clientOwnership!=null)for(Map.Entry<String,? extends Iterable<String>> entry:clientOwnership.entrySet()){
            if(blank(entry.getKey()))continue;Set<String> units=new HashSet<>();if(entry.getValue()!=null)for(String id:entry.getValue())if(!blank(id))units.add(id);ownership.put(entry.getKey(),units);
        }
        validateOwnership();int controllingClients=0;for(Set<String> ids:ownership.values())if(!ids.isEmpty())controllingClients++;engine.getState().setNetworkCoop(controllingClients>1);
    }

    public static AuthoritativeBattleSession solo(String matchId,BattleEngine engine,String clientId){
        List<String> unitIds=new ArrayList<>();for(BattleUnit unit:engine.getState().getUnits())if(unit.getTeam()==Team.PLAYER)unitIds.add(unit.getUnitId());Map<String,List<String>> map=new HashMap<>();map.put(clientId,unitIds);return new AuthoritativeBattleSession(matchId,engine,map);
    }

    public synchronized CommandReceipt start(){
        if(started)return syncReceipt("START");started=true;BattleResult result=engine.start();if(result.isSuccess())revision++;
        CommandReceipt receipt=CommandReceipt.fromResult("START",revision,result,currentSnapshot());if(receipt.isAccepted())notifyObservers(null);return receipt;
    }

    public synchronized CommandReceipt submit(CommandEnvelope envelope){
        if(envelope==null)return reject(null,CommandErrorCode.INVALID_PAYLOAD,"명령 메시지가 없습니다.");
        if(blank(envelope.getClientId())||blank(envelope.getRequestId())||blank(envelope.getMatchId()))return reject(envelope.getRequestId(),CommandErrorCode.INVALID_PAYLOAD,"matchId, clientId, requestId가 필요합니다.");
        String cacheKey=envelope.getClientId()+"\n"+envelope.getRequestId();String requestJson;
        try{requestJson=envelope.toJson();}catch(RuntimeException e){return reject(envelope.getRequestId(),CommandErrorCode.INVALID_PAYLOAD,e.getMessage());}
        Cached cached=processed.get(cacheKey);if(cached!=null)return cached.requestJson.equals(requestJson)?cached.receipt:reject(envelope.getRequestId(),CommandErrorCode.REQUEST_ID_CONFLICT,"같은 요청 ID에 다른 명령이 사용되었습니다.");
        CommandReceipt receipt=validateAndExecute(envelope);processed.put(cacheKey,new Cached(requestJson,receipt));if(receipt.isAccepted())notifyObservers(envelope.getClientId());return receipt;
    }

    public synchronized CommandReceipt synchronize(String clientId){if(!ownership.containsKey(clientId))return reject("SYNC",CommandErrorCode.UNKNOWN_CLIENT,"등록되지 않은 클라이언트입니다.");return syncReceipt("SYNC");}
    public synchronized long getRevision(){return revision;}public String getMatchId(){return matchId;}public BattleEngine getEngine(){return engine;}
    public synchronized BattleSnapshot currentSnapshot(){return BattleSnapshot.capture(engine.getState(),revision);}
    public synchronized Set<String> unitsOwnedBy(String clientId){Set<String> ids=ownership.get(clientId);return ids==null?Collections.emptySet():Collections.unmodifiableSet(new HashSet<>(ids));}
    public void addObserver(Observer observer){if(observer!=null)observers.add(observer);}public void removeObserver(Observer observer){observers.remove(observer);}

    private CommandReceipt validateAndExecute(CommandEnvelope envelope){
        if(!started)return reject(envelope.getRequestId(),CommandErrorCode.COMMAND_REJECTED,"아직 전투가 시작되지 않았습니다.");
        if(envelope.getProtocolVersion()!=MultiplayerProtocol.VERSION)return reject(envelope.getRequestId(),CommandErrorCode.VERSION_MISMATCH,"지원하지 않는 프로토콜 버전입니다.");
        if(!matchId.equals(envelope.getMatchId()))return reject(envelope.getRequestId(),CommandErrorCode.MATCH_MISMATCH,"다른 매치의 명령입니다.");
        Set<String> owned=ownership.get(envelope.getClientId());if(owned==null)return reject(envelope.getRequestId(),CommandErrorCode.UNKNOWN_CLIENT,"등록되지 않은 클라이언트입니다.");
        BattleCommand command=envelope.getCommand();if(command==null||blank(command.getActorUnitId()))return reject(envelope.getRequestId(),CommandErrorCode.INVALID_PAYLOAD,"행동 유닛이 없습니다.");
        if(!owned.contains(command.getActorUnitId()))return reject(envelope.getRequestId(),CommandErrorCode.NOT_OWNER,"다른 플레이어의 캐릭터는 조종할 수 없습니다.");
        if(envelope.getExpectedRevision()!=revision)return reject(envelope.getRequestId(),CommandErrorCode.STALE_REVISION,"전투 상태가 갱신되었습니다. 최신 상태를 동기화하세요.");
        BattleResult result=engine.execute(command);if(result.isSuccess()){runServerTurns();revision++;}return CommandReceipt.fromResult(envelope.getRequestId(),revision,result,currentSnapshot());
    }
    private void runServerTurns(){
        if(enemyAI==null)return;int guard=0;
        while(engine.getState().getOutcome()==com.pas.game.battle.state.BattleOutcome.ONGOING&&engine.activeUnit()!=null&&engine.activeUnit().getTeam()==Team.ENEMY&&guard++<32){BattleResult result=enemyAI.takeTurn(engine);if(!result.isSuccess())throw new IllegalStateException("server AI command failed: "+(result.getEvents().isEmpty()?"unknown":result.getEvents().get(0).getMessage()));}
        if(guard>=32)throw new IllegalStateException("server AI turn guard exceeded");
    }
    private CommandReceipt syncReceipt(String requestId){BattleSnapshot snapshot=currentSnapshot();return new CommandReceipt(MultiplayerProtocol.VERSION,requestId,true,CommandErrorCode.OK,"",revision,snapshot.toJson(),snapshot.getDigest());}
    private CommandReceipt reject(String requestId,CommandErrorCode code,String message){BattleSnapshot snapshot=currentSnapshot();return new CommandReceipt(MultiplayerProtocol.VERSION,requestId,false,code,message,revision,snapshot.toJson(),snapshot.getDigest());}
    private void validateOwnership(){
        Set<String> assigned=new HashSet<>();for(Map.Entry<String,Set<String>> entry:ownership.entrySet())for(String id:entry.getValue()){
            BattleUnit unit=engine.getState().find(id);if(unit==null||unit.getTeam()!=Team.PLAYER)throw new IllegalArgumentException("ownership contains unknown player unit: "+id);
            if(!assigned.add(id))throw new IllegalArgumentException("a unit cannot have multiple owners: "+id);
        }
        for(BattleUnit unit:engine.getState().getUnits())if(unit.getTeam()==Team.PLAYER&&!assigned.contains(unit.getUnitId()))throw new IllegalArgumentException("player unit has no owner: "+unit.getUnitId());
    }
    private void notifyObservers(String sourceClientId){BattleSnapshot snapshot=currentSnapshot();for(Observer observer:observers)observer.onStateChanged(sourceClientId,snapshot);}
    private static boolean blank(String value){return value==null||value.trim().isEmpty();}
}
