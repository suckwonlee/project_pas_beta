package com.pas.game.multiplayer;

import com.pas.game.battle.result.BattleEvent;
import com.pas.game.battle.result.BattleResult;
import java.util.Map;

/** 서버의 승인 여부와 승인 뒤의 권위 상태를 한 응답으로 돌려준다. */
public final class CommandReceipt {
    private final int protocolVersion;
    private final String requestId,message,snapshotJson,stateDigest;
    private final boolean accepted;
    private final CommandErrorCode code;
    private final long revision;

    public CommandReceipt(int protocolVersion,String requestId,boolean accepted,CommandErrorCode code,String message,long revision,String snapshotJson,String stateDigest){
        this.protocolVersion=protocolVersion;this.requestId=requestId;this.accepted=accepted;this.code=code;this.message=message==null?"":message;this.revision=revision;this.snapshotJson=snapshotJson;this.stateDigest=stateDigest;
    }
    static CommandReceipt fromResult(String requestId,long revision,BattleResult result,BattleSnapshot snapshot){
        String message="";if(result!=null&&!result.getEvents().isEmpty()){BattleEvent event=result.getEvents().get(result.getEvents().size()-1);message=event.getMessage();}
        boolean accepted=result!=null&&result.isSuccess();return new CommandReceipt(MultiplayerProtocol.VERSION,requestId,accepted,accepted?CommandErrorCode.OK:CommandErrorCode.COMMAND_REJECTED,message,revision,snapshot==null?null:snapshot.toJson(),snapshot==null?null:snapshot.getDigest());
    }
    public int getProtocolVersion(){return protocolVersion;}public String getRequestId(){return requestId;}public boolean isAccepted(){return accepted;}public CommandErrorCode getCode(){return code;}public String getMessage(){return message;}public long getRevision(){return revision;}public String getSnapshotJson(){return snapshotJson;}public String getStateDigest(){return stateDigest;}
    public String toJson(){StringBuilder out=new StringBuilder("{");field(out,"protocolVersion",String.valueOf(protocolVersion));field(out,"requestId",WireJson.quote(requestId));field(out,"accepted",String.valueOf(accepted));field(out,"code",WireJson.quote(code.name()));field(out,"message",WireJson.quote(message));field(out,"revision",String.valueOf(revision));field(out,"stateDigest",WireJson.quote(stateDigest));field(out,"snapshotJson",WireJson.quote(snapshotJson));return out.append('}').toString();}
    public static CommandReceipt fromJson(String json){Map<String,String> f=WireJson.parseFlatObject(json);return new CommandReceipt(integer(f,"protocolVersion"),f.get("requestId"),Boolean.parseBoolean(required(f,"accepted")),CommandErrorCode.valueOf(required(f,"code")),f.get("message"),longValue(f,"revision"),f.get("snapshotJson"),f.get("stateDigest"));}
    private static void field(StringBuilder out,String name,String value){if(out.length()>1)out.append(',');out.append(WireJson.quote(name)).append(':').append(value);}
    private static String required(Map<String,String> f,String key){String value=f.get(key);if(value==null)throw new IllegalArgumentException("missing field: "+key);return value;}
    private static int integer(Map<String,String> f,String key){return Integer.parseInt(required(f,key));}private static long longValue(Map<String,String> f,String key){return Long.parseLong(required(f,key));}
}
