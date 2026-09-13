package com.pas.game.network;

import okhttp3.HttpUrl;

/** A single trusted origin for REST and WebSocket, never a server-supplied redirect. */
public final class ServerEndpoint {
    private final HttpUrl origin;

    public ServerEndpoint(String address, boolean allowLocalHttp) {
        HttpUrl parsed = HttpUrl.parse(address == null ? "" : address.trim());
        if (parsed == null || !parsed.username().isEmpty() || !parsed.password().isEmpty()
                || parsed.query() != null || parsed.fragment() != null
                || !"/".equals(parsed.encodedPath())) {
            throw new IllegalArgumentException("서버 주소는 https://호스트:포트 형태로 입력하세요.");
        }
        if (!parsed.isHttps() && !(allowLocalHttp && isLocalHost(parsed.host()))) {
            throw new IllegalArgumentException("HTTPS가 필요합니다. 디버그에서만 로컬 HTTP를 허용합니다.");
        }
        origin = parsed;
    }

    private static boolean isLocalHost(String host) {
        if ("localhost".equals(host) || "::1".equals(host)) return true;
        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        int[] octets = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] < 0 || octets[i] > 255) return false;
            }
        } catch (NumberFormatException e) { return false; }
        return octets[0] == 127 || octets[0] == 10
                || (octets[0] == 192 && octets[1] == 168)
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31);
    }

    public HttpUrl rooms(String... segments) {
        HttpUrl.Builder url = origin.newBuilder().addPathSegments("api/v1/rooms");
        for (String segment : segments) url.addPathSegment(segment);
        return url.build();
    }

    public HttpUrl battle(String roomCode) {
        return origin.newBuilder().addPathSegments("ws/battle")
                .addQueryParameter("roomCode", roomCode).build();
    }
    public HttpUrl redeem() { return origin.newBuilder().addPathSegments("api/v1/auth/redeem").build(); }
}
