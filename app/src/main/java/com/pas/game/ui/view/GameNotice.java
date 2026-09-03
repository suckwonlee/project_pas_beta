package com.pas.game.ui.view;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.pas.game.R;

/** 시스템 Toast 대신 앱 화면 안에서 잠깐 표시되는 게임 알림 배너. */
public final class GameNotice {
    private static final String TAG="PAS_GAME_NOTICE";
    private GameNotice(){}
    public static void show(Activity activity,String message){
        FrameLayout content=activity.findViewById(android.R.id.content);if(content==null)return;View previous=content.findViewWithTag(TAG);if(previous!=null)content.removeView(previous);
        TextView banner=new TextView(activity);banner.setTag(TAG);banner.setText(message);banner.setTextColor(Color.rgb(255,224,154));banner.setTextSize(15);banner.setGravity(Gravity.CENTER);banner.setPadding(dp(activity,18),dp(activity,12),dp(activity,18),dp(activity,12));banner.setBackgroundResource(R.drawable.bg_intent_panel);banner.setAlpha(0);banner.setTranslationY(-dp(activity,18));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.TOP|Gravity.CENTER_HORIZONTAL);lp.setMargins(dp(activity,18),dp(activity,18),dp(activity,18),0);content.addView(banner,lp);banner.animate().alpha(1).translationY(0).setDuration(180).start();
        new Handler(Looper.getMainLooper()).postDelayed(()->{if(banner.getParent()!=null)banner.animate().alpha(0).translationY(-dp(activity,12)).setDuration(180).withEndAction(()->{if(banner.getParent()==content)content.removeView(banner);}).start();},1800);
    }
    private static int dp(Activity activity,int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
