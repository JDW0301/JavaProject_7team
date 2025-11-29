package io.github.freeze.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Preferences;

import io.github.freeze.Core;
import io.github.freeze.game.*;
import io.github.freeze.net.Net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 얼음땡 게임 화면
 * - Chaser 1명: 공격(Q), Runner들을 얼릴 수 있음
 * - Runner 3명: 안개(Q), 대시(E), 해빙(F) 스킬 사용 가능
 */
public class GameScreen implements Screen {
    private static final int VW = 1280, VH = 960;

    // 거리 상수
    private static final float FREEZE_RANGE = 250f;      // ★ 공격 범위
    private static final float UNFREEZE_RANGE = 100f;    // 해빙 범위

    private final Core app;
    private final Stage stage;

    // 맵/골대
    private Texture texMap, texHoopL, texHoopR;
    private Image imgMap, imgHoopL, imgHoopR;

    // 장애물 충돌
    private final List<Rectangle> hoopLRects = new ArrayList<>();
    private final List<Rectangle> hoopRRects = new ArrayList<>();
    private static final float EPS = 0.5f;
    private static final float HERO_PAD = 0.12f;

    // 캐릭터 텍스처 (Runner용)
    private Texture texIdle;
    private Texture[] texRight = new Texture[8];
    private Texture[] texLeft  = new Texture[8];

    // Chaser 텍스처
    private Texture texChaserIdle;  // ★ Chaser idle (chaser1.png)
    private Texture[] texChaserLeft = new Texture[8];
    private Texture[] texChaserRight = new Texture[8];
    private Texture[] texChaserAttackL = new Texture[12];
    private Texture[] texChaserAttackR = new Texture[12];

    // Runner 대시 텍스처
    private Texture texRunnerDashL, texRunnerDashR;

    // 빙결 텍스처 [walkFrame][freezeFrame]
    private Texture[][] freezeLeftFrames = new Texture[8][5];
    private Texture[][] freezeRightFrames = new Texture[8][5];

    // 안개 텍스처
    private Texture[] texFoggy = new Texture[5];

    // 스킬 아이콘 텍스처
    private Texture texDashIcon;                      // Run.png
    private Texture[] texDashCooldown = new Texture[5]; // Run (1)~(5).png
    private Texture texFogIcon;                        // could.png
    private Texture[] texFogCooldown = new Texture[10]; // could (1)~(10).png
    private Texture texUnfreezeIcon;                   // iccbroken.png
    private Texture[] texUnfreezeCooldown = new Texture[2]; // iccbroken1~2.png

    // 해빙 게이지
    private Texture texGage1, texGage2;  // gage1.png, gage2.png
    
    // ★ 닉네임/게이지 표시용
    private com.badlogic.gdx.graphics.g2d.BitmapFont font;

    // 월드
    private Group world;
    private float worldW, worldH;
    private final Rectangle playArea = new Rectangle();

    // 플레이어들
    private Map<String, Player> players = new HashMap<>();
    private String myPlayerId;
    private Player myPlayer;

    // ★ 로컬 테스트 모드
    private boolean localTestMode = true;  // true면 테스트 모드
    private Player testRunner;  // 화살표로 조작할 Runner

    // UI
    private SkillUI skillUI;
    private FogEffect fogEffect;

    // 테스트/디버그
    private final ShapeRenderer sr = new ShapeRenderer();
    private boolean debugColliders = false;

    // 입력
    private float moveSendTimer = 0f;
    private static final float MOVE_SEND_INTERVAL = 0.05f; // 20fps로 위치 전송

    // ★ 서버에서 받은 역할 정보
    private Map<String, PlayerRole> serverRoles = new HashMap<>();

    public GameScreen(Core app) {
        this(app, null);  // 테스트 모드
    }

    // ★ 서버 연동용 생성자
    public GameScreen(Core app, Map<String, PlayerRole> roles) {
        this.app = app;
        this.stage = new Stage(new FitViewport(VW, VH), app.batch);
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        loadTextures();
        setupWorld();
        setupUI();
        setupNetworkListener();

        if (roles != null && !roles.isEmpty()) {
            // ★ 서버 모드: 역할 정보로 플레이어 생성
            localTestMode = false;
            serverRoles = roles;
            createPlayersFromServer(roles);
        } else {
            // ★ 테스트 모드: 로컬 플레이어 생성
            localTestMode = true;
            createTestPlayers();
        }
    }

    // ========== 텍스처 로딩 ==========
    private void loadTextures() {
        // 맵
        texMap   = load("images/map.png");
        texHoopL = load("images/basketballL.png");
        texHoopR = load("images/basketballR.png");

        // Runner 기본 텍스처
        texIdle = load("images/Front_C.png");
        for (int i = 0; i < 8; i++) {
            texRight[i] = load("images/Right_C" + (i + 1) + ".png");
            texLeft[i] = load("images/Left_C" + (i + 1) + ".png");
        }

        // Chaser 텍스처
        texChaserIdle = load("images/chaser1.png");  // ★ Chaser idle
        for (int i = 0; i < 8; i++) {
            texChaserLeft[i] = load("images/Chaser_Left" + (i + 1) + ".png");
            texChaserRight[i] = load("images/Chaser_Right" + (i + 1) + ".png");
        }
        for (int i = 0; i < 12; i++) {
            texChaserAttackL[i] = load("images/Chaser_attack_L" + (i + 1) + ".png");
            texChaserAttackR[i] = load("images/Chaser_attack_R" + (i + 1) + ".png");
        }

        // Runner 대시
        texRunnerDashL = load("images/L_run.png");
        texRunnerDashR = load("images/R_run.png");

        // 빙결 프레임
        for (int walkFrame = 1; walkFrame <= 8; walkFrame++) {
            for (int freezeFrame = 1; freezeFrame <= 5; freezeFrame++) {
                freezeLeftFrames[walkFrame - 1][freezeFrame - 1] =
                    load("images/L_motion" + walkFrame + "_F" + freezeFrame + ".png");
                freezeRightFrames[walkFrame - 1][freezeFrame - 1] =
                    load("images/R_motion" + walkFrame + "_F" + freezeFrame + ".png");
            }
        }

        // 안개
        for (int i = 0; i < 5; i++) {
            texFoggy[i] = load("images/Foggy" + (i + 1) + ".png");
        }

        // 스킬 아이콘 - 대시
        texDashIcon = load("images/Run.png");
        for (int i = 0; i < 5; i++) {
            texDashCooldown[i] = load("images/Run (" + (i + 1) + ").png");
        }

        // 스킬 아이콘 - 안개
        texFogIcon = load("images/could.png");
        for (int i = 0; i < 10; i++) {
            texFogCooldown[i] = load("images/could (" + (i + 1) + ").png");
        }

        // 스킬 아이콘 - 해빙
        texUnfreezeIcon = load("images/iccbroken.png");
        texUnfreezeCooldown[0] = load("images/iccbroken1.png");
        texUnfreezeCooldown[1] = load("images/iccbroken2.png");

        // 해빙 게이지
        texGage1 = load("images/gage1.png");
        texGage2 = load("images/gage2.png");
        
        // ★ 폰트 생성
        font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        font.getData().setScale(1.2f);
    }

    // ========== 월드 설정 ==========
    private void setupWorld() {
        world = new Group();
        stage.addActor(world);

        // 맵
        imgMap = new Image(new TextureRegionDrawable(new TextureRegion(texMap)));
        world.addActor(imgMap);

        // 골대
        imgHoopL = new Image(new TextureRegionDrawable(new TextureRegion(texHoopL)));
        imgHoopR = new Image(new TextureRegionDrawable(new TextureRegion(texHoopR)));
        world.addActor(imgHoopL);
        world.addActor(imgHoopR);

        layout();
        updateHoopBlocks();
        updatePlayArea();
    }

    private void layout() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // 맵을 뷰포트보다 크게(스크롤 생기게)
        float tw = texMap.getWidth(), th = texMap.getHeight();
        float scale = Math.max(vw / tw, vh / th) * 1.5f;  // 1.5배 확대
        worldW = tw * scale;
        worldH = th * scale;
        imgMap.setSize(worldW, worldH);
        imgMap.setPosition(0, 0);

        // 골대
        float hoopH = worldH * 0.372f;
        float lR = (float) texHoopL.getWidth() / texHoopL.getHeight();
        float rR = (float) texHoopR.getWidth() / texHoopR.getHeight();
        imgHoopL.setSize(hoopH * lR, hoopH);
        imgHoopR.setSize(hoopH * rR, hoopH);
        imgHoopL.setPosition(worldW * 0.182f, worldH * 0.55f, Align.center);
        imgHoopR.setPosition(worldW * 0.8225f, worldH * 0.55f, Align.center);
    }

    private void updateHoopBlocks() {
        hoopLRects.clear();
        hoopRRects.clear();

        float xL = imgHoopL.getX(), yL = imgHoopL.getY(), wL = imgHoopL.getWidth(), hL = imgHoopL.getHeight();
        float xR = imgHoopR.getX(), yR = imgHoopR.getY(), wR = imgHoopR.getWidth(), hR = imgHoopR.getHeight();

        // 오른쪽 골대
        addRect(hoopRRects, xR, yR, wR, hR, 0.680f, 0.117f, 0.070f, 0.510f);
        addRect(hoopRRects, xR, yR, wR, hR, 0.460f, 0.510f, 0.040f, 0.330f);
        addRect(hoopRRects, xR, yR, wR, hR, 0.480f, 0.600f, 0.115f, 0.100f);
        addRect(hoopRRects, xR, yR, wR, hR, 0.585f, 0.600f, 0.098f, 0.050f);
        addRect(hoopRRects, xR, yR, wR, hR, 0.200f, 0.470f, 0.200f, 0.205f);

        // 왼쪽 골대
        mirAdd(hoopLRects, xL, yL, wL, hL, 0.680f, 0.117f, 0.070f, 0.510f);
        mirAdd(hoopLRects, xL, yL, wL, hL, 0.460f, 0.510f, 0.040f, 0.330f);
        mirAdd(hoopLRects, xL, yL, wL, hL, 0.480f, 0.600f, 0.115f, 0.100f);
        mirAdd(hoopLRects, xL, yL, wL, hL, 0.585f, 0.600f, 0.098f, 0.050f);
        mirAdd(hoopLRects, xL, yL, wL, hL, 0.200f, 0.470f, 0.200f, 0.205f);
    }

    private void addRect(List<Rectangle> list, float bx, float by, float bw, float bh,
                         float ax, float ay, float aw, float ah) {
        list.add(new Rectangle(bx + bw * ax, by + bh * ay, bw * aw, bh * ah));
    }

    private void mirAdd(List<Rectangle> list, float bx, float by, float bw, float bh,
                        float axR, float ay, float aw, float ah) {
        float axL = 1f - axR - aw;
        list.add(new Rectangle(bx + bw * axL, by + bh * ay, bw * aw, bh * ah));
    }

    private void updatePlayArea() {
        float x = imgMap.getX(), y = imgMap.getY();
        float w = imgMap.getWidth(), h = imgMap.getHeight();

        final float LM = 0.066f, RM = 0.066f, TM = 0.090f, BM = 0.090f;
        playArea.set(x + w * LM, y + h * BM, w * (1f - LM - RM), h * (1f - TM - BM));
    }

    // ========== UI 설정 ==========
    private void setupUI() {
        // 스킬 UI (아이콘 크기 키움)
        skillUI = new SkillUI(120f, 15f);  // 80f → 120f

        // 스킬 아이콘 애니메이션 - 안개 (쿨타임만)
        Array<TextureRegion> fogCooldownFrames = new Array<>();
        for (Texture t : texFogCooldown) fogCooldownFrames.add(new TextureRegion(t));
        skillUI.setFogIconAnimation(
            new TextureRegion(texFogIcon),  // 기본 이미지
            new Animation<>(1.0f, fogCooldownFrames, Animation.PlayMode.LOOP_REVERSED)  // 역순 재생
        );

        // 스킬 아이콘 애니메이션 - 대시 (쿨타임만)
        Array<TextureRegion> dashCooldownFrames = new Array<>();
        for (Texture t : texDashCooldown) dashCooldownFrames.add(new TextureRegion(t));
        skillUI.setDashIconAnimation(
            new TextureRegion(texDashIcon),  // 기본 이미지
            new Animation<>(1.0f, dashCooldownFrames, Animation.PlayMode.LOOP_REVERSED)  // 역순 재생
        );

        // 스킬 아이콘 애니메이션 - 해빙 (쿨타임만)
        Array<TextureRegion> unfreezeCooldownFrames = new Array<>();
        for (Texture t : texUnfreezeCooldown) unfreezeCooldownFrames.add(new TextureRegion(t));
        skillUI.setUnfreezeIconAnimation(
            new TextureRegion(texUnfreezeIcon),  // 기본 이미지
            new Animation<>(1.0f, unfreezeCooldownFrames, Animation.PlayMode.LOOP_REVERSED)  // 역순 재생
        );

        skillUI.setUnfreezeGaugeTextures(texGage1, texGage2);

        // 안개 효과
        Array<TextureRegion> foggyFrames = new Array<>();
        for (Texture t : texFoggy) foggyFrames.add(new TextureRegion(t));
        fogEffect = new FogEffect(new Animation<>(0.1f, foggyFrames, Animation.PlayMode.NORMAL));
    }

    // ========== 네트워크 설정 ==========
    private void setupNetworkListener() {
        Net.get().setListener(new Net.Listener() {
            @Override
            public void onGameStart(String roleJson) {
                // TODO: 역할 파싱 및 플레이어 생성
                Gdx.app.log("GAME", "Game started! Role: " + roleJson);
            }

            @Override
            public void onPlayerMove(String playerId, float x, float y) {
                Player p = players.get(playerId);
                if (p != null && !playerId.equals(myPlayerId)) {
                    p.setPosition(x, y);
                }
            }

            @Override
            public void onPlayerFreeze(String targetId, String attackerId) {
                Player target = players.get(targetId);
                if (target != null) {
                    target.startFreeze();
                    Gdx.app.log("GAME", "서버: " + targetId + " 빙결됨 (by " + attackerId + ")");
                }
            }

            @Override
            public void onPlayerUnfreeze(String targetId, String unfreezeId) {
                Player target = players.get(targetId);
                if (target != null) {
                    target.startUnfreeze();
                    Gdx.app.log("GAME", "서버: " + targetId + " 해빙됨 (by " + unfreezeId + ")");
                }
            }

            @Override
            public void onSkillUsed(String playerId, String skillType) {
                Player p = players.get(playerId);
                if (p == null) return;

                switch (skillType) {
                    case "dash":
                        p.useDashSkill();
                        Gdx.app.log("GAME", playerId + " 대시 스킬!");
                        break;
                    case "fog":
                        p.useFogSkill();
                        // ★ Chaser 화면에 안개 표시
                        if (myPlayer != null && myPlayer.getRole() == PlayerRole.CHASER) {
                            fogEffect.activate();
                            Gdx.app.log("GAME", playerId + " 안개 스킬! Chaser 화면에 안개!");
                        }
                        break;
                }
            }

            @Override
            public void onFogActivated(String playerId) {
                // ★ Chaser 화면에만 안개 표시 (서버가 별도로 보내는 경우)
                if (myPlayer != null && myPlayer.getRole() == PlayerRole.CHASER) {
                    fogEffect.activate();
                    Gdx.app.log("GAME", "안개 활성화!");
                }
            }
        });
    }

    // ========== 테스트용 플레이어 생성 ==========
    private void createTestPlayers() {
        float heroH = worldH * 0.15f;
        
        // ★ 내 캐릭터: CHASER (WASD + Q로 공격)
        myPlayerId = "chaser1";
        
        // ★ Chaser는 chaser1.png로 시작
        Image chaserImage = new Image(new TextureRegionDrawable(new TextureRegion(texChaserIdle)));
        float sChaser = heroH / texChaserIdle.getHeight();
        chaserImage.setSize(texChaserIdle.getWidth() * sChaser, texChaserIdle.getHeight() * sChaser);
        chaserImage.setPosition(worldW * 0.3f - chaserImage.getWidth() / 2f,
                              worldH * 0.5f - chaserImage.getHeight() / 2f);
        world.addActor(chaserImage);

        myPlayer = new Player(myPlayerId, PlayerRole.CHASER, chaserImage);
        myPlayer.setPosition(chaserImage.getX(), chaserImage.getY());
        myPlayer.setNickname("술래");  // ★ 테스트용 닉네임

        // Chaser 애니메이션 설정
        Array<TextureRegion> chaserRightFrames = new Array<>();
        for (Texture t : texChaserRight) chaserRightFrames.add(new TextureRegion(t));
        Animation<TextureRegion> chaserWalkRight = new Animation<>(0.09f, chaserRightFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> chaserLeftFrames = new Array<>();
        for (Texture t : texChaserLeft) chaserLeftFrames.add(new TextureRegion(t));
        Animation<TextureRegion> chaserWalkLeft = new Animation<>(0.09f, chaserLeftFrames, Animation.PlayMode.LOOP);

        myPlayer.setWalkAnimations(chaserWalkLeft, chaserWalkRight);
        myPlayer.setIdleTexture(texChaserIdle);  // ★ Chaser idle 이미지

        // Chaser 공격 애니메이션
        Array<TextureRegion> attackL = new Array<>();
        for (Texture t : texChaserAttackL) attackL.add(new TextureRegion(t));
        Array<TextureRegion> attackR = new Array<>();
        for (Texture t : texChaserAttackR) attackR.add(new TextureRegion(t));
        myPlayer.setChaserAttackAnimations(
            new Animation<>(0.25f, attackL, Animation.PlayMode.NORMAL),
            new Animation<>(0.25f, attackR, Animation.PlayMode.NORMAL)
        );

        players.put(myPlayerId, myPlayer);

        // ★ 테스트용 Runner (화살표로 조작)
        if (localTestMode) {
            String runnerId = "runner1";
            
            Image runnerImage = new Image(new TextureRegionDrawable(new TextureRegion(texIdle)));
            float sRunner = heroH / texIdle.getHeight();
            runnerImage.setSize(texIdle.getWidth() * sRunner, texIdle.getHeight() * sRunner);
            runnerImage.setPosition(worldW * 0.7f - runnerImage.getWidth() / 2f,
                                  worldH * 0.5f - runnerImage.getHeight() / 2f);
            world.addActor(runnerImage);

            testRunner = new Player(runnerId, PlayerRole.RUNNER, runnerImage);
            testRunner.setPosition(runnerImage.getX(), runnerImage.getY());
            testRunner.setNickname("도망자");  // ★ 테스트용 닉네임

            // Runner 애니메이션 설정
            Array<TextureRegion> rightFrames = new Array<>();
            for (Texture t : texRight) rightFrames.add(new TextureRegion(t));
            Animation<TextureRegion> walkRight = new Animation<>(0.09f, rightFrames, Animation.PlayMode.LOOP);

            Array<TextureRegion> leftFrames = new Array<>();
            for (Texture t : texLeft) leftFrames.add(new TextureRegion(t));
            Animation<TextureRegion> walkLeft = new Animation<>(0.09f, leftFrames, Animation.PlayMode.LOOP);

            testRunner.setWalkAnimations(walkLeft, walkRight);
            testRunner.setIdleTexture(texIdle);

            // 대시 애니메이션
            Array<TextureRegion> dashL = new Array<>();
            dashL.add(new TextureRegion(texRunnerDashL));
            Array<TextureRegion> dashR = new Array<>();
            dashR.add(new TextureRegion(texRunnerDashR));
            testRunner.setRunnerDashAnimations(
                new Animation<>(1f, dashL, Animation.PlayMode.NORMAL),
                new Animation<>(1f, dashR, Animation.PlayMode.NORMAL)
            );

            // 빙결 프레임 설정
            testRunner.setFreezeFrames(freezeLeftFrames, freezeRightFrames);

            players.put(runnerId, testRunner);
            
            Gdx.app.log("TEST", "=== 로컬 테스트 모드 ===");
            Gdx.app.log("TEST", "Chaser: WASD 이동, Q 공격");
            Gdx.app.log("TEST", "Runner: 화살표 이동, 1 안개, 2 대시, 3 해동");
        }

        chaserImage.toFront();
        centerCameraOnPlayer(myPlayer);
    }

    // ========== 서버 모드: 역할 정보로 플레이어 생성 ==========
    private void createPlayersFromServer(Map<String, PlayerRole> roles) {
        // ★ Preferences에서 내 닉네임 가져오기
        Preferences pref = Gdx.app.getPreferences("settings");
        String myNick = pref.getString("nickname", "");
        if (myNick.isEmpty()) {
            myNick = "Player" + (int)(Math.random() * 10000);
        }
        myPlayerId = myNick;

        Gdx.app.log("GAME", "서버 모드 - 플레이어 생성 시작 (내 ID: " + myPlayerId + ")");

        for (Map.Entry<String, PlayerRole> entry : roles.entrySet()) {
            String playerId = entry.getKey();
            PlayerRole role = entry.getValue();

            Player player = createPlayerWithRole(playerId, role);
            players.put(playerId, player);

            if (playerId.equals(myPlayerId)) {
                myPlayer = player;
                Gdx.app.log("GAME", "내 캐릭터 생성: " + playerId + " (" + role + ")");
            } else {
                Gdx.app.log("GAME", "다른 플레이어 생성: " + playerId + " (" + role + ")");
            }
        }

        if (myPlayer != null) {
            myPlayer.getImage().toFront();
            centerCameraOnPlayer(myPlayer);
        }
    }

    // ★ 역할에 따라 플레이어 생성
    private Player createPlayerWithRole(String playerId, PlayerRole role) {
        float heroH = worldH * 0.15f;
        
        // 랜덤 시작 위치
        float startX = worldW * (0.2f + (float)Math.random() * 0.6f);
        float startY = worldH * (0.3f + (float)Math.random() * 0.4f);

        if (role == PlayerRole.CHASER) {
            // ★ Chaser 생성
            Image chaserImage = new Image(new TextureRegionDrawable(new TextureRegion(texChaserIdle)));
            float scale = heroH / texChaserIdle.getHeight();
            chaserImage.setSize(texChaserIdle.getWidth() * scale, texChaserIdle.getHeight() * scale);
            chaserImage.setPosition(startX, startY);
            world.addActor(chaserImage);

            Player player = new Player(playerId, PlayerRole.CHASER, chaserImage);
            player.setPosition(startX, startY);
            player.setNickname(playerId);  // ★ 서버에서 받은 ID를 닉네임으로

            // Chaser 애니메이션
            Array<TextureRegion> chaserRightFrames = new Array<>();
            for (Texture t : texChaserRight) chaserRightFrames.add(new TextureRegion(t));
            Animation<TextureRegion> chaserWalkRight = new Animation<>(0.09f, chaserRightFrames, Animation.PlayMode.LOOP);

            Array<TextureRegion> chaserLeftFrames = new Array<>();
            for (Texture t : texChaserLeft) chaserLeftFrames.add(new TextureRegion(t));
            Animation<TextureRegion> chaserWalkLeft = new Animation<>(0.09f, chaserLeftFrames, Animation.PlayMode.LOOP);

            player.setWalkAnimations(chaserWalkLeft, chaserWalkRight);
            player.setIdleTexture(texChaserIdle);

            // Chaser 공격 애니메이션
            Array<TextureRegion> attackL = new Array<>();
            for (Texture t : texChaserAttackL) attackL.add(new TextureRegion(t));
            Array<TextureRegion> attackR = new Array<>();
            for (Texture t : texChaserAttackR) attackR.add(new TextureRegion(t));
            player.setChaserAttackAnimations(
                new Animation<>(0.25f, attackL, Animation.PlayMode.NORMAL),
                new Animation<>(0.25f, attackR, Animation.PlayMode.NORMAL)
            );

            return player;

        } else {
            // ★ Runner 생성
            Image runnerImage = new Image(new TextureRegionDrawable(new TextureRegion(texIdle)));
            float scale = heroH / texIdle.getHeight();
            runnerImage.setSize(texIdle.getWidth() * scale, texIdle.getHeight() * scale);
            runnerImage.setPosition(startX, startY);
            world.addActor(runnerImage);

            Player player = new Player(playerId, PlayerRole.RUNNER, runnerImage);
            player.setPosition(startX, startY);
            player.setNickname(playerId);  // ★ 서버에서 받은 ID를 닉네임으로

            // Runner 애니메이션
            Array<TextureRegion> rightFrames = new Array<>();
            for (Texture t : texRight) rightFrames.add(new TextureRegion(t));
            Animation<TextureRegion> walkRight = new Animation<>(0.09f, rightFrames, Animation.PlayMode.LOOP);

            Array<TextureRegion> leftFrames = new Array<>();
            for (Texture t : texLeft) leftFrames.add(new TextureRegion(t));
            Animation<TextureRegion> walkLeft = new Animation<>(0.09f, leftFrames, Animation.PlayMode.LOOP);

            player.setWalkAnimations(walkLeft, walkRight);
            player.setIdleTexture(texIdle);

            // 대시 애니메이션
            Array<TextureRegion> dashL = new Array<>();
            dashL.add(new TextureRegion(texRunnerDashL));
            Array<TextureRegion> dashR = new Array<>();
            dashR.add(new TextureRegion(texRunnerDashR));
            player.setRunnerDashAnimations(
                new Animation<>(1f, dashL, Animation.PlayMode.NORMAL),
                new Animation<>(1f, dashR, Animation.PlayMode.NORMAL)
            );

            // 빙결 프레임
            player.setFreezeFrames(freezeLeftFrames, freezeRightFrames);

            return player;
        }
    }

    // ★ 다른 플레이어 추가 (게임 중 입장)
    public void addOtherPlayer(String playerId, PlayerRole role) {
        if (players.containsKey(playerId)) return;

        Player player = createPlayerWithRole(playerId, role);
        players.put(playerId, player);
        Gdx.app.log("GAME", "플레이어 추가: " + playerId + " (" + role + ")");
    }

    // ★ 플레이어 제거 (퇴장)
    public void removePlayer(String playerId) {
        Player player = players.remove(playerId);
        if (player != null && player.getImage() != null) {
            player.getImage().remove();
            Gdx.app.log("GAME", "플레이어 제거: " + playerId);
        }
    }

    // ========== 입력 처리 ==========
    private void handleInput(float delta) {
        if (myPlayer == null) return;
        
        // ★ 스킬 입력은 항상 처리 (canMove 상관없이)
        if (myPlayer.getRole() == PlayerRole.RUNNER) {
            handleRunnerSkills();
        } else if (myPlayer.getRole() == PlayerRole.CHASER) {
            handleChaserSkills();
        }
        
        // 이동은 canMove일 때만
        if (!myPlayer.canMove()) return;

        float dx = 0f, dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;

        // 이동 처리
        if (dx != 0f || dy != 0f) {
            movePlayerWithCollision(myPlayer, dx, dy, delta);

            // ★ 테스트 모드가 아닐 때만 서버 전송
            if (!localTestMode) {
                moveSendTimer += delta;
                if (moveSendTimer >= MOVE_SEND_INTERVAL) {
                    Net.get().sendPlayerMove(myPlayerId, dx, dy);
                    moveSendTimer = 0f;
                }
            }
        } else {
            // 이동 안 할 때 velocity 초기화 (애니메이션 멈춤)
            myPlayer.stopMoving();
        }
        
        // ★ 테스트 모드: testRunner 조작
        if (localTestMode && testRunner != null) {
            handleTestRunnerInput(delta);
        }
    }
    
    // ★ 테스트용 Runner 조작 (화살표 키)
    private void handleTestRunnerInput(float delta) {
        // 이동은 canMove일 때만
        if (testRunner.canMove()) {
            float dx = 0f, dy = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1f;

            if (dx != 0f || dy != 0f) {
                movePlayerWithCollision(testRunner, dx, dy, delta);
            } else {
                testRunner.stopMoving();
            }
        }
        
        // ★ 스킬은 항상 처리 (얼린 상태에서도 해동 가능)
        
        // NUMPAD 1 또는 1: 안개 스킬
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            testRunner.useFogSkill();
            if (testRunner.getFogSkill().isActive()) {
                // Chaser 화면에 안개 표시
                fogEffect.activate();
                Gdx.app.log("TEST", "★ Runner 안개 스킬! Chaser 화면에 안개!");
            } else {
                Gdx.app.log("TEST", "안개 쿨타임 중...");
            }
        }
        
        // NUMPAD 2 또는 2: 대시 스킬
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            testRunner.useDashSkill();
            if (testRunner.getDashSkill().isActive()) {
                Gdx.app.log("TEST", "★ Runner 대시 스킬!");
            } else {
                Gdx.app.log("TEST", "대시 쿨타임 중...");
            }
        }
        
        // NUMPAD 3 또는 3: 해동 (자기 자신)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            if (testRunner.isFrozen() || testRunner.getState() == PlayerState.FREEZING) {
                testRunner.startUnfreeze();
                Gdx.app.log("TEST", "★ Runner 해동!");
            } else {
                Gdx.app.log("TEST", "얼지 않은 상태");
            }
        }
    }

    private void handleRunnerSkills() {
        // Q: 안개
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            myPlayer.useFogSkill();
            if (myPlayer.getFogSkill().isActive()) {
                // ★ 서버 모드일 때 서버로 전송
                if (!localTestMode) {
                    Net.get().sendSkillUse("fog");
                } else {
                    // ★ 테스트 모드: Chaser 화면에 안개 표시 (Chaser가 myPlayer이므로)
                    // 실제로는 다른 플레이어(Chaser)에게 전달되어야 함
                    // 테스트 모드에서는 myPlayer가 Chaser이므로 이 코드는 실행 안 됨
                }
                Gdx.app.log("GAME", "Runner 안개 스킬 사용!");
            }
        }

        // E: 대시
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            myPlayer.useDashSkill();
            if (myPlayer.getDashSkill().isActive()) {
                // ★ 서버 모드일 때 서버로 전송
                if (!localTestMode) {
                    Net.get().sendSkillUse("dash");
                }
                Gdx.app.log("GAME", "Runner 대시 스킬 사용!");
            }
        }

        // F: 해빙
        if (Gdx.input.isKeyPressed(Input.Keys.F)) {
            // 가까운 얼린 플레이어 찾기
            Player frozenTarget = findNearestFrozenPlayer();
            if (frozenTarget != null && myPlayer.getState() != PlayerState.UNFREEZING_TARGET) {
                myPlayer.startUnfreezeTarget(frozenTarget);
                Gdx.app.log("GAME", "Runner " + frozenTarget.getPlayerId() + " 해빙 시도 중...");
            }
        } else {
            // F키를 떼면 해빙 취소
            if (myPlayer.getState() == PlayerState.UNFREEZING_TARGET) {
                myPlayer.cancelUnfreeze();
                Gdx.app.log("GAME", "Runner 해빙 취소");
            }
        }
    }

    private void handleChaserSkills() {
        // Q: 공격 (누르고 있는 동안만)
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            // 공격 시작/유지
            if (!myPlayer.isAttacking()) {
                myPlayer.startAttack();
                Gdx.app.log("GAME", "Chaser Q 공격 시작!");
            }

            // 범위 내 Runner 빙결 시작/유지
            for (Player p : players.values()) {
                if (p.getRole() == PlayerRole.RUNNER) {
                    float dist = myPlayer.distanceTo(p);
                    
                    if (dist <= FREEZE_RANGE) {
                        // 범위 안 → 빙결 시작/유지
                        if (!p.isFrozen() && p.getState() != PlayerState.FREEZING) {
                            // ★ 테스트/서버 모드 모두 빙결 시작
                            p.startFreeze();
                            Gdx.app.log("GAME", "★ " + p.getPlayerId() + " 빙결 시작!");
                            
                            // ★ 서버 모드일 때 서버에도 전송
                            if (!localTestMode) {
                                Net.get().sendFreeze(p.getPlayerId());
                            }
                        }
                    }
                }
            }
        } else {
            // Q 뗌 → 공격 멈춤
            if (myPlayer.isAttacking()) {
                myPlayer.cancelAttack();
                Gdx.app.log("GAME", "Chaser Q 공격 멈춤!");
                
                // ★ 빙결 중인 Runner들 해빙 시작 (테스트/서버 모드 모두)
                for (Player p : players.values()) {
                    if (p.getRole() == PlayerRole.RUNNER && p.getState() == PlayerState.FREEZING) {
                        p.startUnfreeze();
                        Gdx.app.log("GAME", "★ " + p.getPlayerId() + " 해빙 시작!");
                        
                        // ★ 서버 모드일 때 서버에도 전송
                        if (!localTestMode) {
                            Net.get().sendUnfreeze(p.getPlayerId());
                        }
                    }
                }
            }
        }
    }

    private Player findNearestFrozenPlayer() {
        Player nearest = null;
        float minDist = UNFREEZE_RANGE;

        for (Player p : players.values()) {
            if (p.isFrozen() && !p.getPlayerId().equals(myPlayerId)) {
                float dist = myPlayer.distanceTo(p);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = p;
                }
            }
        }

        return nearest;
    }

    // ========== 이동 & 충돌 ==========
    private void movePlayerWithCollision(Player player, float dx, float dy, float delta) {
        // 정규화
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0f) {
            dx /= len;
            dy /= len;
        }

        // ★ 맵 크기 1.5배 보정
        float speed = player.getSpeed() * 1.5f;
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
        
        // X축 충돌 시 → X 이동만 취소
        if (collideWithObstacle(hb) != null || collideWithPlayer(player, hb) != null) {
            nx = origX;
        }
        
        // 맵 경계 체크 (X)
        float left = playArea.x - px;
        float right = playArea.x + playArea.width - (w - px);
        nx = MathUtils.clamp(nx, left, right);

        // ★ Y축 이동 시도
        float ny = origY + vy;
        hb = new Rectangle(nx + px, ny + py, hbW, hbH);
        
        // Y축 충돌 시 → Y 이동만 취소
        if (collideWithObstacle(hb) != null || collideWithPlayer(player, hb) != null) {
            ny = origY;
        }
        
        // 맵 경계 체크 (Y)
        float bottom = playArea.y - py;
        float top = playArea.y + playArea.height - (h - py);
        ny = MathUtils.clamp(ny, bottom, top);
        
        // 위치 적용
        img.setX(nx);
        img.setY(ny);
        player.setPosition(nx, ny);
        
        // ★ 방향 설정 (애니메이션용)
        if (nx != origX || ny != origY) {
            player.updateDirection(dx, dy);  // 이동 중
        } else {
            player.stopMoving();  // 완전히 막힘
        }
    }

    private Rectangle collideWithObstacle(Rectangle hb) {
        for (Rectangle r : hoopLRects) if (hb.overlaps(r)) return r;
        for (Rectangle r : hoopRRects) if (hb.overlaps(r)) return r;
        return null;
    }
    
    // ★ 다른 플레이어와 충돌 체크
    private Rectangle collideWithPlayer(Player me, Rectangle hb) {
        for (Player p : players.values()) {
            if (p == me) continue;  // 자기 자신 제외
            
            // 플레이어 히트박스 계산
            Image img = p.getImage();
            if (img == null) continue;
            
            float pw = img.getWidth(), ph = img.getHeight();
            float ppx = pw * HERO_PAD, ppy = ph * HERO_PAD;
            Rectangle playerRect = new Rectangle(
                img.getX() + ppx, 
                img.getY() + ppy, 
                pw - 2f * ppx, 
                ph - 2f * ppy
            );
            
            if (hb.overlaps(playerRect)) {
                return playerRect;
            }
        }
        return null;
    }

    // ========== 카메라 ==========
    private void centerCameraOnPlayer(Player player) {
        if (player == null) return;

        float vw = stage.getViewport().getWorldWidth(), vh = stage.getViewport().getWorldHeight();
        float halfW = vw / 2f, halfH = vh / 2f;
        Vector2 pos = player.getPosition();
        Image img = player.getImage();
        float cx = pos.x + img.getWidth() / 2f;
        float cy = pos.y + img.getHeight() / 2f;
        float camX = MathUtils.clamp(cx, halfW, Math.max(halfW, worldW - halfW));
        float camY = MathUtils.clamp(cy, halfH, Math.max(halfH, worldH - halfH));
        stage.getCamera().position.set(camX, camY, 0);
        stage.getCamera().update();
    }

    // ========== 헬퍼 ==========
    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    // ========== Screen 메서드 ==========
    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        stage.getViewport().apply(true);

        // 입력 처리
        handleInput(delta);

        // 플레이어 업데이트
        for (Player p : players.values()) {
            p.update(delta);
        }

        // 카메라
        if (myPlayer != null) {
            centerCameraOnPlayer(myPlayer);
        }

        // UI 업데이트
        if (skillUI != null) skillUI.update(delta);
        if (fogEffect != null) fogEffect.update(delta);

        // 그리기
        stage.act(delta);
        stage.draw();
        
        // ★ 닉네임 및 공격 게이지 렌더링 (월드 좌표)
        renderPlayerOverlays();

        // 스킬 UI 렌더링 (화면 고정 - HUD)
        if (myPlayer != null && myPlayer.getRole() == PlayerRole.RUNNER) {
            // 카메라를 화면 좌표로 전환해서 UI 렌더링
            app.batch.setProjectionMatrix(app.batch.getProjectionMatrix().idt());
            app.batch.getProjectionMatrix().setToOrtho2D(0, 0,
                stage.getViewport().getScreenWidth(),
                stage.getViewport().getScreenHeight());

            skillUI.renderRunnerSkills(app.batch, myPlayer,
                stage.getViewport().getScreenWidth(),
                stage.getViewport().getScreenHeight());
        }

        // 안개 효과 (Chaser 전용)
        if (myPlayer != null && myPlayer.getRole() == PlayerRole.CHASER) {
            app.batch.setProjectionMatrix(app.batch.getProjectionMatrix().idt());
            app.batch.getProjectionMatrix().setToOrtho2D(0, 0,
                stage.getViewport().getScreenWidth(),
                stage.getViewport().getScreenHeight());

            fogEffect.render(app.batch,
                stage.getViewport().getScreenWidth(),
                stage.getViewport().getScreenHeight());
        }
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        layout();
        if (myPlayer != null) {
            centerCameraOnPlayer(myPlayer);
        }
    }
    
    // ========== 닉네임 및 공격 게이지 렌더링 ==========
    private void renderPlayerOverlays() {
        // 월드 좌표로 카메라 설정
        app.batch.setProjectionMatrix(stage.getCamera().combined);
        app.batch.begin();
        
        for (Player p : players.values()) {
            Image img = p.getImage();
            if (img == null) continue;
            
            float centerX = img.getX() + img.getWidth() / 2f;
            float topY = img.getY() + img.getHeight();
            
            // ★ 닉네임 표시 (캐릭터 머리 위)
            String nickname = p.getNickname();
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, nickname);
            float textX = centerX - layout.width / 2f;
            float textY = topY + 25f + layout.height;
            
            // 닉네임 배경 (가독성용)
            font.setColor(com.badlogic.gdx.graphics.Color.BLACK);
            font.draw(app.batch, nickname, textX + 1, textY - 1);
            font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            font.draw(app.batch, nickname, textX, textY);
        }
        
        // ★ 해빙 게이지 (해빙 중인 타겟 머리 위에 표시)
        for (Player p : players.values()) {
            if (p.isUnfreezingTarget()) {
                Player target = p.getUnfreezeTarget();
                if (target != null && target.getImage() != null) {
                    Image targetImg = target.getImage();
                    float targetCenterX = targetImg.getX() + targetImg.getWidth() / 2f;
                    float targetTopY = targetImg.getY() + targetImg.getHeight();
                    
                    float progress = p.getUnfreezeProgress();
                    float gageW = 60f;
                    float gageH = 10f;
                    float gageX = targetCenterX - gageW / 2f;
                    float gageY = targetTopY + 5f;
                    
                    // 배경 게이지 (gage1)
                    app.batch.draw(texGage1, gageX, gageY, gageW, gageH);
                    
                    // 진행 게이지 (gage2)
                    float fillW = gageW * progress;
                    app.batch.draw(texGage2, gageX, gageY, fillW, gageH);
                }
            }
        }
        
        app.batch.end();
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
        texMap.dispose();
        texHoopL.dispose();
        texHoopR.dispose();
        texIdle.dispose();

        for (Texture t : texRight) t.dispose();
        for (Texture t : texLeft) t.dispose();
        texChaserIdle.dispose();  // ★ Chaser idle
        for (Texture t : texChaserLeft) t.dispose();
        for (Texture t : texChaserRight) t.dispose();
        for (Texture t : texChaserAttackL) t.dispose();
        for (Texture t : texChaserAttackR) t.dispose();

        texRunnerDashL.dispose();
        texRunnerDashR.dispose();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 5; j++) {
                freezeLeftFrames[i][j].dispose();
                freezeRightFrames[i][j].dispose();
            }
        }

        for (Texture t : texFoggy) t.dispose();

        // 스킬 아이콘 dispose
        texDashIcon.dispose();
        for (Texture t : texDashCooldown) t.dispose();
        texFogIcon.dispose();
        for (Texture t : texFogCooldown) t.dispose();
        texUnfreezeIcon.dispose();
        for (Texture t : texUnfreezeCooldown) t.dispose();

        // 게이지 dispose
        texGage1.dispose();
        texGage2.dispose();

        if (skillUI != null) skillUI.dispose();
        if (sr != null) sr.dispose();
        if (font != null) font.dispose();  // ★ 폰트 해제
    }
}
