package com.pas.game;

import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.repository.HeroSkillRepository;
import com.pas.game.ui.selection.CharacterSelectionRules;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CharacterSelectionRulesTest {
    private SkillData[] completeSlots(){
        List<SkillData> skills=new HeroSkillRepository().all();
        SkillData[] slots=new SkillData[6];
        slots[0]=skills.get(0);slots[1]=skills.get(3);
        int normalSlot=2;
        for(int i=6;i<skills.size();i++){
            SkillData skill=skills.get(i);
            if(skill.isUltimate())slots[5]=skill;
            else if(normalSlot<5)slots[normalSlot++]=skill;
            if(normalSlot==5&&slots[5]!=null)break;
        }
        return slots;
    }

    @Test public void sameAvailableCharacterReopensRunesOnlyAfterFourDebugSkillsAreSelected(){
        SkillData[] slots=completeSlots();
        assertTrue(CharacterSelectionRules.hasCompleteDebugSkillSet(slots));
        assertTrue(CharacterSelectionRules.shouldOpenRuneSelection(true,true,0,0,slots));
        assertFalse(CharacterSelectionRules.shouldOpenRuneSelection(false,true,0,0,slots));
        assertFalse(CharacterSelectionRules.shouldOpenRuneSelection(true,true,0,1,slots));
        slots[4]=null;
        assertFalse(CharacterSelectionRules.shouldOpenRuneSelection(true,true,0,0,slots));
    }
}
