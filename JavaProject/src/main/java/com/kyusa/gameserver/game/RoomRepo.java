package com.kyusa.gameserver.game;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomRepo {

    private final ConcurrentHashMap<String, Room> byCode = new ConcurrentHashMap<>();

    public Room create(String code, String name, String pw) {
        code = code.toUpperCase();

        if (byCode.containsKey(code)) {
            throw new IllegalArgumentException("이미 존재하는 방 코드입니다.");
        }

        Room room = new Room(code, name, pw);
        byCode.put(code, room);
        return room;
    }

    public Room get(String code) {
        if (code == null) return null;
        return byCode.get(code.toUpperCase());
    }

    public void remove(String code) {
        if (code != null) byCode.remove(code.toUpperCase());
    }

    public Room findRoomByPlayer(String playerId) {
        return byCode.values().stream()
                .filter(r -> r.hasPlayer(playerId))
                .findFirst()
                .orElse(null);
    }
}
