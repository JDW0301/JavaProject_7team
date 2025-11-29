package io.github.freeze.net;

import com.badlogic.gdx.Gdx;
import com.google.gson.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Queue;

public final class Net {
    public interface Listener {
        default void onOpen() {}
        default void onClose(int code, String reason, boolean remote) {}
        default void onError(Throwable t) {}
        default void onCreateRoomOk(String roomId) {}
        default void onJoinOk(String roomId) {}
        default void onServerError(String errCode, String message) {}
        
        // 게임 관련
        default void onGameStart(String roleJson) {}
        default void onPlayerMove(String playerId, float x, float y) {}
        default void onPlayerFreeze(String targetId, String attackerId) {}
        default void onPlayerUnfreeze(String targetId, String unfreezeId) {}
        default void onSkillUsed(String playerId, String skillType) {}
        default void onFogActivated(String playerId) {}
        default void onPlayerReady(String playerId, boolean isReady) {}  // ★ Ready 추가
    }

    private static final Net I = new Net();
    public static Net get() { return I; }
    private final Gson gson = new Gson();
    
    private final Queue<Object> sendQueue = new ArrayDeque<>();
    private WebSocketClient ws;
    private Listener listener;
    
    private volatile String lastRoomId;
    private volatile String lastErrCode;
    private volatile String lastErrMsg;
    
    private Net() {}
    
    public synchronized void resetLastResult() { 
        lastRoomId=null; 
        lastErrCode=null; 
        lastErrMsg=null; 
    }
    
    public String getLastRoomId(){ return lastRoomId; }
    public String getLastErrCode(){ return lastErrCode; }
    public String getLastErrMsg(){ return lastErrMsg; }
    
    public synchronized void setListener(Listener l) { this.listener = l; }

    public synchronized void connect(String wsUrl) throws Exception {
        if (ws != null && ws.isOpen()) return;
        ws = new WebSocketClient(new URI(wsUrl), new Draft_6455()) {
            @Override 
            public void onOpen(ServerHandshake h) {
                Gdx.app.postRunnable(() -> {
                    Gdx.app.log("WS", "Connected to server!");
                    if (listener != null) listener.onOpen();
                    
                    // 대기 중이던 메시지 전송
                    synchronized (Net.this) {
                        while (!sendQueue.isEmpty()) {
                            ws.send(gson.toJson(sendQueue.poll()));
                        }
                    }
                });
            }
            
            @Override 
            public void onMessage(String msg) {
                Gdx.app.log("WS", "Received: " + msg);
                Gdx.app.postRunnable(() -> handleMessage(msg));
            }
            
            @Override 
            public void onClose(int code, String reason, boolean remote) {
                Gdx.app.log("WS", "Disconnected: " + reason);
                Gdx.app.postRunnable(() -> { 
                    if (listener != null) listener.onClose(code, reason, remote); 
                });
            }
            
            @Override 
            public void onError(Exception ex) {
                Gdx.app.log("WS", "Error: " + ex.getMessage());
                Gdx.app.postRunnable(() -> { 
                    if (listener != null) listener.onError(ex); 
                });
            }
        };
        ws.connect();
    }

    public synchronized boolean isOpen() { 
        return ws != null && ws.isOpen(); 
    }

    public synchronized void close() {
        if (ws != null) ws.close();
    }

    // ====== API (서버 문서 포맷에 맞춤) ======
    
    // 방 생성
    public void sendCreateRoom(String code, String title, String password) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("title", title);
        payload.put("password", password);

        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "createRoom");  // ★ "op" → "type"
        msg.put("payload", payload);     // ★ "data" → "payload"
        sendJson(msg);
    }

    // 방 입장
    public void sendJoinRoom(String roomId, String playerId) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("playerId", playerId);

        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "join");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // Ready 상태 전송
    public void sendReady(String playerId, boolean isReady) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("playerId", playerId);
        payload.put("isReady", isReady);

        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "ready");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // 게임 시작
    public void sendGameStart(String roomId) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("roomId", roomId);

        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "start");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // 플레이어 이동 (★ dx, dy로 변경!)
    public void sendPlayerMove(String playerId, float dx, float dy) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("playerId", playerId);
        payload.put("dx", dx);  // ★ x → dx
        payload.put("dy", dy);  // ★ y → dy
        
        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "move");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // 나가기
    public void sendLeave(String roomId, String playerId) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("playerId", playerId);
        
        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "leave");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // 얼리기
    public void sendFreeze(String targetId) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("targetId", targetId);
        
        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "freeze");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // 녹이기
    public void sendUnfreeze(String targetId) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("targetId", targetId);
        
        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "unfreeze");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // 스킬 사용
    public void sendSkillUse(String skillType) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("skillType", skillType);
        
        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "skillUse");
        msg.put("payload", payload);
        sendJson(msg);
    }
    
    // Ready
    public void sendGameReady() {
        Map<String,Object> msg = new HashMap<>();
        msg.put("type", "gameReady");
        sendJson(msg);
    }

    // ====== 내부 함수 ======
    private synchronized void sendJson(Object obj) {
        if (ws != null && ws.isOpen()) {
            String json = gson.toJson(obj);
            Gdx.app.log("WS", "Sent: " + json);
            ws.send(json);
        } else {
            sendQueue.add(obj); // 연결될 때까지 대기
        }
    }

    private void handleMessage(String msg) {
        try {
            JsonObject jo = JsonParser.parseString(msg).getAsJsonObject();
            
            // ★ "op" → "type"으로 변경
            String type = jo.has("type") ? jo.get("type").getAsString() : "";
            
            switch (type) {
                case "roomCreated": {
                    // data 객체에서 정보 추출
                    if (jo.has("data")) {
                        JsonObject data = jo.getAsJsonObject("data");
                        String code = data.has("code") ? data.get("code").getAsString() : "";
                        String name = data.has("name") ? data.get("name").getAsString() : "";
                        
                        lastRoomId = code;
                        lastErrCode = null;
                        lastErrMsg = null;
                        
                        Gdx.app.log("WS", "Room created: " + code);
                        if (listener != null) listener.onCreateRoomOk(code);
                    }
                    break;
                }
                
                case "playerJoined": {
                    // snapshot에서 roomId 추출
                    String roomId = jo.has("roomId") ? jo.get("roomId").getAsString() : "";
                    Gdx.app.log("WS", "Joined room: " + roomId);
                    if (listener != null) listener.onJoinOk(roomId);
                    break;
                }
                
                case "gameStarted": {
                    // snapshot에서 role 정보 추출
                    if (jo.has("snapshot")) {
                        JsonObject snapshot = jo.getAsJsonObject("snapshot");
                        String roleJson = snapshot.toString();
                        Gdx.app.log("WS", "Game started!");
                        if (listener != null) listener.onGameStart(roleJson);
                    }
                    break;
                }
                
                case "playerMoved": {
                    String playerId = jo.has("playerId") ? jo.get("playerId").getAsString() : "";
                    float x = jo.has("x") ? jo.get("x").getAsFloat() : 0f;
                    float y = jo.has("y") ? jo.get("y").getAsFloat() : 0f;
                    
                    if (listener != null) listener.onPlayerMove(playerId, x, y);
                    break;
                }
                
                case "playerLeft": {
                    Gdx.app.log("WS", "Player left");
                    break;
                }
                
                case "playerReady": {
                    String playerId = jo.has("playerId") ? jo.get("playerId").getAsString() : "";
                    boolean isReady = jo.has("isReady") ? jo.get("isReady").getAsBoolean() : false;
                    
                    Gdx.app.log("WS", "Player ready: " + playerId + " = " + isReady);
                    if (listener != null) listener.onPlayerReady(playerId, isReady);
                    break;
                }
                
                case "freeze": {
                    String targetId = jo.has("targetId") ? jo.get("targetId").getAsString() : "";
                    String attackerId = jo.has("attackerId") ? jo.get("attackerId").getAsString() : "";
                    if (listener != null) listener.onPlayerFreeze(targetId, attackerId);
                    break;
                }
                
                case "unfreeze": {
                    String targetId = jo.has("targetId") ? jo.get("targetId").getAsString() : "";
                    String unfreezeId = jo.has("unfreezeId") ? jo.get("unfreezeId").getAsString() : "";
                    if (listener != null) listener.onPlayerUnfreeze(targetId, unfreezeId);
                    break;
                }
                
                case "skillUse": {
                    String playerId = jo.has("playerId") ? jo.get("playerId").getAsString() : "";
                    String skillType = jo.has("skillType") ? jo.get("skillType").getAsString() : "";
                    if (listener != null) listener.onSkillUsed(playerId, skillType);
                    break;
                }
                
                case "fogActivated": {
                    String playerId = jo.has("playerId") ? jo.get("playerId").getAsString() : "";
                    if (listener != null) listener.onFogActivated(playerId);
                    break;
                }
                
                case "error": {
                    String code = jo.has("code") ? jo.get("code").getAsString() : "UNKNOWN";
                    String message = jo.has("message") ? jo.get("message").getAsString() : "";
                    
                    lastRoomId = null;
                    lastErrCode = code;
                    lastErrMsg = message;
                    
                    Gdx.app.error("WS", "Server error: " + code + " - " + message);
                    if (listener != null) listener.onServerError(code, message);
                    break;
                }
            }
        } catch (Exception e) {
            Gdx.app.error("WS", "Failed to parse message", e);
        }
    }
}
