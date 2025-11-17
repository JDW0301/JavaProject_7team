package io.github.freeze.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.freeze.Core;

public class SettingScreen implements Screen {

    private static final int VW = 1280; // 4:3 고정
    private static final int VH = 960;

    private final Core app;
    private final Stage stage;

    // 텍스처
    private Texture texBg, texBoard, texInput, texWhite;
    private Texture texCheckUp, texCheckOver, texCancelUp, texCancelOver;

    // 액터(보드/입력칸/버튼만)
    private Image bg, board;
    private Table inputNick;          // roomade_W.png 프레임 배경
    private TextField tfNick;
    private ImageButton btnCheck, btnCancel;

    // 폰트
    private BitmapFont fontKR;

    public SettingScreen(Core app) {
        this.app = app;
        this.stage = new Stage(new FitViewport(VW, VH), app.batch);
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        // === 리소스 로드 ===
        //texBg        = load("images/bg_school.png");
        texBoard     = load("images/Setting2.png");     // 보드(설정/닉네임/캐릭터 포함된 이미지)
        texInput     = load("images/roomade_W.png");    // 텍스트 필드 프레임
        texWhite     = make1x1(Color.WHITE);
        texCheckUp   = load("images/check.png");
        texCheckOver = load("images/check_C.png");
        texCancelUp  = load("images/cancel.png");
        texCancelOver= load("images/cancel_O.png");

        board = new Image(new TextureRegionDrawable(new TextureRegion(texBoard)));
        stage.addActor(board);

        // === 한글 폰트(입력 전용) ===
        fontKR = makeKoreanFont(Math.round(VH * 0.045f)); // 필요 시 숫자만 조절

        // === 텍스트 필드(라벨/아바타 없음) ===
        TextFieldStyle tfs = new TextFieldStyle();
        tfs.font = fontKR;
        tfs.fontColor = Color.WHITE;
        tfs.messageFont = fontKR;
        tfs.messageFontColor = new Color(1,1,1,0.40f); // 힌트 색
        TextureRegionDrawable cursor = new TextureRegionDrawable(new TextureRegion(texWhite));
        cursor.setMinWidth(2);
        tfs.cursor = cursor;
        tfs.background = null;

        tfNick = new TextField("", tfs);
        tfNick.setMessageText(""); // 보드에 '닉네임' 표기 이미 있으므로 힌트는 공백
        // 저장한 닉네임 불러오기
        Preferences pref = Gdx.app.getPreferences("settings");
        tfNick.setText(pref.getString("nickname", ""));
        tfNick.setMessageText("닉네임을 입력");
        // 프레임 있는 입력칸(Table 배경으로 roomade_W.png)
        inputNick = new Table();
        inputNick.setBackground(new TextureRegionDrawable(new TextureRegion(texInput)));
        inputNick.add(tfNick).grow();
        stage.addActor(inputNick);

        // === 버튼 ===
        btnCheck  = hoverBtn(texCheckUp,  texCheckOver);
        btnCancel = hoverBtn(texCancelUp, texCancelOver);
        stage.addActor(btnCheck);
        stage.addActor(btnCancel);

        btnCancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                app.setScreen(new FirstScreen(app));
            }
        });
        btnCheck.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                Preferences p = Gdx.app.getPreferences("settings");
                p.putString("nickname", tfNick.getText());
                p.flush();
                // app.setScreen(new FirstScreen(app)); // 필요 시 복귀
            }
        });

        layout();
    }

    // ===== 레이아웃 =====
    private void layout() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // 보드: 화면 폭 1.0로 스케일
        TextureRegionDrawable bd = (TextureRegionDrawable) board.getDrawable();
        float s = (vw * 1.0f) / bd.getMinWidth();
        float bw = bd.getMinWidth()  * s;
        float bh = bd.getMinHeight() * s;
        board.setSize(bw, bh);
        board.setPosition(vw * 0.5f, vh * 0.50f, Align.center);

        // 닉네임 입력칸(보드 좌측 영역)
        float inW = bw * 0.30f;   // 가로폭
        float inH = bh * 0.10f;   // 세로폭
        inputNick.setSize(inW, inH);
        inputNick.setPosition(board.getX() + bw * 0.40f,  // X 위치
            board.getY() + bh * 0.48f,  // Y 위치
            Align.center);

        // 프레임 안쪽 여백(텍스트 실제 영역)
        float padX = inW * 0.10f;
        float padY = inH * 0.20f;
        inputNick.getCell(tfNick).pad(padY, padX, padY, padX);
        inputNick.invalidate();

        // 버튼(보드 하단 중앙)
        float btnW = bw * 0.15f;
        placeBtn(btnCheck,  btnW, board.getX() + bw * 0.42f, board.getY() + bh * 0.23f); // 확인(왼쪽)
        placeBtn(btnCancel, btnW, board.getX() + bw * 0.58f, board.getY() + bh * 0.23f); // 취소(오른쪽)

        // 가림 방지
        inputNick.toFront();
        btnCheck.toFront(); btnCancel.toFront();
    }

    // ===== 헬퍼 =====
    private Texture load(String path){
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }
    private Texture make1x1(Color c){
        Pixmap p = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        p.setColor(c); p.fill();
        Texture t = new Texture(p); p.dispose(); return t;
    }
    private BitmapFont makeKoreanFont(int px){
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansKR-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter par = new FreeTypeFontGenerator.FreeTypeFontParameter();
        par.size = px;
        par.minFilter = Texture.TextureFilter.Nearest; par.magFilter = Texture.TextureFilter.Nearest;
        // 한글 포함
        StringBuilder sb = new StringBuilder();
        for(char c=0xAC00;c<=0xD7A3;c++) sb.append(c);
        for(char c=0x3131;c<=0x318E;c++) sb.append(c);
        par.characters = FreeTypeFontGenerator.DEFAULT_CHARS + sb;
        BitmapFont f = gen.generateFont(par);
        gen.dispose();
        return f;
    }
    private ImageButton hoverBtn(Texture up, Texture over){
        TextureRegionDrawable u = new TextureRegionDrawable(new TextureRegion(up));
        TextureRegionDrawable o = new TextureRegionDrawable(new TextureRegion(over));
        ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
        st.imageUp = u; st.imageOver = o; st.imageDown = u.tint(new Color(0.6f,0.6f,0.6f,1));
        return new ImageButton(st);
    }
    private void placeBtn(ImageButton b, float targetW, float cx, float cy){
        TextureRegionDrawable up = (TextureRegionDrawable) b.getStyle().imageUp;
        float s = targetW / up.getMinWidth();
        float w = up.getMinWidth()*s, h = up.getMinHeight()*s;
        b.getImageCell().size(w, h);
        b.setSize(w, h);
        b.setPosition(cx, cy, Align.center);
    }

    // ===== Screen =====
    @Override public void show() {}
    @Override public void render(float delta) {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glViewport(0,0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0,0,0,1);
        stage.getViewport().apply(true);

        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) {
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        layout();
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override public void dispose() {
        stage.dispose();
        texBg.dispose(); texBoard.dispose(); texInput.dispose(); texWhite.dispose();
        texCheckUp.dispose(); texCheckOver.dispose();
        texCancelUp.dispose(); texCancelOver.dispose();
        if (fontKR != null) fontKR.dispose();
    }
}
