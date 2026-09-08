package com.pas.game.ui.activity;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.pas.game.BuildConfig;
import com.pas.game.R;
import com.pas.game.battle.command.EndTurnCommand;
import com.pas.game.battle.command.MoveCommand;
import com.pas.game.battle.command.UseSkillCommand;
import com.pas.game.battle.turn.MovementType;
import com.pas.game.character.CharacterData;
import com.pas.game.character.CharacterRepository;
import com.pas.game.network.*;
import com.pas.game.skill.data.SkillData;
import com.pas.game.skill.data.TargetType;
import com.pas.game.skill.repository.CharacterSkillRegistry;
import com.pas.game.skill.repository.SkillRepository;
import com.pas.game.ui.view.GameModal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/** Cloud beta entry or local debug harness. The full game UI integration remains separate. */
public final class OnlineTestActivity extends AppCompatActivity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final OkHttpClient http = new OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build();
    private final List<CharacterData> characters = new ArrayList<>();
    private final List<Button> actions = new ArrayList<>();
    private final CharacterSkillRegistry registry = new CharacterSkillRegistry();
    private RoomApiClient api;
    private ServerEndpoint endpoint;
    private RoomApiClient.Connection connection;
    private WebSocketBattleTransport transport;
    private OnlineBattleSession session;
    private RoomApiClient.Mode mode = RoomApiClient.Mode.COOP;
    private EditText address, nickname, betaKey, roomCode;
    private TextView notice, roomInfo, battleInfo;
    private LinearLayout content, arena, commandBar;
    private boolean disposed, waiting, battleShown, inFlight;
    private String selectedTarget;
    private int selectedTile = -1;
    private final Runnable poll = this::pollStatus;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (!BuildConfig.DEBUG && !BuildConfig.PAS_ONLINE_ENABLED) { finish(); return; }
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        for (CharacterData c : new CharacterRepository().all()) if (c.isAvailable()) characters.add(c);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                GameModal.confirm(OnlineTestActivity.this, "게임 종료", "정말 게임을 종료하시겠습니까?", "종료", () -> finishAffinity(), "취소", null);
            }
        });
        showEntry();
    }

    private void showEntry() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        resetContent();
        content.addView(text(BuildConfig.PAS_ONLINE_ENABLED ? "온라인 협동 베타" : "온라인 서버 연결 테스트", 24));
        content.addView(text("현재 베타는 기본 공격·방어 전투를 지원합니다.\n기존 싱글 플레이와 별도로 동작합니다.", 15));
        if (!BuildConfig.PAS_ONLINE_ENABLED) {
            address = input("서버 주소 (PC 에뮬레이터: http://10.0.2.2:8080)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        }
        nickname = input("플레이어 이름", InputType.TYPE_CLASS_TEXT); nickname.setText("테스터");
        betaKey = input(BuildConfig.PAS_ONLINE_ENABLED ? "PAS 접속 키 (Google Cloud 관리 키 아님)" : "베타 접속 키 (로컬 서버는 비워도 됩니다)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        // Do not save access keys in Android view-state bundles.
        betaKey.setSaveEnabled(false);
        Button modeButton = button("모드: 2인 협동");
        modeButton.setOnClickListener(v -> GameModal.list(this, "서버 테스트 모드", new String[]{"1인 1캐릭터", "1인 2캐릭터", "2인 협동"}, null, index -> {
            mode = RoomApiClient.Mode.values()[index]; modeButton.setText("모드: " + new String[]{"1인 1캐릭터", "1인 2캐릭터", "2인 협동"}[index]);
        }, null));
        Button create = button("방 만들기"); create.setOnClickListener(v -> connectRoom(false));
        roomCode = input("참가할 6자리 방 코드", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        Button join = button("방 참가"); join.setOnClickListener(v -> connectRoom(true));
        notice = text(BuildConfig.PAS_ONLINE_ENABLED ? "서버는 설정되어 있습니다. 전달받은 PAS 접속 키를 입력하세요." : "서버 주소를 입력하세요. 접속 키는 저장하지 않습니다.", 15); content.addView(notice);
        button("기존 게임으로 돌아가기").setOnClickListener(v -> finish());
    }
    private void connectRoom(boolean join) {
        if (inFlight) return;
        try {
            String url = BuildConfig.PAS_ONLINE_ENABLED ? BuildConfig.PAS_SERVER_URL : address.getText().toString();
            String key = betaKey.getText().toString().trim();
            if (BuildConfig.PAS_ONLINE_ENABLED && key.isEmpty()) throw new IllegalArgumentException("PAS 접속 키를 입력하세요.");
            endpoint = new ServerEndpoint(url, BuildConfig.DEBUG && !BuildConfig.PAS_ONLINE_ENABLED);
            if (api != null) api.close();
            api = new RoomApiClient(endpoint, key, http, main::post);
            inFlight = true; notice.setText("방 접속 중…");
            RoomApiClient.Result<RoomApiClient.Connection> result = new RoomApiClient.Result<RoomApiClient.Connection>() {
                @Override public void success(RoomApiClient.Connection c) { inFlight = false; connection = c; betaKey.setText(""); showLoadout(); }
                @Override public void failure(Throwable error) { inFlight = false; showError(error.getMessage()); }
            };
            if (join) api.join(roomCode.getText().toString(), nickname.getText().toString(), result);
            else api.create(nickname.getText().toString(), mode, result);
        } catch (IllegalArgumentException e) { inFlight = false; showError(e.getMessage()); }
    }

    private void showLoadout() {
        resetContent(); content.addView(text("방 코드: " + connection.roomCode, 24));
        content.addView(text("각 캐릭터의 기본 공격·방어를 장착합니다.\n전체 스킬·룬 선택 화면 연결은 후속 작업입니다.", 15));
        List<RoomApiClient.CharacterLoadout> loadouts = new ArrayList<>();
        for (Integer slot : connection.playerSlots) {
            RoomApiClient.CharacterLoadout loadout = new RoomApiClient.CharacterLoadout(slot, "HERO"); loadouts.add(loadout);
            Button choose = button("P" + slot + ": 용사후보");
            choose.setOnClickListener(v -> {
                String[] labels = new String[characters.size()]; int[] icons = new int[characters.size()];
                for (int i = 0; i < labels.length; i++) { labels[i] = characters.get(i).getName(); icons[i] = characters.get(i).getPortraitResource(); }
                GameModal.list(this, "캐릭터 선택", labels, icons, index -> {
                    loadout.characterId = characters.get(index).getId(); choose.setText("P" + slot + ": " + labels[index]);
                }, null);
            });
        }
        Button ready = button("구성 저장 후 준비 완료");
        roomInfo = text("준비 완료 후 상대를 기다립니다.", 16); content.addView(roomInfo);
        notice = text("", 15); content.addView(notice);
        ready.setOnClickListener(v -> {
            if (inFlight || waiting) return; inFlight = true; ready.setEnabled(false);
            api.configure(connection, loadouts, new RoomApiClient.Result<RoomApiClient.RoomStatus>() {
                @Override public void success(RoomApiClient.RoomStatus status) {
                    api.ready(connection, new RoomApiClient.Result<RoomApiClient.RoomStatus>() {
                        @Override public void success(RoomApiClient.RoomStatus state) { inFlight = false; waiting = true; roomStatus(state); }
                        @Override public void failure(Throwable error) { inFlight = false; ready.setEnabled(true); showError(error.getMessage()); }
                    });
                }
                @Override public void failure(Throwable error) { inFlight = false; ready.setEnabled(true); showError(error.getMessage()); }
            });
        });
    }
    private void pollStatus() {
        if (disposed || !waiting || transport != null) return;
        api.status(connection, new RoomApiClient.Result<RoomApiClient.RoomStatus>() {
            @Override public void success(RoomApiClient.RoomStatus status) { roomStatus(status); }
            @Override public void failure(Throwable error) {
                showError(error.getMessage());
                if (error instanceof RoomApiClient.ApiException) { waiting = false; return; }
                main.postDelayed(poll, 3000);
            }
        });
    }
    private void roomStatus(RoomApiClient.RoomStatus status) {
        if (disposed) return;
        StringBuilder text = new StringBuilder();
        if (status.participants != null) for (RoomApiClient.Participant player : status.participants) {
            text.append(player.nickname).append(player.ready ? " · 준비 완료\n" : " · 구성 중\n");
        }
        roomInfo.setText(text.toString()); main.removeCallbacks(poll);
        if (status.started) { waiting = false; startBattle(); }
        else main.postDelayed(poll, 1500);
    }

    private void startBattle() {
        if (transport != null) return;
        transport = new WebSocketBattleTransport(endpoint, connection, http, main::post, (state, message) -> {
            if (disposed) return; notice.setText(message);
            session.connected(state == WebSocketBattleTransport.State.CONNECTED);
        });
        session = new OnlineBattleSession(connection, transport, new OnlineBattleSession.Listener() {
            @Override public void updated(RemoteBattleSnapshot snapshot) { render(snapshot); }
            @Override public void failed(String message) { showError(message); }
            @Override public void busyChanged(boolean busy) { updateButtons(); }
        });
        transport.connect();
    }
    private void showBattle() {
        battleShown = true; setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); resetContent();
        content.addView(text("온라인 전투 연결 테스트 · 방 " + connection.roomCode, 20));
        notice = text("서버 상태로 전투를 표시합니다.", 14); content.addView(notice);
        LinearLayout boardRow = new LinearLayout(this); content.addView(boardRow);
        battleInfo = text("", 14); boardRow.addView(battleInfo, new LinearLayout.LayoutParams(0, -2, .32f));
        arena = column(); boardRow.addView(arena, new LinearLayout.LayoutParams(0, -2, .68f));
        commandBar = new LinearLayout(this); content.addView(commandBar);
        LinearLayout utilities = new LinearLayout(this); content.addView(utilities);
        Button reconnect = rowButton(utilities, "상태 다시 연결"); reconnect.setOnClickListener(v -> transport.requestSnapshot(connection.roomCode, connection.clientId,
                new com.pas.game.multiplayer.MultiplayerTransport.Callback() {
                    @Override public void onResponse(String json) {
                        // Keep the session listener as the single snapshot update path.
                        // Reconnect auto-SYNC is routed to this explicit callback instead of PUSH.
                        session.acceptSnapshot(json);
                    }
                    @Override public void onFailure(Throwable error) { showError(error.getMessage()); }
                }));
        rowButton(utilities, "상세 로그").setOnClickListener(v -> {
            TextView logs = text("", 14); logs.setTextIsSelectable(true);
            StringBuilder lines = new StringBuilder();
            if (session.getSnapshot() != null) for (String log : session.getSnapshot().logs) lines.append(log).append('\n');
            logs.setText(lines.toString()); ScrollView scroll = new ScrollView(this); scroll.addView(logs);
            GameModal.custom(this, "전투 로그", scroll);
        });
    }
    private void render(RemoteBattleSnapshot snapshot) {
        if (!battleShown) showBattle();
        RemoteBattleSnapshot.Unit actor = snapshot.find(snapshot.turn.activeUnitId);
        StringBuilder info = new StringBuilder("라운드 ").append(snapshot.round).append(" · ")
                .append(actor == null ? "종료" : actor.name + "의 턴").append(" · 상태 #").append(snapshot.revision);
        for (RemoteBattleSnapshot.Unit unit : snapshot.units) info.append("\n").append(unit.name).append("  HP ").append(unit.hp).append('/').append(unit.maxHp);
        battleInfo.setText(info.toString());
        arena.removeAllViews();
        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(this); arena.addView(line);
            for (int col = 0; col < 4; col++) {
                int tile = row * 4 + col + 1; StringBuilder label = new StringBuilder();
                for (RemoteBattleSnapshot.Unit u : snapshot.units) if (u.tile == tile && u.onField && !u.dead) label.append(u.name).append('\n');
                Button cell = new Button(this); cell.setText(label.length() == 0 ? "·" : label.toString().trim());
                cell.setTextColor(Color.WHITE); cell.setTextSize(12); cell.setAllCaps(false);
                cell.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tile == selectedTile ? 0xff957442 : 0xff325367));
                cell.setContentDescription("전장 " + tile + "번 칸 " + label);
                line.addView(cell, new LinearLayout.LayoutParams(0, dp(48), 1));
                cell.setOnClickListener(v -> { selectedTile = tile; selectedTarget = null;
                    List<RemoteBattleSnapshot.Unit> targets = new ArrayList<>();
                    for (RemoteBattleSnapshot.Unit u : snapshot.units) if (u.tile == tile && u.onField && !u.dead) targets.add(u);
                    if (targets.size() == 1) selectedTarget = targets.get(0).unitId;
                    else if (targets.size() > 1) {
                        String[] labels = new String[targets.size()]; for (int i = 0; i < labels.length; i++) labels[i] = targets.get(i).name;
                        GameModal.list(this, "대상 선택", labels, null, index -> selectedTarget = targets.get(index).unitId, null);
                    }
                    render(session.getSnapshot());
                });
            }
        }
        actions.clear(); commandBar.removeAllViews();
        if (actor != null && actor.skills != null) for (RemoteBattleSnapshot.Skill skill : actor.skills) {
            if (skill == null) continue;
            SkillData data = findSkill(skill.id); if (data == null) continue;
            Button use = action(data.getName() + (skill.cooldownRemaining > 0 ? " · " + skill.cooldownRemaining + "턴" : ""));
            use.setOnClickListener(v -> {
                String target = data.getTargetType() == TargetType.SELF ? actor.unitId : selectedTarget;
                if (data.getTargetType() == TargetType.ENEMY && target == null) { showError("적이 있는 칸을 먼저 선택하세요."); return; }
                session.submit(new UseSkillCommand(actor.unitId, skill.id, target, selectedTile > 0 ? selectedTile : null, null));
            });
        }
        action("선택 칸으로 이동").setOnClickListener(v -> {
            if (actor != null && selectedTile > 0) session.submit(new MoveCommand(actor.unitId, selectedTile, MovementType.VOLUNTARY));
            else showError("이동할 칸을 선택하세요.");
        });
        action("턴 종료").setOnClickListener(v -> { if (actor != null) session.submit(new EndTurnCommand(actor.unitId)); });
        updateButtons();
    }
    private SkillData findSkill(String id) {
        for (CharacterData c : characters) { SkillRepository repo = registry.find(c.getId()); SkillData data = repo.find(id); if (data != null) return data; }
        return null;
    }
    private void updateButtons() {
        if (session == null) return; RemoteBattleSnapshot state = session.getSnapshot();
        boolean enabled = !session.isBusy() && state != null && "ONGOING".equals(state.outcome)
                && session.owns(state.find(state.turn.activeUnitId));
        for (Button button : actions) button.setEnabled(enabled);
    }
    private Button action(String label) {
        Button b = rowButton(commandBar, label); actions.add(b); return b;
    }
    private Button rowButton(LinearLayout row, String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff3d6d86));
        row.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1)); return b;
    }
    private void showError(String message) { if (!disposed && notice != null) notice.setText(message == null ? "연결 상태를 확인하세요." : message); }
    private void resetContent() {
        content = column(); content.setPadding(dp(16), dp(12), dp(16), dp(12));
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xff14212a); scroll.setFillViewport(true); scroll.addView(content);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> { Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()); v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets; });
        setContentView(scroll); ViewCompat.requestApplyInsets(scroll);
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private TextView text(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.WHITE); v.setPadding(dp(4), dp(6), dp(4), dp(6)); return v; }
    private EditText input(String hint, int type) {
        EditText v = new EditText(this); v.setHint(hint); v.setInputType(type); v.setSingleLine(true); v.setTextColor(Color.WHITE); v.setHintTextColor(0xffaebdc9); v.setTextSize(14);
        content.addView(v, new LinearLayout.LayoutParams(-1, dp(56))); return v;
    }
    private Button button(String label) {
        Button v = new Button(this); v.setText(label); v.setAllCaps(false); v.setTextColor(Color.WHITE); v.setTextSize(15); v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff3d6d86));
        content.addView(v, new LinearLayout.LayoutParams(-1, dp(56))); return v;
    }
    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }
    @Override protected void onDestroy() {
        disposed = true; main.removeCallbacksAndMessages(null);
        if (transport != null) transport.close(); if (api != null) api.close();
        http.dispatcher().cancelAll(); http.connectionPool().evictAll(); http.dispatcher().executorService().shutdown();
        super.onDestroy();
    }
}
