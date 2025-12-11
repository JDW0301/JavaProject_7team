package com.kyusa.gameserver.ws;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** WebSocketSession.id -> [roomCode, playerId] 역색인 */
@Component
public class SessionIndex {
    private final ConcurrentHashMap<String, String[]> map = new ConcurrentHashMap<>();

    public void put(String sessionId, String roomCode, String playerId) {
        map.put(sessionId, new String[]{roomCode, playerId});
    }

    public String[] get(String sessionId) {
        return map.get(sessionId);
    }

    public String[] remove(String sessionId) {
        return map.remove(sessionId);
    }
}
