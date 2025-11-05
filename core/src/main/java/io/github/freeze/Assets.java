package io.github.freeze;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public final class Assets {
    private Assets() {}

    // 파일 경로 상수 (assets/ 기준)
    public static final String BG_HOME   = "bg/newhome.png";
    public static final String UI_LOGO   = "ui/Newlogo.png";
    public static final String UI_ROOM1  = "ui/room1.png";
    public static final String UI_ROOM2  = "ui/room2.png";
    public static final String UI_OPTION = "ui/option.png";
    public static final String UI_EXIT   = "ui/out.png";

    public static void queue(AssetManager m) {
        m.load(BG_HOME,   Texture.class);
        m.load(UI_LOGO,   Texture.class);
        m.load(UI_ROOM1,  Texture.class);
        m.load(UI_ROOM2,  Texture.class);
        m.load(UI_OPTION, Texture.class);
        m.load(UI_EXIT,   Texture.class);
    }
}
