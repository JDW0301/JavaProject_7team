package com.kyusa.gameserver.api;

public class JoinRoomReq {
    public String op;
    public Data data;

    public static class Data {
        public String code;
        public String password;
        public String nick;
    }
}
