package com.kyusa.gameserver.game;

import org.springframework.web.socket.WebSocketSession;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    public enum State { LOBBY, PLAYING, ENDED }

    private final String roomCode;
    private final String name;
    private final String password;

    private final Map<String, WebSocketSession> players = new ConcurrentHashMap<>();
    private final List<String> joinOrder = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> roles = new ConcurrentHashMap<>();
    private final Map<String, int[]> positions = new ConcurrentHashMap<>();

    private volatile String ownerId;
    private volatile State state = State.LOBBY;

    public Room(String roomCode, String name, String password) {
        this.roomCode = roomCode;
        this.name = name;
        this.password = password;
    }

    public String code() { return roomCode; }
    public String name() { return name; }

    public Map<String, WebSocketSession> players() { return players; }

    public synchronized boolean join(String playerId, WebSocketSession s) {
        if (state != State.LOBBY) return false;
        if (players.size() >= 4) return false;

        players.put(playerId, s);
        joinOrder.add(playerId);

        if (ownerId == null) ownerId = playerId;

        positions.put(playerId, new int[]{0,0});
        return true;
    }

    public synchronized void leave(String playerId) {
        players.remove(playerId);
        joinOrder.remove(playerId);
        roles.remove(playerId);
        positions.remove(playerId);

        if (players.isEmpty()) {
            state = State.ENDED;
            return;
        }

        if (Objects.equals(ownerId, playerId)) {
            ownerId = joinOrder.isEmpty() ? null : joinOrder.get(0);
        }
    }

    public synchronized boolean start() {
        if (state != State.LOBBY) return false;
        if (players.size() < 2) return false;

        List<String> ids = new ArrayList<>(players.keySet());
        Collections.shuffle(ids, new SecureRandom());

        String chaser = ids.get(0);

        for (String id : ids) {
            roles.put(id, id.equals(chaser) ? "chaser" : "runner");
        }

        int[][] spawn = {{0,0},{9,9},{0,9},{9,0}};
        for (int i = 0; i < ids.size() && i < spawn.length; i++) {
            positions.put(ids.get(i), new int[]{spawn[i][0], spawn[i][1]});
        }

        state = State.PLAYING;
        return true;
    }

    public synchronized Map<String,Object> move(String playerId, int dx, int dy) {
        if (state != State.PLAYING) return Map.of("ok",false,"reason","NOT_PLAYING");

        int[] pos = positions.get(playerId);
        if (pos == null) return Map.of("ok",false,"reason","NOT_IN_ROOM");

        int nx = Math.max(0, Math.min(9, pos[0] + dx));
        int ny = Math.max(0, Math.min(9, pos[1] + dy));

        positions.put(playerId, new int[]{nx, ny});

        return Map.of("ok",true,"x",nx,"y",ny);
    }

    public Map<String,Object> snapshot() {
        Map<String,Object> m = new HashMap<>();
        m.put("state", state.name());
        m.put("ownerId", ownerId);
        m.put("players", new ArrayList<>(players.keySet()));
        m.put("roles", new HashMap<>(roles));

        Map<String,Object> pos = new HashMap<>();
        positions.forEach((k,v) -> pos.put(k, Map.of("x",v[0], "y",v[1])));

        m.put("positions", pos);

        return m;
    }

    public boolean hasPlayer(String playerId) {
        return players.containsKey(playerId);
    }

    public boolean checkPassword(String pw) {
        return Objects.equals(password, pw);
    }

}
