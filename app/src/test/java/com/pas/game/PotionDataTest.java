package com.pas.game;

import static org.junit.Assert.*;
import com.pas.game.item.potion.PotionData;
import com.pas.game.item.potion.PotionInventory;
import com.pas.game.item.potion.PotionRepository;
import org.junit.Test;

public class PotionDataTest {
    @Test public void chapterSelectsMatchingPotionTier(){
        PotionRepository repository=new PotionRepository();
        assertArrayEquals(new int[]{50,80,100,150,200},repository.all().stream().mapToInt(PotionData::getHealing).toArray());
        assertEquals("치유 물약",repository.forChapter(1).getName());
        assertEquals("엘릭시르",repository.forChapter(5).getName());
        assertEquals("엘릭시르",repository.forChapter(99).getName());
    }
    @Test public void inventoryNeverExceedsThree(){
        PotionInventory inventory=new PotionInventory(new PotionRepository().forChapter(1),9);
        assertEquals(3,inventory.getCount());assertEquals(0,inventory.add(2));assertTrue(inventory.consumeOne());assertEquals(2,inventory.getCount());assertEquals(1,inventory.add(5));assertEquals(3,inventory.getCount());
    }
    @Test public void adventureStartsWithoutFreePotions(){
        PotionInventory inventory=new PotionInventory(new PotionRepository().forChapter(1),0);
        assertTrue(inventory.isEmpty());assertEquals(0,inventory.getCount());
    }
    @Test public void differentPotionTypesShareTheThreeItemLimit(){
        PotionRepository repository=new PotionRepository();PotionData healing=repository.forChapter(1),elixir=repository.forChapter(5);PotionInventory inventory=new PotionInventory(healing,1);
        assertEquals(2,inventory.add(elixir,2));assertEquals(3,inventory.getCount());assertEquals(1,inventory.getCount(healing));assertEquals(2,inventory.getCount(elixir));assertEquals(2,inventory.getOwnedPotions().size());
        assertTrue(inventory.consumeOne(healing));assertEquals(elixir,inventory.getPotion());assertEquals(2,inventory.getCount());
    }
}
