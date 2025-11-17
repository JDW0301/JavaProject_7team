package io.github.freeze.net;

import com.badlogic.gdx.Gdx;
import com.google.gson.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
//연결확인----------------------------------
import java.util.ArrayDeque;
import java.util.Queue;
//-----------------------------------------
public final class Net {
    public interface Listener {
        default void onOpen() {}
        default void onClose(int code, String reason, boolean remote) {}
        default void onError(Throwable t) {}
        default void onCreateRoomOk(String roomId) {}
        default void onJoinOk(String roomId) {}
        default void onServerError(String errCode, String message) {}
    }

    private static final Net I = new Net();
    public static Net get() { return I; }
    private final Gson gson = new Gson();
    //연결확인---------------------------------------------
    private final Queue<Object> sendQueue = new ArrayDeque<>();
    //연결확인---------------------------------------------
    private WebSocketClient ws;
    private Listener listener;
    //방 생성---------------------------------------
    private volatile String lastRoomId;
    private volatile String lastErrCode;
    private volatile String lastErrMsg;
    //---------------------------------------------
    private Net() {}
    //방 생성---------------------------------------
    public synchronized void resetLastResult() { lastRoomId=null; lastErrCode=null; lastErrMsg=null; }
    public String getLastRoomId(){ return lastRoomId; }
    public String getLastErrCode(){ return lastErrCode; }
    public String getLastErrMsg(){ return lastErrMsg; }
    //---------------------------------------------
    public synchronized void setListener(Listener l) { this.listener = l; }

    public synchronized void connect(String wsUrl) throws Exception {
        if (ws != null && ws.isOpen()) return;
        ws = new WebSocketClient(new URI(wsUrl), new Draft_6455()) {
            @Override public void onOpen(ServerHandshake h) {
                //Gdx.app.postRunnable(() -> { if (listener != null) listener.onOpen(); });
                Gdx.app.postRunnable(() -> {
                    // 기존 onOpen 콜백 호출
                    if (listener != null) listener.onOpen();
                    // 대기 중이던 메시지 전송
                    synchronized (Net.this) {
                        while (!sendQueue.isEmpty()) {
                            ws.send(gson.toJson(sendQueue.poll()));
                        }
                    }
                });
            }
            @Override public void onMessage(String msg) {
                Gdx.app.postRunnable(() -> handleMessage(msg));
            }
            @Override public void onClose(int code, String reason, boolean remote) {
                Gdx.app.postRunnable(() -> { if (listener != null) listener.onClose(code, reason, remote); });
            }
            @Override public void onError(Exception ex) {
                Gdx.app.postRunnable(() -> { if (listener != null) listener.onError(ex); });
            }
        };
        ws.connect(); // 비동기
    }

    public synchronized boolean isOpen() { return ws != null && ws.isOpen(); }

    public synchronized void close() {
        if (ws != null) ws.close();
    }

    // ====== API ======
    public void sendCreateRoom(String title, String code, String password, String nick) {
        Map<String,Object> data = new HashMap<>();
        data.put("title", title);
        data.put("code", code);
        data.put("password", password);
        data.put("nick", nick);

        Map<String,Object> env = new HashMap<>();
        env.put("op", "createRoom");
        env.put("data", data);
        sendJson(env);
    }

    // ====== 내부 ======
    private synchronized void sendJson(Object obj) {
//        if (ws == null || !ws.isOpen()) throw new IllegalStateException("WebSocket not connected");
//        ws.send(gson.toJson(obj));
        if (ws != null && ws.isOpen()) {
            ws.send(gson.toJson(obj));
        } else {
            sendQueue.add(obj); // 연결될 때까지 대기
        }
    }
    public void sendJoinRoom(String code, String password, String nick) {
        Map<String,Object> data = new HashMap<>();
        data.put("code", code);
        data.put("password", password);
        data.put("nick", nick);

        Map<String,Object> env = new HashMap<>();
        env.put("op", "joinRoom");
        env.put("data", data);
        sendJson(env);
    }

    private void handleMessage(String msg) {
        JsonObject jo = JsonParser.parseString(msg).getAsJsonObject();
        String op = jo.has("op") ? jo.get("op").getAsString() : "";
        switch (op) {
            case "roomCreated": {
                boolean ok = jo.has("ok") && jo.get("ok").getAsBoolean();
                if (ok) {
                    String roomId = jo.has("roomId") ? jo.get("roomId").getAsString() : "";
                    lastRoomId = roomId; lastErrCode=null; lastErrMsg=null;            // ← 추가
                    if (listener != null) listener.onCreateRoomOk(roomId);
                } else {
                    String code = jo.has("code") ? jo.get("code").getAsString() : "UNKNOWN";
                    String msg2 = jo.has("message") ? jo.get("message").getAsString() : "";
                    lastRoomId=null; lastErrCode=code; lastErrMsg=msg2;                // ← 추가
                    if (listener != null) listener.onServerError(code, msg2);
                }
                break;
            }
            case "roomJoined":  // 서버에 따라 둘 중 하나로 올 수 있음
            case "joined": {
                boolean ok = jo.has("ok") && jo.get("ok").getAsBoolean();
                if (ok) {
                    String roomId = jo.has("roomId") ? jo.get("roomId").getAsString() : "";
                    if (listener != null) listener.onJoinOk(roomId);
                } else {
                    String code = jo.has("code") ? jo.get("code").getAsString() : "UNKNOWN";
                    String msg2 = jo.has("message") ? jo.get("message").getAsString() : "";
                    if (listener != null) listener.onServerError(code, msg2);
                }
                break;
            }


            case "error": {
                String code = jo.has("code") ? jo.get("code").getAsString() : "UNKNOWN";
                String msg2 = jo.has("message") ? jo.get("message").getAsString() : "";
                lastRoomId=null; lastErrCode=code; lastErrMsg=msg2;                    // ← 추가
                if (listener != null) listener.onServerError(code, msg2);
                break;
            }

            // 필요 시 다른 op 추가: joined, kicked, chat, snapshot 등
        }
    }


}
