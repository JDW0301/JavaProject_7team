package io.github.freeze.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 안개 효과 (Chaser 시야 차단)
 */
public class FogEffect {
    private Animation<TextureRegion> fogAnimation;
    private float animTime = 0f;
    private boolean isActive = false;
    private float duration = 0f;
    private float maxDuration = 2f;
    
    public FogEffect(Animation<TextureRegion> fogAnimation) {
        this.fogAnimation = fogAnimation;
    }
    
    public void activate() {
        isActive = true;
        duration = maxDuration;
        animTime = 0f;
    }
    
    public void update(float delta) {
        if (!isActive) return;
        
        animTime += delta;
        duration -= delta;
        
        if (duration <= 0f) {
            isActive = false;
            duration = 0f;
        }
    }
    
    /**
     * Chaser 화면 전체에 안개 렌더링
     */
    public void render(SpriteBatch batch, float screenWidth, float screenHeight) {
        if (!isActive) return;
        
        batch.begin();
        
        // 마지막 프레임 유지 (Foggy5)
        TextureRegion frame;
        if (fogAnimation.isAnimationFinished(animTime)) {
            frame = fogAnimation.getKeyFrame(fogAnimation.getAnimationDuration());
        } else {
            frame = fogAnimation.getKeyFrame(animTime, false);
        }
        
        // 화면 전체 덮기
        batch.draw(frame, 0, 0, screenWidth, screenHeight);
        
        batch.end();
    }
    
    public boolean isActive() {
        return isActive;
    }
}
