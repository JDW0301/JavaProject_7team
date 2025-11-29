package io.github.freeze.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * 스킬 UI 표시 클래스
 */
public class SkillUI {
    // 기본 아이콘
    private TextureRegion fogBaseIcon;
    private TextureRegion dashBaseIcon;
    private TextureRegion unfreezeBaseIcon;

    // 쿨타임 애니메이션
    private Animation<TextureRegion> fogCooldownAnim;
    private Animation<TextureRegion> dashCooldownAnim;
    private Animation<TextureRegion> unfreezeCooldownAnim;

    private float iconSize;
    private float iconSpacing;
    private float posX, posY; // 좌하단 기준 위치

    private float animTime = 0f;

    // 해빙 게이지 텍스처 (2프레임)
    private Texture unfreezeGauge1, unfreezeGauge2;

    public SkillUI(float iconSize, float iconSpacing) {
        this.iconSize = iconSize;
        this.iconSpacing = iconSpacing;
    }

    public void setFogIconAnimation(TextureRegion baseIcon, Animation<TextureRegion> cooldownAnim) {
        this.fogBaseIcon = baseIcon;
        this.fogCooldownAnim = cooldownAnim;
    }

    public void setDashIconAnimation(TextureRegion baseIcon, Animation<TextureRegion> cooldownAnim) {
        this.dashBaseIcon = baseIcon;
        this.dashCooldownAnim = cooldownAnim;
    }

    public void setUnfreezeIconAnimation(TextureRegion baseIcon, Animation<TextureRegion> cooldownAnim) {
        this.unfreezeBaseIcon = baseIcon;
        this.unfreezeCooldownAnim = cooldownAnim;
    }

    public void setUnfreezeGaugeTextures(Texture gauge1, Texture gauge2) {
        this.unfreezeGauge1 = gauge1;
        this.unfreezeGauge2 = gauge2;
    }

    public void setPosition(float screenWidth, float screenHeight) {
        // 좌하단 기준으로 변경
        float margin = 20f;
        this.posX = margin;  // 왼쪽
        this.posY = margin;  // 하단
    }

    public void update(float delta) {
        animTime += delta;
    }

    /**
     * Runner 스킬 UI 렌더링
     */
    public void renderRunnerSkills(SpriteBatch batch, Player player, float viewportWidth, float viewportHeight) {
        if (player.getRole() != PlayerRole.RUNNER) return;

        setPosition(viewportWidth, viewportHeight);

        Skill fogSkill = player.getFogSkill();
        Skill dashSkill = player.getDashSkill();
        Skill unfreezeSkill = player.getUnfreezeSkill();

        batch.begin();

        // 1. 안개 스킬 (Q)
        if (fogBaseIcon != null) {
            TextureRegion frame;
            if (fogSkill != null && fogSkill.getRemainingCooldown() > 0f && fogCooldownAnim != null) {
                // 쿨타임 중 - 쿨타임 애니메이션 (could (1) ~ (10))
                frame = fogCooldownAnim.getKeyFrame(fogSkill.getRemainingCooldown(), false);
            } else {
                // 기본 상태 - 기본 아이콘 (could.png)
                frame = fogBaseIcon;
            }
            batch.draw(frame, posX, posY, iconSize, iconSize);
        }

        // 2. 대시 스킬 (E)
        if (dashBaseIcon != null) {
            TextureRegion frame;
            if (dashSkill != null && dashSkill.getRemainingCooldown() > 0f && dashCooldownAnim != null) {
                // 쿨타임 중 - 쿨타임 애니메이션 (Run (1) ~ (5))
                frame = dashCooldownAnim.getKeyFrame(dashSkill.getRemainingCooldown(), false);
            } else {
                // 기본 상태 - 기본 아이콘 (Run.png)
                frame = dashBaseIcon;
            }
            batch.draw(frame, posX + iconSize + iconSpacing, posY, iconSize, iconSize);
        }

        // 3. 해빙 스킬 (F)
        if (unfreezeBaseIcon != null) {
            TextureRegion frame;
            // 해빙 중일 때 (F키 누르고 2초 대기 중) - iccbroken1, iccbroken2 표시
            if (player.getState() == PlayerState.UNFREEZING_TARGET && unfreezeCooldownAnim != null) {
                frame = unfreezeCooldownAnim.getKeyFrame(player.getUnfreezeProgress() * 2f, false);
            } else {
                // 기본 상태 - 기본 아이콘 (iccbroken.png)
                frame = unfreezeBaseIcon;
            }
            batch.draw(frame, posX + (iconSize + iconSpacing) * 2, posY, iconSize, iconSize);
        }

        batch.end();

        // 해빙 진행 게이지 (플레이어 위에 표시)
        if (player.getState() == PlayerState.UNFREEZING_TARGET) {
            renderUnfreezeGauge(batch, player);
        }
    }

    /**
     * 해빙 진행 게이지 렌더링 (플레이어 위)
     */
    private void renderUnfreezeGauge(SpriteBatch batch, Player player) {
        float progress = player.getUnfreezeProgress(); // 0~1

        batch.begin();

        // 플레이어 위치 위에 게이지 표시
        float gaugeX = player.getPosition().x + player.getImage().getWidth() / 2f - 30f;
        float gaugeY = player.getPosition().y + player.getImage().getHeight() + 10f;

        // 2프레임 게이지 (progress에 따라 전환)
        Texture gaugeTexture = (progress < 0.5f) ? unfreezeGauge1 : unfreezeGauge2;
        if (gaugeTexture != null) {
            batch.draw(gaugeTexture, gaugeX, gaugeY, 60f, 10f);
        }

        batch.end();
    }

    public void dispose() {
        // 외부에서 받은 텍스처는 여기서 dispose하지 않음
        // GameScreen에서 관리
    }
}
