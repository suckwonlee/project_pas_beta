package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.battle.command.BattleCommand;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.ResolveWizardReturnCommand;
import com.pas.game.battle.command.UsePotionCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.BattleFactory;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.state.BattleState;
import com.pas.game.battle.turn.MovementType;
import com.pas.game.character.CharacterData;
import com.pas.game.debug.DebugOptions;
import com.pas.game.item.potion.PotionInventory;
import com.pas.game.multiplayer.AuthoritativeBattleSession;
import com.pas.game.multiplayer.BattleCommandWireCodec;
import com.pas.game.multiplayer.BattleSnapshot;
import com.pas.game.multiplayer.CommandEnvelope;
import com.pas.game.multiplayer.CommandErrorCode;
import com.pas.game.multiplayer.CommandReceipt;
import com.pas.game.multiplayer.ControlledPlayerSetup;
import com.pas.game.multiplayer.LoopbackBattleTransport;
import com.pas.game.multiplayer.MultiplayerProtocol;
import com.pas.game.multiplayer.MultiplayerMatchFactory;
import com.pas.game.multiplayer.PlayerBattleSetup;
import com.pas.game.multiplayer.RemotePlayerController;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class MultiplayerSessionTest {
    private static final RandomProvider ZERO=bound->0;

    @Test public void allCommandsRoundTripThroughJson(){
        List<BattleCommand> commands=Arrays.asList(
                new MoveCommand("P1",5,MovementType.VOLUNTARY),new UseSkillCommand("P1","skill_\"x","E1",12,"선택\nA"),
                new UsePotionCommand("P1",3),new EndTurnCommand("P1"),new ResolveWizardReturnCommand("P1",6));
        for(int i=0;i<commands.size();i++){
            CommandEnvelope decoded=BattleCommandWireCodec.decode(CommandEnvelope.create("match","client","req-"+i,7,commands.get(i)).toJson());
            assertEquals(MultiplayerProtocol.VERSION,decoded.getProtocolVersion());assertEquals(commands.get(i).getType(),decoded.getCommand().getType());assertEquals(commands.get(i).getActorUnitId(),decoded.getCommand().getActorUnitId());assertEquals(7,decoded.getExpectedRevision());
        }
        UseSkillCommand skill=(UseSkillCommand)BattleCommandWireCodec.decode(CommandEnvelope.create("m","c","r",0,commands.get(1)).toJson()).getCommand();assertEquals("skill_\"x",skill.getSkillId());assertEquals("선택\nA",skill.getOptionId());
    }

    @Test public void oneClientCanControlTwoCharactersWithoutNetworkCoopRules(){
        Fixture f=fixture();Map<String,List<String>> owners=new HashMap<>();owners.put("solo",Arrays.asList("P1","P2"));AuthoritativeBattleSession session=new AuthoritativeBattleSession("M",f.engine,owners);
        assertTrue(session.start().isAccepted());assertFalse(f.state.isNetworkCoop());
        assertTrue(session.submit(CommandEnvelope.create("M","solo","1",1,new EndTurnCommand("P1"))).isAccepted());
        assertEquals("P2",f.state.getTurn().getActiveUnitId());assertTrue(session.submit(CommandEnvelope.create("M","solo","2",2,new EndTurnCommand("P2"))).isAccepted());
    }

    @Test public void coopClientsCanOnlyControlTheirOwnCharacter(){
        Fixture f=fixture();Map<String,List<String>> owners=new HashMap<>();owners.put("a",Collections.singletonList("P1"));owners.put("b",Collections.singletonList("P2"));AuthoritativeBattleSession session=new AuthoritativeBattleSession("M",f.engine,owners);session.start();
        assertTrue(f.state.isNetworkCoop());CommandReceipt rejected=session.submit(CommandEnvelope.create("M","b","wrong",1,new EndTurnCommand("P1")));
        assertFalse(rejected.isAccepted());assertEquals(CommandErrorCode.NOT_OWNER,rejected.getCode());assertEquals("P1",f.state.getTurn().getActiveUnitId());
    }

    @Test public void duplicateRequestIsIdempotentAndStaleRevisionIsRejected(){
        Fixture f=fixture();AuthoritativeBattleSession session=AuthoritativeBattleSession.solo("M",f.engine,"solo");session.start();CommandEnvelope request=CommandEnvelope.create("M","solo","same",1,new EndTurnCommand("P1"));
        CommandReceipt first=session.submit(request),duplicate=session.submit(request);assertTrue(first.isAccepted());assertTrue(duplicate.isAccepted());assertEquals(2,session.getRevision());assertEquals("P2",f.state.getTurn().getActiveUnitId());
        CommandReceipt stale=session.submit(CommandEnvelope.create("M","solo","stale",1,new EndTurnCommand("P2")));assertFalse(stale.isAccepted());assertEquals(CommandErrorCode.STALE_REVISION,stale.getCode());assertEquals(2,session.getRevision());
    }

    @Test public void snapshotDigestIsStableAndChangesWithAcceptedState(){
        Fixture f=fixture();AuthoritativeBattleSession session=AuthoritativeBattleSession.solo("M",f.engine,"solo");session.start();BattleSnapshot before=session.currentSnapshot();BattleSnapshot same=session.currentSnapshot();assertEquals(before.getDigest(),same.getDigest());assertTrue(before.toJson().contains("\"networkCoop\":false"));
        session.submit(CommandEnvelope.create("M","solo","end",1,new EndTurnCommand("P1")));assertNotEquals(before.getDigest(),session.currentSnapshot().getDigest());
    }

    @Test public void partyFactoryKeepsCharacterConfigurationsSeparateAndTilesUnique(){
        CharacterData hero=new CharacterData("HERO","용사","",0,true,180,12,14,6,6,null,0);CharacterData cleric=new CharacterData("CLERIC","성직자","",0,true,160,11,18,3,4,null,0);
        List<PlayerBattleSetup> setups=Arrays.asList(new PlayerBattleSetup(1,hero,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none()),new PlayerBattleSetup(2,cleric,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none()));
        BattleEngine solo=BattleFactory.createParty(setups,false,ZERO,new DebugOptions(),PotionInventory.empty());assertFalse(solo.getState().isNetworkCoop());assertEquals(180,solo.getState().find("P1_HERO_1").getMaxHp());assertEquals(160,solo.getState().find("P2_CLERIC_1").getMaxHp());assertNotEquals(solo.getState().find("P1_HERO_1").getTile(),solo.getState().find("P2_CLERIC_1").getTile());
        assertTrue(BattleFactory.createMultiplayer(setups,ZERO,new DebugOptions(),PotionInventory.empty()).getState().isNetworkCoop());
    }

    @Test public void oneCharacterSinglePlayRemainsTheDefault(){
        CharacterData hero=new CharacterData("HERO","용사","",0,true,180,12,14,6,6,null,0);PlayerBattleSetup setup=new PlayerBattleSetup(1,hero,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none());
        AuthoritativeBattleSession session=MultiplayerMatchFactory.create("ONE",Collections.singletonList(new ControlledPlayerSetup("solo",setup)),77,new DebugOptions(),PotionInventory.empty());
        assertFalse(session.getEngine().getState().isNetworkCoop());assertEquals(2,session.getEngine().getState().getUnits().size());assertTrue(session.start().isAccepted());assertEquals("P1_HERO_1",session.getEngine().getState().getTurn().getActiveUnitId());
        BattleEngine legacy=BattleFactory.create(Collections.emptyList(),0,Collections.emptyList(),ZERO,new DebugOptions(),RuneLoadout.none(),hero,PotionInventory.empty());assertFalse(legacy.getState().isNetworkCoop());assertEquals(2,legacy.getState().getUnits().size());assertTrue(legacy.start().isSuccess());
    }

    @Test public void rosterFactoryDistinguishesSoloPartyFromOnlineCoop(){
        CharacterData hero=new CharacterData("HERO","용사","",0,true,180,12,14,6,6,null,0);CharacterData cleric=new CharacterData("CLERIC","성직자","",0,true,160,11,18,3,4,null,0);PlayerBattleSetup one=new PlayerBattleSetup(1,hero,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none()),two=new PlayerBattleSetup(2,cleric,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none());
        AuthoritativeBattleSession solo=MultiplayerMatchFactory.create("S",Arrays.asList(new ControlledPlayerSetup("same",one),new ControlledPlayerSetup("same",two)),1,new DebugOptions(),PotionInventory.empty());assertFalse(solo.getEngine().getState().isNetworkCoop());assertEquals(2,solo.unitsOwnedBy("same").size());
        AuthoritativeBattleSession coop=MultiplayerMatchFactory.create("C",Arrays.asList(new ControlledPlayerSetup("left",one),new ControlledPlayerSetup("right",two)),1,new DebugOptions(),PotionInventory.empty());assertTrue(coop.getEngine().getState().isNetworkCoop());assertEquals(1,coop.unitsOwnedBy("left").size());assertEquals(1,coop.unitsOwnedBy("right").size());
    }

    @Test public void authoritativeServerRunsEnemyTurnBeforeBroadcastingResult(){
        CharacterData hero=new CharacterData("HERO","용사","",0,true,180,12,14,6,6,null,0);PlayerBattleSetup one=new PlayerBattleSetup(1,hero,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none()),two=new PlayerBattleSetup(2,hero,Collections.emptyList(),Collections.emptyList(),RuneLoadout.none());
        AuthoritativeBattleSession session=MultiplayerMatchFactory.create("AI",Arrays.asList(new ControlledPlayerSetup("left",one),new ControlledPlayerSetup("right",two)),2,new DebugOptions(),PotionInventory.empty());session.start();
        assertTrue(session.submit(CommandEnvelope.create("AI","left","l1",1,new EndTurnCommand("P1_HERO_1"))).isAccepted());CommandReceipt receipt=session.submit(CommandEnvelope.create("AI","right","r1",2,new EndTurnCommand("P2_HERO_1")));assertTrue(receipt.isAccepted());assertEquals("P1_HERO_1",session.getEngine().getState().getTurn().getActiveUnitId());assertTrue(receipt.getSnapshotJson().contains("\"activeUnitId\":\"P1_HERO_1\""));
    }

    @Test public void remoteControllerWorksAcrossSerializedLoopbackAndCanResync(){
        Fixture f=fixture();AuthoritativeBattleSession session=AuthoritativeBattleSession.solo("M",f.engine,"solo");session.start();List<CommandReceipt> receipts=new ArrayList<>();List<Throwable> failures=new ArrayList<>();RemotePlayerController controller=new RemotePlayerController("M","solo",1,new LoopbackBattleTransport(session,"solo"),new RemotePlayerController.Listener(){public void onReceipt(CommandReceipt receipt){receipts.add(receipt);}public void onTransportError(Throwable error){failures.add(error);}});
        controller.submit(new EndTurnCommand("P1"));assertTrue(failures.isEmpty());assertEquals(1,receipts.size());assertTrue(receipts.get(0).isAccepted());assertEquals(2,controller.getRevision());assertNotNull(receipts.get(0).getSnapshotJson());
        controller.synchronize();assertEquals(2,receipts.size());assertEquals(receipts.get(0).getStateDigest(),receipts.get(1).getStateDigest());
    }

    @Test public void otherClientReceivesAuthoritativeStatePush(){
        Fixture f=fixture();Map<String,List<String>> owners=new HashMap<>();owners.put("left",Collections.singletonList("P1"));owners.put("right",Collections.singletonList("P2"));AuthoritativeBattleSession session=new AuthoritativeBattleSession("PUSH",f.engine,owners);session.start();List<CommandReceipt> left=new ArrayList<>(),right=new ArrayList<>();
        RemotePlayerController leftController=new RemotePlayerController("PUSH","left",1,new LoopbackBattleTransport(session,"left"),listener(left));new RemotePlayerController("PUSH","right",1,new LoopbackBattleTransport(session,"right"),listener(right));leftController.submit(new EndTurnCommand("P1"));
        assertEquals(1,left.size());assertEquals(1,right.size());assertEquals("PUSH",right.get(0).getRequestId());assertEquals(2,right.get(0).getRevision());assertTrue(right.get(0).getSnapshotJson().contains("\"activeUnitId\":\"P2\""));
    }

    private static RemotePlayerController.Listener listener(List<CommandReceipt> receipts){return new RemotePlayerController.Listener(){public void onReceipt(CommandReceipt receipt){receipts.add(receipt);}public void onTransportError(Throwable error){throw new AssertionError(error);}};}

    private static Fixture fixture(){BattleState state=new BattleState();PlayerUnit p1=new PlayerUnit("P1","P1",1,1);PlayerUnit p2=new PlayerUnit("P2","P2",2,2);EnemyUnit enemy=new EnemyUnit("E","적",12);state.addUnit(p1);state.addUnit(p2);state.addUnit(enemy);return new Fixture(state,new BattleEngine(state,ZERO,new DebugOptions()));}
    private static final class Fixture {final BattleState state;final BattleEngine engine;Fixture(BattleState state,BattleEngine engine){this.state=state;this.engine=engine;}}
}
