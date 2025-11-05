package io.github.freeze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Core extends Game {
    private SpriteBatch batch;
    private AssetManager assets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new AssetManager();
        setScreen(new FirstScreen(this));   // ← FirstScreen이 로딩+메뉴를 모두 담당
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (batch != null) batch.dispose();
        if (assets != null) assets.dispose();
    }

    public SpriteBatch getBatch() { return batch; }
    public AssetManager getAssets() { return assets; }
}
