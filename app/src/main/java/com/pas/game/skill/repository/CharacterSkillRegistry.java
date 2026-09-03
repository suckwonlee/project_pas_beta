package com.pas.game.skill.repository;

import java.util.LinkedHashMap;
import java.util.Map;

/** 캐릭터 ID를 해당 캐릭터 전용 스킬 목록에 연결한다. */
public final class CharacterSkillRegistry {
    private final Map<String,SkillRepository> repositories=new LinkedHashMap<>();
    public CharacterSkillRegistry(){
        repositories.put("HERO",new HeroSkillRepository());
        repositories.put("HUNTER",new HunterSkillRepository());
        repositories.put("CLERIC",new ClericSkillRepository());
        repositories.put("WIZARD",new WizardSkillRepository());
    }
    public SkillRepository find(String characterId){return repositories.get(characterId);}
}
