package com.pas.game.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import com.pas.game.R;
import com.pas.game.battle.state.BattleState;
import com.pas.game.battle.state.BattleDecoy;
import com.pas.game.battle.state.BattleTrap;
import com.pas.game.battle.state.BattleSanctuary;
import com.pas.game.map.BattleGrid;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.PlayerUnit;
import com.pas.game.unit.Team;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 상태를 그리며 터치를 칸 번호로 변환한다. 게임 규칙 판단은 Activity/Engine에 위임한다. */
public final class BattleBoardView extends View {
    public interface Listener { void onTileTapped(int tile); }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tileRect=new RectF();
    private final Drawable playerMarker;
    private final Drawable enemyMarker;
    private com.pas.game.network.RemoteBattleSnapshot remote;
    private final com.pas.game.character.CharacterRepository characters=new com.pas.game.character.CharacterRepository();
    private BattleState state; private Listener listener; private Set<Integer> highlights=Collections.emptySet(); private Integer selectedTile; private Drawable battlefieldBackground; private boolean showTileNumbers=true;
    public BattleBoardView(Context context){super(context);paint.setTextAlign(Paint.Align.CENTER);playerMarker=context.getDrawable(R.drawable.hero);enemyMarker=context.getDrawable(R.drawable.enemy_south_1);}
    public void bind(BattleState value){state=value;remote=null;invalidate();} public void setListener(Listener value){listener=value;}
    public void bindRemote(com.pas.game.network.RemoteBattleSnapshot value){state=null;remote=value;invalidate();}
    public void setBattlefieldBackgroundResource(int resourceId){battlefieldBackground=getContext().getDrawable(resourceId);invalidate();}
    public void setShowTileNumbers(boolean value){showTileNumbers=value;invalidate();}
    public void setSelection(Set<Integer> tiles,Integer selected){highlights=new HashSet<>(tiles);selectedTile=selected;invalidate();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float cw=getWidth()/4f,ch=getHeight()/3f;
        if(battlefieldBackground!=null){battlefieldBackground.setBounds(0,0,getWidth(),getHeight());battlefieldBackground.draw(canvas);}
        for(int visualRow=0;visualRow<3;visualRow++)for(int col=0;col<4;col++){
            int logicalRow=2-visualRow,tile=BattleGrid.tile(col,logicalRow);tileRect.set(col*cw,visualRow*ch,(col+1)*cw,(visualRow+1)*ch);RectF r=tileRect;
            paint.setStyle(Paint.Style.FILL);paint.setColor(tile==selectedTileValue()?Color.argb(150,245,189,66):highlights.contains(tile)?Color.argb(145,60,140,105):battlefieldBackground==null?Color.rgb(34,43,54):Color.argb(45,20,28,35));canvas.drawRect(r,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.argb(150,130,145,158));canvas.drawRect(r,paint);
            if(showTileNumbers){paint.setStyle(Paint.Style.FILL);paint.setTextSize(Math.min(cw,ch)*0.18f);paint.setColor(Color.LTGRAY);canvas.drawText(String.valueOf(tile),r.left+18,r.top+25,paint);}
            if(state!=null){drawObjects(canvas,tile,r);drawUnits(canvas,state.atTile(tile,false),r);}
            if(remote!=null)drawRemote(canvas,tile,r);
        }
    }
    private void drawObjects(Canvas canvas,int tile,RectF rect){for(BattleTrap trap:state.getTraps())if(trap.getTile()==tile){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(245,189,66));paint.setTextSize(Math.min(rect.width(),rect.height())*.16f);canvas.drawText("덫",rect.right-20,rect.bottom-12,paint);}for(BattleDecoy decoy:state.getDecoys())if(decoy.getTile()==tile){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(125,210,255));paint.setTextSize(Math.min(rect.width(),rect.height())*.16f);canvas.drawText("미끼",rect.right-28,rect.bottom-12,paint);}for(BattleSanctuary sanctuary:state.getSanctuaries())if(BattleGrid.distance(sanctuary.getTile(),tile)<=sanctuary.getRange()){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb(55,255,226,128));canvas.drawRect(rect,paint);if(sanctuary.getTile()==tile){paint.setColor(Color.rgb(255,226,128));paint.setTextSize(Math.min(rect.width(),rect.height())*.14f);canvas.drawText("성역 "+sanctuary.getRemainingPlayerTurns()+"T",rect.centerX(),rect.bottom-10,paint);}}for(com.pas.game.battle.state.WizardBattleState.Star star:state.getWizardState().getStars())if(star.tile==tile){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(205,146,255));paint.setTextSize(Math.min(rect.width(),rect.height())*.13f);canvas.drawText("굶주린 별",rect.centerX(),rect.top+20,paint);}for(com.pas.game.battle.state.WizardBattleState.Gate gate:state.getWizardState().getGates())if(gate.tile==tile){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(188,95,255));paint.setTextSize(Math.min(rect.width(),rect.height())*.13f);canvas.drawText("이계의 문",rect.centerX(),rect.bottom-10,paint);}}
    private int selectedTileValue(){return selectedTile==null?-1:selectedTile;}
    private void drawRemote(Canvas c,int tile,RectF r){
        int i=0,count=0;for(com.pas.game.network.RemoteBattleSnapshot.Unit u:remote.units)if(u.onField&&u.tile==tile)count++;
        for(com.pas.game.network.RemoteBattleSnapshot.Unit u:remote.units)if(u.onField&&u.tile==tile){
            RectF frame=unitFrame(r,i++,count);boolean player="PLAYER".equals(u.team);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);paint.setColor(player?Color.rgb(63,155,255):Color.rgb(225,78,78));c.drawRoundRect(frame,9,9,paint);
            Drawable marker=player?playerMarker:getContext().getDrawable(com.pas.game.ui.EnemyPortraits.resource(u.enemyKind));
            if(player)for(com.pas.game.character.CharacterData character:characters.all())if(character.getId().equals(u.characterId)||(u.characterId==null&&u.unitId.contains("_"+character.getId()+"_"))){marker=getContext().getDrawable(character.getMarkerResource(u.skinId));break;}
            if(marker!=null){int inset=Math.max(0,Math.min(4,(int)(frame.width()*.08f)));marker.setAlpha(u.dead?105:255);marker.setBounds((int)frame.left+inset,(int)frame.top+inset,(int)frame.right-inset,(int)frame.bottom-inset);marker.draw(c);marker.setAlpha(255);}
        }
        remoteObjects(c,r,tile,remote.traps,"덫");remoteObjects(c,r,tile,remote.decoys,"미끼");remoteObjects(c,r,tile,remote.sanctuaries,"성역");
        if(remote.wizard!=null){remoteObjects(c,r,tile,remote.wizard.has("stars")?new com.google.gson.Gson().fromJson(remote.wizard.get("stars"),new com.google.gson.reflect.TypeToken<java.util.List<com.google.gson.JsonObject>>(){}.getType()):null,"굶주린 별");remoteObjects(c,r,tile,remote.wizard.has("gates")?new com.google.gson.Gson().fromJson(remote.wizard.get("gates"),new com.google.gson.reflect.TypeToken<java.util.List<com.google.gson.JsonObject>>(){}.getType()):null,"이계의 문");}
    }
    private void remoteObjects(Canvas c,RectF r,int tile,List<com.google.gson.JsonObject> objects,String label){if(objects==null)return;for(com.google.gson.JsonObject object:objects)if(object.has("tile")&&object.get("tile").getAsInt()==tile){paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(255,226,128));paint.setTextSize(Math.min(r.width(),r.height())*.13f);c.drawText(label,r.centerX(),r.bottom-10,paint);}}
    private RectF unitFrame(RectF tile,int index,int count){
        int columns=(int)Math.ceil(Math.sqrt(Math.max(1,count))),rows=(count+columns-1)/columns;
        float size=Math.min(Math.min(tile.width(),tile.height())*.38f,Math.min(tile.width()/(columns+0.25f),tile.height()/(rows+0.25f))*.85f);
        float gap=size*1.1f,x=tile.centerX()+(index%columns-(columns-1)/2f)*gap,y=tile.centerY()+(index/columns-(rows-1)/2f)*gap;
        return new RectF(x-size/2,y-size/2,x+size/2,y+size/2);
    }
    private void drawUnits(Canvas c,List<BattleUnit> units,RectF r){if(units.isEmpty())return;for(int i=0;i<units.size();i++){
        BattleUnit u=units.get(i);RectF frame=unitFrame(r,i,units.size());
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.argb(210,10,15,20));c.drawRoundRect(frame,9,9,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);paint.setColor(u.getTeam()==Team.PLAYER?Color.rgb(63,155,255):Color.rgb(225,78,78));c.drawRoundRect(frame,9,9,paint);
        Drawable marker=u.getTeam()==Team.PLAYER?playerMarker:getContext().getDrawable(com.pas.game.ui.EnemyPortraits.resource(u));if(u instanceof PlayerUnit&&((PlayerUnit)u).getMarkerResource()!=0)marker=getContext().getDrawable(((PlayerUnit)u).getMarkerResource());if(marker!=null){int inset=Math.max(0,Math.min(5,(int)(frame.width()*.08f)));marker.setAlpha(u.isDead()?105:255);marker.setBounds((int)frame.left+inset,(int)frame.top+inset,(int)frame.right-inset,(int)frame.bottom-inset);marker.draw(c);marker.setAlpha(255);}
    }}
    @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()!=MotionEvent.ACTION_UP)return true;performClick();int col=Math.min(3,(int)(event.getX()/(getWidth()/4f)));int visual=Math.min(2,(int)(event.getY()/(getHeight()/3f)));int tile=BattleGrid.tile(col,2-visual);if(listener!=null)listener.onTileTapped(tile);return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
