package com.pas.game.ai;

import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.result.BattleResult;
import com.pas.game.battle.turn.MovementType;
import com.pas.game.map.BattleGrid;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusType;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 도발 → 공격 가능 → 최단거리 이동 → 공격 규칙을 구현한 허수아비 전략이다. */
public final class TrainingDummyAI implements EnemyAI {
    private final RandomProvider random;
    public TrainingDummyAI(RandomProvider random){this.random=random;}
    @Override public BattleResult takeTurn(BattleEngine engine){
        BattleUnit current=engine.activeUnit();
        if(!(current instanceof EnemyUnit))return BattleResult.error("허수아비의 턴이 아닙니다.");
        EnemyUnit enemy=(EnemyUnit)current;
        if(engine.getDebug().isSkipEnemyAi()||engine.getState().getTurn().isBlocked())return engine.execute(new EndTurnCommand(enemy.getUnitId()));
        BattleUnit target=chooseTarget(engine,enemy);
        if(target==null)return engine.execute(new EndTurnCommand(enemy.getUnitId()));
        if(enemy.getHp()<=enemy.getMaxHp()*0.3&&!enemy.isStruggleUsed()){enemy.markStruggleUsed();engine.grantSkillAction(enemy);engine.log(enemy.getName()+" 몸부림: 스킬 사용 기회 +1");}
        if(BattleGrid.distance(enemy.getTile(),target.getTile())>0){
            Integer decoyTile=engine.preferredDecoyTile(enemy,0);int pursuitTile=decoyTile==null?target.getTile():decoyTile;
            List<Integer> best=new ArrayList<>();int bestDistance=Integer.MAX_VALUE;
            for(int tile:BattleGrid.adjacent(enemy.getTile())){int d=BattleGrid.distance(tile,pursuitTile);if(d<bestDistance){best.clear();bestDistance=d;}if(d==bestDistance)best.add(tile);}
            if(!best.isEmpty())engine.execute(new MoveCommand(enemy.getUnitId(),random.choose(best),MovementType.VOLUNTARY));
        }
        if(!target.isDead()&&BattleGrid.distance(enemy.getTile(),target.getTile())==0){
            String skill=enemy.getOwnTurnCount()%3==0?"dummy_slam":"dummy_attack";
            engine.execute(new UseSkillCommand(enemy.getUnitId(),skill,target.getUnitId(),target.getTile()));
            if(engine.getState().getTurn().getSkillsRemaining()>0&&!target.isDead())engine.execute(new UseSkillCommand(enemy.getUnitId(),"dummy_attack",target.getUnitId(),target.getTile()));
        }
        if(engine.getState().getOutcome()==com.pas.game.battle.state.BattleOutcome.ONGOING&&engine.activeUnit()==enemy)return engine.execute(new EndTurnCommand(enemy.getUnitId()));
        return BattleResult.ok();
    }
    private BattleUnit chooseTarget(BattleEngine engine,EnemyUnit enemy){
        for(StatusEffect s:enemy.getStatuses())if(s.getType()==StatusType.TAUNT){BattleUnit taunter=engine.getState().find(s.getSourceUnitId());if(taunter!=null&&!taunter.isDead())return taunter;}
        List<BattleUnit> candidates=engine.getState().living(Team.PLAYER);if(candidates.isEmpty())return null;
        int min=Integer.MAX_VALUE;for(BattleUnit u:candidates)min=Math.min(min,BattleGrid.distance(enemy.getTile(),u.getTile()));
        List<BattleUnit> nearest=new ArrayList<>();for(BattleUnit u:candidates)if(BattleGrid.distance(enemy.getTile(),u.getTile())==min)nearest.add(u);
        nearest.sort(Comparator.comparing(BattleUnit::getUnitId));return nearest.size()==1?nearest.get(0):random.choose(nearest);
    }
}
