package com.pas.game.multiplayer;

import com.pas.game.battle.command.BattleCommand;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.ResolveWizardReturnCommand;
import com.pas.game.battle.command.UsePotionCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.turn.MovementType;
import java.util.Map;

/** 모든 플레이어 명령을 버전이 붙은 평면 JSON 메시지로 변환한다. */
public final class BattleCommandWireCodec {
    private BattleCommandWireCodec(){}

    public static String encode(CommandEnvelope envelope){
        if(envelope==null||envelope.getCommand()==null)throw new IllegalArgumentException("command is required");
        BattleCommand command=envelope.getCommand();StringBuilder out=new StringBuilder("{");
        field(out,"protocolVersion",String.valueOf(envelope.getProtocolVersion()),false);
        field(out,"matchId",WireJson.quote(envelope.getMatchId()),true);field(out,"clientId",WireJson.quote(envelope.getClientId()),true);
        field(out,"requestId",WireJson.quote(envelope.getRequestId()),true);field(out,"expectedRevision",String.valueOf(envelope.getExpectedRevision()),false);
        field(out,"commandType",WireJson.quote(command.getType()),true);field(out,"actorUnitId",WireJson.quote(command.getActorUnitId()),true);
        if(command instanceof MoveCommand){MoveCommand move=(MoveCommand)command;field(out,"destinationTile",String.valueOf(move.getDestinationTile()),false);field(out,"movementType",WireJson.quote(move.getMovementType().name()),true);}
        else if(command instanceof UseSkillCommand){UseSkillCommand skill=(UseSkillCommand)command;field(out,"skillId",WireJson.quote(skill.getSkillId()),true);field(out,"targetUnitId",WireJson.quote(skill.getTargetUnitId()),true);field(out,"targetTile",skill.getTargetTile()==null?"null":String.valueOf(skill.getTargetTile()),false);field(out,"optionId",WireJson.quote(skill.getOptionId()),true);}
        else if(command instanceof UsePotionCommand)field(out,"potionTier",String.valueOf(((UsePotionCommand)command).getPotionTier()),false);
        else if(command instanceof ResolveWizardReturnCommand)field(out,"destinationTile",String.valueOf(((ResolveWizardReturnCommand)command).getDestinationTile()),false);
        else if(!(command instanceof EndTurnCommand))throw new IllegalArgumentException("unsupported command type: "+command.getClass().getName());
        return out.append('}').toString();
    }

    public static CommandEnvelope decode(String json){
        Map<String,String> f=WireJson.parseFlatObject(json);int version=integer(f,"protocolVersion");long revision=longValue(f,"expectedRevision");
        String actor=required(f,"actorUnitId"),type=required(f,"commandType");BattleCommand command;
        switch(type){
            case "MOVE":command=new MoveCommand(actor,integer(f,"destinationTile"),MovementType.valueOf(required(f,"movementType")));break;
            case "USE_SKILL":command=new UseSkillCommand(actor,required(f,"skillId"),f.get("targetUnitId"),nullableInteger(f,"targetTile"),f.get("optionId"));break;
            case "USE_POTION":command=new UsePotionCommand(actor,integer(f,"potionTier"));break;
            case "END_TURN":command=new EndTurnCommand(actor);break;
            case "RESOLVE_WIZARD_RETURN":command=new ResolveWizardReturnCommand(actor,integer(f,"destinationTile"));break;
            default:throw new IllegalArgumentException("unsupported command type: "+type);
        }
        return new CommandEnvelope(version,required(f,"matchId"),required(f,"clientId"),required(f,"requestId"),revision,command);
    }

    private static void field(StringBuilder out,String name,String value,boolean ignoredStringFlag){if(out.length()>1)out.append(',');out.append(WireJson.quote(name)).append(':').append(value);}
    private static String required(Map<String,String> fields,String key){String value=fields.get(key);if(value==null||value.isEmpty())throw new IllegalArgumentException("missing field: "+key);return value;}
    private static int integer(Map<String,String> fields,String key){try{return Integer.parseInt(required(fields,key));}catch(NumberFormatException e){throw new IllegalArgumentException("invalid integer: "+key,e);}}
    private static long longValue(Map<String,String> fields,String key){try{return Long.parseLong(required(fields,key));}catch(NumberFormatException e){throw new IllegalArgumentException("invalid long: "+key,e);}}
    private static Integer nullableInteger(Map<String,String> fields,String key){String value=fields.get(key);if(value==null)return null;try{return Integer.valueOf(value);}catch(NumberFormatException e){throw new IllegalArgumentException("invalid integer: "+key,e);}}
}
