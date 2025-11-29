package io.github.freeze.screens;

import com.badlogic.gdx.*;
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
import io.github.freeze.net.Net;

public class CreateRoomScreen implements Screen {

    private final Core app;
    private final Stage stage;
    
    // ★ 내 닉네임 저장
    private String myNickname;

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
        texInput      = load("images/roomade_W.png"); // 입력 프레임
        texCancelUp   = load("images/cancel.png");
        texCancelOver = load("images/cancel_O.png");
        texCheckUp    = load("images/check.png");
        texCheckOver  = load("images/check_C.png");
        texDim        = makePixel(new Color(0,0,0,1f));
        texWhite      = makePixel(new Color(1,1,1,1f));

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
            @Override public void clicked(InputEvent e, float x, float y) {
                String title = tfTitle.getText().trim();
                String code  = tfCode.getText().trim();
                String pass  = tfPassword.getText();
                
                // ★ Preferences에서 닉네임 가져오기
                Preferences pref = Gdx.app.getPreferences("settings");
                String nick = pref.getString("nickname", "");
                if (nick.isEmpty()) {
                    // 랜덤 닉네임 생성 + 저장!
                    nick = "Player" + (int)(Math.random() * 10000);
                    pref.putString("nickname", nick);
                    pref.flush();
                    Gdx.app.log("CREATE", "랜덤 닉네임 생성 및 저장: " + nick);
                }
                
                // ★ 필드에 저장 (LobbyScreen에 전달용)
                myNickname = nick;

                // 입력 검증
                if (code.isEmpty() || title.isEmpty()) {
                    Gdx.app.log("CREATE", "방 코드와 제목을 입력하세요!");
                    return;
                }

                // 결과 초기화
                Net.get().resetLastResult();

                // 필요 시 즉시 연결 시도
                if (!Net.get().isOpen()) {
                    try { 
                        Net.get().connect("ws://203.234.62.48:9090/ws"); 
                    } catch (Exception ex) { 
                        Gdx.app.error("NET", "connect fail", ex); 
                        return;
                    }
                }

                // 방 생성 요청
                try {
                    Gdx.app.log("CREATE", "방 생성: code=" + code + ", title=" + title + ", nick=" + nick);
                    Net.get().sendCreateRoom(code, title, pass, nick);
                } catch (Throwable t) {
                    Gdx.app.error("NET", "createRoom send failed", t);
                }
            }
        });

        stage.addActor(btnCancel);
        stage.addActor(btnCheck);

        // ---- Inputs (프레임 + TextField를 Stack으로 겹침) ----
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansKR-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter par = new FreeTypeFontGenerator.FreeTypeFontParameter();

        par.size = 28;

        // 한글 포함 문자 집합
        StringBuilder hangul = new StringBuilder();
        for (char c = 0xAC00; c <= 0xD7A3; c++) hangul.append(c);
        for (char c = 0x3131; c <= 0x318E; c++) hangul.append(c);
        par.characters = FreeTypeFontGenerator.DEFAULT_CHARS + hangul;

        par.minFilter = Texture.TextureFilter.Nearest;
        par.magFilter = Texture.TextureFilter.Nearest;

        BitmapFont fontInput = gen.generateFont(par);
        gen.dispose();

        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = fontInput;
        tfs.fontColor = Color.WHITE;
        tfs.messageFont = fontInput;
        tfs.messageFontColor = new Color(1,1,1,0.45f);

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

        com.badlogic.gdx.scenes.scene2d.ui.Container<TextField> box =
            new com.badlogic.gdx.scenes.scene2d.ui.Container<>(tf);
        box.fill();

        Stack st = new Stack();
        st.add(frame);
        st.add(box);
        return st;
    }

    private void layoutActors() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        final float BOARD_W = 1f;
        float targetBoardW = vw * BOARD_W;
        float sBoard = targetBoardW / board.getDrawable().getMinWidth();
        float bw = board.getDrawable().getMinWidth()  * sBoard;
        float bh = board.getDrawable().getMinHeight() * sBoard;
        board.setSize(bw, bh);
        board.setPosition(vw * 0.5f, vh * 0.50f, Align.center);

        float btnW = bw * 0.15f;
        positionButton(btnCheck,  btnW, board.getX() + bw*0.42f, board.getY() + bh*0.24f);
        positionButton(btnCancel, btnW, board.getX() + bw*0.58f, board.getY() + bh*0.24f);

        final float INPUT_W_P = 0.245f;
        final float INPUT_H_P = 0.082f;
        final float COL_X_P   = 0.62f;
        final float TOP_Y_P   = 0.55f;
        final float GAP_Y_P   = 0.094f;

        float inputW = bw * INPUT_W_P;
        float inputH = bh * INPUT_H_P;

        setStackSize(input1, inputW, inputH);
        setStackSize(input2, inputW, inputH);
        setStackSize(input3, inputW, inputH);

        final float INSET_X_P = 0.14f;
        final float INSET_Y_P = 0.30f;
        applyInsets(input1, inputW, inputH, INSET_X_P, INSET_Y_P);
        applyInsets(input2, inputW, inputH, INSET_X_P, INSET_Y_P);
        applyInsets(input3, inputW, inputH, INSET_X_P, INSET_Y_P);

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
        ((Image) st.getChildren().get(0)).setSize(w, h);
    }

    private void applyInsets(Stack st, float frameW, float frameH,
                             float insetXPct, float insetYPct) {
        com.badlogic.gdx.scenes.scene2d.ui.Container<?> box =
            (com.badlogic.gdx.scenes.scene2d.ui.Container<?>) st.getChildren().get(1);

        float padX = frameW * insetXPct;
        float padY = frameH * insetYPct;

        box.padLeft(padX);
        box.padRight(padX);
        box.padTop(padY);
        box.padBottom(padY);
        box.invalidate();
    }

    // ========= Screen =========

    @Override 
    public void show() {
        // ★ 플레이어 목록 + 위치 저장용
        java.util.Map<String, float[]> playerPositions = new java.util.HashMap<>();
        
        // ★ 네트워크 리스너 설정
        Net.get().setListener(new Net.Listener() {
            @Override
            public void onPlayerJoined(String playerId, float x, float y) {
                // ★ 플레이어 위치 저장
                playerPositions.put(playerId, new float[]{x, y});
                Gdx.app.log("CREATE", "플레이어 추가: " + playerId + " at (" + x + ", " + y + ")");
            }
            
            @Override
            public void onCreateRoomOk(String roomId) {
                Gdx.app.log("CREATE", "방 생성 완료! roomId=" + roomId + ", players=" + playerPositions.keySet() + ", myNick=" + myNickname);
                // ★ LobbyScreen으로 이동 (플레이어 위치 + 내 닉네임 전달)
                app.setScreen(new LobbyScreen(app, roomId, playerPositions, myNickname));
            }

            @Override
            public void onServerError(String code, String message) {
                Gdx.app.error("CREATE", "서버 에러: " + code + " - " + message);
                // TODO: 에러 메시지 표시
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glViewport(0, 0,
            Gdx.graphics.getBackBufferWidth(),
            Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply(true);

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
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override 
    public void hide() {
        // 리스너 해제
        Net.get().setListener(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        texBg.dispose(); texBoard.dispose(); texInput.dispose();
        texCancelUp.dispose(); texCancelOver.dispose();
        texCheckUp.dispose();  texCheckOver.dispose();
        texDim.dispose(); texWhite.dispose();
    }
}
