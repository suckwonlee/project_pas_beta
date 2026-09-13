package com.pas.game.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.TextViewCompat;
import android.util.TypedValue;
import com.pas.game.R;
import com.pas.game.skill.data.SkillRuntime;

/** 아이콘 배지와 쿨타임 어둠막을 포함하는 전투 조작 버튼. */
public final class BattleCommandButtonView extends FrameLayout {
    private final ImageView icon;
    private final FrameLayout iconBox;
    private final TextView label;
    private final TextView useBadge;
    private final View cooldownShade;
    private final TextView cooldownLabel;

    public BattleCommandButtonView(Context context){
        super(context);setClickable(true);setFocusable(true);setBackgroundResource(R.drawable.bg_btn_start);setPadding(dp(4),0,dp(4),0);
        LinearLayout content=new LinearLayout(context);content.setGravity(Gravity.CENTER);content.setOrientation(LinearLayout.HORIZONTAL);addView(content,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        iconBox=new FrameLayout(context);content.addView(iconBox,new LinearLayout.LayoutParams(dp(31),dp(31)));
        icon=new ImageView(context);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams iconLp=new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER);iconBox.addView(icon,iconLp);
        useBadge=new TextView(context);useBadge.setTextColor(Color.WHITE);useBadge.setTextSize(8);useBadge.setGravity(Gravity.CENTER);useBadge.setSingleLine(true);useBadge.setMinWidth(dp(22));useBadge.setPadding(dp(3),0,dp(3),0);useBadge.setBackgroundResource(R.drawable.bg_skill_use_badge);FrameLayout.LayoutParams badgeLp=new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dp(13),Gravity.END|Gravity.BOTTOM);iconBox.addView(useBadge,badgeLp);
        label=new TextView(context);label.setTextColor(Color.WHITE);label.setTextSize(10);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setPadding(dp(6),0,0,0);TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label,8,10,1,TypedValue.COMPLEX_UNIT_SP);content.addView(label,new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
        cooldownShade=new View(context);cooldownShade.setBackgroundColor(Color.argb(112,0,0,0));cooldownShade.setClickable(false);addView(cooldownShade,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        cooldownLabel=new TextView(context);cooldownLabel.setTextColor(Color.WHITE);cooldownLabel.setTextSize(13);cooldownLabel.setGravity(Gravity.CENTER);cooldownLabel.setShadowLayer(3,0,1,Color.BLACK);cooldownLabel.setClickable(false);addView(cooldownLabel,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        bind("",0,null);
    }

    public void bind(String text,int iconResource,SkillRuntime skill){
        bindValues(text,iconResource,skill==null?-1:skill.getRemainingUses(),skill==null?-1:skill.getMaxUses(),skill==null?0:skill.getCooldownRemaining());
    }
    public void bindValues(String text,int iconResource,int remaining,int max,int cooldown){
        label.setText(text);iconBox.setVisibility(iconResource==0?GONE:VISIBLE);if(iconResource!=0)icon.setImageResource(iconResource);
        useBadge.setVisibility(max<0?GONE:VISIBLE);if(max>=0)useBadge.setText(remaining+"/"+max);
        boolean cooling=cooldown>0;cooldownShade.setVisibility(cooling?VISIBLE:GONE);cooldownLabel.setVisibility(cooling?VISIBLE:GONE);cooldownLabel.setText(cooling?cooldown+"턴":"");
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
