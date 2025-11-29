package io.github.freeze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.freeze.net.Net;
import io.github.freeze.screens.FirstScreen;

public class Core extends Game {
    public static final int V_WIDTH  = 1280;
    public static final int V_HEIGHT = 960;

    public SpriteBatch batch;

    @Override 
    public void create() {
        batch = new SpriteBatch();
        try {
            // ★ 콜론(:)을 점(.)으로 수정!
            Net.get().connect("ws://203.234.62.47:8080/ws");
            Gdx.app.log("CORE", "Connecting to server...");
        } catch (Exception e) {
            Gdx.app.error("NET", "connect failed", e);
        }
        setScreen(new FirstScreen(this));
    }

    @Override 
    public void dispose() {
        if (batch != null) batch.dispose();
        Net.get().close();  // ★ 서버 연결 종료
        super.dispose();
    }
    
    @Override
    public void render() {
        super.render();
    }
}
