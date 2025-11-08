package io.github.freeze.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import com.badlogic.gdx.graphics.GL20;
import io.github.freeze.Core;

public class CreateRoomScreen implements Screen {

    private final Core app;
    private final Stage stage;

    // === Textures ===
    private Texture texBg;           // 배경
    private Texture texBoard;        // 큰 보드 (roomade.png)
    private Texture texInput;        // 입력칸 프레임 (roomage_W.png)
    private Texture texCancelUp, texCancelOver;
    private Texture texCheckUp,  texCheckOver;
    private Texture texDim, texWhite; // 1x1 픽셀

    // === Actors ===
    private Image bg, dim, board;
    private ImageButton btnCancel, btnCheck;

    // === Inputs ===
    private TextField tfTitle, tfCode, tfPassword;
    private Stack input1, input2, input3;


    public CreateRoomScreen(Core app) {
        this.app = app;
        this.stage = new Stage(new FitViewport(Core.V_WIDTH, Core.V_HEIGHT), app.batch);
        Gdx.input.setInputProcessor(stage);
        // 전환 직후 밀림 방지
        stage.getViewport().update(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            true);

        // ---- Load textures ----
        texBg         = load("images/bg_school.png");
        texBoard      = load("images/roomade.png");
        texInput      = load("images/roomade_W.png"); // 입력 프레임 (파일명이 다르면 이 줄만 수정)
        texCancelUp   = load("images/cancel.png");
        texCancelOver = load("images/cancel_O.png");
        texCheckUp    = load("images/check.png");
        texCheckOver  = load("images/check_C.png");
        texDim        = makePixel(new Color(0,0,0,1f));
        texWhite      = makePixel(new Color(1,1,1,1f));

        // ---- Background ----
//        bg = new Image(new TextureRegionDrawable(new TextureRegion(texBg)));
//        bg.setFillParent(true);
//        stage.addActor(bg);
//        bg.setPosition(0, 0);

        dim = new Image(new TextureRegionDrawable(new TextureRegion(texDim)));
        dim.setFillParent(true);
        dim.setColor(0,0,0,0.50f);
        stage.addActor(dim);

        // ---- Board ----
        board = new Image(new TextureRegionDrawable(new TextureRegion(texBoard)));
        board.setScaling(Scaling.fit);
        stage.addActor(board);

        // ---- Buttons (hover 적용) ----
        btnCancel = makeHoverImageButton(texCancelUp, texCancelOver);
        btnCheck  = makeHoverImageButton(texCheckUp,  texCheckOver);

        btnCancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                app.setScreen(new FirstScreen(app)); // 뒤로
            }
        });
        btnCheck.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("JOIN", "확인 클릭: title=" + tfTitle.getText()
                    + ", code=" + tfCode.getText()
                    + ", pw=" + tfPassword.getText());
                // TODO: 검증 및 다음 단계
            }
        });

        stage.addActor(btnCancel);
        stage.addActor(btnCheck);

        // ---- Inputs (프레임 + TextField를 Stack으로 겹침) ----
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansKR-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter par = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // 글자 크기(px). 보드 높이에 비례하게도 가능: Math.round(bh * 0.035f)
        par.size = 28;

        // 한글 포함 문자 집합(완성형 + 호환자모)
        StringBuilder hangul = new StringBuilder();
        for (char c = 0xAC00; c <= 0xD7A3; c++) hangul.append(c); // 완성형
        for (char c = 0x3131; c <= 0x318E; c++) hangul.append(c); // 호환 자모
        par.characters = FreeTypeFontGenerator.DEFAULT_CHARS + hangul;

        // 픽셀 느낌 유지
        par.minFilter = Texture.TextureFilter.Nearest;
        par.magFilter = Texture.TextureFilter.Nearest;

        BitmapFont fontInput = gen.generateFont(par);
        gen.dispose();

        // === TextField 스타일에 적용 ===
        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = fontInput;
        tfs.fontColor = Color.WHITE;
        tfs.messageFont = fontInput;                 // placeholder도 같은 폰트
        tfs.messageFontColor = new Color(1,1,1,0.45f);

        // 커서/선택 (그대로 사용 가능)
        TextureRegionDrawable cursor = new TextureRegionDrawable(new TextureRegion(texWhite));
        cursor.setMinWidth(2);
        tfs.cursor = cursor;
        tfs.selection = new TextureRegionDrawable(new TextureRegion(texWhite)).tint(new Color(1,1,1,0.25f));

        tfTitle = new TextField("", tfs);
        tfTitle.setMessageText("방 이름 입력");
        tfTitle.setAlignment(Align.left);

        tfCode = new TextField("", tfs);
        tfCode.setMessageText("코드 입력");

        tfPassword = new TextField("", tfs);
        tfPassword.setMessageText("비밀번호(선택)");
        tfPassword.setPasswordMode(true);
        tfPassword.setPasswordCharacter('*');

        input1 = makeInputStack(tfTitle);
        input2 = makeInputStack(tfCode);
        input3 = makeInputStack(tfPassword);

        stage.addActor(input1);
        stage.addActor(input2);
        stage.addActor(input3);

        stage.setKeyboardFocus(tfTitle);

        // 최초 배치
        layoutActors();
    }

    // ========= Helpers =========

    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private Texture makePixel(Color c) {
        Pixmap p = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private ImageButton makeHoverImageButton(Texture up, Texture over) {
        TextureRegionDrawable upD   = new TextureRegionDrawable(new TextureRegion(up));
        TextureRegionDrawable overD = new TextureRegionDrawable(new TextureRegion(over));
        ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
        st.imageUp   = upD;
        st.imageOver = overD;
        st.imageDown = upD.tint(new Color(0.6f,0.6f,0.6f,1f));
        ImageButton b = new ImageButton(st);
        b.getImage().setScaling(Scaling.fit);
        return b;
    }

    private Stack makeInputStack(TextField tf) {
        Image frame = new Image(new TextureRegionDrawable(new TextureRegion(texInput)));

        // ★ 텍스트필드를 컨테이너로 감싸 padding 적용 가능하게
        com.badlogic.gdx.scenes.scene2d.ui.Container<TextField> box =
            new com.badlogic.gdx.scenes.scene2d.ui.Container<>(tf);
        box.fill(); // 컨테이너는 스택 전체를 채우게 하고,

        Stack st = new Stack();
        st.add(frame); // 0: 프레임 이미지
        st.add(box);   // 1: 패딩을 줄 컨테이너(안에 TextField)
        return st;
    }

    /** 뷰포트 기준으로 전체 배치/스케일 계산 */
    private void layoutActors() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // ---- Board 크기/위치 ----
        final float BOARD_W = 1f; // (원하면 1.0f 등으로 변경)
        float targetBoardW = vw * BOARD_W;
        float sBoard = targetBoardW / board.getDrawable().getMinWidth();
        float bw = board.getDrawable().getMinWidth()  * sBoard;
        float bh = board.getDrawable().getMinHeight() * sBoard;
        board.setSize(bw, bh);
        board.setPosition(vw * 0.5f, vh * 0.50f, Align.center);

        // ---- Buttons 크기/위치 ----
        float btnW = bw * 0.15f;
        positionButton(btnCheck,  btnW, board.getX() + bw*0.42f, board.getY() + bh*0.24f); // 확인(왼쪽)
        positionButton(btnCancel, btnW, board.getX() + bw*0.58f, board.getY() + bh*0.24f); // 취소(오른쪽)

        // ---- 입력칸 크기/위치 (보드가 화면폭 1.0 기준 튜닝값) ----
        final float INPUT_W_P = 0.245f;  // 입력칸 가로폭(보드 폭 대비)
        final float INPUT_H_P = 0.082f; // 입력칸 세로폭(보드 높이 대비)
        final float COL_X_P   = 0.62f;  // 오른쪽 열 중심 X(보드 폭 대비)
        final float TOP_Y_P   = 0.55f;  // 첫 입력칸 중심 Y(보드 높이 대비)
        final float GAP_Y_P   = 0.094f;  // 입력칸 사이 중심-중심 간격(보드 높이 대비)

        float inputW = bw * INPUT_W_P;
        float inputH = bh * INPUT_H_P;

        setStackSize(input1, inputW, inputH);
        setStackSize(input2, inputW, inputH);
        setStackSize(input3, inputW, inputH);

        // 프레임 안쪽 여백(텍스트 박스 패딩)
        final float INSET_X_P = 0.14f;  // 좌우 안쪽 여백(프레임 폭 대비)
        final float INSET_Y_P = 0.30f;  // 상하 안쪽 여백(프레임 높이 대비)
        applyInsets(input1, inputW, inputH, INSET_X_P, INSET_Y_P);
        applyInsets(input2, inputW, inputH, INSET_X_P, INSET_Y_P);
        applyInsets(input3, inputW, inputH, INSET_X_P, INSET_Y_P);

        // 위치(오른쪽 열에 위→아래로 일정 간격)
        float colX = board.getX() + bw * COL_X_P;
        float y1   = board.getY() + bh * TOP_Y_P;
        float y2   = board.getY() + bh * (TOP_Y_P - GAP_Y_P);
        float y3   = board.getY() + bh * (TOP_Y_P - 2f*GAP_Y_P);

        input1.setPosition(colX, y1, Align.center);
        input2.setPosition(colX, y2, Align.center);
        input3.setPosition(colX, y3, Align.center);
    }

    private void positionButton(ImageButton b, float targetW, float cx, float cy) {
        TextureRegionDrawable up = (TextureRegionDrawable) b.getStyle().imageUp;
        float s = targetW / up.getMinWidth();
        float w = up.getMinWidth()  * s;
        float h = up.getMinHeight() * s;
        b.getImageCell().size(w, h);
        b.setSize(w, h);
        b.setPosition(cx, cy, Align.center);
    }

    private void setStackSize(Stack st, float w, float h) {
        st.setSize(w, h);
        ((Image) st.getChildren().get(0)).setSize(w, h); // 프레임 Image
    }


    private void applyInsets(Stack st, float frameW, float frameH,
                             float insetXPct, float insetYPct) {
        // Stack의 두 번째 자식이 Container<TextField> 라는 가정
        com.badlogic.gdx.scenes.scene2d.ui.Container<?> box =
            (com.badlogic.gdx.scenes.scene2d.ui.Container<?>) st.getChildren().get(1);

        float padX = frameW * insetXPct;
        float padY = frameH * insetYPct;

        box.padLeft(padX);
        box.padRight(padX);
        box.padTop(padY);
        box.padBottom(padY);
        box.invalidate(); // 레이아웃 갱신
    }

    // ========= Screen =========

    @Override public void show() {}

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

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        texBg.dispose(); texBoard.dispose(); texInput.dispose();
        texCancelUp.dispose(); texCancelOver.dispose();
        texCheckUp.dispose();  texCheckOver.dispose();
        texDim.dispose(); texWhite.dispose();

        // ★ TTF로 만든 폰트 객체 정리 (이름에 맞춰 수정)
        //if (fontInput != null) fontInput.dispose();

        // Skin을 안 쓰면 아래 둘은 지워도 됩니다.
        // if (skin != null) skin.dispose();
        // if (font != null) font.dispose();
    }
}
