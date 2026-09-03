package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.character.CharacterData;
import com.pas.game.character.CharacterRepository;
import com.pas.game.passive.PassiveRepository;
import com.pas.game.status.StatusDisplay;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.PlayerUnit;
import java.util.List;
import org.junit.Test;

public class CharacterStatusDisplayTest {
    @Test public void legacyCharacterFlavorTextsArePreserved(){
        List<CharacterData> characters=new CharacterRepository().all();
        assertEquals(8,characters.size());
        assertEquals("용사후보",characters.get(0).getName());
        assertEquals(PassiveRepository.HEAVY_ARMOR,characters.get(0).getStartingPassiveId());
        assertEquals(2,characters.get(0).getStartingPassiveLevel());
        assertEquals("추상적이었기에 수많았던 용사 후보 중 한 명. 자신이 용사라 믿고 세상의 구원을 위해 일어섰다.",characters.get(0).getFlavorText());
        assertEquals("침략해온 이계의 존재들과는 달리 물질계를 관찰하는걸 즐기는 존재. 죽어가던 병사와 계약을 맺어 빙의했다.",characters.get(7).getFlavorText());
    }

    @Test public void statusLabelsAndCategoriesCoverBuffsAndDebuffs(){
        assertEquals("화염",StatusDisplay.label(StatusType.FIRE));
        assertEquals("멍해짐",StatusDisplay.label(StatusType.DAZED));
        assertEquals("공격력↑",StatusDisplay.label(StatusType.ATTACK_UP));
        assertTrue(StatusDisplay.isDebuff(StatusType.POISON));
        assertFalse(StatusDisplay.isDebuff(StatusType.DAMAGE_REDUCTION));
        for(StatusType type:StatusType.values())assertFalse(StatusDisplay.label(type).isEmpty());
    }

    @Test public void statusChipValueShowsMagnitudeTurnsAndStacks(){
        StatusEffect fire=new StatusEffect("F",StatusType.FIRE,"E",2,10,true,true);
        StatusEffect dazed=new StatusEffect("D",StatusType.DAZED,"E",2,0,false,true);
        assertEquals("10",StatusDisplay.compactValue(fire));
        assertTrue(StatusDisplay.description(fire).contains("다음 턴까지 화염이 추가되지 않으면"));
        assertTrue(StatusDisplay.description(fire).contains("5% 감소"));
        assertTrue(StatusDisplay.description(fire).contains("소수점 이하는 버립니다"));
        assertEquals("2T",StatusDisplay.compactValue(dazed));
        fire.refresh(2,10);
        assertEquals("10×2",StatusDisplay.compactValue(fire));
        assertEquals("60% · 2T",StatusDisplay.compactValue(new StatusEffect("A",StatusType.ATTACK_UP,"P",2,60,false,true)));
        assertEquals("20%p · 2T",StatusDisplay.compactValue(new StatusEffect("C",StatusType.CRIT_RATE_UP,"P",2,20,false,true)));
        assertEquals("18",StatusDisplay.compactValue(new StatusEffect("F",StatusType.FLAT_DAMAGE_REDUCTION,"P",-1,18,false,false)));
        StatusEffect aura=new StatusEffect("AURA",StatusType.GUARDIAN_AURA,"P",-1,3,false,false);
        aura.addMagnitude(3);
        assertEquals("6",StatusDisplay.compactValue(aura));
        assertTrue(StatusDisplay.description(aura).contains("6만큼 감소"));
        assertFalse(StatusDisplay.compactValue(aura).contains("%"));
        assertFalse(StatusDisplay.description(aura).contains("해제"));
    }

    @Test public void barrierDisplayExplainsOneHitConsumption(){
        assertEquals("피해방어 35",StatusDisplay.barrierLabel(35));
        assertTrue(StatusDisplay.barrierDescription(35).contains("피해방어 수치: 35"));
        assertTrue(StatusDisplay.barrierDescription(35).contains("다음 한 번"));
        assertTrue(StatusDisplay.barrierDescription(35).contains("사라집니다"));
    }

    @Test public void barrierDisappearsAfterBlockingOnePositiveHit(){
        PlayerUnit player=new PlayerUnit("P1","용사",1,1);player.addBarrier(35);
        assertEquals(0,player.absorbWithBarrier(10));assertEquals(0,player.getBarrier());
        player.addBarrier(35);assertEquals(0,player.absorbWithBarrier(0));assertEquals(35,player.getBarrier());
    }
}
