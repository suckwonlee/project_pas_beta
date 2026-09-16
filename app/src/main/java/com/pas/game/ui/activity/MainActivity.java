package com.pas.game.ui.activity;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import com.pas.game.R;
import com.pas.game.ai.EncounterEnemyAI;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.ResolveWizardReturnCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.command.UsePotionCommand;
import com.pas.game.battle.engine.BattleEngine;
import com.pas.game.battle.engine.BattleFactory;
import com.pas.game.battle.engine.JavaRandomProvider;
import com.pas.game.battle.engine.RandomProvider;
import com.pas.game.battle.result.BattleResult;
import com.pas.game.battle.state.BattleOutcome;
import com.pas.game.battle.turn.MovementType;
import com.pas.game.character.CharacterData;
import com.pas.game.character.CharacterRepository;
import com.pas.game.debug.DebugOptions;
import com.pas.game.config.BetaFeatures;
import com.pas.game.effect.UnitEffectType;
import com.pas.game.map.BattleGrid;
import com.pas.game.multiplayer.LocalPlayerController;
import com.pas.game.multiplayer.PlayerCommandSource;
import com.pas.game.item.potion.PotionData;
import com.pas.game.item.potion.PotionInventory;
import com.pas.game.item.potion.PotionRepository;
import com.pas.game.passive.PassiveData;
import com.pas.game.passive.PassiveRuntime;
import com.pas.game.rune.RuneData;
import com.pas.game.rune.RuneLoadout;
import com.pas.game.rune.RuneRepository;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.SkillDetailFormatter;
import com.pas.game.skill.data.SkillGrade;
import com.pas.game.skill.data.SkillRuntime;
import com.pas.game.skill.data.TargetType;
import com.pas.game.skill.repository.CharacterSkillRegistry;
import com.pas.game.skill.repository.SkillRepository;
import com.pas.game.status.StatusEffect;
import com.pas.game.status.StatusDisplay;
import com.pas.game.status.StatusType;
import com.pas.game.ui.view.BattleBoardView;
import com.pas.game.ui.view.BattleCommandButtonView;
import com.pas.game.ui.view.GameModal;
import com.pas.game.ui.view.GameNotice;
import com.pas.game.ui.selection.CharacterSelectionRules;
import com.pas.game.ui.selection.PlayMode;
import com.pas.game.ui.selection.PartySelection;
import com.pas.game.multiplayer.PlayerBattleSetup;
import com.pas.game.unit.BattleUnit;
import com.pas.game.unit.EnemyUnit;
import com.pas.game.unit.PlayerUnit;
import com.pas.game.unit.Team;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 화면 입력을 Command로 변환하고 BattleState 결과만 렌더링한다. */
public final class MainActivity extends AppCompatActivity {
    private final CharacterSkillRegistry skillRegistry=new CharacterSkillRegistry();
    private SkillRepository repository;
    private String skillCharacterId;
    private final PotionRepository potionRepository=new PotionRepository();
    private final RuneRepository runeRepository=new RuneRepository();
    private final CharacterRepository characterRepository=new CharacterRepository();
    private final DebugOptions debug=new DebugOptions();
    private final RandomProvider random=new JavaRandomProvider();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final SkillData[] slots=new SkillData[6];
    private final int[] upgrades=new int[6];
    private boolean moveMode,outcomeShown,learnedSkillMode,returnPickerShowing;
    private int selectedSlot,continentIndex,screenStage,selectedCharacterIndex;
    private SkillRuntime selectedSkill;
    private String selectedTargetId,selectedEnemyId;
    private Integer selectedTile;
    private BattleEngine engine;
    private PlayerCommandSource commandSource;
    private EncounterEnemyAI enemyAI;
    private BattleBoardView board;
    private ImageView enemyImage;
    private TextView turnInfo,preview,logView,playerName,playerHpText,enemyName,enemyHpText,enemyIntent;
    private ProgressBar playerHpBar,enemyHpBar;
    private LinearLayout skillButtons;
    private LinearLayout playerStatusRow,enemyStatusRow,enemySelectorRow;
    private GridLayout commandGrid;
    private Button battleUtilityButton;
    private RuneData selectedRune1=runeRepository.primary("fighting"),selectedRune2=runeRepository.secondary("smash");
    private int chapter=1;
    private PotionInventory adventurePotions;
    private com.pas.game.shop.ShopSession shopSession;
    private int selectedRune1Level=1,selectedRune2Level=1;
    private PlayMode playMode=PlayMode.SINGLE_ONE;
    private final PartySelection partySelection=new PartySelection();
    private int editingPartySlot=1;
    private ImageView playerPortrait;
    private OnlineCoopFlow onlineFlow;
    private com.pas.game.ui.shop.RemoteShopScreen remoteShop;

    private final String[] continentNames={"남대륙","동대륙","서대륙","북대륙","중앙대륙"};
    private final String[] continentDescriptions={
            "5대 대륙 중 가장 작아 '대륙'이라 부르기엔 부족하고, '섬'이라 하기엔 큰 땅. 해안선을 따라 거대한 산맥이 이어져 있고, 북쪽의 거대한 항구를 중심으로 해상 무역이 번성한다. 허나 이 평화롭지만 진입하기 어려운 환경이 마계의 악마 숭배자들이 몰려드는 것을 막아야 했던 파견군의 작전을 방해했고, 결국 이계 침략의 시초가 되었다.",
            "험한 산악과 긴 겨울이 이어지는 혹한의 땅. 이곳은 다른 대륙과의 교류 없이, 강인한 투사들의 수호 아래 짐승과 괴물들과 투쟁하며 살아온 대륙이다. 그러나 적은 인구수는 치명적인 약점이 되었고, 명계의 침략으로 동대륙의 시체들이 되살아나기 시작하면서 이 대륙의 생명 또한 멸종의 위기에 놓이게 되었다.",
            "문명의 손길이 닿지 않은, 전통과 야만이 공존하는 대륙. 투박하지만 따뜻한 야만전사들과 주술사들이 대대로 지켜온 삶의 방식은, 어느 날부터인가 정체 모를 존재들의 출현으로 균열을 일으켰다. 이해할 수 없는 것들이 일상이 되고, 혼돈계의 시선이 이들을 덮친 순간부터—그들의 육신은 변이를 시작했고, 전통과 평화는 조용히, 그러나 확실히 끝을 향해 나아가고 있다.",
            "드루이드들이 수호하는 정글로 뒤덮인 대륙. 지나치게 울창한 숲은 지상에서의 삶을 거의 불가능하게 만들었고, 드루이드의 축복 아래 나무 위에서 살아가는 방식이 자연스럽게 정착되었다. 그러나 이계의 침략이 시작되며 퍼져나간 ‘심연’의 기운에 접촉한 이후, 정글은 점차 본모습을 잃어갔다. 초목은 뒤틀리고, 숲의 존재인 드루이드들, 그리고 오랫동안 잠들어 있던 야생신들마저 그 침식에서 벗어나지 못하고 있다는 소문이 들려오고 있다.",
            "강대한 제국이 지배하는 비옥한 중심지. 이계의 침공 당시, 이 땅 역시 침략의 대상이 되었으나 제국은 놀라울 만큼 빠르고 효율적으로 위협을 막아냈다. 그러나 승리 뒤, 제국은 전쟁에서 얻은 지식과 전리품을 바탕으로 이계의 힘을 다시 끌어내기 시작했다. 이제 그들은 다른 대륙을 침략해 그 땅과 생존자들을 거대한 제물로 삼으려 한다. 그리고 그 힘을 발판으로, 자신들이 처단했던 이계 너머로 스스로 발을 들이려 한다."
    };
    private final int[] continentImages={R.drawable.south,R.drawable.east,R.drawable.west,R.drawable.north,R.drawable.central};

@Override protected void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);if(getSupportActionBar()!=null)getSupportActionBar().hide();getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){handleBackNavigation();}});ensureCharacterLoadout(characterRepository.all().get(selectedCharacterIndex));adventurePotions=new PotionInventory(potionRepository.forChapter(chapter),0);if(savedInstanceState!=null&&savedInstanceState.containsKey("pas.shop")){
            try{
                shopSession=com.pas.game.shop.ShopSession.restore(new com.google.gson.Gson().fromJson(savedInstanceState.getString("pas.shop"),com.pas.game.shop.ShopSession.Save.class));
                playMode=shopSession.players().size()==2?PlayMode.SINGLE_PARTY:PlayMode.SINGLE_ONE;
                adventurePotions=shopSession.potions();
                // Selection presets remain pristine. Purchased bonuses belong only to this run.
                for(PlayerBattleSetup p:shopSession.players()){
                    CharacterData original=null;for(CharacterData c:characterRepository.all())if(c.getId().equals(p.getCharacter().getId()))original=c;
                    partySelection.save(new PlayerBattleSetup(p.getPlayerSlot(),original,p.getLoadout(),java.util.Collections.nCopies(p.getLoadout().size(),0),p.getRunes()));
                }
                loadPartyEditor(1);showShop(this::enterPreparedBattle,"전투로 이동");return;
            }catch(RuntimeException e){shopSession=null;}
        }showMain();}
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);
        if(screenStage==6&&playMode!=PlayMode.ONLINE_COOP&&shopSession!=null)
            out.putString("pas.shop",new com.google.gson.Gson().toJson(shopSession.save()));
    }

    private void ensureCharacterLoadout(CharacterData character){
        if(character==null||character.getId().equals(skillCharacterId))return;
        SkillRepository next=skillRegistry.find(character.getId());if(next==null)return;
        repository=next;skillCharacterId=character.getId();Arrays.fill(slots,null);Arrays.fill(upgrades,0);
        List<SkillData> all=repository.all();if(!all.isEmpty())slots[0]=all.get(0);if(all.size()>3)slots[1]=all.get(3);
    }

    private void showMain(){
        screenStage=0;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);View root=getLayoutInflater().inflate(R.layout.activity_main,null);View.OnClickListener start=v->showPlayModeSelection();root.findViewById(R.id.main).setOnClickListener(start);root.findViewById(R.id.start_content).setOnClickListener(start);root.findViewById(R.id.touch_to_start).setOnClickListener(start);View title=root.findViewById(R.id.project_title);title.setOnClickListener(start);title.setOnLongClickListener(v->{showSettings();return true;});
        if(BetaFeatures.SERVER_TEST_TOOLS&&com.pas.game.BuildConfig.PAS_ONLINE_ENABLED){
            Button online=button("온라인 협동 베타");
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(52));lp.topMargin=dp(24);
            ((LinearLayout)root.findViewById(R.id.start_content)).addView(online,lp);
            online.setOnClickListener(v->startActivity(new android.content.Intent(this,OnlineTestActivity.class)));
        }
        setScreen(root);
    }

    private void showContinentSelection(){
        screenStage=1;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);View root=getLayoutInflater().inflate(R.layout.activity_continent_selection,null);ImageView image=root.findViewById(R.id.continent_image);TextView name=root.findViewById(R.id.continent_name);TextView desc=root.findViewById(R.id.description_text);ImageButton left=root.findViewById(R.id.arrow_left);ImageButton right=root.findViewById(R.id.arrow_right);Button next=root.findViewById(R.id.btn_next_center);image.setImageResource(continentImages[continentIndex]);name.setText(continentNames[continentIndex]);desc.setText(continentDescriptions[continentIndex]);left.setOnClickListener(v->{continentIndex=(continentIndex+continentNames.length-1)%continentNames.length;showContinentSelection();});right.setOnClickListener(v->{continentIndex=(continentIndex+1)%continentNames.length;showContinentSelection();});next.setOnClickListener(v->{if(continentIndex!=0){GameModal.notice(this,"개발 중","해당 대륙은 아직 개발 중입니다.");return;}if(playMode==PlayMode.ONLINE_COOP)showOnlineLobby();else showCharacterSelection();});setScreen(root);
    }

    private void showPlayModeSelection(){
        if(onlineFlow!=null)onlineFlow.pause();
        screenStage=2;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);View root=getLayoutInflater().inflate(R.layout.activity_play_mode,null);
        root.findViewById(R.id.btn_mode_single_one).setOnClickListener(v->selectPlayMode(PlayMode.SINGLE_ONE));
        root.findViewById(R.id.btn_mode_single_party).setOnClickListener(v->selectPlayMode(PlayMode.SINGLE_PARTY));
        root.findViewById(R.id.btn_mode_online_coop).setOnClickListener(v->selectPlayMode(PlayMode.ONLINE_COOP));
        root.findViewById(R.id.btn_mode_back).setOnClickListener(v->showMain());setScreen(root);
    }

    private void selectPlayMode(PlayMode mode){savePartyEditor();loadPartyEditor(1);playMode=mode==null?PlayMode.SINGLE_ONE:mode;showContinentSelection();}

    private void showCharacterSelection(){
        screenStage=3;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);View root=getLayoutInflater().inflate(R.layout.activity_character_selection,null);GridLayout characterGrid=root.findViewById(R.id.character_grid);List<CharacterData> characterChoices=characterRepository.all();CharacterData selectedCharacter=characterChoices.get(selectedCharacterIndex);ensureCharacterLoadout(selectedCharacter);savePartyEditor();bindPartyControls(root);for(int i=0;i<characterChoices.size();i++)addCharacterTile(characterGrid,characterChoices.get(i),i);TextView modeText=root.findViewById(R.id.selected_play_mode);modeText.setText(playMode.getTitle()+"\n"+modeSelectionDescription());root.findViewById(R.id.btn_change_play_mode).setOnClickListener(v->showPlayModeSelection());TextView characterName=root.findViewById(R.id.character_name);TextView flavorText=root.findViewById(R.id.character_flavor_text);characterName.setText((playMode==PlayMode.SINGLE_PARTY?"P"+editingPartySlot+" · ":"")+selectedCharacter.getName());flavorText.setText(selectedCharacter.getFlavorText());TextView guide=root.findViewById(R.id.selection_guide);View runePanel=root.findViewById(R.id.rune_selection);View debugPanel=root.findViewById(R.id.debug_skill_selection);Button start=root.findViewById(R.id.btn_action);
        if(!selectedCharacter.isAvailable()){guide.setText("현재 개발 중인 캐릭터입니다.");runePanel.setVisibility(View.GONE);debugPanel.setVisibility(View.GONE);start.setText("개발 중");start.setOnClickListener(v->toast(developmentMessage(selectedCharacter)));setScreen(root);return;}
        debugPanel.setVisibility(View.VISIBLE);Button attack=root.findViewById(R.id.btn_attack_skill);Button defense=root.findViewById(R.id.btn_defense_skill);styleSkillSelectionButton(attack,"공격: "+slots[0].getName(),skillIcon(slots[0]));styleSkillSelectionButton(defense,"방어: "+slots[1].getName(),skillIcon(slots[1]));attack.setOnClickListener(v->showCharacterSkillPicker(0,0,3,"공격 스킬 선택"));defense.setOnClickListener(v->showCharacterSkillPicker(1,3,6,"방어 스킬 선택"));GridLayout learnedGrid=root.findViewById(R.id.learned_skill_grid);
        if(BetaFeatures.SKILL_SELECTION){guide.setText(CharacterSelectionRules.hasCompleteDebugSkillSet(slots)?"캐릭터를 다시 눌러 룬 선택":"일반 스킬 3칸 · 우측 하단 궁극기 1칸");runePanel.setVisibility(View.GONE);learnedGrid.setVisibility(View.VISIBLE);for(int i=2;i<6;i++){final int slot=i;SkillData selected=slots[i];String prefix=i==5?"궁극기: ":"스킬 "+(i-1)+": ";View skill=learnedSkillSelectionView(prefix+(selected==null?"비어 있음":selected.getName()),selected==null?R.drawable.ic_skill_empty:skillIcon(selected));int gridIndex=i-2;GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(52);lp.rowSpec=GridLayout.spec(gridIndex/2);lp.columnSpec=GridLayout.spec(gridIndex%2,1f);lp.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP);lp.setMargins(dp(2),dp(2),dp(2),dp(2));learnedGrid.addView(skill,lp);skill.setOnClickListener(v->showLearnedSkillPicker(slot));}}
        else{guide.setText("공격 · 방어 스킬과 룬 1 · 룬 2를 선택하세요.");runePanel.setVisibility(View.VISIBLE);learnedGrid.setVisibility(View.GONE);Button rune1=root.findViewById(R.id.btn_rune1);Button rune2=root.findViewById(R.id.btn_rune2);styleRuneSelectionButton(rune1,"룬 1",selectedRune1,selectedRune1Level);styleRuneSelectionButton(rune2,"룬 2",selectedRune2,selectedRune2Level);rune1.setOnClickListener(v->showRunePicker(true));rune2.setOnClickListener(v->showRunePicker(false));}
        start.setText(playMode==PlayMode.ONLINE_COOP?"대기실로 돌아가기":playMode==PlayMode.SINGLE_PARTY?(partySelection.get(2)==null?"P2 새 캐릭터 만들기":"상점으로"):"상점으로");start.setOnClickListener(v->{if(playMode==PlayMode.ONLINE_COOP)showOnlineLobby();else if(playMode==PlayMode.SINGLE_PARTY&&partySelection.get(2)==null)switchPartyEditor(2);else startBattle();});setScreen(root);
    }
    private String modeSelectionDescription(){if(playMode==PlayMode.SINGLE_PARTY)return "P1/P2 탭에서 캐릭터·스킬·룬을 각각 선택";return playMode.getDescription();}

    private void savePartyEditor(){
        List<SkillData> loadout=new ArrayList<>(Arrays.asList(slots));List<Integer> levels=new ArrayList<>();for(int level:upgrades)levels.add(level);
        partySelection.save(new PlayerBattleSetup(editingPartySlot,characterRepository.all().get(selectedCharacterIndex),loadout,levels,new RuneLoadout(selectedRune1,selectedRune1Level,selectedRune2,selectedRune2Level)));
    }

    private void loadPartyEditor(int slot){
        PlayerBattleSetup setup=partySelection.get(slot);if(setup==null){editingPartySlot=slot;selectedCharacterIndex=0;skillCharacterId=null;ensureCharacterLoadout(characterRepository.all().get(0));selectedRune1=runeRepository.primary("fighting");selectedRune2=runeRepository.secondary("smash");selectedRune1Level=1;selectedRune2Level=1;return;}
        editingPartySlot=slot;
        List<CharacterData> characters=characterRepository.all();for(int i=0;i<characters.size();i++)if(characters.get(i).getId().equals(setup.getCharacter().getId())){selectedCharacterIndex=i;break;}
        repository=skillRegistry.find(setup.getCharacter().getId());skillCharacterId=setup.getCharacter().getId();
        for(int i=0;i<6;i++){slots[i]=i<setup.getLoadout().size()?setup.getLoadout().get(i):null;upgrades[i]=i<setup.getUpgrades().size()?setup.getUpgrades().get(i):0;}
        RuneLoadout runes=setup.getRunes();selectedRune1=runes.getPrimary();selectedRune2=runes.getSecondary();selectedRune1Level=runes.getPrimaryLevel();selectedRune2Level=runes.getSecondaryLevel();
    }

    private void switchPartyEditor(int slot){savePartyEditor();loadPartyEditor(slot);showCharacterSelection();}

    private void bindPartyControls(View root){
        View controls=root.findViewById(R.id.party_selection_controls);controls.setVisibility(playMode==PlayMode.SINGLE_PARTY?View.VISIBLE:View.GONE);
        if(playMode!=PlayMode.SINGLE_PARTY)return;
        int[] ids={R.id.btn_party_p1,R.id.btn_party_p2};
        for(int i=0;i<2;i++){final int slot=i+1;Button tab=root.findViewById(ids[i]);PlayerBattleSetup setup=partySelection.get(slot);tab.setText("P"+slot+" · "+(setup==null?"새 캐릭터 만들기":setup.getCharacter().getName())+"\n"+(editingPartySlot==slot?"편집 중":"설정 열기"));tab.setBackgroundTintList(null);tab.setBackgroundResource(editingPartySlot==slot?R.drawable.bg_enemy_tab_selected:R.drawable.bg_enemy_tab);tab.setOnClickListener(v->switchPartyEditor(slot));}
    }

    private void showOnlineLobby(){
        savePartyEditor();
        screenStage=4;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if(!com.pas.game.BuildConfig.PAS_ONLINE_ENABLED){GameModal.notice(this,"협동 준비 중","온라인 접속 설정이 포함된 앱이 필요합니다.");showPlayModeSelection();return;}
        try{if(onlineFlow==null)onlineFlow=new OnlineCoopFlow(this,this::setScreen,this::showCharacterSelection,this::showPlayModeSelection,this::visitOnlineShop);onlineFlow.show(partySelection.get(1));}
        catch(RuntimeException e){GameModal.notice(this,"온라인 연결",e.getMessage());showPlayModeSelection();}
    }
    private ImageButton arrowButton(int icon){ImageButton button=new ImageButton(this);button.setImageResource(icon);button.setBackgroundResource(R.drawable.bg_battle_panel);button.setPadding(dp(14),dp(14),dp(14),dp(14));return button;}
    private void addCharacterTile(GridLayout grid,CharacterData character,int index){Button button=button(character.getName());styleCharacterSelectionButton(button,character.getName(),character.getPortraitResource());button.setAlpha(index==selectedCharacterIndex?1f:character.isAvailable()?.72f:.42f);button.setScaleX(index==selectedCharacterIndex?1f:.96f);button.setScaleY(index==selectedCharacterIndex?1f:.96f);button.setOnClickListener(v->{if(CharacterSelectionRules.shouldOpenRuneSelection(BetaFeatures.SKILL_SELECTION,character.isAvailable(),selectedCharacterIndex,index,slots)){showDebugRuneSelection();return;}if(BetaFeatures.SKILL_SELECTION&&character.isAvailable()&&selectedCharacterIndex==index){GameModal.notice(this,"룬 선택 잠김","일반 스킬 3개와 궁극기 1개를 먼저 선택하세요.");return;}selectedCharacterIndex=index;showCharacterSelection();if(!character.isAvailable())toast(developmentMessage(character));});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(118);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(button,lp);}
    private String developmentMessage(CharacterData character){String name=character.getName();boolean useNeun="마법사".equals(name)||"드루이드".equals(name);return name+(useNeun?"는":"은")+" 아직 개발 중입니다.";}
    private Button selectionButton(String text,int iconId){Button button=button(text);styleSelectionButton(button,text,iconId);return button;}
    private void styleSelectionButton(Button button,String text,int iconId){button.setText(text);button.setAllCaps(false);button.setTextColor(Color.WHITE);button.setBackgroundResource(R.drawable.bg_btn_start);Drawable icon=getDrawable(iconId);if(icon!=null){icon.setBounds(0,0,dp(30),dp(30));button.setCompoundDrawables(icon,null,null,null);button.setCompoundDrawablePadding(dp(7));}}
    private void styleCharacterSelectionButton(Button button,String text,int iconId){button.setText(text);button.setAllCaps(false);button.setTextSize(13);button.setTextColor(Color.WHITE);button.setGravity(Gravity.CENTER);button.setPadding(dp(3),dp(7),dp(3),dp(5));button.setBackgroundResource(R.drawable.bg_btn_start);Drawable icon=getDrawable(iconId);if(icon!=null){icon.setBounds(0,0,dp(58),dp(58));button.setCompoundDrawables(null,icon,null,null);button.setCompoundDrawablePadding(dp(6));}}
    private void styleRuneSelectionButton(Button button,String slot,RuneData rune,int level){styleSelectionButton(button,slot+" · LV."+level+"\n"+rune.getName(),runeIcon(rune));button.setLines(2);button.setMaxLines(2);button.setEllipsize(TextUtils.TruncateAt.END);button.setPadding(dp(5),0,dp(5),0);button.setCompoundDrawablePadding(dp(5));TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(button,10,14,1,TypedValue.COMPLEX_UNIT_SP);}
    private void styleSkillSelectionButton(Button button,String text,int iconId){button.setText(text);button.setAllCaps(false);button.setTextColor(Color.WHITE);button.setBackgroundResource(R.drawable.bg_btn_start);Drawable icon=getDrawable(iconId);if(icon!=null){icon.setBounds(0,0,dp(40),dp(40));button.setCompoundDrawables(icon,null,null,null);button.setCompoundDrawablePadding(dp(9));}}
    private View learnedSkillSelectionView(String text,int iconId){FrameLayout button=new FrameLayout(this);button.setClickable(true);button.setFocusable(true);button.setBackgroundResource(R.drawable.bg_btn_start);ImageView icon=new ImageView(this);icon.setImageResource(iconId);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams iconLp=new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.START|Gravity.CENTER_VERTICAL);iconLp.setMarginStart(dp(8));button.addView(icon,iconLp);TextView label=new TextView(this);label.setText(text);label.setTextColor(Color.WHITE);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setPadding(dp(4),0,dp(5),0);TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label,8,12,1,TypedValue.COMPLEX_UNIT_SP);FrameLayout.LayoutParams labelLp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT);labelLp.setMarginStart(dp(50));button.addView(label,labelLp);return button;}
    private void showRunePicker(boolean passive){
        List<RuneData> runes=passive?runeRepository.primary():runeRepository.secondary();int level=passive?selectedRune1Level:selectedRune2Level;String[] items=new String[runes.size()];int[] icons=new int[runes.size()];for(int i=0;i<runes.size();i++){RuneData rune=runes.get(i);items[i]=rune.getName()+"    LV."+level;icons[i]=runeIcon(rune);}
        boolean returnToDebugRunes=BetaFeatures.SKILL_SELECTION&&CharacterSelectionRules.hasCompleteDebugSkillSet(slots);
        GameModal.list(this,passive?"룬 1 선택":"룬 2 선택",items,icons,index->{if(passive)selectedRune1=runes.get(index);else selectedRune2=runes.get(index);if(returnToDebugRunes)showDebugRuneSelection();else showCharacterSelection();},returnToDebugRunes?this::showDebugRuneSelection:null);
    }
    private void showDebugRuneSelection(){String[] items={"룬 1 · LV."+selectedRune1Level+"\n"+selectedRune1.getName(),"룬 2 · LV."+selectedRune2Level+"\n"+selectedRune2.getName()};int[] icons={runeIcon(selectedRune1),runeIcon(selectedRune2)};GameModal.list(this,"룬 선택",items,icons,index->showRunePicker(index==0),this::showCharacterSelection);}
    private int runeIcon(RuneData rune){String id=rune.getId();if("fire".equals(id))return R.drawable.rune_p_fire;if("frost".equals(id))return R.drawable.rune_p_frost;if("guardian".equals(id))return R.drawable.rune_p_guardian;if("venom".equals(id))return R.drawable.rune_p_venom;if("fighting".equals(id))return R.drawable.rune_p_fighting;if("vampire".equals(id))return R.drawable.rune_p_vempire;if("must_live".equals(id))return R.drawable.rune_p_mustlive;if("charge".equals(id))return R.drawable.rune_p_charge;if("smash".equals(id))return R.drawable.rune_s_smash;if("immovable".equals(id))return R.drawable.rune_s_notmove;if("mountain".equals(id))return R.drawable.rune_s_mountain;if("afterimage".equals(id))return R.drawable.rune_s_spectrum;return R.drawable.rune_s_fleshy;}
    private void showCharacterSkillPicker(int slot,int from,int to,String title){List<SkillData> all=repository.all();List<SkillData> choices=new ArrayList<>(all.subList(from,to));BattleUnit previewActor=skillPreviewActor();String[] items=new String[choices.size()];int[] icons=new int[choices.size()];for(int i=0;i<choices.size();i++){SkillData skill=choices.get(i);items[i]=skill.getName()+"\n"+SkillDetailFormatter.summary(skill,0,previewActor);icons[i]=skillIcon(skill);}GameModal.detailedList(this,title,items,icons,index->{SkillData selected=choices.get(index);slots[slot]=selected;upgrades[slot]=0;showCharacterSelection();},null);}
    private void showLearnedSkillPicker(int slot){boolean ultimateSlot=slot==5;List<SkillData> choices=new ArrayList<>();for(int i=6;i<repository.all().size();i++){SkillData skill=repository.all().get(i);if(skill.isUltimate()==ultimateSlot)choices.add(skill);}BattleUnit previewActor=skillPreviewActor();String[] items=new String[choices.size()+1];int[] icons=new int[choices.size()+1];items[0]=ultimateSlot?"궁극기 슬롯 비우기":"스킬 슬롯 비우기";icons[0]=R.drawable.ic_skill_empty;for(int i=0;i<choices.size();i++){SkillData skill=choices.get(i);items[i+1]=skill.getName()+" · "+skill.getStartingGrade().getLabel()+(skill.isUltimate()?" · 궁극기":"")+"\n"+SkillDetailFormatter.summary(skill,0,previewActor);icons[i+1]=skillIcon(skill);}GameModal.detailedList(this,ultimateSlot?"궁극기 선택 · 시작 등급 전설/이계":"일반 습득 스킬 선택",items,icons,index->{if(index==0){slots[slot]=null;upgrades[slot]=0;showCharacterSelection();return;}SkillData selected=choices.get(index-1);for(int i=2;i<6;i++)if(i!=slot&&slots[i]!=null&&slots[i].getId().equals(selected.getId())){toast("이미 다른 습득 슬롯에 선택한 스킬입니다.");return;}slots[slot]=selected;upgrades[slot]=0;showCharacterSelection();},null);}
    private BattleUnit skillPreviewActor(){CharacterData c=characterRepository.all().get(selectedCharacterIndex);return new PlayerUnit("SKILL_PREVIEW",c.getName(),1,1,RuneLoadout.none(),c.getMaxHp(),c.getAttack(),c.getDefense(),c.getCriticalRate(),c.getEvasionRate(),c.getPortraitResource());}
    private int skillIcon(SkillData skill){String id=skill.getId();int approvedIcon=com.pas.game.ui.ApprovedSkillIcons.find(id);if(approvedIcon!=0)return approvedIcon;if("slash".equals(id))return R.drawable.skill_slash_art;if("shield_art".equals(id))return R.drawable.skill_shield_art;if("sword_faith".equals(id))return R.drawable.skill_sword_faith;if("defend".equals(id))return R.drawable.skill_defend;if("weapon_guard".equals(id))return R.drawable.skill_weapon_guard;if("unyielding_faith".equals(id))return R.drawable.skill_unyielding_faith;if("head_bash".equals(id))return R.drawable.skill_head_bash;if("first_aid".equals(id))return R.drawable.skill_first_aid;if("certain_strike".equals(id))return R.drawable.skill_certain_strike;if("defense_focus".equals(id))return R.drawable.skill_defense_focus;if("guardian_aura".equals(id))return R.drawable.skill_guardian_aura;if("battle_cry".equals(id))return R.drawable.skill_battle_cry;if("stone_throw".equals(id))return R.drawable.skill_stone_throw;if("mangle".equals(id))return R.drawable.skill_mangle;if("reckless_charge".equals(id))return R.drawable.skill_reckless_charge;if("resolve".equals(id))return R.drawable.skill_resolve;if("full_will".equals(id))return R.drawable.skill_full_will;if("hero_swordsmanship".equals(id))return R.drawable.skill_hero_swordsmanship;if("judgment".equals(id))return R.drawable.skill_judgment;if("salvation_vow".equals(id))return R.drawable.skill_salvation_vow;
        if("archery".equals(id))return R.drawable.skill_hunter_archery;if("rapid_fire".equals(id))return R.drawable.skill_hunter_rapid_fire;if("brow_shot".equals(id))return R.drawable.skill_hunter_brow_shot;if("hunter_defend".equals(id))return R.drawable.skill_hunter_defend;if("evasion_focus".equals(id))return R.drawable.skill_hunter_evasion_focus;if("emergency_escape".equals(id))return R.drawable.skill_hunter_emergency_escape;if("fixed_trap".equals(id))return R.drawable.skill_hunter_fixed_trap;if("stealth_movement".equals(id))return R.drawable.skill_hunter_stealth_movement;if("cold_aim".equals(id))return R.drawable.skill_hunter_cold_aim;if("venom_injection".equals(id))return R.drawable.skill_hunter_venom_injection;if("piercing_shot".equals(id))return R.drawable.skill_hunter_piercing_shot;if("install_decoy".equals(id))return R.drawable.skill_hunter_install_decoy;if("swift_movement".equals(id))return R.drawable.skill_hunter_swift_movement;if("venom_chase".equals(id))return R.drawable.skill_hunter_venom_chase;if("execution_shot".equals(id))return R.drawable.skill_hunter_execution_shot;if("joint_shot".equals(id))return R.drawable.skill_hunter_joint_shot;if("solitude".equals(id))return R.drawable.skill_hunter_solitude;if("arrow_rain".equals(id))return R.drawable.skill_hunter_arrow_rain;if("deadly_poison_curse".equals(id))return R.drawable.skill_hunter_deadly_poison_curse;if("shadow_chase".equals(id))return R.drawable.skill_hunter_shadow_chase;
        if("magic_ward".equals(id)||"chaos_distortion".equals(id)||"devour".equals(id))return R.drawable.placeholder_defense;if("dimensional_drift".equals(id)||"phantom_body".equals(id))return R.drawable.ic_arrow_right;if("abyssal_mark".equals(id))return R.drawable.ic_status_debuff;if("mana_disruption".equals(id)||"mana_payment".equals(id)||"existence_loan".equals(id)||"double_cast".equals(id))return R.drawable.ic_status_buff;if("mana_discharge".equals(id)||"gluttonous_hand".equals(id)||"nether_repulsion".equals(id)||"hungry_star".equals(id)||"meteor_shower".equals(id)||"otherworld_gate".equals(id))return R.drawable.placeholder_attack;
        if(id.contains("defend")||id.contains("shield")||id.contains("prayer")||id.contains("intervention"))return R.drawable.skill_defend;if(id.contains("poison")||id.contains("venom")||id.contains("solitude"))return R.drawable.ic_status_poison;if(id.contains("movement")||id.contains("trap")||id.contains("decoy"))return R.drawable.ic_arrow_right;return R.drawable.skill_strike;}

    private void showSettings(){
        GameModal.notice(this,"베타 안내","캐릭터 선택에서 일반 스킬 3개와 궁극기 1개를 설정할 수 있습니다.\n전투 디버그 기능은 비활성화되어 있습니다.");
    }

    private void showLoadout(){
        if(!BetaFeatures.DEBUG_TOOLS)return;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setPadding(dp(16),dp(10),dp(16),dp(10));
        TextView list=label(skillCatalogText(),14);ScrollView scroll=new ScrollView(this);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.25f));
        LinearLayout controls=column();controls.setPadding(dp(18),0,0,0);TextView heading=label("스킬 장착 (최대 6개)",20);controls.addView(heading);
        LinearLayout slotRow=new LinearLayout(this);for(int i=0;i<6;i++){final int slot=i;Button b=button(String.valueOf(i+1));b.setOnClickListener(v->{selectedSlot=slot;showLoadout();});slotRow.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));}controls.addView(slotRow);
        TextView current=label("선택 슬롯 "+(selectedSlot+1)+": "+(slots[selectedSlot]==null?"비어 있음":slots[selectedSlot].getName())+" / 강화 "+upgrades[selectedSlot],17);controls.addView(current);
        EditText number=new EditText(this);number.setHint("스킬 번호 입력");number.setInputType(InputType.TYPE_CLASS_NUMBER);controls.addView(number);
        Button equip=button("선택 슬롯에 장착");controls.addView(equip);equip.setOnClickListener(v->{int n=parse(number.getText().toString(),-1);if(n<1||n>repository.all().size()){toast("올바른 스킬 번호를 입력하세요.");return;}slots[selectedSlot]=repository.all().get(n-1);upgrades[selectedSlot]=0;showLoadout();});
        LinearLayout grades=new LinearLayout(this);Button down=button("강화 -");Button up=button("강화 +");grades.addView(down,new LinearLayout.LayoutParams(0,dp(45),1));grades.addView(up,new LinearLayout.LayoutParams(0,dp(45),1));controls.addView(grades);down.setEnabled(BetaFeatures.DEBUG_TOOLS);up.setEnabled(BetaFeatures.DEBUG_TOOLS);down.setOnClickListener(v->{upgrades[selectedSlot]=Math.max(0,upgrades[selectedSlot]-1);showLoadout();});up.setOnClickListener(v->{SkillData s=slots[selectedSlot];if(s!=null)upgrades[selectedSlot]=Math.min(SkillGrade.values().length-1-s.getStartingGrade().ordinal(),upgrades[selectedSlot]+1);showLoadout();});
        Button start=button("살아있는 허수아비와 전투 시작");Button back=button("메인으로");controls.addView(start);controls.addView(back);start.setOnClickListener(v->startBattle());back.setOnClickListener(v->showMain());root.addView(controls,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1));setScreen(root);
    }

    private String skillCatalogText(){CharacterData c=characterRepository.all().get(selectedCharacterIndex);StringBuilder b=new StringBuilder(c.getName()+" 전체 스킬\n\n");BattleUnit actor=skillPreviewActor();int i=1;for(SkillData s:repository.all()){b.append(i++).append(". ").append(s.getName()).append(" [").append(s.getStartingGrade().getLabel()).append(s.isUltimate()?" · 궁극기":"").append("]\n").append(SkillDetailFormatter.detail(s,0,actor,null)).append("\n\n");}return b.toString();}

    private void startBattle(){
        if(playMode==PlayMode.ONLINE_COOP){showOnlineLobby();return;}
        savePartyEditor();
        try{partySelection.build(playMode);}catch(IllegalStateException e){GameModal.notice(this,"파티 설정 확인",e.getMessage());return;}
        adventurePotions=PotionInventory.empty();
        shopSession=new com.pas.game.shop.ShopSession(partySelection.build(playMode),chapter,com.pas.game.shop.ShopRules.beta(),adventurePotions);
        showShop(this::enterPreparedBattle,"전투로 이동");
    }

    private void showShop(Runnable next,String nextLabel){
        screenStage=6;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setScreen(new com.pas.game.ui.shop.ShopScreen(this,next,nextLabel,playMode==PlayMode.ONLINE_COOP?null:shopSession).view());
    }

    private void visitOnlineShop(com.pas.game.network.RoomApiClient api,com.pas.game.network.RoomApiClient.Connection connection,Runnable next){
        screenStage=6;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if(remoteShop!=null)remoteShop.dispose();
        remoteShop=new com.pas.game.ui.shop.RemoteShopScreen(this,api,connection,()->{
            screenStage=4;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            next.run();
        });
        setScreen(remoteShop.view());
    }

    private void enterPreparedBattle(){
        if(playMode==PlayMode.ONLINE_COOP){showOnlineLobby();return;}
        List<PlayerBattleSetup> players;
        try{players=shopSession==null?partySelection.build(playMode):shopSession.players();}catch(IllegalStateException e){GameModal.notice(this,"파티 설정 확인",e.getMessage());return;}
        screenStage=5;selectedEnemyId=null;
        engine=BattleFactory.createParty(players,false,random,debug,adventurePotions);
        commandSource=new LocalPlayerController(engine,this::handle);enemyAI=new EncounterEnemyAI(random);outcomeShown=false;clearSelection();engine.start();showBattle();scheduleEnemyIfNeeded();
    }

    private void showBattle(){
        screenStage=3;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);FrameLayout root=new FrameLayout(this);root.setPadding(dp(8),dp(6),dp(8),dp(6));
        ImageView background=new ImageView(this);background.setImageResource(R.drawable.test_battle);background.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(background,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        View shade=new View(this);shade.setBackgroundColor(Color.argb(125,0,0,0));root.addView(shade,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.HORIZONTAL);content.setGravity(Gravity.CENTER);root.addView(content,new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout left=column();left.setPadding(dp(8),dp(6),dp(8),dp(6));left.setBackgroundResource(R.drawable.bg_battle_panel);LinearLayout.LayoutParams leftLp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,.44f);leftLp.setMargins(0,0,dp(4),0);content.addView(left,leftLp);
        board=new BattleBoardView(this);board.setBattlefieldBackgroundResource(R.drawable.test_battle);board.setShowTileNumbers(false);board.bind(engine.getState());board.setListener(this::onTileTapped);LinearLayout.LayoutParams boardLp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.12f);boardLp.setMargins(dp(4),0,dp(4),0);content.addView(board,boardLp);
        LinearLayout right=column();right.setPadding(dp(8),dp(6),dp(8),dp(6));right.setGravity(Gravity.CENTER_HORIZONTAL);right.setBackgroundResource(R.drawable.bg_battle_panel);LinearLayout.LayoutParams rightLp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,.44f);rightLp.setMargins(dp(4),0,0,0);content.addView(right,rightLp);

        turnInfo=battleLabel("",10,Color.rgb(190,205,218));turnInfo.setPadding(0,0,0,0);left.addView(turnInfo,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(18)));
        LinearLayout playerHeader=new LinearLayout(this);playerHeader.setOrientation(LinearLayout.HORIZONTAL);playerHeader.setGravity(Gravity.CENTER_VERTICAL);left.addView(playerHeader,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(44)));
        ImageView portrait=new ImageView(this);playerPortrait=portrait;portrait.setImageResource(characterRepository.all().get(selectedCharacterIndex).getPortraitResource());portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);portrait.setContentDescription("내 캐릭터 상세 정보");portrait.setOnClickListener(v->{BattleUnit player=displayedPlayer();if(player!=null)showCharacterDetails(player);});playerHeader.addView(portrait,new LinearLayout.LayoutParams(dp(40),dp(40)));
        LinearLayout playerStats=column();playerStats.setPadding(dp(6),0,0,0);playerHeader.addView(playerStats,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));playerName=battleLabel("",14,Color.WHITE);playerName.setPadding(0,0,0,0);playerStats.addView(playerName);playerHpBar=hpBar(Color.rgb(64,170,104));playerStats.addView(playerHpBar,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(7)));playerHpText=battleLabel("",10,Color.WHITE);playerHpText.setPadding(0,0,0,0);playerHpText.setIncludeFontPadding(false);playerHpText.setSingleLine(true);playerStats.addView(playerHpText);

        HorizontalScrollView playerStatusScroll=new HorizontalScrollView(this);playerStatusScroll.setHorizontalScrollBarEnabled(false);playerStatusRow=new LinearLayout(this);playerStatusRow.setOrientation(LinearLayout.HORIZONTAL);playerStatusRow.setGravity(Gravity.CENTER_VERTICAL);playerStatusScroll.addView(playerStatusRow);left.addView(playerStatusScroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(30)));
        preview=battleLabel("",9,Color.rgb(222,228,233));preview.setGravity(Gravity.CENTER);preview.setPadding(0,0,0,0);left.addView(preview,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(18)));
        commandGrid=new GridLayout(this);commandGrid.setColumnCount(2);left.addView(commandGrid,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
        battleUtilityButton=button("물약");left.addView(battleUtilityButton,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(32)));
        Button end=compactBattleButton("턴 종료",12);left.addView(end,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(34)));
        LinearLayout lower=new LinearLayout(this);lower.setGravity(Gravity.CENTER_VERTICAL);Button inspectLog=compactBattleButton("상세 로그",10);Button debugButton=compactBattleButton("DEBUG",9);lower.addView(inspectLog,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1));if(BetaFeatures.DEBUG_TOOLS)lower.addView(debugButton,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,.55f));left.addView(lower,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(32)));
        logView=battleLabel("",1,Color.TRANSPARENT);logView.setVisibility(View.GONE);left.addView(logView,new LinearLayout.LayoutParams(0,0));

        HorizontalScrollView enemySelectorScroll=new HorizontalScrollView(this);enemySelectorScroll.setHorizontalScrollBarEnabled(false);enemySelectorScroll.setFillViewport(true);enemySelectorRow=new LinearLayout(this);enemySelectorRow.setOrientation(LinearLayout.HORIZONTAL);enemySelectorRow.setGravity(Gravity.CENTER);enemySelectorScroll.addView(enemySelectorRow);right.addView(enemySelectorScroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(36)));
        enemyName=battleLabel("",14,Color.WHITE);enemyName.setGravity(Gravity.CENTER);enemyName.setPadding(dp(4),0,dp(4),0);enemyName.setIncludeFontPadding(false);enemyName.setSingleLine(true);right.addView(enemyName,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(30)));
        enemyImage=new ImageView(this);enemyImage.setImageResource(R.drawable.enemy_south_1);enemyImage.setScaleType(ImageView.ScaleType.FIT_CENTER);right.addView(enemyImage,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));
        enemyHpBar=hpBar(Color.rgb(205,62,62));right.addView(enemyHpBar,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(8)));enemyHpText=battleLabel("",10,Color.WHITE);enemyHpText.setGravity(Gravity.CENTER);enemyHpText.setPadding(dp(4),0,dp(4),0);enemyHpText.setIncludeFontPadding(false);enemyHpText.setSingleLine(true);right.addView(enemyHpText,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(24)));HorizontalScrollView enemyStatusScroll=new HorizontalScrollView(this);enemyStatusScroll.setHorizontalScrollBarEnabled(false);enemyStatusRow=new LinearLayout(this);enemyStatusRow.setOrientation(LinearLayout.HORIZONTAL);enemyStatusRow.setGravity(Gravity.CENTER_VERTICAL);enemyStatusScroll.addView(enemyStatusRow);right.addView(enemyStatusScroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(30)));enemyIntent=battleLabel("",12,Color.rgb(255,221,139));enemyIntent.setGravity(Gravity.CENTER);enemyIntent.setBackgroundResource(R.drawable.bg_intent_panel);right.addView(enemyIntent,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(68)));

        end.setOnClickListener(v->{BattleUnit a=engine.activeUnit();if(a!=null&&a.getTeam()==Team.PLAYER)submitCommand(new EndTurnCommand(a.getUnitId()));});inspectLog.setOnClickListener(v->showFullLog());debugButton.setOnClickListener(v->showDebugPanel());setScreen(root);refreshBattle();
    }

    private void refreshBattle(){if(engine==null)return;if(board!=null)board.bind(engine.getState());BattleUnit active=engine.activeUnit();BattleUnit player=displayedPlayer();List<BattleUnit> enemies=engine.getState().living(Team.ENEMY);BattleUnit enemy=resolveSelectedEnemy(enemies);turnInfo.setText("ROUND "+engine.getState().getRound()+" · "+(isPlayerTurn()?"나의 턴":"적의 턴"));if(player!=null){if(playerPortrait!=null&&player instanceof PlayerUnit)playerPortrait.setImageResource(((PlayerUnit)player).getMarkerResource());playerName.setText(player.getName());setHp(playerHpBar,playerHpText,player);renderStatuses(playerStatusRow,player);preview.setText(engine.hasPendingWizardReturn()?"차원 표류 복귀 위치 선택":moveMode?"중앙에서 이동 칸 선택":"위치 "+player.getTile()+" · 이동 "+engine.getState().getTurn().getMovesRemaining()+" · 행동 "+engine.getState().getTurn().getSkillsRemaining());}else renderStatuses(playerStatusRow,null);renderEnemySelector(enemies);if(enemy!=null){enemyName.setText(enemy.getName());enemyImage.setImageResource(enemyImageResource(enemy));setHp(enemyHpBar,enemyHpText,enemy);renderStatuses(enemyStatusRow,enemy);enemyIntent.setText(enemyIntentText(enemy,player));}else{enemyName.setText("적 없음");enemyImage.setImageDrawable(null);enemyHpBar.setProgress(0);enemyHpText.setText("HP 0 / 0");renderStatuses(enemyStatusRow,null);enemyIntent.setText("전투가 끝났다.");}if(commandGrid!=null){renderCommandGrid();boolean mine=isPlayerTurn()&&engine.getState().getOutcome()==BattleOutcome.ONGOING;View panel=(View)commandGrid.getParent();panel.setAlpha(mine?1f:.55f);for(int i=0;i<commandGrid.getChildCount();i++)commandGrid.getChildAt(i).setEnabled(mine);if(battleUtilityButton!=null)battleUtilityButton.setEnabled(mine||learnedSkillMode);}if(board!=null)highlightBoard(active);List<String> logs=engine.getState().getLogs();logView.setText(logs.isEmpty()?"":logs.get(logs.size()-1));if(engine.hasPendingWizardReturn())showWizardReturnPicker();if(engine.getState().getOutcome()!=BattleOutcome.ONGOING&&!outcomeShown){outcomeShown=true;GameModal.confirm(this,engine.getState().getOutcome()==BattleOutcome.VICTORY?"승리":"패배","전투 로그에서 상세 결과를 확인할 수 있습니다.","다시 준비",this::showCharacterSelection,"상세 로그",this::showFullLog);}}
    private BattleUnit resolveSelectedEnemy(List<BattleUnit> enemies){if(enemies.isEmpty()){selectedEnemyId=null;return null;}for(BattleUnit enemy:enemies)if(enemy.getUnitId().equals(selectedEnemyId))return enemy;BattleUnit active=engine.activeUnit();BattleUnit fallback=active!=null&&active.getTeam()==Team.ENEMY?active:enemies.get(0);selectedEnemyId=fallback.getUnitId();return fallback;}
    private BattleUnit selectedEnemy(){if(engine==null)return null;return resolveSelectedEnemy(engine.getState().living(Team.ENEMY));}
    private void renderEnemySelector(List<BattleUnit> enemies){if(enemySelectorRow==null)return;enemySelectorRow.removeAllViews();if(enemies.isEmpty()){TextView empty=battleLabel("적 0",9,Color.rgb(132,147,158));empty.setGravity(Gravity.CENTER);enemySelectorRow.addView(empty,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(34)));return;}for(int i=0;i<enemies.size();i++){BattleUnit unit=enemies.get(i);boolean selected=unit.getUnitId().equals(selectedEnemyId);Button tab=compactBattleButton("적 "+(i+1)+" · "+unit.getHp()+"/"+unit.getMaxHp(),8);tab.setSingleLine(true);tab.setTextColor(selected?Color.WHITE:Color.rgb(190,205,218));tab.setBackgroundResource(selected?R.drawable.bg_enemy_tab_selected:R.drawable.bg_enemy_tab);tab.setOnClickListener(v->{selectedEnemyId=unit.getUnitId();refreshBattle();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(32));lp.setMargins(dp(2),dp(1),dp(2),dp(1));enemySelectorRow.addView(tab,lp);}}
    private int enemyImageResource(BattleUnit enemy){return com.pas.game.ui.EnemyPortraits.resource(enemy);}
    private BattleUnit firstLiving(Team team){for(BattleUnit unit:engine.getState().getUnits())if(unit.getTeam()==team&&unit.countsForOutcome()&&!unit.isDead())return unit;return null;}
    private BattleUnit displayedPlayer(){if(engine==null)return null;BattleUnit active=engine.activeUnit();return active!=null&&active.getTeam()==Team.PLAYER?active:firstLiving(Team.PLAYER);}
    private void setHp(ProgressBar bar,TextView text,BattleUnit unit){bar.setMax(unit.getMaxHp());bar.setProgress(unit.getHp());text.setText("HP "+unit.getHp()+" / "+unit.getMaxHp()+(unit.getBarrier()>0?"  ·  피해방어 "+unit.getBarrier():""));}
    private void renderStatuses(LinearLayout row,BattleUnit unit){
        if(row==null)return;row.removeAllViews();if(unit==null)return;
        for(com.pas.game.status.StatusSummary.Group group:com.pas.game.status.StatusSummary.of(unit,engine.getState())){
            TextView chip=battleLabel(group.label+" "+group.value,9,Color.WHITE);chip.setSingleLine(true);chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(6),0,dp(6),0);chip.setBackgroundResource(group.debuff?R.drawable.bg_status_debuff:R.drawable.bg_status_buff);
            Drawable icon=getDrawable(com.pas.game.ui.StatusChipIcons.forGroup(group));if(icon!=null){icon.setBounds(0,0,dp(18),dp(18));chip.setCompoundDrawables(icon,null,null,null);chip.setCompoundDrawablePadding(dp(3));}
            chip.setOnClickListener(v->GameModal.notice(this,group.label,group.description));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(27));lp.setMargins(dp(2),1,dp(2),1);row.addView(chip,lp);
        }
        if(row.getChildCount()==0)row.addView(battleLabel("상태효과 없음",8,Color.GRAY));
    }
    private void addBarrierChip(LinearLayout row,int amount){LinearLayout chip=new LinearLayout(this);chip.setOrientation(LinearLayout.HORIZONTAL);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setPadding(dp(5),0,dp(6),0);chip.setBackgroundResource(R.drawable.bg_status_buff);ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.skill_shield);chip.addView(icon,new LinearLayout.LayoutParams(dp(18),dp(18)));TextView text=battleLabel(StatusDisplay.barrierLabel(amount),8,Color.WHITE);text.setSingleLine(true);text.setPadding(dp(3),0,0,0);chip.addView(text,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT));chip.setOnClickListener(v->GameModal.notice(this,"피해방어",StatusDisplay.barrierDescription(amount)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(27));lp.setMargins(dp(2),1,dp(2),1);row.addView(chip,lp);}
    private void addUnitEffectChip(LinearLayout row,BattleUnit unit,UnitEffectType type){double total=unit.sumEffect(type);String suffix=type.getValueSuffix();LinearLayout chip=new LinearLayout(this);chip.setOrientation(LinearLayout.HORIZONTAL);chip.setGravity(Gravity.CENTER_VERTICAL);chip.setPadding(dp(5),0,dp(6),0);chip.setBackgroundResource(R.drawable.bg_status_buff);ImageView icon=new ImageView(this);icon.setImageResource(type==UnitEffectType.HEAVY_ARMOR?R.drawable.skill_shield:R.drawable.ic_status_buff);chip.addView(icon,new LinearLayout.LayoutParams(dp(18),dp(18)));TextView text=battleLabel(type.getDisplayName()+" "+formatNumber(total)+suffix,8,Color.WHITE);text.setSingleLine(true);text.setPadding(dp(3),0,0,0);chip.addView(text,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT));String detail="현재 합계: "+formatNumber(total)+suffix+"\n"+unitEffectRule(type);chip.setOnClickListener(v->GameModal.notice(this,"전투 효과 · "+type.getDisplayName(),detail));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(27));lp.setMargins(dp(2),1,dp(2),1);row.addView(chip,lp);}
    private String unitEffectRule(UnitEffectType type){switch(type){case HEAVY_ARMOR:return "받는 일반 피해를 해당 수치만큼 감소시킵니다.";case VENOM_COATING:return "즉발 공격의 각 타수마다 적에게 해당 수치의 중독을 부여합니다.";case LIFESTEAL:return "가한 즉발형 최종 피해의 해당 비율만큼 HP를 회복합니다.\n소수점 회복량은 올림합니다.";case SURVIVAL:return "자신의 3번째 턴마다 해당 수치만큼 HP를 회복합니다.";case CRITICAL_DAMAGE_UP:return "기본 치명타 배율 150%에 해당 비율을 더합니다.";case CRITICAL_RATE_UP:return "치명타율에 해당 확률 포인트를 가산합니다.";case DETERMINATION:return "방어력이 해당 수치만큼 증가합니다.";case SHARP_GAIN:return "일반 공격이 치명타면 해당 비율의 예리함을 얻습니다. 비치명타면 보유한 예리함이 사라집니다.";default:return "";}}
    private String formatNumber(double value){return value==(long)value?String.valueOf((long)value):String.format(java.util.Locale.US,"%.1f",value);}
    private int statusIcon(StatusType type){if(type==StatusType.FIRE)return R.drawable.ic_status_fire;if(type==StatusType.POISON)return R.drawable.ic_status_poison;if(type==StatusType.DAZED||type==StatusType.STIFF||type==StatusType.BIND)return R.drawable.ic_status_control;return StatusDisplay.isDebuff(type)?R.drawable.ic_status_debuff:R.drawable.ic_status_buff;}
    private ProgressBar hpBar(int color){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setProgressTintList(ColorStateList.valueOf(color));bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(55,62,70)));return bar;}
    private TextView battleLabel(String text,float size,int color){TextView view=label(text,size);view.setTextColor(color);return view;}
    private void addCommandButton(GridLayout grid,String text,int iconId,View.OnClickListener action){addCommandButton(grid,text,iconId,null,action);}
    private void addCommandButton(GridLayout grid,String text,int iconId,SkillRuntime runtime,View.OnClickListener action){int index=grid.getChildCount();BattleCommandButtonView button=new BattleCommandButtonView(this);button.bind(text,iconId,runtime);button.setOnClickListener(action);if(runtime!=null){button.setContentDescription(text+". 길게 눌러 상세 정보 확인");button.setOnLongClickListener(v->{showSkillDetail(runtime);return true;});}GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(46);lp.rowSpec=GridLayout.spec(index/2);lp.columnSpec=GridLayout.spec(index%2,1f);lp.setGravity(Gravity.FILL_HORIZONTAL|Gravity.TOP);lp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(button,lp);}
    private void showSkillDetail(SkillRuntime skill){showSkillDetail(skill,engine==null?skillPreviewActor():engine.activeUnit());}
    private void showSkillDetail(SkillRuntime skill,BattleUnit actor){
        if(engine!=null&&skill.getData().getTargetType()==TargetType.ALLY){
            List<BattleUnit> allies=new ArrayList<>();for(BattleUnit unit:engine.getState().living(actor.getTeam()))if(BattleGrid.distance(actor.getTile(),unit.getTile())<=skill.getData().getRange())allies.add(unit);
            if(allies.size()>1){String[] names=new String[allies.size()];for(int i=0;i<allies.size();i++){BattleUnit ally=allies.get(i);names[i]=ally.getName()+" · HP "+ally.getHp()+"/"+ally.getMaxHp();}GameModal.list(this,"상세 계산 대상 선택",names,null,index->showSkillDetailForTarget(skill,actor,allies.get(index)),null);return;}
            if(!allies.isEmpty()){showSkillDetailForTarget(skill,actor,allies.get(0));return;}
        }
        BattleUnit target=actor;if(skill.getData().getTargetType()==TargetType.ENEMY||skill.getData().getTargetType()==TargetType.ALL_ENEMIES||skill.getData().getTargetType()==TargetType.ALL_ENEMIES_IN_RANGE){BattleUnit enemy=actor!=null&&actor.getTeam()==Team.ENEMY?displayedPlayer():selectedEnemy();if(enemy!=null)target=enemy;}showSkillDetailForTarget(skill,actor,target);
    }
    private void showSkillDetailForTarget(SkillRuntime skill,BattleUnit actor,BattleUnit target){GameModal.notice(this,skill.getData().getName()+" 상세",SkillDetailFormatter.detail(skill,actor,target));}
    private void showCharacterDetails(BattleUnit unit){LinearLayout content=column();content.setPadding(dp(8),dp(8),dp(8),dp(8));String stats="HP  "+unit.getHp()+" / "+unit.getMaxHp()+"\n공격력  "+unit.getAttack()+"    ·    방어력  "+unit.getDefense()+"\n치명타율  "+formatNumber(unit.getCriticalRate())+"%    ·    회피율  "+formatNumber(unit.getEvasionRate())+"%\n현재 위치  "+unit.getTile();TextView statText=battleLabel(stats,14,Color.WHITE);statText.setPadding(dp(10),dp(8),dp(10),dp(8));statText.setBackgroundResource(R.drawable.bg_intent_panel);content.addView(statText,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));for(com.pas.game.status.StatusSummary.Group group:com.pas.game.status.StatusSummary.of(unit,engine.getState())){Button effectRow=button(group.label+" "+group.value);effectRow.setOnClickListener(v->GameModal.notice(this,group.label,group.description));content.addView(effectRow,new LinearLayout.LayoutParams(-1,dp(42)));}TextView passiveTitle=battleLabel("패시브 스킬",17,Color.rgb(255,221,139));passiveTitle.setPadding(dp(4),dp(10),0,dp(4));content.addView(passiveTitle);if(unit.getPassives().isEmpty()){TextView empty=battleLabel("장착된 패시브가 없습니다.",14,Color.rgb(160,174,185));content.addView(empty);}else for(PassiveRuntime passive:unit.getPassives()){PassiveData data=passive.getData();Button row=button(data.getName()+"  LV."+passive.getLevel()+"  ·  현재 "+formatNumber(passive.currentValue())+data.getValueSuffix());row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setOnClickListener(v->showPassiveGrowth(passive));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(50));lp.setMargins(0,dp(2),0,dp(2));content.addView(row,lp);}TextView activeTitle=battleLabel("액티브 스킬",17,Color.rgb(255,221,139));activeTitle.setPadding(dp(4),dp(10),0,dp(4));content.addView(activeTitle);boolean hasSkill=false;for(SkillRuntime skill:unit.getEquippedSkills()){if(skill==null)continue;hasSkill=true;String uses=skill.getRemainingUses()<0?"∞":skill.getRemainingUses()+" / "+skill.getMaxUses();String cooldown=skill.getCooldownRemaining()==0?"사용 가능":"쿨타임 "+skill.getCooldownRemaining()+"턴";Button row=button((skill.getData().isUltimate()?"★ ":"")+skill.getData().getName()+"  ·  "+uses+"회\n"+skill.getCurrentGrade().getLabel()+"  ·  "+cooldown);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setLines(2);row.setOnClickListener(v->showSkillDetail(skill,unit));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(58));lp.setMargins(0,dp(2),0,dp(2));content.addView(row,lp);}if(!hasSkill){TextView empty=battleLabel("장착된 액티브 스킬이 없습니다.",14,Color.rgb(160,174,185));content.addView(empty);}ScrollView scroll=new ScrollView(this);scroll.addView(content);GameModal.custom(this,unit.getName()+" 상세 정보",scroll);}
    private void showPassiveGrowth(PassiveRuntime runtime){PassiveData data=runtime.getData();StringBuilder growth=new StringBuilder("현재 LV.").append(runtime.getLevel()).append("\n").append(data.describeAt(runtime.getLevel())).append("\n\n");int levels=Math.max(5,data.getLevelCount());for(int level=1;level<=levels;level++)growth.append(level==runtime.getLevel()?"▶ ":"   ").append("LV.").append(level).append("  ").append(data.describeAt(level)).append('\n');GameModal.notice(this,"패시브 · "+data.getName(),growth.toString());}
    private void styleBattleCommandButton(Button button,int iconId,float textSize){button.setTextSize(textSize);button.setTextColor(Color.WHITE);button.setGravity(Gravity.CENTER);button.setPadding(0,0,0,0);button.setMinHeight(0);button.setMinimumHeight(0);button.setBackgroundResource(R.drawable.bg_btn_start);Drawable icon=getDrawable(iconId);if(icon!=null){icon.setBounds(0,0,dp(26),dp(26));button.setCompoundDrawables(icon,null,null,null);button.setCompoundDrawablePadding(dp(6));}}
    private Button compactBattleButton(String text,float textSize){Button button=button(text);button.setTextSize(textSize);button.setGravity(Gravity.CENTER);button.setPadding(dp(4),0,dp(4),0);button.setMinHeight(0);button.setMinimumHeight(0);button.setMinimumWidth(0);return button;}
    private void renderCommandGrid(){
        if(commandGrid==null||engine==null)return;commandGrid.removeAllViews();renderBattleUtilityButton();
        if(learnedSkillMode){BattleUnit actor=displayedPlayer();List<SkillRuntime> equipped=actor==null?new ArrayList<>():actor.getEquippedSkills();for(int i=2;i<6;i++){SkillRuntime skill=i<equipped.size()?equipped.get(i):null;boolean ultimate=i==5;if(skill!=null)addCommandButton(commandGrid,(ultimate?"★ ":"")+skill.getData().getName(),skillIcon(skill.getData()),skill,v->beginSkill(skill));else addCommandButton(commandGrid,ultimate?"궁극기 · 비어 있음":"비어 있음",R.drawable.ic_skill_empty,v->toast(ultimate?"장착한 궁극기가 없습니다.":"선택한 스킬이 없습니다."));}return;}
        BattleUnit actor=displayedPlayer();List<SkillRuntime> equipped=actor==null?new ArrayList<>():actor.getEquippedSkills();SkillRuntime attack=equipped.size()>0?equipped.get(0):null;SkillRuntime defense=equipped.size()>1?equipped.get(1):null;
        addCommandButton(commandGrid,attack==null?"공격 없음":attack.getData().getName(),attack==null?0:skillIcon(attack.getData()),attack,v->beginEquippedSkillAt(0));
        addCommandButton(commandGrid,defense==null?"방어 없음":defense.getData().getName(),defense==null?0:skillIcon(defense.getData()),defense,v->beginEquippedSkillAt(1));
        addCommandButton(commandGrid,"이동",R.drawable.ic_arrow_right,v->{if(!isPlayerTurn())return;clearSelection();moveMode=true;refreshBattle();});
        addCommandButton(commandGrid,"스킬",0,v->{if(!isPlayerTurn()){toast("플레이어의 턴이 아닙니다.");return;}clearSelection();learnedSkillMode=true;refreshBattle();});
    }
    private void renderBattleUtilityButton(){if(battleUtilityButton==null)return;if(learnedSkillMode){battleUtilityButton.setText("조작키로 돌아가기");styleBattleCommandButton(battleUtilityButton,R.drawable.ic_arrow_left,10);battleUtilityButton.setOnClickListener(v->{clearSelection();refreshBattle();});}else{PotionInventory inventory=engine==null?PotionInventory.empty():engine.getState().getPotionInventory();List<PotionData> owned=inventory.getOwnedPotions();String label=owned.size()==1?owned.get(0).getName()+" ×"+inventory.getCount(owned.get(0)):"물약 ×"+inventory.getCount();battleUtilityButton.setText(label);styleBattleCommandButton(battleUtilityButton,R.drawable.ic_potion,10);battleUtilityButton.setOnClickListener(v->showPotionDialog());}}
    private void showPotionDialog(){if(!isPlayerTurn()){toast("플레이어의 턴이 아닙니다.");return;}PotionInventory inventory=engine.getState().getPotionInventory();List<PotionData> owned=inventory.getOwnedPotions();if(owned.isEmpty()){GameModal.notice(this,"포션","소지한 포션이 없습니다.");return;}String[] items=new String[owned.size()];for(int i=0;i<owned.size();i++){PotionData potion=owned.get(i);items[i]=potion.getName()+" ×"+inventory.getCount(potion)+"  ·  HP "+potion.getHealing()+" 회복";}GameModal.list(this,"사용할 포션 선택 · "+inventory.getCount()+" / "+PotionInventory.MAX_COUNT,items,null,index->showPotionUseConfirm(owned.get(index)),null);}
    private void showPotionUseConfirm(PotionData potion){PotionInventory inventory=engine.getState().getPotionInventory();String detail=potion.getTier()+"단계 · HP "+potion.getHealing()+" 회복\n소지 "+inventory.getCount(potion)+"개 · 가격 "+potion.getPrice()+"\n\n"+potion.getDescription()+"\n\n사용 시 이번 턴의 행동 기회 1회를 소모합니다.";GameModal.confirm(this,potion.getName(),detail,"사용",()->{BattleUnit actor=engine.activeUnit();if(actor!=null)submitCommand(new UsePotionCommand(actor.getUnitId(),potion.getTier()));},"취소",null);}
    private void beginEquippedSkillAt(int slot){if(!isPlayerTurn()){toast("플레이어의 턴이 아닙니다.");return;}List<SkillRuntime> equipped=engine.activeUnit().getEquippedSkills();if(slot<0||slot>=equipped.size()||equipped.get(slot)==null){toast("선택한 스킬이 없습니다.");return;}beginSkill(equipped.get(slot));}
    private void beginSkill(SkillRuntime skill){
        if(!isPlayerTurn()){toast("플레이어의 턴이 아닙니다.");return;}if(engine.getState().getTurn().getSkillsRemaining()<=0){toast("이번 턴의 행동 기회를 모두 사용했습니다.");return;}if(!skill.canUse()&&!debug.isAllSkillsAvailable()){toast("사용 횟수 또는 쿨타임을 확인하세요.");return;}
        clearSelection();selectedSkill=skill;BattleUnit actor=engine.activeUnit();TargetType type=skill.getData().getTargetType();if(type==TargetType.SELF||type==TargetType.NONE||type==TargetType.ALL_ENEMIES||type==TargetType.ALL_ENEMIES_IN_RANGE){selectedTargetId=actor.getUnitId();selectedTile=actor.getTile();executeSelected();return;}if(type==TargetType.TILE){showSkillTileBoard(skill);return;}
        List<BattleUnit> targets=new ArrayList<>();for(BattleUnit unit:engine.getState().getUnits())if(isPotentialTarget(actor,unit,skill))targets.add(unit);if(targets.isEmpty()){clearSelection();toast("현재 사거리 안에 유효한 대상이 없습니다. 이동이 필요합니다.");return;}if(type!=TargetType.ENEMY&&targets.size()==1){executeSkillOn(targets.get(0));return;}String[] names=new String[targets.size()];for(int i=0;i<targets.size();i++)names[i]=targets.get(i).getName()+"  ·  HP "+targets.get(i).getHp()+"/"+targets.get(i).getMaxHp();GameModal.list(this,type==TargetType.ENEMY?"공격 대상 선택":"대상 선택",names,null,index->executeSkillOn(targets.get(index)),this::clearSelection);
    }
    private void executeSkillOn(BattleUnit target){selectedTargetId=target.getUnitId();selectedTile=target.getTile();executeSelected();}
    private void showMoveBoard(){
        if(!isPlayerTurn()){toast("플레이어의 턴이 아닙니다.");return;}BattleUnit actor=engine.activeUnit();if(!engine.getState().getTurn().canMove()){toast("이번 턴에는 더 이동할 수 없습니다.");return;}Dialog dialog=new Dialog(this);LinearLayout box=column();box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackgroundResource(R.drawable.bg_battle_panel);TextView title=battleLabel("이동할 인접 칸을 선택하세요",18,Color.WHITE);title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(42)));BattleBoardView moveBoard=new BattleBoardView(this);moveBoard.bind(engine.getState());Set<Integer> tiles=new HashSet<>(BattleGrid.adjacent(actor.getTile()));moveBoard.setSelection(tiles,null);box.addView(moveBoard,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));Button close=button("취소");box.addView(close,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(42)));dialog.setContentView(box);moveBoard.setListener(tile->{if(BattleGrid.distance(actor.getTile(),tile)!=1){toast("강조된 인접 칸을 선택하세요.");return;}dialog.dismiss();submitCommand(new MoveCommand(actor.getUnitId(),tile,MovementType.VOLUNTARY));});close.setOnClickListener(v->dialog.dismiss());dialog.show();Window window=dialog.getWindow();if(window!=null){window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.84f));WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.78f;window.setAttributes(attrs);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
    }
    private void showSkillTileBoard(SkillRuntime skill){
        BattleUnit actor=engine.activeUnit();Dialog dialog=new Dialog(this);LinearLayout box=column();box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackgroundResource(R.drawable.bg_battle_panel);TextView title=battleLabel(skill.getData().getName()+" · 대상 칸을 선택하세요",18,Color.WHITE);title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(42)));BattleBoardView skillBoard=new BattleBoardView(this);skillBoard.bind(engine.getState());Set<Integer> tiles=new HashSet<>();for(int tile=1;tile<=BattleGrid.TILE_COUNT;tile++)if(isValidSkillTile(actor,skill,tile))tiles.add(tile);skillBoard.setSelection(tiles,null);box.addView(skillBoard,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));Button close=button("취소");box.addView(close,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(42)));dialog.setContentView(box);skillBoard.setListener(tile->{if(!tiles.contains(tile)){toast("강조된 스킬 사거리 안의 칸을 선택하세요.");return;}selectedTargetId=actor.getUnitId();selectedTile=tile;dialog.dismiss();executeSelected();});close.setOnClickListener(v->{dialog.dismiss();clearSelection();});dialog.setOnCancelListener(v->clearSelection());dialog.show();Window window=dialog.getWindow();if(window!=null){window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.84f));WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.78f;window.setAttributes(attrs);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
    }
    private boolean isValidSkillTile(BattleUnit actor,SkillRuntime skill,int tile){int distance=BattleGrid.distance(actor.getTile(),tile);if(distance<skill.getData().getMinimumRange()||distance>skill.getData().getRange())return false;return !skill.getData().isCardinalTileOnly()||BattleGrid.column(actor.getTile())==BattleGrid.column(tile)||BattleGrid.row(actor.getTile())==BattleGrid.row(tile);}
    private String enemyIntentText(BattleUnit enemy,BattleUnit player){return com.pas.game.battle.ai.EnemyIntent.describe(enemy,engine.getState());}
    private String previewText(){if(moveMode)return "이동 모드: 강조된 인접 칸을 선택하세요. 두 번째 이동은 스킬 기회를 소비합니다.";if(selectedSkill==null)return "스킬 선택 → 대상 선택 → 실행";String target=selectedTargetId==null?"대상 미선택":engine.getState().find(selectedTargetId).getName();return "선택: "+selectedSkill.getData().getName()+" / "+target+" / 값 "+selectedSkill.currentValue();}
    private void renderSkillButtons(BattleUnit active){skillButtons.removeAllViews();if(active==null)return;GridLayout grid=new GridLayout(this);grid.setColumnCount(3);List<SkillRuntime> skills=active.getEquippedSkills();for(SkillRuntime skill:skills){if(skill==null)continue;Button b=button(skill.getData().getName()+"\n"+(skill.getRemainingUses()<0?"∞":skill.getRemainingUses())+"회 | CD"+skill.getCooldownRemaining()+" | "+skill.getCurrentGrade().getLabel());b.setTextSize(11);b.setPadding(0,0,0,0);b.setMinHeight(0);b.setMinimumHeight(0);b.setGravity(Gravity.CENTER);b.setEnabled(active.getTeam()==Team.PLAYER&&skill.getData().isImplemented()&&(skill.canUse()||debug.isAllSkillsAvailable())&&engine.getState().getTurn().getSkillsRemaining()>0);b.setOnClickListener(v->selectSkill(skill));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(48);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(b,lp);}skillButtons.addView(grid);}
    private void selectSkill(SkillRuntime skill){clearSelection();selectedSkill=skill;BattleUnit a=engine.activeUnit();if(skill.getData().getTargetType()==TargetType.SELF||skill.getData().getTargetType()==TargetType.NONE||skill.getData().getTargetType()==TargetType.ALL_ENEMIES||skill.getData().getTargetType()==TargetType.ALL_ENEMIES_IN_RANGE){selectedTargetId=a.getUnitId();selectedTile=a.getTile();}refreshBattle();}
    private void highlightBoard(BattleUnit active){Set<Integer> tiles=new HashSet<>();if(active!=null&&active.getTeam()==Team.PLAYER){if(moveMode&&engine.getState().getTurn().canMove())tiles.addAll(BattleGrid.adjacent(active.getTile()));else if(selectedSkill!=null){for(BattleUnit u:engine.getState().getUnits())if(isPotentialTarget(active,u,selectedSkill))tiles.add(u.getTile());}}board.setSelection(tiles,selectedTile);}
    private boolean isPotentialTarget(BattleUnit actor,BattleUnit target,SkillRuntime skill){if(target.isDead()||!target.isOnField()||BattleGrid.distance(actor.getTile(),target.getTile())>skill.getData().getRange())return false;TargetType t=skill.getData().getTargetType();return(t==TargetType.ENEMY&&target.getTeam()!=actor.getTeam())||(t==TargetType.ALLY&&target.getTeam()==actor.getTeam()&&target.canReceiveSupport())||(t==TargetType.SELF&&target==actor);}

    private void onTileTapped(int tile){BattleUnit active=engine.activeUnit();if(active==null)return;if(moveMode&&active.getTeam()==Team.PLAYER){if(BattleGrid.distance(active.getTile(),tile)==1)submitCommand(new MoveCommand(active.getUnitId(),tile,MovementType.VOLUNTARY));else toast("강조된 인접 칸을 선택하세요.");return;}if(selectedSkill!=null&&active.getTeam()==Team.PLAYER){List<BattleUnit> candidates=new ArrayList<>();for(BattleUnit u:engine.getState().atTile(tile,true))if(isPotentialTarget(active,u,selectedSkill))candidates.add(u);if(candidates.size()==1)chooseTarget(candidates.get(0));else if(candidates.size()>1)showTargetPicker(candidates);else toast("이 스킬의 유효 대상이 없습니다.");return;}showUnitInfo(tile);}
    private void chooseTarget(BattleUnit unit){selectedTargetId=unit.getUnitId();selectedTile=unit.getTile();if(unit.getTeam()==Team.ENEMY)selectedEnemyId=unit.getUnitId();refreshBattle();}
    private void showTargetPicker(List<BattleUnit> units){String[] names=new String[units.size()];for(int i=0;i<units.size();i++)names[i]=units.get(i).getName()+" HP "+units.get(i).getHp();GameModal.list(this,"같은 칸의 대상 선택",names,null,index->chooseTarget(units.get(index)),null);}
    private void showUnitInfo(int tile){
        List<BattleUnit> units=engine.getState().atTile(tile,false);
        if(units.isEmpty())return;
        if(units.size()==1){inspectUnit(units.get(0));return;}
        String[] labels=new String[units.size()];
        for(int i=0;i<labels.length;i++){BattleUnit u=units.get(i);labels[i]=(u.getTeam()==Team.PLAYER?"아군 · ":"적 · ")+u.getName()+" · HP "+u.getHp()+"/"+u.getMaxHp();}
        GameModal.list(this,"이 칸의 유닛",labels,null,i->inspectUnit(units.get(i)),null);
    }
    private void inspectUnit(BattleUnit unit){if(unit.getTeam()==Team.ENEMY){selectedEnemyId=unit.getUnitId();refreshBattle();}showCharacterDetails(unit);}
    private void executeSelected(){BattleUnit active=engine.activeUnit();if(active==null||active.getTeam()!=Team.PLAYER||selectedSkill==null){toast("스킬과 대상을 먼저 선택하세요.");return;}TargetType type=selectedSkill.getData().getTargetType();if(selectedTargetId==null&&type!=TargetType.NONE&&type!=TargetType.ALL_ENEMIES&&type!=TargetType.ALL_ENEMIES_IN_RANGE){toast("대상을 선택하세요.");return;}BattleUnit target=selectedTargetId==null?null:engine.getState().find(selectedTargetId);List<String> ids=engine.wizardOptionIds(active,selectedSkill,target);if(!ids.isEmpty()){List<String> labels=engine.wizardOptionLabels(active,selectedSkill,target);GameModal.list(this,"적용할 효과 선택",labels.toArray(new String[0]),null,index->executeSelectedWithOption(ids.get(index)),this::clearSelection);return;}executeSelectedWithOption(null);}
    private void executeSelectedWithOption(String optionId){BattleUnit active=engine.activeUnit();if(active==null||selectedSkill==null)return;submitCommand(new UseSkillCommand(active.getUnitId(),selectedSkill.getData().getId(),selectedTargetId,selectedTile,optionId));}

    private void showWizardReturnPicker(){if(returnPickerShowing||engine==null||!engine.hasPendingWizardReturn())return;returnPickerShowing=true;Dialog dialog=new Dialog(this);LinearLayout box=column();box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackgroundResource(R.drawable.bg_battle_panel);TextView title=battleLabel("차원 표류 · 복귀할 칸을 선택하세요",18,Color.WHITE);title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(42)));BattleBoardView returnBoard=new BattleBoardView(this);returnBoard.bind(engine.getState());Set<Integer> tiles=new HashSet<>(engine.validWizardReturnTiles());returnBoard.setSelection(tiles,null);box.addView(returnBoard,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));dialog.setContentView(box);dialog.setCancelable(false);returnBoard.setListener(tile->{if(!tiles.contains(tile)){toast("강조된 인접 칸을 선택하세요.");return;}BattleUnit actor=engine.activeUnit();if(actor==null){toast("복귀할 캐릭터를 찾지 못했습니다.");return;}returnPickerShowing=false;dialog.dismiss();submitCommand(new ResolveWizardReturnCommand(actor.getUnitId(),tile));});dialog.show();Window window=dialog.getWindow();if(window!=null){window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.84f));WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.78f;window.setAttributes(attrs);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}}
    private void submitCommand(com.pas.game.battle.command.BattleCommand command){if(commandSource==null){toast("전투 명령 연결이 준비되지 않았습니다.");return;}commandSource.submit(command);}
    private void handle(BattleResult result){if(!result.isSuccess()){toast(result.getEvents().isEmpty()?"명령 실패":result.getEvents().get(0).getMessage());return;}clearSelection();refreshBattle();scheduleEnemyIfNeeded();}
    private void scheduleEnemyIfNeeded(){if(engine==null||engine.getState().getOutcome()!=BattleOutcome.ONGOING)return;BattleUnit active=engine.activeUnit();if(active!=null&&active.getTeam()==Team.ENEMY)handler.postDelayed(()->{enemyAI.takeTurn(engine);clearSelection();refreshBattle();scheduleEnemyIfNeeded();},420);}
    private boolean isPlayerTurn(){return engine!=null&&engine.activeUnit()!=null&&engine.activeUnit().getTeam()==Team.PLAYER;}
    private void clearSelection(){moveMode=false;learnedSkillMode=false;selectedSkill=null;selectedTargetId=null;selectedTile=null;}

    private void showFullLog(){
        StringBuilder lines=new StringBuilder();for(String line:engine.getState().getLogs())lines.append(line).append('\n');Dialog dialog=new Dialog(this);LinearLayout panel=column();panel.setPadding(dp(18),dp(12),dp(18),dp(12));panel.setBackgroundResource(R.drawable.bg_battle_panel);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);TextView title=battleLabel("상세 전투 로그",22,Color.WHITE);heading.addView(title,new LinearLayout.LayoutParams(0,dp(46),1));Button close=button("전투로 돌아가기");heading.addView(close,new LinearLayout.LayoutParams(dp(170),dp(42)));panel.addView(heading);
        TextView text=battleLabel(lines.toString(),14,Color.rgb(225,231,235));text.setPadding(dp(12),dp(8),dp(12),dp(8));text.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.setBackgroundResource(R.drawable.bg_intent_panel);scroll.addView(text);panel.addView(scroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));dialog.setContentView(panel);close.setOnClickListener(v->dialog.dismiss());dialog.show();Window window=dialog.getWindow();if(window!=null){window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.92f),(int)(getResources().getDisplayMetrics().heightPixels*.88f));WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.82f;window.setAttributes(attrs);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
    }

    private void showDebugPanel(){
        if(!BetaFeatures.DEBUG_TOOLS)return;
        LinearLayout box=column();box.setPadding(dp(16),dp(8),dp(16),dp(8));addDebugButton(box,"플레이어 HP 설정",v->promptNumber("HP",value->{BattleUnit p=displayedPlayer();if(p!=null)engine.setHp(p.getUnitId(),value);refreshBattle();}));addDebugButton(box,"선택한 적 HP 설정",v->promptNumber("HP",value->{BattleUnit e=selectedEnemy();if(e!=null)engine.setHp(e.getUnitId(),value);refreshBattle();}));
        addDebugButton(box,"HP·사용횟수 완전 회복 / 쿨타임 초기화",v->{engine.restoreAll();refreshBattle();});addDebugButton(box,"포션 종류별 획득 ("+engine.getState().getPotionInventory().getCount()+"/"+PotionInventory.MAX_COUNT+")",v->showDebugPotionPicker());addDebugButton(box,"치명타 강제: "+debug.getCritical(),v->{debug.setCritical(next(debug.getCritical()));showDebugPanel();});addDebugButton(box,"회피 강제: "+debug.getEvasion(),v->{debug.setEvasion(next(debug.getEvasion()));showDebugPanel();});addDebugButton(box,"적 AI 스킵: "+debug.isSkipEnemyAi(),v->{debug.setSkipEnemyAi(!debug.isSkipEnemyAi());showDebugPanel();});addDebugButton(box,"플레이어 무적: "+debug.isInvinciblePlayers(),v->{debug.setInvinciblePlayers(!debug.isInvinciblePlayers());showDebugPanel();});addDebugButton(box,"모든 스킬 사용 가능: "+debug.isAllSkillsAvailable(),v->{debug.setAllSkillsAvailable(!debug.isAllSkillsAvailable());showDebugPanel();});
        addDebugButton(box,"부활 수단 ON/OFF: "+(engine.getState().getReviveResourceCount()>0),v->{engine.getState().setReviveResourceCount(engine.getState().getReviveResourceCount()>0?0:1);showDebugPanel();});addDebugButton(box,"유닛 위치 강제 이동",v->promptNumber("칸 번호 1~12",tile->{BattleUnit a=engine.activeUnit();if(a!=null)engine.forceMove(a.getUnitId(),tile);refreshBattle();}));addDebugButton(box,"현재 유닛 멍해짐 추가",v->{BattleUnit a=engine.activeUnit();if(a!=null)engine.applyStatus(a,new StatusEffect("DEBUG_DAZE",StatusType.DAZED,"DEBUG",2,0,false,true));refreshBattle();});addDebugButton(box,"플레이어 화염 +10",v->{BattleUnit p=displayedPlayer();if(p!=null)engine.applyStatus(p,new StatusEffect("DEBUG_FIRE",StatusType.FIRE,"DEBUG",-1,10,true,true));refreshBattle();});addDebugButton(box,"선택한 적 중독 +5",v->{BattleUnit e=selectedEnemy();if(e!=null)engine.applyStatus(e,new StatusEffect("DEBUG_POISON",StatusType.POISON,"DEBUG",-1,5,true,true));refreshBattle();});addDebugButton(box,"선택한 적 멍해짐 2턴",v->{BattleUnit e=selectedEnemy();if(e!=null)engine.applyStatus(e,new StatusEffect("DEBUG_ENEMY_DAZE",StatusType.DAZED,"DEBUG",2,0,false,true));refreshBattle();});addDebugButton(box,"현재 유닛 상태이상 모두 제거",v->{BattleUnit a=engine.activeUnit();if(a!=null)engine.clearStatuses(a.getUnitId());refreshBattle();});
        addDebugButton(box,"스킬 강제 장착 / 강화",v->promptText("슬롯,스킬번호,강화 (예: 1,20,0)",text->{String[] p=text.split(",");if(p.length==3){int slot=parse(p[0],0)-1,index=parse(p[1],0)-1,grade=parse(p[2],0);BattleUnit a=engine.activeUnit();PlayerBattleSetup setup=a instanceof PlayerUnit?partySelection.get(((PlayerUnit)a).getPlayerSlot()):null;SkillRepository activeRepository=setup==null?null:skillRegistry.find(setup.getCharacter().getId());if(activeRepository!=null&&index>=0&&index<activeRepository.all().size())a.replaceSkill(slot,new SkillRuntime(activeRepository.all().get(index),grade));refreshBattle();}}));addDebugButton(box,"적 추가 생성",v->{engine.addDebugEnemy(12);refreshBattle();});addDebugButton(box,"적 제거",v->{engine.removeDebugEnemy();refreshBattle();});addDebugButton(box,"현재 턴 강제 종료 / 다음 유닛",v->{BattleUnit a=engine.activeUnit();if(a!=null)submitCommand(new EndTurnCommand(a.getUnitId()));});addDebugButton(box,"전투 재시작",v->startBattle());addDebugButton(box,"상세 전투 로그 표시",v->showFullLog());
        ScrollView scroll=new ScrollView(this);scroll.addView(box);GameModal.custom(this,"PAS Debug Panel",scroll);
    }
    private void showDebugPotionPicker(){if(!BetaFeatures.DEBUG_TOOLS)return;List<PotionData> potions=potionRepository.all();PotionInventory inventory=engine.getState().getPotionInventory();String[] items=new String[potions.size()];for(int i=0;i<potions.size();i++){PotionData potion=potions.get(i);items[i]=potion.getName()+" · HP "+potion.getHealing()+" · 보유 "+inventory.getCount(potion);}GameModal.list(this,"테스트 포션 획득 · "+inventory.getCount()+" / "+PotionInventory.MAX_COUNT,items,null,index->{PotionData potion=potions.get(index);int added=inventory.add(potion,1);toast(added>0?potion.getName()+" 1개 획득":"포션 소지 한도는 3개입니다.");refreshBattle();},null);}
    private DebugOptions.ForcedRoll next(DebugOptions.ForcedRoll v){if(v==DebugOptions.ForcedRoll.NORMAL)return DebugOptions.ForcedRoll.SUCCESS;if(v==DebugOptions.ForcedRoll.SUCCESS)return DebugOptions.ForcedRoll.FAILURE;return DebugOptions.ForcedRoll.NORMAL;}
    private void addDebugButton(LinearLayout box,String text,View.OnClickListener action){Button b=button(text);b.setOnClickListener(action);box.addView(b);}
    private interface IntAction{void run(int value);}private interface TextAction{void run(String value);}
    private void promptNumber(String title,IntAction action){GameModal.input(this,title,"숫자 입력",InputType.TYPE_CLASS_NUMBER,value->action.run(parse(value,0)));}
    private void promptText(String title,TextAction action){GameModal.input(this,title,"내용 입력",InputType.TYPE_CLASS_TEXT,action::run);}
    private int parse(String value,int fallback){try{return Integer.parseInt(value.trim());}catch(Exception ignored){return fallback;}}
    /** Android 15의 강제 edge-to-edge에서도 보드와 버튼이 시스템 바에 가려지지 않게 한다. */
    private void setScreen(View root){
        final int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();
        com.pas.game.ui.view.GameWindow.immersive(getWindow());
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());view.setPadding(left+bars.left,top+bars.top,right+bars.right,bottom+bars.bottom);return insets;});
        setContentView(root);ViewCompat.requestApplyInsets(root);
    }
    private int systemBarDimension(String name,int fallbackDp){int id=getResources().getIdentifier(name,"dimen","android");return id==0?dp(fallbackDp):getResources().getDimensionPixelSize(id);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView label(String text,float size){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setPadding(dp(8),dp(6),dp(8),dp(6));return v;}
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);return b;}
    private void toast(String text){GameNotice.show(this,text);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void confirmExit(){GameModal.confirm(this,"게임 종료","정말 게임을 종료하시겠습니까?","종료",this::finishAffinity,"취소",null);}
    private void handleBackNavigation(){confirmExit();}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus)com.pas.game.ui.view.GameWindow.immersive(getWindow());}
    @Override protected void onStop(){if(remoteShop!=null)remoteShop.pause();if(onlineFlow!=null)onlineFlow.pause();super.onStop();}
    @Override protected void onRestart(){super.onRestart();if(playMode==PlayMode.ONLINE_COOP&&screenStage==4)showOnlineLobby();else if(screenStage==6&&remoteShop!=null)remoteShop.resume();}
    @Override protected void onDestroy(){if(remoteShop!=null)remoteShop.dispose();if(onlineFlow!=null)onlineFlow.close();handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
