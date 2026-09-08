package com.pas.game.battle.engine;

import com.pas.game.battle.state.BattleState;
import com.pas.game.debug.DebugOptions;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.character.CharacterData;
import com.pas.game.passive.PassiveRepository;
import com.pas.game.passive.PassiveRuntime;
import com.pas.game.item.potion.PotionInventory;
import com.pas.game.multiplayer.PlayerBattleSetup;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import com.pas.game.skill.repository.EnemySkillRepository;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public final class BattleFactory {
    private BattleFactory() {}
    public static BattleEngine create(List<SkillData> loadout,int secondPlayer,List<Integer> upgrades,RandomProvider random,DebugOptions debug){
        return create(loadout,secondPlayer,upgrades,random,debug,RuneLoadout.none());
    }
    public static BattleEngine create(List<SkillData> loadout,int secondPlayer,List<Integer> upgrades,RandomProvider random,DebugOptions debug,RuneLoadout runes){
        return create(loadout,secondPlayer,upgrades,random,debug,runes,null);
    }
    public static BattleEngine create(List<SkillData> loadout,int secondPlayer,List<Integer> upgrades,RandomProvider random,DebugOptions debug,RuneLoadout runes,CharacterData character){
        return create(loadout,secondPlayer,upgrades,random,debug,runes,character,PotionInventory.empty());
    }
    public static BattleEngine create(List<SkillData> loadout,int secondPlayer,List<Integer> upgrades,RandomProvider random,DebugOptions debug,RuneLoadout runes,CharacterData character,PotionInventory potions){
        List<PlayerBattleSetup> players=new ArrayList<>();
        players.add(new PlayerBattleSetup(1,character,loadout,upgrades,runes));
        if(secondPlayer>0)players.add(new PlayerBattleSetup(2,character,loadout,upgrades,runes));
        return createParty(players,false,random,debug,potions);
    }

    /** 각 클라이언트가 캐릭터를 나눠 조종하는 네트워크 협동 전투를 만든다. */
    public static BattleEngine createMultiplayer(List<PlayerBattleSetup> players,RandomProvider random,DebugOptions debug,PotionInventory potions){
        return createParty(players,true,random,debug,potions);
    }

    /** 캐릭터 수와 접속자 수를 분리한다. networkCoop=false면 한 사람이 여러 캐릭터를 조종한다. */
    public static BattleEngine createParty(List<PlayerBattleSetup> players,boolean networkCoop,RandomProvider random,DebugOptions debug,PotionInventory potions){
        if(players==null||players.isEmpty())throw new IllegalArgumentException("at least one player is required");
        BattleState state=new BattleState();state.setNetworkCoop(networkCoop);state.setPotionInventory(potions);
        List<Integer> starts=new ArrayList<>(Arrays.asList(1,2,5));List<Integer> enemyStarts=Arrays.asList(8,11,12);
        for(PlayerBattleSetup setup:players){
            if(starts.isEmpty())throw new IllegalArgumentException("the current battle board supports up to 3 players");
            CharacterData character=setup.getCharacter();String characterId=character==null?"HERO":character.getId();String characterName=character==null?"용사후보":character.getName();
            int tile=starts.remove(random.nextInt(starts.size()));int slot=setup.getPlayerSlot();
            PlayerUnit player=createPlayer("P"+slot+"_"+characterId+"_1",characterName+" P"+slot,tile,slot,setup.getRunes(),character);
            equip(player,setup.getLoadout(),setup.getUpgrades());equipRunePassive(player,setup.getRunes(),"rune:P"+slot);equipStartingPassive(player,character,"character:"+characterId);state.addUnit(player);
        }
        EnemyUnit enemy=new EnemyUnit("ENEMY_DUMMY_1","살아있는 허수아비",random.choose(enemyStarts));
        enemy.equip(new SkillRuntime(EnemySkillRepository.basicAttack(),0)); enemy.equip(new SkillRuntime(EnemySkillRepository.slam(),0)); state.addUnit(enemy);
        return new BattleEngine(state,random,debug);
    }
    private static PlayerUnit createPlayer(String id,String name,int tile,int slot,RuneLoadout runes,CharacterData character){return character==null?new PlayerUnit(id,name,tile,slot,runes):new PlayerUnit(id,name,tile,slot,runes,character.getMaxHp(),character.getAttack(),character.getDefense(),character.getCriticalRate(),character.getEvasionRate(),character.getPortraitResource());}
    private static void equip(PlayerUnit player,List<SkillData> loadout,List<Integer> upgrades){for(int i=0;i<6;i++){SkillData skill=loadout!=null&&i<loadout.size()?loadout.get(i):null;player.replaceSkill(i,skill==null?null:new SkillRuntime(skill,upgrades!=null&&i<upgrades.size()?upgrades.get(i):0));}}
    private static void equipRunePassive(PlayerUnit player,RuneLoadout runes,String source){if(runes==null||runes.getPrimary()==null)return;com.pas.game.rune.RuneData rune=runes.getPrimary();PassiveRuntime runtime=new PassiveRepository().create(rune.getPassiveId(),rune.passiveLevelForRuneLevel(runes.getPrimaryLevel()),source+":"+rune.getId());player.equipPassive(runtime);}
    private static void equipStartingPassive(PlayerUnit player,CharacterData character,String source){if(character==null||character.getStartingPassiveId()==null)return;PassiveRuntime runtime=new PassiveRepository().create(character.getStartingPassiveId(),character.getStartingPassiveLevel(),source);player.equipPassive(runtime);}
}
