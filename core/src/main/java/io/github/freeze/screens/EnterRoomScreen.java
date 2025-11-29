package io.github.freeze.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.freeze.Core;
import io.github.freeze.net.Net;

public class EnterRoomScreen implements Screen {

    private final Core app;
    private final Stage stage;
    
    // ★ 내 닉네임 저장
    private String myNickname;

    // textures
    private Texture texBg, texBoard, texInput;
    private Texture texCancelUp, texCancelOver, texCheckUp, texCheckOver;
    private Texture texDim, texWhite;

    // actors
    private Image bg, dim, board;
    private ImageButton btnCancel, btnCheck;

    // inputs (방코드 / 비밀번호)
    private TextField tfCode, tfPassword;
    private Stack inputCode, inputPw;

    // fonts
    private BitmapFont fontInput;

    public EnterRoomScreen(Core app) {
        this.app = app;
        this.stage = new Stage(new FitViewport(Core.V_WIDTH, Core.V_HEIGHT), app.batch);
        Gdx.input.setInputProcessor(stage);

        // HDPI 동기화
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        // load textures
        texBg         = load("images/bg_school.png");
        texBoard      = load("images/roomin.png");
        texInput      = load("images/roomade_W.png");
        texCancelUp   = load("images/cancel.png");
        texCancelOver = load("images/cancel_O.png");
        texCheckUp    = load("images/check.png");
        texCheckOver  = load("images/check_C.png");
        texDim        = makePixel(new Color(0,0,0,1));
        texWhite      = makePixel(new Color(1,1,1,1));

        dim = new Image(new TextureRegionDrawable(new TextureRegion(texDim)));
        dim.setFillParent(true);
        dim.setColor(0,0,0,0.50f);
        stage.addActor(dim);

        // board
        board = new Image(new TextureRegionDrawable(new TextureRegion(texBoard)));
        stage.addActor(board);

        // buttons (hover)
        btnCancel = makeHoverImageButton(texCancelUp, texCancelOver);
        btnCheck  = makeHoverImageButton(texCheckUp,  texCheckOver);
        
        btnCancel.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                app.setScreen(new FirstScreen(app));
            }
        });
        
        btnCheck.addListener(new ClickListener(){
            @Override public void clicked(InputEvent event, float x, float y) {
                String code = tfCode.getText().trim();
                String pass = tfPassword.getText();
                
                // ★ Preferences에서 닉네임 가져오기
                Preferences pref = Gdx.app.getPreferences("settings");
                String nick = pref.getString("nickname", "");
                if (nick.isEmpty()) {
                    // 랜덤 닉네임 생성 + 저장!
                    nick = "Player" + (int)(Math.random() * 10000);
                    pref.putString("nickname", nick);
                    pref.flush();
                    Gdx.app.log("ENTER", "랜덤 닉네임 생성 및 저장: " + nick);
                }
                
                // ★ 필드에 저장 (LobbyScreen에 전달용)
                myNickname = nick;
                
                // 입력 검증
                if (code.isEmpty()) {
                    Gdx.app.log("ENTER", "방 코드를 입력하세요!");
                    return;
                }
                
                // 필요 시 즉시 연결 시도
                if (!Net.get().isOpen()) {
                    try { 
                        Net.get().connect("ws://203.234.62.48:9090/ws"); 
                    } catch (Exception ex) { 
                        Gdx.app.error("NET", "connect fail", ex); 
                        return;
                    }
                }
                
                // 방 입장 요청
                try {
                    Gdx.app.log("ENTER", "방 입장: code=" + code + ", nick=" + nick);
                    Net.get().sendJoinRoom(code, nick);
                } catch (Throwable t) {
                    Gdx.app.error("NET", "joinRoom send failed", t);
                }
            }
        });
        
        stage.addActor(btnCancel);
        stage.addActor(btnCheck);

        // korean font for inputs
        fontInput = makeKoreanFont(28);

        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = fontInput;
        tfs.fontColor = Color.WHITE;
        tfs.messageFont = fontInput;
        tfs.messageFontColor = new Color(1,1,1,0.45f);
        TextureRegionDrawable cursor = new TextureRegionDrawable(new TextureRegion(texWhite));
        cursor.setMinWidth(2);
        tfs.cursor = cursor;

        tfs.selection = new TextureRegionDrawable(new TextureRegion(texWhite)).tint(new Color(1,1,1,0.25f));
        tfs.background = null;

        tfCode = new TextField("", tfs);       
        tfCode.setMessageText("방 코드");
        
        tfPassword = new TextField("", tfs);   
        tfPassword.setMessageText("비밀번호(선택)");
        tfPassword.setPasswordMode(true);      
        tfPassword.setPasswordCharacter('*');

        inputCode = makeInputStack(tfCode);
        inputPw   = makeInputStack(tfPassword);
        stage.addActor(inputCode);
        stage.addActor(inputPw);

        stage.setKeyboardFocus(tfCode);

        layoutActors();
    }

    // ---------- layout ----------
    private void layoutActors() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // board
        final float BOARD_W = 1.0f;
        float targetBoardW = vw * BOARD_W;
        float sBoard = targetBoardW / ((TextureRegionDrawable)board.getDrawable()).getMinWidth();
        float bw = ((TextureRegionDrawable)board.getDrawable()).getMinWidth()  * sBoard;
        float bh = ((TextureRegionDrawable)board.getDrawable()).getMinHeight() * sBoard;
        board.setSize(bw, bh);
        board.setPosition(vw * 0.5f, vh * 0.50f, Align.center);

        // buttons
        float btnW = bw * 0.15f;
        positionButton(btnCheck,  btnW, board.getX() + bw*0.42f, board.getY() + bh*0.28f);
        positionButton(btnCancel, btnW, board.getX() + bw*0.58f, board.getY() + bh*0.28f);

        // inputs – 방코드 / 비밀번호
        final float INPUT_W_P = 0.245f;
        final float INPUT_H_P = 0.080f;
        final float COL_X_P   = 0.62f;
        final float TOP_Y_P   = 0.536f;
        final float GAP_Y_P   = 0.128f;

        float inputW = bw * INPUT_W_P;
        float inputH = bh * INPUT_H_P;

        setStackSize(inputCode, inputW, inputH);
        setStackSize(inputPw,   inputW, inputH);

        float colX = board.getX() + bw * COL_X_P;
        float yCode = board.getY() + bh * TOP_Y_P;
        float yPw   = board.getY() + bh * (TOP_Y_P - GAP_Y_P);

        inputCode.setPosition(colX, yCode, Align.center);
        inputPw.setPosition(colX, yPw, Align.center);

        // 텍스트 실제 입력 영역(프레임 안 inset)
        final float INSET_X_P = 0.09f;
        final float INSET_Y_P = 0.22f;
        applyInsets(inputCode, inputW, inputH, INSET_X_P, INSET_Y_P);
        applyInsets(inputPw,   inputW, inputH, INSET_X_P, INSET_Y_P);
    }

    // ---------- helpers ----------
    private Texture load(String path){
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }
    
    private Texture makePixel(Color c){
        Pixmap p = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        p.setColor(c); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
    
    private ImageButton makeHoverImageButton(Texture up, Texture over){
        TextureRegionDrawable upD = new TextureRegionDrawable(new TextureRegion(up));
        TextureRegionDrawable ovD = new TextureRegionDrawable(new TextureRegion(over));
        ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
        st.imageUp = upD; st.imageOver = ovD; st.imageDown = upD.tint(new Color(0.6f,0.6f,0.6f,1));
        return new ImageButton(st);
    }
    
    private void positionButton(ImageButton b, float targetW, float cx, float cy){
        TextureRegionDrawable up = (TextureRegionDrawable)b.getStyle().imageUp;
        float s = targetW / up.getMinWidth();
        float w = up.getMinWidth()*s, h = up.getMinHeight()*s;
        b.getImageCell().size(w,h);
        b.setSize(w,h);
        b.setPosition(cx, cy, Align.center);
    }
    
    private Stack makeInputStack(TextField tf){
        Image frame = new Image(new TextureRegionDrawable(new TextureRegion(texInput)));
        Container<TextField> box = new Container<>(tf);
        box.fill();
        Stack st = new Stack();
        st.add(frame);
        st.add(box);
        return st;
    }
    
    private void setStackSize(Stack st, float w, float h){
        st.setSize(w,h);
        ((Image)st.getChildren().get(0)).setSize(w,h);
    }
    
    private void applyInsets(Stack st, float frameW, float frameH, float insetXP, float insetYP){
        Container<?> box = (Container<?>) st.getChildren().get(1);
        float padX = frameW*insetXP, padY = frameH*insetYP;
        box.pad(padY, padX, padY, padX);
        box.invalidate();
    }

    private BitmapFont makeKoreanFont(int px){
        FileHandle fh = Gdx.files.internal("fonts/NotoSansKR-Regular.ttf");
        if(!fh.exists()) throw new GdxRuntimeException("폰트 없음: "+fh.path());
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(fh);
        FreeTypeFontGenerator.FreeTypeFontParameter par = new FreeTypeFontGenerator.FreeTypeFontParameter();
        par.size = px;
        StringBuilder sb = new StringBuilder();
        for(char c=0xAC00;c<=0xD7A3;c++) sb.append(c);
        for(char c=0x3131;c<=0x318E;c++) sb.append(c);
        par.characters = FreeTypeFontGenerator.DEFAULT_CHARS + sb;
        par.minFilter = Texture.TextureFilter.Nearest;
        par.magFilter = Texture.TextureFilter.Nearest;
        BitmapFont f = gen.generateFont(par);
        gen.dispose();
        return f;
    }

    // ---------- Screen ----------
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
                Gdx.app.log("ENTER", "플레이어 추가: " + playerId + " at (" + x + ", " + y + ")");
            }
            
            @Override
            public void onJoinOk(String roomId) {
                Gdx.app.log("ENTER", "방 입장 완료! roomId=" + roomId + ", players=" + playerPositions.keySet() + ", myNick=" + myNickname);
                // ★ LobbyScreen으로 이동 (플레이어 위치 + 내 닉네임 전달)
                app.setScreen(new LobbyScreen(app, roomId, playerPositions, myNickname));
            }

            @Override
            public void onServerError(String code, String message) {
                Gdx.app.error("ENTER", "서버 에러: " + code + " - " + message);
                // TODO: 에러 메시지 표시
            }
        });
    }

    @Override
    public void render(float delta) {
        // HDPI/레터박스 전체 클리어
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glViewport(0,0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ScreenUtils.clear(0,0,0,1);
        stage.getViewport().apply(true);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        layoutActors();
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
        if (fontInput != null) fontInput.dispose();
    }
}
