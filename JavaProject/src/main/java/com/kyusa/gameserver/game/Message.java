package com.kyusa.gameserver.game;

import java.util.Map;

public class Message {
    public String type;
    public Map<String, Object> payload;

    public Message() {}

    public Message(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
    }
}
