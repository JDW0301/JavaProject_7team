package io.github.freeze.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.freeze.Core;
import com.badlogic.gdx.graphics.GL20;
public class FirstScreen implements Screen {

    private final Core app;
    private final Stage stage;

    // === 튜닝 값(여기만 바꾸면 크기/위치가 바로 바뀜) ===
    private float logoWidthPercent = 0.75f;   // 로고 폭(뷰포트 대비)
    private float logoYPercent     = 0.67f;   // 로고 중심 Y 위치(0~1)
    private float menuWidthPercent = 0.148f;   // 버튼 폭(뷰포트 대비)
    private float menuXPercent     = 0.487f;   // 버튼 열 중심 X (0~1)
    private float menuYPercent     = 0.24f;   // 버튼 열 중심 Y (0~1)
    private float buttonSpacing    = 10f;     // 버튼 사이 여백(px)

    // 텍스처
    private Texture texBg, texLogo, texCreate, texJoin, texOption, texExit;

    // 액터
    private Image bg;
    private Image logo;
    private Table menuTable;
    private ImageButton btnCreate, btnJoin, btnOption, btnExit;

    public FirstScreen(Core app) {
        this.app = app;
        this.stage = new Stage(new FitViewport(Core.V_WIDTH, Core.V_HEIGHT), app.batch);
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(
            Gdx.graphics.getBackBufferWidth(),
            Gdx.graphics.getBackBufferHeight(),
            true
        );
        // === 로드 ===
        texBg     = load("images/bg_school.png");
        texLogo   = load("images/title_logo.png");
        texCreate = load("images/btn_room_create.png");
        texJoin   = load("images/btn_room_join.png");
        texOption = load("images/btn_option.png");
        texExit   = load("images/btn_exit.png");

        // === 배경 ===
        bg = new Image(new TextureRegionDrawable(new TextureRegion(texBg)));
        bg.setFillParent(true);
        stage.addActor(bg);

        // === 로고 ===
        logo = new Image(new TextureRegionDrawable(new TextureRegion(texLogo)));
        stage.addActor(logo);

        // === 버튼 ===
        btnCreate = imageButton(texCreate);
        btnJoin   = imageButton(texJoin);
        btnOption = imageButton(texOption);
        btnExit   = imageButton(texExit);

        btnCreate.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                Gdx.app.log("UI","방 만들기 클릭");
                app.setScreen(new CreateRoomScreen(app));   // ← 방 만들기 화면
            }
        });

        btnJoin.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                app.setScreen(new EnterRoomScreen(app));
            }
        });

        // 설정
        btnOption.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("UI", "설정");
                app.setScreen(new SettingScreen(app));
            }
        });

        // 나가기
        btnExit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        // 플레이 방 들어가는 임시 코드--------------------------------------------------------
        // FirstScreen.java 내부(생성자 끝부분 즈음)
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(stage);
        mux.addProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.F5) { // F5 누르면 맵 화면
                    app.setScreen(new GameScreen(app));
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(mux);
        //--------------------------------------------------------------------------------


        menuTable = new Table();
        menuTable.defaults().pad(buttonSpacing).center();
        menuTable.add(btnCreate).row();
        menuTable.add(btnJoin).row();
        menuTable.add(btnOption).row();
        menuTable.add(btnExit);
        stage.addActor(menuTable);

        layoutActors(); // ← 최초 배치
    }

    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private ImageButton imageButton(Texture tex) {
        TextureRegionDrawable up = new TextureRegionDrawable(new TextureRegion(tex));
        ImageButton btn = new ImageButton(up);                               // 생성자 1개
        btn.getStyle().imageDown = up.tint(new Color(1,1,1,0.75f));          // 눌림 효과
        return btn;
    }

    /** 뷰포트 기준으로 로고/버튼 크기와 위치를 다시 계산 */
    private void layoutActors() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // --- 로고 크기/위치 ---
        float targetLogoW = vw * logoWidthPercent;
        float logoScale   = targetLogoW / logo.getDrawable().getMinWidth();
        float lw = logo.getDrawable().getMinWidth()  * logoScale;
        float lh = logo.getDrawable().getMinHeight() * logoScale;
        logo.setSize(lw, lh);
        logo.setPosition(vw * 0.5f, vh * logoYPercent, Align.center);

        // --- 버튼 크기(모두 동일 폭으로) ---
        float targetBtnW = vw * menuWidthPercent;
        scaleButton(btnCreate, targetBtnW);
        scaleButton(btnJoin,   targetBtnW);
        scaleButton(btnOption, targetBtnW);
        scaleButton(btnExit,   targetBtnW);

        // 버튼 간격 갱신
        menuTable.defaults().pad(buttonSpacing);
        menuTable.pack();
        // 메뉴 테이블 위치(중심 정렬)
        menuTable.setPosition(vw * menuXPercent, vh * menuYPercent, Align.center);
    }

    private void scaleButton(ImageButton btn, float targetW) {
        TextureRegionDrawable up = (TextureRegionDrawable) btn.getStyle().imageUp;
        float baseW = up.getMinWidth();
        float baseH = up.getMinHeight();
        float s = targetW / baseW;
        btn.getImageCell().size(baseW * s, baseH * s);
    }

    // ----- 외부에서 실시간 조정하고 싶을 때 쓸 메서드들 -----
    public void setLogoWidthPercent(float p) { logoWidthPercent = p; layoutActors(); }
    public void setLogoYPercent(float p)     { logoYPercent     = p; layoutActors(); }
    public void setMenuWidthPercent(float p) { menuWidthPercent = p; layoutActors(); }
    public void setMenuAnchor(float x, float y){ menuXPercent = x; menuYPercent = y; layoutActors(); }
    public void setButtonSpacing(float px)   { buttonSpacing    = px; layoutActors(); }

    @Override
    public void render(float delta) {
        // (1) 가위 테스트 끄고, 백버퍼 픽셀 크기 기준으로 전체 클리어
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glViewport(0, 0,
            Gdx.graphics.getBackBufferWidth(),
            Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // (2) 현재 화면의 뷰포트를 다시 적용(카메라/레터박스 정렬)
        stage.getViewport().apply(true);

        // (3) 그리기
        stage.act(delta);
        stage.draw();
    }
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(
            Gdx.graphics.getBackBufferWidth(),
            Gdx.graphics.getBackBufferHeight(),
            true
        );
        // 선택: 레이아웃 재계산 함수가 있다면 호출
        // layoutActors();
    }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { stage.dispose(); texBg.dispose(); texLogo.dispose(); texCreate.dispose(); texJoin.dispose(); texOption.dispose(); texExit.dispose(); }
}
