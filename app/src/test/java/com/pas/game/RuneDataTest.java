package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.rune.RuneData;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.rune.RuneRepository;
import com.pas.game.unit.PlayerUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RuneDataTest {
    private final RuneRepository repository=new RuneRepository();

    @Test public void legacyRuneNamesArePreserved(){
        assertEquals(Arrays.asList("화염의 룬","혹한의 룬","수호자의 룬","독사의 룬","투지의 룬","혈귀의 룬","필생의 룬","돌격의 룬"),names(repository.primary()));
        assertEquals(Arrays.asList("강격의 룬","부동의 룬","태산의 룬","살의의 룬","잔상의 룬"),names(repository.secondary()));
    }

    @Test public void legacyPassiveTablesArePreserved(){
        assertEquals("rune_fire",repository.primary("fire").getPassiveId());
        assertArrayEquals(new int[]{5,7,10,18,25},repository.primary("fire").getPassiveValues());
        assertArrayEquals(new int[]{2,4,7,11,16},repository.primary("frost").getPassiveValues());
        assertArrayEquals(new int[]{5,10,15,20,30},repository.primary("guardian").getPassiveValues());
        assertArrayEquals(new int[]{1,1,2,3,4},repository.primary("venom").getPassiveValues());
        assertArrayEquals(new int[]{1,2,4,7,10},repository.primary("fighting").getPassiveValues());
        assertArrayEquals(new int[]{3,5,8,12,20},repository.primary("vampire").getPassiveValues());
        assertArrayEquals(new int[]{3,5,8,12,20},repository.primary("must_live").getPassiveValues());
        assertArrayEquals(new int[]{10,20,40,70,100},repository.primary("charge").getPassiveValues());
    }

    @Test public void explicitLegacyStatBonusesReachPlayerStats(){
        RuneLoadout loadout=new RuneLoadout(repository.primary("fire"),2,repository.secondary("mountain"),2);
        PlayerUnit player=new PlayerUnit("P1","용사",1,1,loadout);
        assertEquals(220,player.getMaxHp());
        assertEquals(13,player.getAttack());
        assertEquals(14,player.getDefense());
        assertEquals("화염의 룬 (방화 5 · 공격 +1) / 태산의 룬 (HP +40)",loadout.summary());
    }

    @Test public void criticalAndEvasionUsePercentagePointValues(){
        PlayerUnit critical=new PlayerUnit("P1","용사",1,1,new RuneLoadout(null,1,repository.secondary("killing_intent"),3));
        PlayerUnit evasion=new PlayerUnit("P2","용사",1,2,new RuneLoadout(null,1,repository.secondary("afterimage"),3));
        assertEquals(15,critical.getCriticalRate(),0.001);
        assertEquals(9,evasion.getEvasionRate(),0.001);
        assertEquals("룬 1 없음 / 살의의 룬 (치명타 +9%p)",critical.getRuneLoadout().summary());
        assertEquals("룬 1 없음 / 잔상의 룬 (회피 +3%p)",evasion.getRuneLoadout().summary());
    }

    @Test public void percentagePassiveSummaryShowsItsUnit(){assertEquals("혈귀의 룬 (흡혈 3%) / 룬 2 없음",new RuneLoadout(repository.primary("vampire"),1,null,1).summary());}

    @Test public void battleLogSummaryExcludesDirectStatRunesAndStatBonuses(){
        RuneLoadout loadout=new RuneLoadout(repository.primary("fire"),2,repository.secondary("smash"),1);
        assertEquals("화염의 룬 (방화 5)",loadout.battleEffectSummary());
        assertEquals("",new RuneLoadout(null,1,repository.secondary("smash"),1).battleEffectSummary());
    }

    private List<String> names(List<RuneData> values){java.util.ArrayList<String> names=new java.util.ArrayList<>();for(RuneData value:values)names.add(value.getName());return names;}
}
