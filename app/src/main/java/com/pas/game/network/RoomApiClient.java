package com.pas.game.network;

import com.google.gson.Gson;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Async lobby API. Tokens remain in memory and are never put in URLs or logs. */
public final class RoomApiClient implements Closeable {
    public enum Mode { SOLO_ONE, SOLO_PARTY, COOP }
    public interface Result<T> { void success(T value); void failure(Throwable error); }
    public static final class Connection {
        public String roomCode, clientId, accessToken, webSocketPath;
        public Mode mode;
        public List<Integer> playerSlots;
        public void validate() {
            if (roomCode == null || clientId == null || accessToken == null || accessToken.isEmpty()
                    || mode == null || playerSlots == null || playerSlots.isEmpty()) {
                throw new IllegalArgumentException("서버 접속 정보가 올바르지 않습니다.");
            }
        }
    }
    public static final class Participant {
        public String clientId, nickname;
        public List<Integer> playerSlots;
        public boolean ready;
    }
    public static final class RoomStatus {
        public String roomCode, activeUnitId;
        public Mode mode;
        public List<Participant> participants;
        public boolean started;
        public long revision;
    }
    public static final class CharacterLoadout {
        public int playerSlot;
        public String characterId, primaryRuneId, secondaryRuneId;
        public List<String> skillIds;
        public List<Integer> upgrades;
        public Integer primaryRuneLevel, secondaryRuneLevel;
        public CharacterLoadout(int playerSlot, String characterId) {
            this.playerSlot = playerSlot;
            this.characterId = characterId;
        }
    }
    public static final class ApiException extends IOException {
        public final int statusCode;
        ApiException(int statusCode) {
            super(statusCode == 401 || statusCode == 403 ? "접속 키 또는 방 접속 정보를 확인하세요."
                    : statusCode == 404 ? "방을 찾을 수 없습니다."
                    : statusCode == 409 ? "방이 가득 찼거나 이미 시작됐습니다."
                    : "서버 요청 실패 (HTTP " + statusCode + ")");
            this.statusCode = statusCode;
        }
        ApiException(int statusCode,String message) { super(message);this.statusCode=statusCode; }
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final Gson gson = new Gson();
    private final ServerEndpoint endpoint;
    private final OkHttpClient http;
    private final Executor callbacks;
    private final String betaKey;
    private String playerToken;
    private volatile boolean closed;

    public RoomApiClient(ServerEndpoint endpoint, String betaKey, OkHttpClient http, Executor callbacks) {
        this.endpoint = endpoint;
        this.betaKey = betaKey == null ? "" : betaKey.trim();
        // Never redirect secret headers to another origin or replay a room creation after I/O failure.
        this.http = http.newBuilder().followRedirects(false).followSslRedirects(false)
                .retryOnConnectionFailure(false).build();
        this.callbacks = callbacks;
    }

    public void create(String nickname, Mode mode, Result<Connection> result) {
        if (mode == null) throw new IllegalArgumentException("모드를 선택하세요.");
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("nickname", nickname(nickname)); body.addProperty("mode", mode.name());
        request(endpoint.rooms(), "POST", body, null, true, Connection.class, result);
    }
    public void setPlayerToken(String value) { playerToken = value; }
    public static final class Identity { public String playerId; }
    public static final class Left { public boolean left; }
    public static final class SkillOptions { public long revision; public List<String> ids, labels; public String description; }
    public void redeem(String code, String token, Result<Identity> result) {
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("inviteCode", code); body.addProperty("deviceToken", token);
        request(endpoint.redeem(), "POST", body, null, false, Identity.class, result);
    }
    public void leave(Connection c, Result<Left> result) {
        request(endpoint.rooms(c.roomCode,"leave"), "POST", null, c, false, Left.class, result);
    }
    public void options(Connection c, String actor, String skill, String target, Result<SkillOptions> result) {
        HttpUrl.Builder url = endpoint.rooms(c.roomCode,"skill-options").newBuilder()
                .addQueryParameter("actorId",actor).addQueryParameter("skillId",skill);
        if (target != null) url.addQueryParameter("targetId",target);
        request(url.build(),"GET",null,c,false,SkillOptions.class,result);
    }
    public void join(String roomCode, String nickname, Result<Connection> result) {
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("nickname", nickname(nickname));
        request(endpoint.rooms(code(roomCode), "join"), "POST", body, null, true, Connection.class, result);
    }
    public void configure(Connection c, List<CharacterLoadout> characters, Result<RoomStatus> result) {
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.add("characters", gson.toJsonTree(characters));
        request(endpoint.rooms(c.roomCode, "players", c.clientId, "loadout"), "PUT", body, c, false, RoomStatus.class, result);
    }
    /** 일반 캐릭터 선택 화면에서 저장한 설정을 서버가 배정한 슬롯으로 보낸다. */
    public void configureSelectedCharacter(Connection c, com.pas.game.multiplayer.PlayerBattleSetup selected, Result<RoomStatus> result) {
        c.validate();
        if (c.mode != Mode.COOP || c.playerSlots.size() != 1)
            throw new IllegalArgumentException("2인 협동의 본인 캐릭터 설정만 보낼 수 있습니다.");
        configure(c, java.util.Collections.singletonList(OnlineLoadoutMapper.from(selected, c.playerSlots.get(0))), result);
    }
    public void ready(Connection c, Result<RoomStatus> result) {
        request(endpoint.rooms(c.roomCode, "players", c.clientId, "ready"), "POST", null, c, false, RoomStatus.class, result);
    }
    public void status(Connection c, Result<RoomStatus> result) {
        request(endpoint.rooms(c.roomCode), "GET", null, c, false, RoomStatus.class, result);
    }

    /** Server support is required. No optimistic wallet/stat changes or local fallback. */
    public void shop(Connection c,Result<ShopProtocol.Snapshot> result){
        c.validate();request(endpoint.rooms(c.roomCode,"shop"),"GET",null,c,false,ShopProtocol.Snapshot.class,result);
    }
    public void purchase(Connection c,String requestId,long revision,String offerId,int playerSlot,String skillId,Result<ShopProtocol.Snapshot> result){
        c.validate();
        if(!c.playerSlots.contains(playerSlot))throw new IllegalArgumentException("본인 캐릭터에게만 구매할 수 있습니다.");
        if(offerId==null||offerId.trim().isEmpty())throw new IllegalArgumentException("상품을 선택하세요.");
        ShopProtocol.Mutation body=new ShopProtocol.Mutation(requestId,revision);
        body.offerId=offerId;body.playerSlot=playerSlot;body.skillId=skillId;
        request(endpoint.rooms(c.roomCode,"shop","purchase"),"POST",body,c,false,ShopProtocol.Snapshot.class,result);
    }
    public void refreshShop(Connection c,String requestId,long revision,Result<ShopProtocol.Snapshot> result){
        c.validate();request(endpoint.rooms(c.roomCode,"shop","refresh"),"POST",new ShopProtocol.Mutation(requestId,revision),c,false,ShopProtocol.Snapshot.class,result);
    }
    public void transferGold(Connection c,String requestId,long revision,String recipientClientId,int amount,Result<ShopProtocol.Snapshot> result){
        c.validate();
        if(c.mode!=Mode.COOP||recipientClientId==null||recipientClientId.trim().isEmpty()||recipientClientId.equals(c.clientId)||amount<=0)
            throw new IllegalArgumentException("같은 방의 다른 유저와 보낼 골드 수량을 확인하세요.");
        ShopProtocol.Mutation body=new ShopProtocol.Mutation(requestId,revision);body.recipientClientId=recipientClientId;body.amount=amount;
        request(endpoint.rooms(c.roomCode,"shop","transfer"),"POST",body,c,false,ShopProtocol.Snapshot.class,result);
    }
    private <T> void request(HttpUrl url, String method, Object body, Connection c,
                             boolean beta, Class<T> type, Result<T> result) {
        if (closed) { result.failure(new IOException("접속이 종료됐습니다.")); return; }
        Request.Builder request = new Request.Builder().url(url);
        if (beta && !betaKey.isEmpty()) request.header("X-PAS-BETA-KEY", betaKey);
        if (beta && playerToken != null && !playerToken.isEmpty()) request.header("X-PAS-PLAYER-TOKEN", playerToken);
        if (c != null) { c.validate(); request.header("X-PAS-CLIENT", c.clientId).header("X-PAS-TOKEN", c.accessToken); }
        request.method(method, "GET".equals(method) ? null : RequestBody.create(gson.toJson(body), JSON));
        http.newCall(request.build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(Call call, IOException e) { deliver(() -> result.failure(new IOException("서버에 연결하지 못했습니다. 주소와 네트워크를 확인하세요."))); }
            @Override public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    if (!r.isSuccessful()) {
                        String message=null;
                        try { if(r.body()!=null){com.google.gson.JsonObject error=gson.fromJson(r.body().string(),com.google.gson.JsonObject.class);if(error!=null&&error.has("message"))message=error.get("message").getAsString();} }catch(RuntimeException ignored){}
                        if(message!=null&&!message.isEmpty()&&message.length()<=300)throw new ApiException(r.code(),message);
                        throw new ApiException(r.code());
                    }
                    if (r.body() == null) throw new IOException("서버 응답이 비어 있습니다.");
                    T value = gson.fromJson(r.body().string(), type);
                    if (value == null) throw new IOException("서버 응답이 비어 있습니다.");
                    if (value instanceof Connection) ((Connection) value).validate();
                    if (value instanceof ShopProtocol.Snapshot) ShopProtocol.validate((ShopProtocol.Snapshot)value);
                    deliver(() -> result.success(value));
                } catch (Exception e) { deliver(() -> result.failure(e)); }
            }
        });
    }
    private void deliver(Runnable action) { callbacks.execute(() -> { if (!closed) action.run(); }); }
    private static String nickname(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty() || value.length() > 30) throw new IllegalArgumentException("이름은 1~30자로 입력하세요.");
        return value;
    }
    private static String code(String text) {
        String value = text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z2-9]{6}")) throw new IllegalArgumentException("6자리 방 코드를 입력하세요.");
        return value;
    }
    @Override public void close() { closed = true; }
}
