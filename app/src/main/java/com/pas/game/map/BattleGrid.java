package com.pas.game.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 4x3 전투판의 좌표, 거리, 인접 칸 규칙을 한 곳에서 관리한다. */
public final class BattleGrid {
    public static final int COLUMNS = 4;
    public static final int ROWS = 3;
    public static final int TILE_COUNT = COLUMNS * ROWS;

    private BattleGrid() {}

    public static boolean isValid(int tile) { return tile >= 1 && tile <= TILE_COUNT; }
    public static int column(int tile) { return (tile - 1) % COLUMNS; }
    public static int row(int tile) { return (tile - 1) / COLUMNS; }
    public static int tile(int column, int row) { return row * COLUMNS + column + 1; }

    /** 명세의 사거리 규칙은 맨해튼 거리다. */
    public static int distance(int from, int to) {
        return Math.abs(column(from) - column(to)) + Math.abs(row(from) - row(to));
    }

    public static List<Integer> adjacent(int tile) {
        if (!isValid(tile)) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        int x = column(tile);
        int y = row(tile);
        if (x > 0) result.add(tile(x - 1, y));
        if (x < COLUMNS - 1) result.add(tile(x + 1, y));
        if (y > 0) result.add(tile(x, y - 1));
        if (y < ROWS - 1) result.add(tile(x, y + 1));
        return result;
    }
}
