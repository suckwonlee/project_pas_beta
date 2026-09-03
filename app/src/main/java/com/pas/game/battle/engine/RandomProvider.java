package com.pas.game.battle.engine;

import java.util.List;

public interface RandomProvider {
    int nextInt(int bound);
    default <T> T choose(List<T> values){return values.get(nextInt(values.size()));}
}
