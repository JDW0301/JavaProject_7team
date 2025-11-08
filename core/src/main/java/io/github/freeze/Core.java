package io.github.freeze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.freeze.screens.FirstScreen;

public class Core extends Game {
    public static final int V_WIDTH  = 1280;  // ★ 4:3 가상 해상도
    public static final int V_HEIGHT = 960;

    public SpriteBatch batch;

    @Override public void create() {
        batch = new SpriteBatch();
        setScreen(new FirstScreen(this));
    }

    @Override public void dispose() {
        if (batch != null) batch.dispose();
        super.dispose();
    }
    @Override
    public void render() {
        super.render();   // 현재 setScreen(...)된 Screen들의 render()만 돌림
        // 여기서 batch.begin()/draw() 같은 추가 그리기 절대 금지
    }
}
