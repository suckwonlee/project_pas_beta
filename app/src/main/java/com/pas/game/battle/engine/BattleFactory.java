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
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import com.pas.game.skill.repository.EnemySkillRepository;
import java.util.Arrays;
import java.util.List;

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
        BattleState state=new BattleState(); state.setMultiplayer(secondPlayer>0);state.setPotionInventory(potions); List<Integer> starts=Arrays.asList(1,2,5); List<Integer> enemyStarts=Arrays.asList(8,11,12);
        String characterId=character==null?"HERO":character.getId();String characterName=character==null?"용사후보":character.getName();
        PlayerUnit p1=createPlayer("P1_"+characterId+"_1",characterName+" P1",random.choose(starts),1,runes,character);equip(p1,loadout,upgrades);equipRunePassive(p1,runes,"rune:P1");equipStartingPassive(p1,character,"character:"+characterId);state.addUnit(p1);
        if(secondPlayer>0){PlayerUnit p2=createPlayer("P2_"+characterId+"_1",characterName+" P2",random.choose(starts),2,runes,character);equip(p2,loadout,upgrades);equipRunePassive(p2,runes,"rune:P2");equipStartingPassive(p2,character,"character:"+characterId);state.addUnit(p2);}
        EnemyUnit enemy=new EnemyUnit("ENEMY_DUMMY_1","살아있는 허수아비",random.choose(enemyStarts));
        enemy.equip(new SkillRuntime(EnemySkillRepository.basicAttack(),0)); enemy.equip(new SkillRuntime(EnemySkillRepository.slam(),0)); state.addUnit(enemy);
        return new BattleEngine(state,random,debug);
    }
    private static PlayerUnit createPlayer(String id,String name,int tile,int slot,RuneLoadout runes,CharacterData character){return character==null?new PlayerUnit(id,name,tile,slot,runes):new PlayerUnit(id,name,tile,slot,runes,character.getMaxHp(),character.getAttack(),character.getDefense(),character.getCriticalRate(),character.getEvasionRate(),character.getPortraitResource());}
    private static void equip(PlayerUnit player,List<SkillData> loadout,List<Integer> upgrades){for(int i=0;i<6;i++){SkillData skill=loadout!=null&&i<loadout.size()?loadout.get(i):null;player.replaceSkill(i,skill==null?null:new SkillRuntime(skill,upgrades!=null&&i<upgrades.size()?upgrades.get(i):0));}}
    private static void equipRunePassive(PlayerUnit player,RuneLoadout runes,String source){if(runes==null||runes.getPrimary()==null)return;com.pas.game.rune.RuneData rune=runes.getPrimary();PassiveRuntime runtime=new PassiveRepository().create(rune.getPassiveId(),rune.passiveLevelForRuneLevel(runes.getPrimaryLevel()),source+":"+rune.getId());player.equipPassive(runtime);}
    private static void equipStartingPassive(PlayerUnit player,CharacterData character,String source){if(character==null||character.getStartingPassiveId()==null)return;PassiveRuntime runtime=new PassiveRepository().create(character.getStartingPassiveId(),character.getStartingPassiveLevel(),source);player.equipPassive(runtime);}
}
