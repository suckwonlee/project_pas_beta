package com.pas.game.multiplayer;

import com.pas.game.character.CharacterData;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.skill.data.SkillData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 서버가 확정한 플레이어 한 명의 전투 시작 설정. 플레이어별 설정을 서로 독립시킨다. */
public final class PlayerBattleSetup {
    private final int playerSlot;
    private final CharacterData character;
    private final List<SkillData> loadout;
    private final List<Integer> upgrades;
    private final RuneLoadout runes;

    public PlayerBattleSetup(int playerSlot,CharacterData character,List<SkillData> loadout,List<Integer> upgrades,RuneLoadout runes){
        if(playerSlot<1)throw new IllegalArgumentException("playerSlot must be at least 1");
        this.playerSlot=playerSlot;
        this.character=character;
        this.loadout=loadout==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(loadout));
        this.upgrades=upgrades==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(upgrades));
        this.runes=runes==null?RuneLoadout.none():runes;
    }

    public int getPlayerSlot(){return playerSlot;}
    public CharacterData getCharacter(){return character;}
    public List<SkillData> getLoadout(){return loadout;}
    public List<Integer> getUpgrades(){return upgrades;}
    public RuneLoadout getRunes(){return runes;}
}
