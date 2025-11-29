package io.github.freeze.game;

/**
 * 스킬 쿨타임 관리 클래스
 */
public class Skill {
    private final float cooldownTime;  // 쿨타임 시간
    private float remainingCooldown;   // 남은 쿨타임
    private boolean isActive;          // 스킬 활성화 여부
    private float activeDuration;      // 스킬 지속 시간
    private float activeTimer;         // 활성 타이머

    public Skill(float cooldownTime, float activeDuration) {
        this.cooldownTime = cooldownTime;
        this.activeDuration = activeDuration;
        this.remainingCooldown = 0f;
        this.isActive = false;
        this.activeTimer = 0f;
    }

    public void update(float delta) {
        // 쿨타임 감소
        if (remainingCooldown > 0f) {
            remainingCooldown -= delta;
            if (remainingCooldown < 0f) remainingCooldown = 0f;
        }

        // 활성 타이머 감소
        if (isActive) {
            activeTimer -= delta;
            if (activeTimer <= 0f) {
                isActive = false;
                activeTimer = 0f;
            }
        }
    }

    public boolean canUse() {
        return remainingCooldown <= 0f && !isActive;
    }

    public void use() {
        if (canUse()) {
            remainingCooldown = cooldownTime;
            isActive = true;
            activeTimer = activeDuration;
        }
    }

    public void reset() {
        remainingCooldown = 0f;
        isActive = false;
        activeTimer = 0f;
    }

    public float getRemainingCooldown() { return remainingCooldown; }
    public float getCooldownTime() { return cooldownTime; }
    public boolean isActive() { return isActive; }
    public float getActiveTimer() { return activeTimer; }
    public float getCooldownPercent() {
        return remainingCooldown > 0f ? (remainingCooldown / cooldownTime) : 0f;
    }
}
