package io.github.freeze.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * 안개 효과 클래스 (Chaser 화면 전용)
 * Runner가 안개 스킬 사용 시 Chaser 화면에 표시됨
 * 
 * 애니메이션 순서:
 * 1. Foggy1→2→3→4→5 (0.5초) - 안개 퍼짐
 * 2. Foggy5 유지 (2초) - 최대 안개
 * 3. Foggy5→4→3→2→1 (0.5초) - 안개 사라짐
 */
public class FogEffect {
    
    private enum State {
        INACTIVE,       // 비활성
        FADING_IN,      // 안개 퍼지는 중 (1→5)
        FULL,           // 최대 안개 (5 유지)
        FADING_OUT      // 안개 사라지는 중 (5→1)
    }
    
    private State state = State.INACTIVE;
    private Animation<TextureRegion> fadeInAnim;   // Foggy1→5
    private Animation<TextureRegion> fadeOutAnim;  // Foggy5→1
    private TextureRegion fullFogFrame;            // Foggy5 (최대 안개)
    
    private float animTime = 0f;
    private float fullDuration = 5f;  // ★ 안개 유지 시간 5초
    private float fullTimer = 0f;
    
    private static final float FADE_DURATION = 0.1f;  // 각 프레임당 시간
    
    public FogEffect(Animation<TextureRegion> animation) {
        this.fadeInAnim = animation;
        
        // ★ Array로 프레임 추출 (캐스팅 문제 해결)
        Array<TextureRegion> frameArray = new Array<>();
        float time = 0f;
        float frameDuration = animation.getFrameDuration();
        int frameCount = (int)(animation.getAnimationDuration() / frameDuration);
        
        for (int i = 0; i < frameCount; i++) {
            frameArray.add(animation.getKeyFrame(i * frameDuration));
        }
        
        // 마지막 프레임 저장
        if (frameArray.size > 0) {
            this.fullFogFrame = frameArray.get(frameArray.size - 1);
        }
        
        // fade out 애니메이션 (역순: Foggy5→1)
        Array<TextureRegion> reverseFrames = new Array<>();
        for (int i = frameArray.size - 1; i >= 0; i--) {
            reverseFrames.add(frameArray.get(i));
        }
        this.fadeOutAnim = new Animation<>(FADE_DURATION, reverseFrames);
        this.fadeOutAnim.setPlayMode(Animation.PlayMode.NORMAL);
    }
    
    /**
     * 안개 효과 활성화
     */
    public void activate() {
        if (state == State.INACTIVE) {
            state = State.FADING_IN;
            animTime = 0f;
            fullTimer = 0f;
            Gdx.app.log("FOG", "안개 효과 시작!");
        }
    }
    
    /**
     * 업데이트
     */
    public void update(float delta) {
        switch (state) {
            case INACTIVE:
                break;
                
            case FADING_IN:
                animTime += delta;
                if (fadeInAnim.isAnimationFinished(animTime)) {
                    state = State.FULL;
                    fullTimer = 0f;
                    Gdx.app.log("FOG", "안개 최대!");
                }
                break;
                
            case FULL:
                fullTimer += delta;
                if (fullTimer >= fullDuration) {
                    state = State.FADING_OUT;
                    animTime = 0f;
                    Gdx.app.log("FOG", "안개 사라지는 중...");
                }
                break;
                
            case FADING_OUT:
                animTime += delta;
                if (fadeOutAnim.isAnimationFinished(animTime)) {
                    state = State.INACTIVE;
                    Gdx.app.log("FOG", "안개 효과 종료!");
                }
                break;
        }
    }
    
    /**
     * 렌더링 (화면 전체 덮기)
     */
    public void render(Batch batch, float screenWidth, float screenHeight) {
        if (state == State.INACTIVE) return;
        
        TextureRegion frame = null;
        
        switch (state) {
            case FADING_IN:
                frame = fadeInAnim.getKeyFrame(animTime);
                break;
            case FULL:
                frame = fullFogFrame;
                break;
            case FADING_OUT:
                frame = fadeOutAnim.getKeyFrame(animTime);
                break;
            default:
                break;
        }
        
        if (frame != null) {
            batch.begin();
            batch.draw(frame, 0, 0, screenWidth, screenHeight);
            batch.end();
        }
    }
    
    public boolean isActive() {
        return state != State.INACTIVE;
    }
    
    public void dispose() {
        // 텍스처는 GameScreen에서 관리
    }
}
