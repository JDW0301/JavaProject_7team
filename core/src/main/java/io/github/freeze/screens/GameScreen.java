package io.github.freeze.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;
import io.github.freeze.Core;

public class GameScreen implements Screen {
    private static final int VW = 1280, VH = 960;

    private final Core app;
    private final Stage stage;

    // 맵/골대
    private Texture texMap, texHoopL, texHoopR;
    private Image imgMap, imgHoopL, imgHoopR;
    //골대 충돌 필드
    private final Rectangle hoopLBlock = new Rectangle();
    private final Rectangle hoopRBlock = new Rectangle();
    //그물 충돌 필드
    private final Rectangle hoopLNet   = new Rectangle(); // ← 추가
    private final Rectangle hoopRNet   = new Rectangle(); // ← 추가
    //골대 지나갈수없음
    private static final float EPS = 0.5f;     // 튕김/지터 방지
    private static final float HERO_PAD = 0.12f; // 히트박스 축소(체감 개선)
    // 캐릭터
    private Texture texIdle;
    private Texture[] texRight = new Texture[8];
    private Texture[] texLeft  = new Texture[8];
    private Animation<TextureRegion> walkRight;
    private Animation<TextureRegion> walkLeft;
    private float animTime = 0f;
    private enum Dir { LEFT, RIGHT }
    private Dir lastDir = Dir.RIGHT;
    private Image hero;
    private float heroSpeed = 380f;
    //테스트
    private final ShapeRenderer sr = new ShapeRenderer();
    private boolean debugColliders = true;
    //장애물 처리
    private final List<Rectangle> hoopLRects = new ArrayList<>();
    private final List<Rectangle> hoopRRects = new ArrayList<>();
    // 월드
    private Group world;
    private float worldW, worldH;
    // 캐릭터 이동 범위
    private final Rectangle playArea = new Rectangle();
    public GameScreen(Core app) {
        this.app = app;
        this.stage = new Stage(new FitViewport(VW, VH), app.batch);
        Gdx.input.setInputProcessor(stage);
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        // === 로드 ===
        texMap   = load("images/map.png");
        texHoopL = load("images/basketballL.png");
        texHoopR = load("images/basketballR.png");

        texIdle = load("images/Front_C.png");
        //오른쪽
        for (int i = 0; i < 8; i++) texRight[i] = load("images/Right_C" + (i + 1) + ".png");
        Array<TextureRegion> frames = new Array<>();
        for (Texture t : texRight) frames.add(new TextureRegion(t));
        walkRight = new Animation<>(0.09f, frames, Animation.PlayMode.LOOP);
        //왼쪽
        for (int i = 0; i < 8; i++) texLeft[i] = load("images/Left_C" + (i+1) + ".png");
        Array<TextureRegion> leftFrames = new Array<>();
        for (Texture t : texLeft) leftFrames.add(new TextureRegion(t));
        walkLeft = new Animation<>(0.09f, leftFrames, Animation.PlayMode.LOOP);






        // === 액터 ===
        world = new Group(); stage.addActor(world);

        imgMap   = new Image(new TextureRegionDrawable(new TextureRegion(texMap)));   world.addActor(imgMap);
        imgHoopL = new Image(new TextureRegionDrawable(new TextureRegion(texHoopL))); world.addActor(imgHoopL);
        imgHoopR = new Image(new TextureRegionDrawable(new TextureRegion(texHoopR))); world.addActor(imgHoopR);

        hero = new Image(new TextureRegionDrawable(new TextureRegion(texIdle)));
        world.addActor(hero);

        layout();
        updateHoopBlocks();
        centerCameraOnHero();
        updatePlayArea();
    }
    // 캐릭터 이동 범위 제한
    private void updatePlayArea() {
        float x = imgMap.getX(),  y = imgMap.getY();
        float w = imgMap.getWidth(), h = imgMap.getHeight();

        // 코트 안쪽 테두리까지 여백 비율(필요시 미세조정)
        final float LM = 0.066f; // left  margin
        final float RM = 0.066f; // right margin
        final float TM = 0.090f; // top   margin
        final float BM = 0.090f; // bottom margin

        playArea.set(x + w*LM, y + h*BM, w*(1f - LM - RM), h*(1f - TM - BM));
    }


    private void layout() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();

        // 맵을 뷰포트보다 크게(스크롤 생기게)
        float tw = texMap.getWidth(), th = texMap.getHeight();
        float scale = Math.max(vw / tw, vh / th);
        worldW = tw * scale; worldH = th * scale;
        imgMap.setSize(worldW, worldH); imgMap.setPosition(0, 0);

        // 골대
        float hoopH = worldH * 0.372f;
        float lR = (float) texHoopL.getWidth() / texHoopL.getHeight();
        float rR = (float) texHoopR.getWidth() / texHoopR.getHeight();
        imgHoopL.setSize(hoopH * lR, hoopH);
        imgHoopR.setSize(hoopH * rR, hoopH);
        imgHoopL.setPosition(worldW * 0.182f, worldH * 0.55f, Align.center);
        imgHoopR.setPosition(worldW * 0.8225f, worldH * 0.55f, Align.center);

        // 히어로 크기/위치
        float heroH = worldH * 0.10f;
        float sHero = heroH / texIdle.getHeight();
        hero.setSize(texIdle.getWidth() * sHero, texIdle.getHeight() * sHero);
        hero.setPosition(worldW * 0.5f - hero.getWidth() / 2f,
            worldH * 0.5f - hero.getHeight() / 2f);
        hero.toFront();
    }

    private void updateHoopBlocks() {
        hoopLRects.clear(); hoopRRects.clear();

        float xL=imgHoopL.getX(), yL=imgHoopL.getY(), wL=imgHoopL.getWidth(),  hL=imgHoopL.getHeight();
        float xR=imgHoopR.getX(), yR=imgHoopR.getY(), wR=imgHoopR.getWidth(),  hR=imgHoopR.getHeight();

        // 오른쪽 골대(화면 오른쪽에 보이는 것)
        addRect(hoopRRects, xR,yR,wR,hR, 0.680f, 0.117f, 0.070f, 0.510f); // post(세로 기둥)
        addRect(hoopRRects, xR,yR,wR,hR, 0.460f, 0.510f, 0.040f, 0.330f); // backboard(백보드)
        addRect(hoopRRects, xR,yR,wR,hR, 0.480f, 0.600f, 0.115f, 0.100f); // arm(대각 지지대)
        addRect(hoopRRects, xR,yR,wR,hR, 0.585f, 0.600f, 0.098f, 0.050f); // rim(림)
        addRect(hoopRRects, xR,yR,wR,hR, 0.200f, 0.470f, 0.200f, 0.205f); // net(그물)

        // 왼쪽 골대(우측 값 좌우 반전)
        mirAdd(hoopLRects, xL,yL,wL,hL, 0.680f, 0.117f, 0.070f, 0.510f); // post
        mirAdd(hoopLRects, xL,yL,wL,hL, 0.460f, 0.510f, 0.040f, 0.330f); // backboard(백보드)
        mirAdd(hoopLRects, xL,yL,wL,hL, 0.480f, 0.600f, 0.115f, 0.100f); // arm(대각 지지대)
        mirAdd(hoopLRects, xL,yL,wL,hL, 0.585f, 0.600f, 0.098f, 0.050f); // rim(림)
        mirAdd(hoopLRects, xL,yL,wL,hL, 0.200f, 0.470f, 0.200f, 0.205f); // net(그물)

    }
    private void addRect(List<Rectangle> list, float bx,float by,float bw,float bh,
                         float ax,float ay,float aw,float ah) {
        list.add(new Rectangle(bx + bw*ax, by + bh*ay, bw*aw, bh*ah));
    }
    private void mirAdd(List<Rectangle> list, float bx,float by,float bw,float bh,
                        float axR,float ay,float aw,float ah) {
        float axL = 1f - axR - aw;                    // 좌우 반전
        list.add(new Rectangle(bx + bw*axL, by + bh*ay, bw*aw, bh*ah));
    }

    private Rectangle heroBounds(float x, float y) {
        float px = hero.getWidth()*HERO_PAD, py = hero.getHeight()*HERO_PAD;
        return new Rectangle(x + px, y + py,
            hero.getWidth() - 2f*px, hero.getHeight() - 2f*py);

    }
    private Rectangle collideWith(Rectangle hb) {
        for (Rectangle r : hoopLRects) if (hb.overlaps(r)) return r;
        for (Rectangle r : hoopRRects) if (hb.overlaps(r)) return r;
        return null;
    }
    private boolean overlapsAny(Rectangle hb) {
        return hb.overlaps(hoopLBlock) || hb.overlaps(hoopRBlock)
            || hb.overlaps(hoopLNet)   || hb.overlaps(hoopRNet);
    }

    private void updateHero(float dt) {
        // --- 입력 ---
        float dx = 0f, dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;

        boolean moving = (dx != 0f || dy != 0f);

        // 수평키가 있을 때만 바라보는 방향 갱신
        if (dx > 0f) lastDir = Dir.RIGHT;
        else if (dx < 0f) lastDir = Dir.LEFT;

        // 애니메이션: 이동 중엔 마지막 방향 걷기, 정지 시 해당 방향 정지 프레임
        if (moving) {
            animTime += dt;
            TextureRegion frame = (lastDir == Dir.RIGHT)
                ? walkRight.getKeyFrame(animTime)
                : walkLeft.getKeyFrame(animTime);
            ((TextureRegionDrawable) hero.getDrawable()).setRegion(frame);
        } else {
            TextureRegion idle = (lastDir == Dir.RIGHT)
                ? new TextureRegion(texRight[0])
                : new TextureRegion(texLeft[0]);
            ((TextureRegionDrawable) hero.getDrawable()).setRegion(idle);
            animTime = 0f;
        }

        // 대각 보정
        float len = (dx*dx + dy*dy);
        if (len > 0f) {
            len = (float)Math.sqrt(len);
            dx /= len; dy /= len;
        }

        float vx = dx * heroSpeed * dt;
        float vy = dy * heroSpeed * dt;

        // 히트박스 치수(패딩 반영)
        float w  = hero.getWidth(),  h  = hero.getHeight();
        float px = w * HERO_PAD,     py = h * HERO_PAD;
        float hbW = w - 2f*px,       hbH = h - 2f*py;


        //여러 히트 박스 형성 x축
        float nx = hero.getX() + vx;
        Rectangle hb = heroBounds(nx, hero.getY());
        Rectangle hit = collideWith(hb);
        if (hit != null) {
            nx = (vx > 0f) ? hit.x - (hbW + px) - EPS
                : hit.x + hit.width - px + EPS;
        }

        //캐릭터 위치
        float left  = playArea.x - px;
        float right = playArea.x + playArea.width - (w - px);
        nx = MathUtils.clamp(nx, left, right);
        hero.setX(nx);



        //여러 히트박스 형성 y축
        float ny = hero.getY() + vy;
        hb = heroBounds(hero.getX(), ny);
        hit = collideWith(hb);
        if (hit != null) {
            ny = (vy > 0f) ? hit.y - (hbH + py) - EPS
                : hit.y + hit.height - py + EPS;
        }
        //캐릭터 위치
        float bottom = playArea.y - py;
        float top    = playArea.y + playArea.height - (h - py);
        ny = MathUtils.clamp(ny, bottom, top);
        hero.setY(ny);
    }


    private void centerCameraOnHero() {
        float vw = stage.getViewport().getWorldWidth(), vh = stage.getViewport().getWorldHeight();
        float halfW = vw / 2f, halfH = vh / 2f;
        float cx = hero.getX() + hero.getWidth() / 2f;
        float cy = hero.getY() + hero.getHeight() / 2f;
        float camX = MathUtils.clamp(cx, halfW, Math.max(halfW, worldW - halfW));
        float camY = MathUtils.clamp(cy, halfH, Math.max(halfH, worldH - halfH));
        stage.getCamera().position.set(camX, camY, 0);
        stage.getCamera().update();
    }

    private Texture load(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    @Override public void render(float delta) {
        ScreenUtils.clear(0,0,0,1);
        Gdx.gl.glViewport(0,0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        stage.getViewport().apply(true);

        updateHero(delta);
        centerCameraOnHero();

        stage.act(delta);
        stage.draw();
        //임시 코드-----------------------------------
        /*
        if (debugColliders) {
            sr.setProjectionMatrix(stage.getCamera().combined);
            sr.begin(ShapeRenderer.ShapeType.Line);

            // 왼쪽 골대 박스(빨강)
            sr.setColor(1, 0, 0, 1);
            for (Rectangle r : hoopLRects) sr.rect(r.x, r.y, r.width, r.height);

            // 오른쪽 골대 박스(초록)
            sr.setColor(0, 1, 0, 1);
            for (Rectangle r : hoopRRects) sr.rect(r.x, r.y, r.width, r.height);

            // 히어로 히트박스(청록, 패딩 반영)
            Rectangle hb = heroBounds(hero.getX(), hero.getY());
            sr.setColor(0, 1, 1, 1);
            sr.rect(hb.x, hb.y, hb.width, hb.height);

            sr.end();
        }
        */
        //------------------------------------------

    }

    @Override public void resize(int w,int h){
        stage.getViewport().update(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        layout();
        centerCameraOnHero();
    }

    @Override public void show(){} @Override public void hide(){} @Override public void pause(){} @Override public void resume(){}

    @Override public void dispose() {
        stage.dispose();
        texMap.dispose(); texHoopL.dispose(); texHoopR.dispose();
        texIdle.dispose();
        for (Texture t : texRight) t.dispose();
        for (Texture t : texLeft)  t.dispose();   // ← 추가
        //영역 표시
        //sr.dispose();
    }

}
