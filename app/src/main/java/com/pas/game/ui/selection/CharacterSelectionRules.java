package com.pas.game.ui.selection;

import com.pas.game.skill.data.SkillData;

/** 캐릭터 선택 화면의 디버그 전용 진입 조건. */
public final class CharacterSelectionRules {
    private CharacterSelectionRules(){}

    public static boolean hasCompleteDebugSkillSet(SkillData[] slots){
        if(slots==null||slots.length<6)return false;
        for(int i=2;i<6;i++)if(slots[i]==null)return false;
        return true;
    }

    public static boolean shouldOpenRuneSelection(boolean debugUnlocked,boolean available,int selectedIndex,int tappedIndex,SkillData[] slots){
        return debugUnlocked&&available&&selectedIndex==tappedIndex&&hasCompleteDebugSkillSet(slots);
    }
}
