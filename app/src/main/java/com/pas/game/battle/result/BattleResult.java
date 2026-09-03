package com.pas.game.battle.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleResult {
    private final boolean success; private final List<BattleEvent> events=new ArrayList<>();
    private BattleResult(boolean success){this.success=success;}
    public static BattleResult ok(){return new BattleResult(true);} public static BattleResult error(String message){return new BattleResult(false).add(BattleEvent.Type.ERROR,message);}
    public BattleResult add(BattleEvent.Type type,String message){events.add(new BattleEvent(type,message));return this;}
    public boolean isSuccess(){return success;} public List<BattleEvent> getEvents(){return Collections.unmodifiableList(events);}
}
