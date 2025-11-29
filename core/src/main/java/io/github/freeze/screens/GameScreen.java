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
    private static final float FREEZE_RANGE = 100f;      // 1m = 100 픽셀
    private static final float UNFREEZE_RANGE = 60f;     // 0.6m = 60 픽셀

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

    // 월드
    private Group world;
    private float worldW, worldH;
    private final Rectangle playArea = new Rectangle();

    // 플레이어들
    private Map<String, Player> players = new HashMap<>();
    private String myPlayerId;
    private Player myPlayer;

    // UI
    private SkillUI skillUI;
    private FogEffect fogEffect;

    // 테스트/디버그
    private final ShapeRenderer sr = new ShapeRenderer();
    private boolean debugColliders = false;

    // 입력
    private float moveSendTimer = 0f;
    private static final float MOVE_SEND_INTERVAL = 0.05f; // 20fps로 위치 전송

    public GameScreen(Core app) {
        this.app = app;
        this.stage = new Stage(new FitViewport(VW, VH), app.batch);
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        loadTextures();
        setupWorld();
        setupUI();
        setupNetworkListener();

        // 테스트용: 로컬 플레이어 생성
        createTestPlayers();
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
                }
            }

            @Override
            public void onPlayerUnfreeze(String targetId, String unfreezeId) {
                Player target = players.get(targetId);
                if (target != null) {
                    target.startUnfreeze();
                }
            }

            @Override
            public void onSkillUsed(String playerId, String skillType) {
                Player p = players.get(playerId);
                if (p == null) return;

                switch (skillType) {
                    case "dash":
                        p.useDashSkill();
                        break;
                    case "fog":
                        p.useFogSkill();
                        break;
                }
            }

            @Override
            public void onFogActivated(String playerId) {
                // Chaser 화면에만 안개 표시
                if (myPlayer != null && myPlayer.getRole() == PlayerRole.CHASER) {
                    fogEffect.activate();
                }
            }
        });
    }

    // ========== 테스트용 플레이어 생성 ==========
    private void createTestPlayers() {
        // 임시: 로컬 플레이어 생성
        myPlayerId = "player1";

        Image heroImage = new Image(new TextureRegionDrawable(new TextureRegion(texIdle)));
        float heroH = worldH * 0.15f;  // 0.1f 고정!
        float sHero = heroH / texIdle.getHeight();
        heroImage.setSize(texIdle.getWidth() * sHero, texIdle.getHeight() * sHero);
        heroImage.setPosition(worldW * 0.5f - heroImage.getWidth() / 2f,
                              worldH * 0.5f - heroImage.getHeight() / 2f);
        world.addActor(heroImage);

        myPlayer = new Player(myPlayerId, PlayerRole.RUNNER, heroImage);
        myPlayer.setPosition(heroImage.getX(), heroImage.getY());

        // 애니메이션 설정
        Array<TextureRegion> rightFrames = new Array<>();
        for (Texture t : texRight) rightFrames.add(new TextureRegion(t));
        Animation<TextureRegion> walkRight = new Animation<>(0.09f, rightFrames, Animation.PlayMode.LOOP);

        Array<TextureRegion> leftFrames = new Array<>();
        for (Texture t : texLeft) leftFrames.add(new TextureRegion(t));
        Animation<TextureRegion> walkLeft = new Animation<>(0.09f, leftFrames, Animation.PlayMode.LOOP);

        myPlayer.setWalkAnimations(walkLeft, walkRight);
        myPlayer.setIdleTexture(texIdle);  // Front_C 정지 이미지 설정

        // 대시 애니메이션
        Array<TextureRegion> dashL = new Array<>();
        dashL.add(new TextureRegion(texRunnerDashL));
        Array<TextureRegion> dashR = new Array<>();
        dashR.add(new TextureRegion(texRunnerDashR));
        myPlayer.setRunnerDashAnimations(
            new Animation<>(1f, dashL, Animation.PlayMode.NORMAL),
            new Animation<>(1f, dashR, Animation.PlayMode.NORMAL)
        );

        // 빙결 프레임 설정
        myPlayer.setFreezeFrames(freezeLeftFrames, freezeRightFrames);

        players.put(myPlayerId, myPlayer);

        heroImage.toFront();
        centerCameraOnPlayer(myPlayer);
    }

    // ========== 입력 처리 ==========
    private void handleInput(float delta) {
        if (myPlayer == null || !myPlayer.canMove()) return;

        float dx = 0f, dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;

        // 이동 처리
        if (dx != 0f || dy != 0f) {
            movePlayerWithCollision(myPlayer, dx, dy, delta);

            // ★ 주기적으로 서버에 이동량 전송 (dx, dy)
            moveSendTimer += delta;
            if (moveSendTimer >= MOVE_SEND_INTERVAL) {
                Net.get().sendPlayerMove(myPlayerId, dx, dy);  // 좌표가 아니라 이동량!
                moveSendTimer = 0f;
            }
        } else {
            // 이동 안 할 때 velocity 초기화 (애니메이션 멈춤)
            myPlayer.stopMoving();
        }

        // 스킬 입력
        if (myPlayer.getRole() == PlayerRole.RUNNER) {
            handleRunnerSkills();
        } else if (myPlayer.getRole() == PlayerRole.CHASER) {
            handleChaserSkills();
        }
    }

    private void handleRunnerSkills() {
        // Q: 안개
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            myPlayer.useFogSkill();
            if (myPlayer.getFogSkill().isActive()) {
                Net.get().sendSkillUse("fog");
            }
        }

        // E: 대시
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            myPlayer.useDashSkill();
            if (myPlayer.getDashSkill().isActive()) {
                Net.get().sendSkillUse("dash");
            }
        }

        // F: 해빙
        if (Gdx.input.isKeyPressed(Input.Keys.F)) {
            // 가까운 얼린 플레이어 찾기
            Player frozenTarget = findNearestFrozenPlayer();
            if (frozenTarget != null && myPlayer.getState() != PlayerState.UNFREEZING_TARGET) {
                myPlayer.startUnfreezeTarget(frozenTarget);
            }
        } else {
            // F키를 떼면 해빙 취소
            if (myPlayer.getState() == PlayerState.UNFREEZING_TARGET) {
                myPlayer.cancelUnfreeze();
            }
        }
    }

    private void handleChaserSkills() {
        // Q: 공격
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            myPlayer.startAttack();

            // 범위 내 Runner 빙결
            for (Player p : players.values()) {
                if (p.getRole() == PlayerRole.RUNNER && !p.isFrozen()) {
                    if (myPlayer.distanceTo(p) <= FREEZE_RANGE) {
                        Net.get().sendFreeze(p.getPlayerId());
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

        float speed = player.getSpeed();
        float vx = dx * speed * delta;
        float vy = dy * speed * delta;

        Image img = player.getImage();
        float w = img.getWidth(), h = img.getHeight();
        float px = w * HERO_PAD, py = h * HERO_PAD;
        float hbW = w - 2f * px, hbH = h - 2f * py;

        // X축 이동
        float nx = img.getX() + vx;
        Rectangle hb = new Rectangle(nx + px, img.getY() + py, hbW, hbH);
        Rectangle hit = collideWithObstacle(hb);
        if (hit != null) {
            nx = (vx > 0f) ? hit.x - (hbW + px) - EPS : hit.x + hit.width - px + EPS;
        }

        float left = playArea.x - px;
        float right = playArea.x + playArea.width - (w - px);
        nx = MathUtils.clamp(nx, left, right);
        img.setX(nx);

        // Y축 이동
        float ny = img.getY() + vy;
        hb = new Rectangle(img.getX() + px, ny + py, hbW, hbH);
        hit = collideWithObstacle(hb);
        if (hit != null) {
            ny = (vy > 0f) ? hit.y - (hbH + py) - EPS : hit.y + hit.height - py + EPS;
        }

        float bottom = playArea.y - py;
        float top = playArea.y + playArea.height - (h - py);
        ny = MathUtils.clamp(ny, bottom, top);
        img.setY(ny);

        player.setPosition(img.getX(), img.getY());
        player.move(dx, dy, delta);
    }

    private Rectangle collideWithObstacle(Rectangle hb) {
        for (Rectangle r : hoopLRects) if (hb.overlaps(r)) return r;
        for (Rectangle r : hoopRRects) if (hb.overlaps(r)) return r;
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
    }
}
