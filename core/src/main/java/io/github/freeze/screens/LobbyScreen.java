package io.github.freeze.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.freeze.Core;
import io.github.freeze.game.Player;
import io.github.freeze.game.PlayerRole;
import io.github.freeze.game.PlayerState;
import io.github.freeze.net.Net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대기방 화면
 * - 플레이어들이 입장 대기
 * - 바닥에서만 이동 가능
 * - 방장만 Start 버튼 (Ready 기능 제거됨)
 */
public class LobbyScreen implements Screen {
    private static final int VW = Core.V_WIDTH;  // 1280
    private static final int VH = Core.V_HEIGHT; // 960
    
    // ★ 충돌 처리용 히트박스 패딩
    private static final float HERO_PAD = 0.25f;  // 캐릭터 크기의 25% 여백

    private final Core app;
    private final Stage stage;
    private final String roomId;

    // 배경 이미지
    private Texture texWaitroomOther;
    private Texture texWaitroomFloor;
    private Image imgOther, imgFloor;

    // UI 이미지
    private Texture texNumberCheck0, texNumberCheck1, texNumberCheck2, texNumberCheck3, texNumberCheck4;
    private Texture texReadyUp, texReadyOver;  // ★ Ready 버튼 추가
    private Texture texStartUp, texStartOver;
    private Image imgNumberCheck;

    // 버튼
    private ImageButton btnReady, btnStart;  // ★ Ready 버튼 복구

    // 캐릭터 텍스처
    private Texture texIdle;
    private Texture[] texRight = new Texture[8];
    private Texture[] texLeft = new Texture[8];

    // 플레이어 정보
    private Map<String, Player> players = new HashMap<>();
    private Map<String, Boolean> readyStatus = new HashMap<>();  // ★ Ready 상태 관리
    private String myPlayerId;
    private boolean isHost = false;  // 방장 여부
    private boolean serverSupportsReady = false;  // ★ 서버 Ready 지원 여부

    // 바닥 영역 (이동 가능 영역)
    private Rectangle floorArea;

    // 월드
    private Group world;
    
    // 이동 전송 타이머
    private float moveSendTimer = 0f;
    private static final float MOVE_SEND_INTERVAL = 0.02f;  // ★ 20ms = 50fps (33ms → 20ms)
    
    // ★ 이전 프레임 이동 상태 (정지 메시지 전송용)
    private boolean wasMovingLastFrame = false;
    
    // ★ 닉네임 렌더링용 폰트
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;
    
    // ★ 초기 플레이어 위치
    private Map<String, float[]> initialPositions;
    
    // ★ 초기 Ready 상태
    private Map<String, Boolean> initialReadyStatus;

    // ★★★ 수정: 5개 파라미터 생성자 (readyStatus 추가) ★★★
    public LobbyScreen(Core app, String roomId, Map<String, float[]> playerPositions, 
                       Map<String, Boolean> readyStatusMap, String myNickname) {
        this.app = app;
        this.roomId = roomId;
        this.initialPositions = playerPositions;
        this.initialReadyStatus = readyStatusMap;  // ★ 초기 Ready 상태 저장
        this.myPlayerId = myNickname;
        this.stage = new Stage(new FitViewport(VW, VH), app.batch);
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        loadTextures();
        setupWorld();
        setupUI();
        setupNetworkListener();

        // ★ 서버에서 받은 플레이어들로 생성
        createPlayersFromServer();
    }
    
    // ★ 기존 2개 파라미터 생성자 (테스트용)
    public LobbyScreen(Core app, String roomId) {
        this(app, roomId, new HashMap<>(), new HashMap<>(), "TestPlayer");
        // 테스트용 로컬 플레이어
        createTestPlayer();
    }

    // ========== 텍스처 로딩 ==========
    private void loadTextures() {
        // 배경
        texWaitroomOther = load("images/waitroom2_other.png");
        texWaitroomFloor = load("images/waitroom2_floor.png");

        // UI
        texNumberCheck0 = load("images/Number_check0.png");
        texNumberCheck1 = load("images/Number_check1.png");
        texNumberCheck2 = load("images/Number_check2.png");
        texNumberCheck3 = load("images/Number_check3.png");
        texNumberCheck4 = load("images/Number_check4.png");

        texReadyUp = load("images/Ready.png");  // ★ Ready 버튼
        texReadyOver = load("images/Ready_O.png");
        texStartUp = load("images/start.png");
        texStartOver = load("images/start_O.png");

        // 캐릭터
        texIdle = load("images/Front_C.png");
        for (int i = 0; i < 8; i++) {
            texRight[i] = load("images/Right_C" + (i + 1) + ".png");
            texLeft[i] = load("images/Left_C" + (i + 1) + ".png");
        }
        
        // ★ 닉네임 렌더링용 폰트
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        font.getData().setScale(1.5f);  // 폰트 크기 확대
    }

    // ========== 월드 설정 ==========
    private void setupWorld() {
        world = new Group();
        stage.addActor(world);

        // ★ 1280 고정 너비 (화면 크기에 맞춤)
        float targetWidth = 1280f;
        
        // 비율 유지하면서 높이 계산
        float bgHeight = targetWidth * (texWaitroomOther.getHeight() / (float)texWaitroomOther.getWidth());
        float floorHeight = targetWidth * (texWaitroomFloor.getHeight() / (float)texWaitroomFloor.getWidth());

        // 바닥 (먼저 그리기)
        imgFloor = new Image(new TextureRegionDrawable(new TextureRegion(texWaitroomFloor)));
        imgFloor.setSize(targetWidth, floorHeight);
        imgFloor.setPosition(0, 0);
        world.addActor(imgFloor);

        // 배경 (바닥 위에 배치)
        imgOther = new Image(new TextureRegionDrawable(new TextureRegion(texWaitroomOther)));
        imgOther.setSize(targetWidth, bgHeight);
        imgOther.setPosition(0, floorHeight);
        world.addActor(imgOther);

        // ★ 바닥 전체 영역에서 이동 가능
        floorArea = new Rectangle(0, 0, targetWidth, floorHeight);

        Gdx.app.log("FLOOR", "floor area: " + floorArea);
    }
    
    // ========== UI 설정 ==========
    private void setupUI() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // 인원 체크 (상단 중앙)
        imgNumberCheck = new Image(new TextureRegionDrawable(new TextureRegion(texNumberCheck0)));
        float checkW = vw * 0.15f;
        float checkH = checkW * 0.4f;
        imgNumberCheck.setSize(checkW, checkH);
        imgNumberCheck.setPosition(vw * 0.5f, vh * 0.95f, Align.center);
        stage.addActor(imgNumberCheck);

        // ★ Ready 버튼 (모든 플레이어)
        btnReady = makeHoverButton(texReadyUp, texReadyOver);
        float btnW = vw * 0.15f;
        positionButton(btnReady, btnW, vw * 0.35f, vh * 0.1f);  // 왼쪽
        btnReady.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleReady();
            }
        });
        stage.addActor(btnReady);

        // Start 버튼 (방장만)
        btnStart = makeHoverButton(texStartUp, texStartOver);
        positionButton(btnStart, btnW, vw * 0.65f, vh * 0.1f);  // 오른쪽
        btnStart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGame();
            }
        });
        btnStart.setVisible(isHost);  // 방장만 보임
        stage.addActor(btnStart);
    }

    // ========== 네트워크 리스너 ==========
    private void setupNetworkListener() {
        Net.get().setListener(new Net.Listener() {
            // ★★★ 수정: dx, dy 파라미터 추가 및 moveOther 호출 ★★★
            @Override
            public void onPlayerMove(String playerId, float dx, float dy, float x, float y) {
                Player p = players.get(playerId);
                if (p != null && !playerId.equals(myPlayerId)) {
                    p.moveOther(dx, dy, x, y);  // ★ 애니메이션 포함 이동
                    Gdx.app.log("LOBBY", "Player moved: " + playerId + " dx=" + dx + " dy=" + dy);
                }
            }

            @Override
            public void onGameStart(String roleJson) {
                Gdx.app.log("LOBBY", "게임 시작! roleJson=" + roleJson);
                
                // ★ roleJson 파싱해서 역할 정보 추출
                Map<String, PlayerRole> roles = new HashMap<>();
                
                try {
                    com.google.gson.JsonObject jo = com.google.gson.JsonParser.parseString(roleJson).getAsJsonObject();
                    
                    if (jo.has("roles")) {
                        com.google.gson.JsonObject rolesObj = jo.getAsJsonObject("roles");
                        for (String playerId : rolesObj.keySet()) {
                            String roleStr = rolesObj.get(playerId).getAsString();
                            PlayerRole role = "CHASER".equalsIgnoreCase(roleStr) ? PlayerRole.CHASER : PlayerRole.RUNNER;
                            roles.put(playerId, role);
                            Gdx.app.log("LOBBY", "역할 배정: " + playerId + " → " + role);
                        }
                    }
                } catch (Exception e) {
                    Gdx.app.error("LOBBY", "역할 파싱 실패: " + e.getMessage());
                }
                
                // ★ GameScreen으로 이동 (역할 정보 전달)
                app.setScreen(new GameScreen(app, roles));
            }

            @Override
            public void onPlayerReady(String playerId, boolean isReady) {
                Gdx.app.log("LOBBY", "Player ready: " + playerId + " = " + isReady);
                
                // ★ 서버가 Ready를 지원함을 확인
                serverSupportsReady = true;
                
                // Ready 상태 업데이트
                readyStatus.put(playerId, isReady);
                updatePlayerCount();
            }

            @Override
            public void onPlayerJoined(String playerId, float x, float y) {
                Gdx.app.log("LOBBY", "Player joined: " + playerId + " at (" + x + ", " + y + ")");
                
                // ★ 이미 있는 플레이어면 무시
                if (players.containsKey(playerId)) {
                    return;
                }
                
                // ★ 새 플레이어 생성 (위치 포함)
                addPlayerAt(playerId, x, y);
                updatePlayerCount();
            }

            @Override
            public void onPlayerLeft(String playerId) {
                Gdx.app.log("LOBBY", "Player left: " + playerId);
                
                // ★ 플레이어 제거
                Player p = players.remove(playerId);
                if (p != null && p.getImage() != null) {
                    p.getImage().remove();  // 화면에서 제거
                }
                readyStatus.remove(playerId);
                updatePlayerCount();
            }
        });
    }

    // ========== 테스트용 플레이어 생성 ==========
    private void createTestPlayer() {
        // ★ Preferences에서 닉네임 가져오기
        Preferences pref = Gdx.app.getPreferences("settings");
        String nick = pref.getString("nickname", "");
        if (nick.isEmpty()) {
            nick = "Player" + (int)(Math.random() * 10000);
        }
        
        myPlayerId = nick;  // 닉네임을 playerId로 사용
        isHost = true;  // 테스트용으로 방장

        addPlayer(myPlayerId);
        
        // ★ 초기 위치를 서버에 전송
        Player me = players.get(myPlayerId);
        if (me != null) {
            Vector2 pos = me.getPosition();
            Net.get().sendPlayerMove(myPlayerId, 0, 0, pos.x, pos.y);
            Gdx.app.log("LOBBY", "초기 위치 전송: " + pos.x + ", " + pos.y);
        }

        // UI 업데이트
        btnStart.setVisible(isHost);
        updatePlayerCount();
    }

    // ★ 플레이어 추가 (자신 or 다른 플레이어)
    private void addPlayer(String playerId) {
        addPlayerAt(playerId, -1, -1);  // -1이면 랜덤 위치
    }
    
    // ★★★ 추가: 위치 지정 플레이어 추가 ★★★
    private void addPlayerAt(String playerId, float x, float y) {
        // 이미 있으면 무시
        if (players.containsKey(playerId)) {
            return;
        }

        // 캐릭터 이미지
        Image playerImage = new Image(new TextureRegionDrawable(new TextureRegion(texIdle)));
        float charH = VH * 0.225f;  // GameScreen과 동일
        float charScale = charH / texIdle.getHeight();
        playerImage.setSize(texIdle.getWidth() * charScale, texIdle.getHeight() * charScale);

        // ★ 위치 설정 (x < 0 이면 랜덤)
        float posX, posY;
        if (x < 0 || y < 0) {
            posX = floorArea.x + (float)(Math.random() * (floorArea.width - playerImage.getWidth()));
            posY = floorArea.y + (float)(Math.random() * (floorArea.height - playerImage.getHeight()));
        } else {
            posX = x;
            posY = y;
        }
        
        playerImage.setPosition(posX, posY);
        world.addActor(playerImage);

        // Player 객체 생성 (Runner로 설정)
        Player player = new Player(playerId, PlayerRole.RUNNER, playerImage);
        player.setPosition(playerImage.getX(), playerImage.getY());
        player.setNickname(playerId);  // ★ 닉네임 설정

        // 애니메이션 설정
        Array<TextureRegion> rightFrames = new Array<>();
        for (Texture t : texRight) rightFrames.add(new TextureRegion(t));
        Animation<TextureRegion> walkRight = new Animation<>(0.09f, rightFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> leftFrames = new Array<>();
        for (Texture t : texLeft) leftFrames.add(new TextureRegion(t));
        Animation<TextureRegion> walkLeft = new Animation<>(0.09f, leftFrames, Animation.PlayMode.LOOP);

        player.setWalkAnimations(walkLeft, walkRight);
        player.setIdleTexture(texIdle);

        players.put(playerId, player);
        
        // ★ 초기 Ready 상태 적용 (서버에서 받은 값 또는 false)
        boolean initialReady = false;
        if (initialReadyStatus != null && initialReadyStatus.containsKey(playerId)) {
            initialReady = initialReadyStatus.get(playerId);
        }
        readyStatus.put(playerId, initialReady);
        
        Gdx.app.log("LOBBY", "Player added: " + playerId + " at (" + posX + ", " + posY + ") Ready=" + initialReady + " (total: " + players.size() + ")");
    }
    
    // ★★★ 추가: 서버에서 받은 플레이어들 생성 ★★★
    private void createPlayersFromServer() {
        if (initialPositions == null || initialPositions.isEmpty()) {
            Gdx.app.log("LOBBY", "No initial players from server");
            return;
        }
        
        for (Map.Entry<String, float[]> entry : initialPositions.entrySet()) {
            String playerId = entry.getKey();
            float[] pos = entry.getValue();
            float x = pos.length > 0 ? pos[0] : 0;
            float y = pos.length > 1 ? pos[1] : 0;
            
            addPlayerAt(playerId, x, y);
            
            // 내 캐릭터면 호스트 설정
            if (playerId.equals(myPlayerId)) {
                isHost = (initialPositions.size() == 1);  // 첫 번째 플레이어면 방장
            }
        }
        
        btnStart.setVisible(isHost);
        updatePlayerCount();
    }

    // ========== 입력 처리 ==========
    private void handleInput(float delta) {
        Player me = players.get(myPlayerId);
        if (me == null) return;

        float dx = 0f, dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;

        if (dx != 0f || dy != 0f) {
            movePlayer(me, dx, dy, delta);
            
            // ★★★ 수정: 로비에서도 이동 서버 전송 ★★★
            moveSendTimer += delta;
            if (moveSendTimer >= MOVE_SEND_INTERVAL) {
                float x = me.getPosition().x;
                float y = me.getPosition().y;
                Net.get().sendPlayerMove(myPlayerId, dx, dy, x, y);
                moveSendTimer = 0f;
            }
            wasMovingLastFrame = true;  // ★ 이동 중 플래그
        } else {
            me.stopMoving();
            
            // ★ 이동 → 정지 전환 시점에만 정지 메시지 전송!
            if (wasMovingLastFrame) {
                float x = me.getPosition().x;
                float y = me.getPosition().y;
                Net.get().sendPlayerMove(myPlayerId, 0, 0, x, y);  // dx=0, dy=0으로 정지 알림
                Gdx.app.log("LOBBY", "정지 메시지 전송: " + myPlayerId);
                wasMovingLastFrame = false;
            }
            
            moveSendTimer = 0f;
        }
        
        me.update(delta);
    }

    private void movePlayer(Player player, float dx, float dy, float delta) {
        // 정규화
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0f) {
            dx /= len;
            dy /= len;
        }

        float speed = player.getSpeed();
        float vx = dx * speed * delta;
        float vy = dy * speed * delta;

        Image img = player.getImage();
        float w = img.getWidth(), h = img.getHeight();
        float px = w * HERO_PAD, py = h * HERO_PAD;
        float hbW = w - 2f * px, hbH = h - 2f * py;
        
        float origX = img.getX();
        float origY = img.getY();

        // ★ X축 이동 시도
        float nx = origX + vx;
        Rectangle hb = new Rectangle(nx + px, origY + py, hbW, hbH);
        
        // X축 충돌 체크 (바닥 영역 + 다른 플레이어)
        if (!isInFloorArea(hb) || collideWithPlayer(player, hb) != null) {
            nx = origX;  // 충돌 시 X 이동 취소
        }

        // ★ Y축 이동 시도
        float ny = origY + vy;
        hb = new Rectangle(nx + px, ny + py, hbW, hbH);
        
        // Y축 충돌 체크 (바닥 영역 + 다른 플레이어)
        if (!isInFloorArea(hb) || collideWithPlayer(player, hb) != null) {
            ny = origY;  // 충돌 시 Y 이동 취소
        }
        
        // 위치 적용
        img.setX(nx);
        img.setY(ny);
        player.setPosition(nx, ny);
        
        // 방향 설정
        if (nx != origX || ny != origY) {
            player.updateDirection(dx, dy);
        } else {
            player.stopMoving();
        }
    }
    
    // ★ 바닥 영역 체크 (히트박스 기준)
    private boolean isInFloorArea(Rectangle hb) {
        // 히트박스의 4개 모서리가 모두 바닥 영역 안에 있는지 확인
        return floorArea.contains(hb.x, hb.y) &&
               floorArea.contains(hb.x + hb.width, hb.y) &&
               floorArea.contains(hb.x, hb.y + hb.height) &&
               floorArea.contains(hb.x + hb.width, hb.y + hb.height);
    }
    
    // ★ 다른 플레이어와 충돌 체크
    private Rectangle collideWithPlayer(Player me, Rectangle hb) {
        for (Player p : players.values()) {
            if (p == me) continue;  // 자기 자신 제외
            
            Image otherImg = p.getImage();
            if (otherImg == null) continue;
            
            // 다른 플레이어의 히트박스 계산
            float ow = otherImg.getWidth(), oh = otherImg.getHeight();
            float opx = ow * HERO_PAD, opy = oh * HERO_PAD;
            Rectangle otherHb = new Rectangle(
                otherImg.getX() + opx,
                otherImg.getY() + opy,
                ow - 2f * opx,
                oh - 2f * opy
            );
            
            // 충돌 검사
            if (hb.overlaps(otherHb)) {
                return otherHb;
            }
        }
        return null;
    }

    // ========== 버튼 액션 ==========
    private void toggleReady() {
        Boolean currentReady = readyStatus.get(myPlayerId);
        if (currentReady != null) {
            boolean newReady = !currentReady;
            readyStatus.put(myPlayerId, newReady);
            
            Gdx.app.log("LOBBY", "Ready: " + newReady);
            
            // ★★★ 수정: sendReady 호출 활성화 ★★★
            Net.get().sendReady(myPlayerId, newReady);
            
            updatePlayerCount();
        }
    }

    private void startGame() {
        if (!isHost) return;

        // ★ 서버가 Ready 지원하는 경우 → 모든 플레이어 Ready 체크
        if (serverSupportsReady) {
            boolean allReady = true;
            int totalPlayers = players.size();
            
            for (Map.Entry<String, Boolean> entry : readyStatus.entrySet()) {
                String playerId = entry.getKey();
                Boolean isReady = entry.getValue();
                
                // 방장 제외하고 다른 플레이어들이 Ready인지 확인
                if (!playerId.equals(myPlayerId) && !isReady) {
                    allReady = false;
                    break;
                }
            }

            if (!allReady) {
                Gdx.app.log("LOBBY", "모든 플레이어가 준비되지 않았습니다.");
                return;
            }
            
            // 4명 체크 (옵션)
            if (totalPlayers < 4) {
                Gdx.app.log("LOBBY", "플레이어가 4명 미만입니다. (현재 " + totalPlayers + "명)");
                // return; // 4명 강제하려면 주석 해제
            }
        }

        // ★ 서버가 Ready 미지원 또는 모두 Ready → 게임 시작
        if (players.size() >= 1) {
            Gdx.app.log("LOBBY", "게임 시작 요청!");
            Net.get().sendGameStart(roomId);
        } else {
            Gdx.app.log("LOBBY", "플레이어가 없습니다.");
        }
    }

    private void updatePlayerCount() {
        // ★ Ready 상태인 플레이어 수 세기
        int readyCount = 0;
        for (Boolean isReady : readyStatus.values()) {
            if (isReady) {
                readyCount++;
            }
        }

        Gdx.app.log("LOBBY", "Ready count: " + readyCount);

        Texture tex = texNumberCheck0;

        switch (readyCount) {
            case 0: tex = texNumberCheck0; break;  // 0명 Ready → 0:4
            case 1: tex = texNumberCheck1; break;  // 1명 Ready → 1:4
            case 2: tex = texNumberCheck2; break;  // 2명 Ready → 2:4
            case 3: tex = texNumberCheck3; break;  // 3명 Ready → 3:4
            case 4: tex = texNumberCheck4; break;  // 4명 Ready → 4:4
            default: tex = texNumberCheck0; break;
        }

        ((TextureRegionDrawable) imgNumberCheck.getDrawable()).setRegion(new TextureRegion(tex));
    }

    // ========== 헬퍼 ==========
    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    private ImageButton makeHoverButton(Texture up, Texture over) {
        TextureRegionDrawable upD = new TextureRegionDrawable(new TextureRegion(up));
        TextureRegionDrawable ovD = new TextureRegionDrawable(new TextureRegion(over));
        ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
        st.imageUp = upD;
        st.imageOver = ovD;
        st.imageDown = upD.tint(new Color(0.6f, 0.6f, 0.6f, 1));
        return new ImageButton(st);
    }

    private void positionButton(ImageButton btn, float targetW, float cx, float cy) {
        TextureRegionDrawable up = (TextureRegionDrawable) btn.getStyle().imageUp;
        float s = targetW / up.getMinWidth();
        float w = up.getMinWidth() * s, h = up.getMinHeight() * s;
        btn.getImageCell().size(w, h);
        btn.setSize(w, h);
        btn.setPosition(cx, cy, Align.center);
    }

    // ========== Screen 메서드 ==========
    @Override
    public void show() {
        // ★ 화면 활성화 시 리스너 재설정 (hide()에서 null로 설정되기 때문)
        setupNetworkListener();
        Gdx.app.log("LOBBY", "Screen shown, listener reset!");
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        stage.getViewport().apply(true);

        handleInput(delta);

        // ★ 모든 플레이어 업데이트 (애니메이션 재생)
        for (Player p : players.values()) {
            p.update(delta);
        }

        stage.act(delta);
        stage.draw();
        
        // ★ 닉네임 렌더링 (플레이어 머리 위)
        renderPlayerNicknames();
    }
    
    // ★ 닉네임 렌더링 메서드
    private void renderPlayerNicknames() {
        // 월드 좌표로 카메라 설정
        app.batch.setProjectionMatrix(stage.getCamera().combined);
        app.batch.begin();
        
        for (Player p : players.values()) {
            Image img = p.getImage();
            if (img == null) continue;
            
            float centerX = img.getX() + img.getWidth() / 2f;
            float topY = img.getY() + img.getHeight();
            
            // 닉네임 표시 (캐릭터 머리 위)
            String nickname = p.getNickname();
            if (nickname == null || nickname.isEmpty()) continue;
            
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = 
                new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, nickname);
            float textX = centerX - layout.width / 2f;
            float textY = topY + 30f + layout.height;
            
            // 닉네임 배경 (가독성용)
            font.setColor(com.badlogic.gdx.graphics.Color.BLACK);
            font.draw(app.batch, nickname, textX + 1, textY - 1);
            font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            font.draw(app.batch, nickname, textX, textY);
        }
        
        app.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        // 리스너 해제
        Net.get().setListener(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        texWaitroomOther.dispose();
        texWaitroomFloor.dispose();
        texNumberCheck0.dispose();
        texNumberCheck1.dispose();
        texNumberCheck2.dispose();
        texNumberCheck3.dispose();
        texNumberCheck4.dispose();
        texReadyUp.dispose();      // ★ Ready 버튼
        texReadyOver.dispose();
        texStartUp.dispose();
        texStartOver.dispose();
        texIdle.dispose();
        for (Texture t : texRight) t.dispose();
        for (Texture t : texLeft) t.dispose();
        if (font != null) font.dispose();  // ★ 폰트 해제
    }
}
