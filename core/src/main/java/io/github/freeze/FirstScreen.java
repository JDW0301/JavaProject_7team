package io.github.freeze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;                 // ✅ ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class FirstScreen implements Screen {
    // 월드 비율(원하면 1024x768로 바꿔도 됨)
    private static final float W = 1280f, H = 720f;

    private enum Phase {LOADING, MENU}

    private final Core game;
    private final AssetManager manager;
    private Stage stage;
    private Phase phase = Phase.LOADING;

    // 로딩 UI
    private ShapeRenderer shapes;
    private BitmapFont font;
    private float shownTime = 0f;
    private static final float MIN_SHOW = 1.00f; // 너무 빨리 안 넘어가게 최소 노출

    // 메뉴용 텍스처/이미지
    private Texture texBg, texLogo, texRoomMake, texRoomJoin, texOption, texExit;
    private Image imgBg, imgLogo, btnRoomMake, btnRoomJoin, btnOption, btnExit;

    public FirstScreen(Core game) {
        this.game = game;
        this.manager = game.getAssets();
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(W, H), game.getBatch());
        Gdx.input.setInputProcessor(stage);

        // 로딩 리소스 큐잉 (이미 큐잉되어 있어도 중복 없이 처리됨)
        queueAssets();

        // 로딩 화면 도구
        shapes = new ShapeRenderer();
        font = new BitmapFont();
    }

    private void queueAssets() {
        // 경로는 assets/ 기준 (대소문자 정확히)
        queue("bg/newhome.png");
        queue("ui/Newlogo.png");
        queue("ui/room1.png");
        queue("ui/room2.png");
        queue("ui/option.png");
        queue("ui/out.png");
    }

    private void queue(String path) {
        if (!manager.isLoaded(path, Texture.class)) {
            manager.load(path, Texture.class);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (phase == Phase.LOADING) {
            renderLoading(delta);
            return;
        }

        // MENU
        stage.act(delta);
        stage.draw();
    }

    private void renderLoading(float delta) {
        shownTime += delta;
        boolean done = manager.update();

        // ⬇ 시간 기반 진행도: MIN_SHOW 동안 0 → 1
        float visual = MathUtils.clamp(shownTime / MIN_SHOW, 0f, 1f);
        visual = Interpolation.smooth.apply(visual);  // 부드럽게

        // ⬇ 표시용 진행도는 "시간만" 따른다.
        // - 아직 로딩 안 끝났으면 99%까지만 차오름(가짜 100% 방지)
        // - 로딩이 끝났으면 바로 100%
        float progress = done ? 1f : Math.min(visual, 0.99f);


        stage.getViewport().apply();
        shapes.setProjectionMatrix(stage.getViewport().getCamera().combined);
        float barW = W * 0.6f, barH = 18f;
        float barX = (W - barW) / 2f, barY = H * 0.25f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0x2A2D37FF));
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(new Color(0x4FC3F7FF));
        shapes.rect(barX, barY, barW * progress, barH);
        shapes.end();
        game.getBatch().setProjectionMatrix(stage.getViewport().getCamera().combined);
        game.getBatch().begin();
        font.setColor(Color.WHITE);
        font.draw(game.getBatch(), "Loading... " + Math.round(progress * 100f) + "%", barX, barY + 36f);

        game.getBatch().end();
        // 실제 로딩 완료 + 시간 진행도 100% 충족 시 화면 전환
        if (done && visual >= 1f) {
            manager.finishLoading();
            applyNearestFilters();
            buildMenuUI();
            phase = Phase.MENU;
        }
    }

    private void applyNearestFilters() {
        for (String p : new String[]{
            "bg/newhome.png", "ui/Newlogo.png", "ui/room1.png",
            "ui/room2.png", "ui/option.png", "ui/out.png"
        }) {
            manager.get(p, Texture.class).setFilter(
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
    }

    private void buildMenuUI() {
        // 텍스처 획득 (이미 로딩 완료)
        texBg = manager.get("bg/newhome.png", Texture.class);
        texLogo = manager.get("ui/Newlogo.png", Texture.class);
        texRoomMake = manager.get("ui/room1.png", Texture.class);
        texRoomJoin = manager.get("ui/room2.png", Texture.class);
        texOption = manager.get("ui/option.png", Texture.class);
        texExit = manager.get("ui/out.png", Texture.class);

        // 배경: 화면 크기에 맞춤(왜곡X cover 방식)
        imgBg = new Image(texBg);
        float tw = texBg.getWidth(), th = texBg.getHeight();
        float scale = Math.max(W / tw, H / th);
        float bw = tw * scale, bh = th * scale;
        imgBg.setSize(bw, bh);
        imgBg.setPosition((W - bw) / 2f, (H - bh) / 2f);

        // 로고
        imgLogo = new Image(texLogo);
        float logoW = W * 0.55f; // 원하는 크기만 여기서 조절
        float logoH = texLogo.getHeight() * (logoW / texLogo.getWidth());
        imgLogo.setSize(logoW, logoH);
        imgLogo.setPosition(W / 2f - logoW / 2f, H * 0.47f); // 위치는 유지

        // 버튼(원본 271x84 비율)
        float btnW = W * 0.28f;
        float btnH = 84f * (btnW / 271f);
        float gap = H * 0.018f;
        float topY = H * 0.38f;
        float cx = W / 2f - btnW / 2f;

        btnRoomMake = new Image(texRoomMake);
        btnRoomJoin = new Image(texRoomJoin);
        btnOption = new Image(texOption);
        btnExit = new Image(texExit);

        btnRoomMake.setBounds(cx, topY, btnW, btnH);
        btnRoomJoin.setBounds(cx, topY - (btnH + gap), btnW, btnH);
        btnOption.setBounds(cx, topY - 2 * (btnH + gap), btnW, btnH);
        btnExit.setBounds(cx, topY - 3 * (btnH + gap), btnW, btnH);

        // 클릭 액션(필요 시 다른 Screen으로 전환)
        addClick(btnRoomMake, () -> Gdx.app.log("MENU", "방 만들기 클릭"));
        addClick(btnRoomJoin, () -> Gdx.app.log("MENU", "방 들어가기 클릭"));
        addClick(btnOption, () -> Gdx.app.log("MENU", "설정 클릭"));
        addClick(btnExit, Gdx.app::exit);

        // z-순서: 배경 → 로고 → 버튼들
        stage.addActor(imgBg);
        stage.addActor(imgLogo);
        stage.addActor(btnRoomMake);
        stage.addActor(btnRoomJoin);
        stage.addActor(btnOption);
        stage.addActor(btnExit);
    }

    private void addClick(Image img, Runnable action) {
        img.setOrigin(Align.center);
        img.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                img.setScale(0.98f);
                return true;
            }

            @Override
            public void touchUp(InputEvent e, float x, float y, int p, int b) {
                img.setScale(1f);
            }

            @Override
            public void clicked(InputEvent e, float x, float y) {
                action.run();
            }
        });
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        // 텍스처는 AssetManager가 관리하므로 여기서 dispose() 하지 않음
    }
}
