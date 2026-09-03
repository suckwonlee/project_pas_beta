package com.pas.game.battle.engine;

import java.util.Random;

public final class JavaRandomProvider implements RandomProvider {
    private final Random random;
    public JavaRandomProvider(){this(new Random());} public JavaRandomProvider(long seed){this(new Random(seed));}
    private JavaRandomProvider(Random random){this.random=random;}
    @Override public int nextInt(int bound){return random.nextInt(bound);}
}
