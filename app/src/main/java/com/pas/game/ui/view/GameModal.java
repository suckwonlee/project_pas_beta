package com.pas.game.ui.view;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.pas.game.R;

/** PAS 화면 스타일을 공유하는 선택·안내·입력 모달. */
public final class GameModal {
    public interface ItemAction { void select(int index); }
    public interface TextAction { void apply(String value); }
    private GameModal(){}

    public static Dialog notice(Activity activity,String title,String message){return confirm(activity,title,message,"확인",null,null,null);}
    public static Dialog confirm(Activity activity,String title,String message,String primaryLabel,Runnable primary,String secondaryLabel,Runnable secondary){
        Dialog dialog=create(activity,title);LinearLayout panel=dialog.findViewById(R.id.game_modal_panel);TextView body=text(activity,message,15,Color.rgb(220,228,234));body.setGravity(Gravity.CENTER);body.setPadding(dp(activity,8),dp(activity,18),dp(activity,8),dp(activity,18));panel.addView(body,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));addActions(activity,dialog,panel,primaryLabel,primary,secondaryLabel,secondary);show(activity,dialog);return dialog;
    }
    public static Dialog list(Activity activity,String title,String[] labels,int[] icons,ItemAction action,Runnable cancelled){
        return list(activity,title,labels,icons,58,15,action,cancelled);
    }
    public static Dialog detailedList(Activity activity,String title,String[] labels,int[] icons,ItemAction action,Runnable cancelled){
        return list(activity,title,labels,icons,78,14,action,cancelled);
    }
    private static Dialog list(Activity activity,String title,String[] labels,int[] icons,int rowHeight,int textSize,ItemAction action,Runnable cancelled){
        Dialog dialog=create(activity,title);LinearLayout panel=dialog.findViewById(R.id.game_modal_panel);ScrollView scroll=new ScrollView(activity);LinearLayout choices=new LinearLayout(activity);choices.setOrientation(LinearLayout.VERTICAL);scroll.addView(choices);int visible=Math.min(labels.length,7);panel.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,Math.max(72,visible*(rowHeight+6)))));
        for(int i=0;i<labels.length;i++){final int index=i;Button row=button(activity,labels[i]);row.setTextSize(textSize);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setMaxLines(3);if(icons!=null&&i<icons.length&&icons[i]!=0){Drawable icon=activity.getDrawable(icons[i]);if(icon!=null){icon.setBounds(0,0,dp(activity,34),dp(activity,34));row.setCompoundDrawables(icon,null,null,null);row.setCompoundDrawablePadding(dp(activity,10));}}row.setOnClickListener(v->{dialog.dismiss();action.select(index);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,rowHeight));lp.setMargins(0,dp(activity,3),0,dp(activity,3));choices.addView(row,lp);}
        Button close=button(activity,"닫기");close.setOnClickListener(v->{dialog.dismiss();if(cancelled!=null)cancelled.run();});LinearLayout.LayoutParams closeLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,48));closeLp.setMargins(0,dp(activity,10),0,0);panel.addView(close,closeLp);dialog.setOnCancelListener(d->{if(cancelled!=null)cancelled.run();});show(activity,dialog);return dialog;
    }
    public static Dialog input(Activity activity,String title,String hint,int inputType,TextAction action){
        Dialog dialog=create(activity,title);LinearLayout panel=dialog.findViewById(R.id.game_modal_panel);EditText input=new EditText(activity);input.setHint(hint);input.setHintTextColor(Color.rgb(145,160,174));input.setTextColor(Color.WHITE);input.setTextSize(16);input.setSingleLine(true);input.setInputType(inputType);input.setBackgroundResource(R.drawable.bg_intent_panel);input.setPadding(dp(activity,14),0,dp(activity,14),0);LinearLayout.LayoutParams inputLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,54));inputLp.setMargins(0,dp(activity,14),0,dp(activity,12));panel.addView(input,inputLp);addActions(activity,dialog,panel,"적용",()->action.apply(input.getText().toString()),"취소",null);show(activity,dialog);Window window=dialog.getWindow();if(window!=null){window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);input.requestFocus();}return dialog;
    }
    public static Dialog custom(Activity activity,String title,View content){
        Dialog dialog=create(activity,title);LinearLayout panel=dialog.findViewById(R.id.game_modal_panel);panel.addView(content,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));Button close=button(activity,"닫기");close.setOnClickListener(v->dialog.dismiss());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,48));lp.setMargins(0,dp(activity,8),0,0);panel.addView(close,lp);show(activity,dialog);Window window=dialog.getWindow();if(window!=null)window.setLayout(modalWidth(activity),(int)(activity.getResources().getDisplayMetrics().heightPixels*.82f));return dialog;
    }
    private static Dialog create(Activity activity,String title){Dialog dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout outer=new LinearLayout(activity);outer.setId(R.id.game_modal_panel);outer.setOrientation(LinearLayout.VERTICAL);outer.setPadding(dp(activity,18),dp(activity,14),dp(activity,18),dp(activity,16));outer.setBackgroundResource(R.drawable.bg_battle_panel);TextView heading=text(activity,title,21,Color.WHITE);heading.setGravity(Gravity.CENTER);outer.addView(heading,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,48)));View divider=new View(activity);divider.setBackgroundColor(Color.rgb(185,133,85));outer.addView(divider,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(activity,1)));dialog.setContentView(outer);return dialog;}
    private static void addActions(Activity activity,Dialog dialog,LinearLayout panel,String primaryLabel,Runnable primary,String secondaryLabel,Runnable secondary){LinearLayout actions=new LinearLayout(activity);actions.setGravity(Gravity.CENTER);if(secondaryLabel!=null){Button secondaryButton=button(activity,secondaryLabel);secondaryButton.setOnClickListener(v->{dialog.dismiss();if(secondary!=null)secondary.run();});actions.addView(secondaryButton,new LinearLayout.LayoutParams(0,dp(activity,48),1));}Button primaryButton=button(activity,primaryLabel);primaryButton.setOnClickListener(v->{dialog.dismiss();if(primary!=null)primary.run();});LinearLayout.LayoutParams primaryLp=new LinearLayout.LayoutParams(0,dp(activity,48),1);if(secondaryLabel!=null)primaryLp.setMargins(dp(activity,6),0,0,0);actions.addView(primaryButton,primaryLp);panel.addView(actions,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
    private static void show(Activity activity,Dialog dialog){dialog.show();Window window=dialog.getWindow();GameWindow.immersive(window);if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setDimAmount(.82f);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setLayout(modalWidth(activity),WindowManager.LayoutParams.WRAP_CONTENT);}}
    private static int modalWidth(Activity activity){int width=activity.getResources().getDisplayMetrics().widthPixels,height=activity.getResources().getDisplayMetrics().heightPixels;return (int)(width*(width>height?.62f:.90f));}
    private static Button button(Activity activity,String label){Button button=new Button(activity);button.setText(label);button.setAllCaps(false);button.setTextColor(Color.WHITE);button.setTextSize(15);button.setBackgroundResource(R.drawable.bg_btn_start);button.setPadding(dp(activity,12),0,dp(activity,12),0);return button;}
    private static TextView text(Activity activity,String value,float size,int color){TextView text=new TextView(activity);text.setText(value);text.setTextSize(size);text.setTextColor(color);return text;}
    private static int dp(Activity activity,int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
