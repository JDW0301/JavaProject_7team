package com.kyusa.gameserver.api;

import com.kyusa.gameserver.game.Room;
import com.kyusa.gameserver.game.RoomRepo;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomsController {

    private final RoomRepo repo;

    public RoomsController(RoomRepo repo) {
        this.repo = repo;
    }

    /** 방 생성 (HTTP) */
    @PostMapping(consumes = "application/json", produces = "application/json; charset=UTF-8")
    public Map<String, Object> create(@RequestBody Map<String, Object> req) {

        System.out.println("==================================================");
        System.out.println("[HTTP] /api/rooms (CreateRoom) 요청 수신:");
        System.out.println(req);
        System.out.println("==================================================");

        // 클라이언트 JSON 구조: { op: "createRoom", data: {...} }
        Map<String, Object> data = (Map<String, Object>) req.get("data");

        String code = (String) data.get("code");
        String name = (String) data.getOrDefault("title", "room");
        String password = (String) data.getOrDefault("password", "");

        Room r = repo.create(code, name, password);

        return Map.of("ok", true, "code", r.code(), "name", r.name());
    }

    /** 방 입장 (HTTP) */
    @PostMapping("/check")
    public Map<String, Object> check(@RequestBody Map<String, Object> req) {

        System.out.println("==================================================");
        System.out.println("[HTTP] /api/rooms/check (EnterRoom) 요청 수신:");
        System.out.println(req);
        System.out.println("==================================================");

        Map<String, Object> data = (Map<String, Object>) req.get("data");

        String code = (String) data.get("code");
        String password = (String) data.getOrDefault("password", "");

        Room r = repo.get(code);
        if (r == null) return Map.of("ok", false, "reason", "NOT_FOUND");
        if (!r.checkPassword(password)) return Map.of("ok", false, "reason", "BAD_PASSWORD");

        return Map.of("ok", true, "name", r.name());
    }
}
