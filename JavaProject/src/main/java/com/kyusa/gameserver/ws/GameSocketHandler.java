package com.kyusa.gameserver.ws;

import com.kyusa.gameserver.game.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSocketHandler extends TextWebSocketHandler {

    private final RoomRepo repo;

    public GameSocketHandler(RoomRepo repo) {
        this.repo = repo;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("[WS] 연결됨 sessionId=" + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        System.out.println("==================================================");
        System.out.println("[WS] 수신 RAW JSON = " + message.getPayload());
        System.out.println("==================================================");

        Message m;
        try {
            m = Jsons.fromJson(message.getPayload(), Message.class);
        } catch (Exception e) {
            send(session, Map.of("type", "error", "payload", Map.of("code", "BAD_JSON")));
            return;
        }

        String type = m.type == null ? "" : m.type;
        Map<String, Object> p = m.payload == null ? Map.of() : m.payload;

        System.out.println("[WS] type=" + type);
        System.out.println("[WS] payload=" + p);

        switch (type) {

            // =====================================================
            // 1) createRoom
            // =====================================================
            case "createRoom" -> {
                String code = (String)p.get("code");
                String title = (String)p.getOrDefault("title", "room");
                String pw = (String)p.getOrDefault("password", "");

                try {
                    Room r = repo.create(code, title, pw);
                    send(session, Map.of(
                            "type", "roomCreated",
                            "data", Map.of("code", r.code(), "title", r.name())
                    ));
                } catch (Exception e) {
                    send(session, Map.of("type", "error",
                            "payload", Map.of("code", "ROOM_EXISTS")));
                }
            }

            // =====================================================
            // 2) join
            // =====================================================
            case "join" -> {
                String code = (String)p.get("roomId");
                String playerId = (String)p.get("playerId");

                Room room = repo.get(code);
                if (room == null) {
                    send(session, Map.of("type","error",
                            "payload", Map.of("code","ROOM_NOT_FOUND")));
                    return;
                }

                if (!room.join(playerId, session)) {
                    send(session, Map.of("type","error",
                            "payload", Map.of("code","JOIN_FAILED")));
                    return;
                }

                broadcast(room, Map.of(
                        "type", "playerJoined",
                        "roomId", code,
                        "snapshot", room.snapshot()
                ));
            }

            // =====================================================
            // 3) start
            // =====================================================
            case "start" -> {
                String code = (String)p.get("roomId");
                Room room = repo.get(code);
                if (room == null) return;

                if (!room.start()) {
                    send(session, Map.of("type","error",
                            "payload", Map.of("code","START_FAILED")));
                    return;
                }

                broadcast(room, Map.of(
                        "type", "gameStarted",
                        "roomId", code,
                        "snapshot", room.snapshot()
                ));
            }

            // =====================================================
            // 4) move (dx/dy)
            // =====================================================
            case "move" -> {
                String playerId = (String)p.get("playerId");
                Number dx = (Number)p.getOrDefault("dx", 0);
                Number dy = (Number)p.getOrDefault("dy", 0);

                Room room = repo.findRoomByPlayer(playerId);
                if (room == null) return;

                Map<String,Object> result = room.move(
                        playerId,
                        dx.intValue(),
                        dy.intValue()
                );

                if (!(Boolean)result.get("ok")) {
                    send(session, Map.of(
                            "type","error",
                            "payload", Map.of("code", result.get("reason"))
                    ));
                    return;
                }

                broadcast(room, Map.of(
                        "type", "playerMoved",
                        "playerId", playerId,
                        "x", result.get("x"),
                        "y", result.get("y")
                ));
            }

            // =====================================================
            // 5) leave
            // =====================================================
            case "leave" -> {
                String code = (String)p.get("roomId");
                String playerId = (String)p.get("playerId");

                Room room = repo.get(code);
                if (room == null) return;

                room.leave(playerId);

                broadcast(room, Map.of(
                        "type", "playerLeft",
                        "playerId", playerId,
                        "snapshot", room.snapshot()
                ));
            }

            default -> {
                send(session, Map.of("type","error",
                        "payload", Map.of("code","UNKNOWN_TYPE")));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
        System.out.println("[WS] 연결 종료 sessionId=" + s.getId());
        // 플레이어 ID 추적 로직이 별도로 있다면 여기에서 leave 처리 가능
    }

    private void send(WebSocketSession s, Object o) {
        try {
            s.sendMessage(new TextMessage(Jsons.toJson(o)));
        } catch (Exception ignore) {}
    }

    private void broadcast(Room room, Object msg) {
        String json = Jsons.toJson(msg);
        for (WebSocketSession s : room.players().values()) {
            try {
                s.sendMessage(new TextMessage(json));
            } catch (Exception ignore) {}
        }
    }
}
