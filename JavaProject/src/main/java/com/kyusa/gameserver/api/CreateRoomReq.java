package com.kyusa.gameserver.api;

public class CreateRoomReq {
    public String op;
    public Data data;

    public static class Data {
        public String title;
        public String code;
        public String password;
        public String nick;
    }
}
