package io.github.freeze.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;

/**
 * 플레이어 캐릭터 클래스
 */
public class Player {
    // 기본 정보
    private String playerId;
    private PlayerRole role;
    private PlayerState state;
    private Image image;

    // 위치 및 이동
    private Vector2 position;
    private Vector2 velocity;
    private float baseSpeed = 380f;
    private float currentSpeed = 380f;

    // 애니메이션
    private Animation<TextureRegion> walkLeft, walkRight;
    private Animation<TextureRegion> chaserAttackLeft, chaserAttackRight;
    private Animation<TextureRegion> runnerDashLeft, runnerDashRight;
    private Texture[][] freezeLeftFrames;   // [walkFrame][freezeFrame]
    private Texture[][] freezeRightFrames;
    private Texture idleTexture;  // Front_C 정지 이미지

    private float animTime = 0f;
    private int currentWalkFrame = 0;  // 현재 걷기 프레임 (0~7)
    private boolean facingRight = true;

    // 빙결 관련
    private int freezeMotionFrame = 0;  // 빙결 애니메이션 프레임 (0~4)
    private float freezeAnimTimer = 0f;
    private static final float FREEZE_FRAME_DURATION = 0.08f; // 5프레임 = 0.4초

    // 스킬 (Runner용)
    private Skill fogSkill;      // Q: 안개 (쿨타임 10초, 지속 2초)
    private Skill dashSkill;     // E: 대시 (쿨타임 5초, 지속 1초)
    private Skill unfreezeSkill; // F: 해빙 (쿨타임 없음, 2초 정지)

    // Chaser 공격
    private Skill attackSkill;   // Q: 공격 (쿨타임 없음, 애니메이션 시간)
    private float attackAnimTimer = 0f;
    private static final float ATTACK_FRAME_DURATION = 0.05f; // 12프레임 = 0.6초

    // 해빙 진행
    private float unfreezeProgress = 0f;
    private Player unfreezeTarget = null;

    public Player(String playerId, PlayerRole role, Image image) {
        this.playerId = playerId;
        this.role = role;
        this.state = PlayerState.NORMAL;
        this.image = image;
        this.position = new Vector2();
        this.velocity = new Vector2();

        // 스킬 초기화
        if (role == PlayerRole.RUNNER) {
            fogSkill = new Skill(10f, 3f);
            dashSkill = new Skill(5f, 0.2f);  // 쿨타임 5초, 지속시간 0.2초
            unfreezeSkill = new Skill(0f, 2f);
        } else {
            attackSkill = new Skill(0f, 0.6f); // 공격 애니메이션 시간
        }

        freezeLeftFrames = new Texture[8][5];
        freezeRightFrames = new Texture[8][5];
    }

    // === Getters & Setters ===
    public String getPlayerId() { return playerId; }
    public PlayerRole getRole() { return role; }
    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }
    public Image getImage() { return image; }
    public Vector2 getPosition() { return position; }
    public void setPosition(float x, float y) {
        position.set(x, y);
        image.setPosition(x, y);
    }
    public float getSpeed() { return currentSpeed; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean right) { this.facingRight = right; }

    // === 애니메이션 설정 ===
    public void setIdleTexture(Texture texture) {
        this.idleTexture = texture;
    }

    public void setWalkAnimations(Animation<TextureRegion> left, Animation<TextureRegion> right) {
        this.walkLeft = left;
        this.walkRight = right;
    }

    public void setChaserAttackAnimations(Animation<TextureRegion> left, Animation<TextureRegion> right) {
        this.chaserAttackLeft = left;
        this.chaserAttackRight = right;
    }

    public void setRunnerDashAnimations(Animation<TextureRegion> left, Animation<TextureRegion> right) {
        this.runnerDashLeft = left;
        this.runnerDashRight = right;
    }

    public void setFreezeFrames(Texture[][] left, Texture[][] right) {
        this.freezeLeftFrames = left;
        this.freezeRightFrames = right;
    }

    // === 스킬 ===
    public Skill getFogSkill() { return fogSkill; }
    public Skill getDashSkill() { return dashSkill; }
    public Skill getUnfreezeSkill() { return unfreezeSkill; }
    public Skill getAttackSkill() { return attackSkill; }

    // === 상태 체크 ===
    public boolean isFrozen() {
        return state == PlayerState.FROZEN || state == PlayerState.FREEZING;
    }

    public boolean canMove() {
        return state == PlayerState.NORMAL || state == PlayerState.DASHING;
    }

    public boolean isAttacking() {
        return state == PlayerState.ATTACKING;
    }

    // === 이동 ===
    public void move(float dx, float dy, float delta) {
        if (!canMove()) return;

        // 대시 중이면 속도 2배
        if (state == PlayerState.DASHING) {
            currentSpeed = baseSpeed * 2f;
        } else if (state == PlayerState.ATTACKING) {
            currentSpeed = baseSpeed * 0.5f; // 공격 중 50% 감속
        } else {
            currentSpeed = baseSpeed;
        }

        // 방향 정규화
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0f) {
            dx /= len;
            dy /= len;
        }

        velocity.set(dx * currentSpeed * delta, dy * currentSpeed * delta);
        position.add(velocity);
        image.setPosition(position.x, position.y);

        // 방향 결정
        if (dx > 0) facingRight = true;
        else if (dx < 0) facingRight = false;
    }

    // 이동 멈춤 (velocity 초기화)
    public void stopMoving() {
        velocity.set(0, 0);
        
        // Front_C 이미지로 변경
        if (idleTexture != null && state == PlayerState.NORMAL) {
            ((TextureRegionDrawable)image.getDrawable()).setRegion(new TextureRegion(idleTexture));
        }
    }

    // === 애니메이션 업데이트 ===
    public void update(float delta) {
        // animTime은 이동 중일 때만 증가
        if (velocity.len() > 0.1f) {
            animTime += delta;
        }

        // 스킬 업데이트
        if (role == PlayerRole.RUNNER) {
            if (fogSkill != null) fogSkill.update(delta);
            if (dashSkill != null) dashSkill.update(delta);
            if (unfreezeSkill != null) unfreezeSkill.update(delta);
        } else {
            if (attackSkill != null) attackSkill.update(delta);
        }

        switch (state) {
            case NORMAL:
                updateNormalAnimation(delta);
                break;
            case FREEZING:
                updateFreezingAnimation(delta);
                break;
            case FROZEN:
                // 얼린 상태 유지 (마지막 프레임)
                break;
            case UNFREEZING:
                updateUnfreezingAnimation(delta);
                break;
            case ATTACKING:
                updateAttackAnimation(delta);
                break;
            case DASHING:
                updateDashAnimation(delta);
                break;
            case UNFREEZING_TARGET:
                updateUnfreezeProgress(delta);
                break;
        }
    }

    private void updateNormalAnimation(float delta) {
        // 이동 중이면 걷기 애니메이션
        if (velocity.len() > 0.1f) {
            Animation<TextureRegion> anim = facingRight ? walkRight : walkLeft;
            TextureRegion frame = anim.getKeyFrame(animTime);
            ((TextureRegionDrawable)image.getDrawable()).setRegion(frame);

            // 현재 걷기 프레임 계산 (0~7)
            currentWalkFrame = anim.getKeyFrameIndex(animTime);
        } else {
            // 정지 시 Front_C 이미지
            if (idleTexture != null) {
                ((TextureRegionDrawable)image.getDrawable()).setRegion(new TextureRegion(idleTexture));
            } else {
                // idleTexture가 없으면 첫 프레임 사용
                Animation<TextureRegion> anim = facingRight ? walkRight : walkLeft;
                TextureRegion frame = anim.getKeyFrame(0);
                ((TextureRegionDrawable)image.getDrawable()).setRegion(frame);
            }
            animTime = 0f;
            currentWalkFrame = 0;
        }
    }

    private void updateFreezingAnimation(float delta) {
        freezeAnimTimer += delta;

        // 5프레임 애니메이션
        int frame = (int)(freezeAnimTimer / FREEZE_FRAME_DURATION);
        if (frame >= 5) {
            frame = 4;
            state = PlayerState.FROZEN;
        }

        freezeMotionFrame = frame;

        // 텍스처 설정
        Texture[][] frames = facingRight ? freezeRightFrames : freezeLeftFrames;
        if (currentWalkFrame < 8 && freezeMotionFrame < 5) {
            Texture tex = frames[currentWalkFrame][freezeMotionFrame];
            if (tex != null) {
                ((TextureRegionDrawable)image.getDrawable()).setRegion(new TextureRegion(tex));
            }
        }
    }

    private void updateUnfreezingAnimation(float delta) {
        freezeAnimTimer += delta;

        // 역순 5프레임 (4→0)
        int frame = 4 - (int)(freezeAnimTimer / FREEZE_FRAME_DURATION);
        if (frame < 0) {
            frame = 0;
            state = PlayerState.NORMAL;
            freezeAnimTimer = 0f;
        }

        freezeMotionFrame = frame;

        // 텍스처 설정
        Texture[][] frames = facingRight ? freezeRightFrames : freezeLeftFrames;
        if (currentWalkFrame < 8 && freezeMotionFrame < 5) {
            Texture tex = frames[currentWalkFrame][freezeMotionFrame];
            if (tex != null) {
                ((TextureRegionDrawable)image.getDrawable()).setRegion(new TextureRegion(tex));
            }
        }
    }

    private void updateAttackAnimation(float delta) {
        attackAnimTimer += delta;

        Animation<TextureRegion> anim = facingRight ? chaserAttackRight : chaserAttackLeft;
        TextureRegion frame = anim.getKeyFrame(attackAnimTimer);
        ((TextureRegionDrawable)image.getDrawable()).setRegion(frame);

        // 애니메이션 종료
        if (attackAnimTimer >= ATTACK_FRAME_DURATION * 12) {
            state = PlayerState.NORMAL;
            attackAnimTimer = 0f;
        }
    }

    private void updateDashAnimation(float delta) {
        // 대시는 단일 이미지로 표현
        // dashSkill의 타이머가 끝나면 자동으로 NORMAL로 복귀
        if (dashSkill != null && !dashSkill.isActive()) {
            state = PlayerState.NORMAL;
            // 크기 변경 없음 (0.1f 고정)
        }
    }

    private void updateUnfreezeProgress(float delta) {
        unfreezeProgress += delta;

        if (unfreezeProgress >= 2f && unfreezeTarget != null) {
            // 해빙 성공
            unfreezeTarget.startUnfreeze();
            unfreezeProgress = 0f;
            unfreezeTarget = null;
            state = PlayerState.NORMAL;
        }
    }

    // === 빙결/해빙 ===
    public void startFreeze() {
        if (role == PlayerRole.CHASER) return; // Chaser는 안 얼음
        state = PlayerState.FREEZING;
        freezeAnimTimer = 0f;
        freezeMotionFrame = 0;
    }

    public void startUnfreeze() {
        if (state != PlayerState.FROZEN) return;
        state = PlayerState.UNFREEZING;
        freezeAnimTimer = 0f;
    }

    // === Chaser 공격 ===
    public void startAttack() {
        if (role != PlayerRole.CHASER) return;
        if (state != PlayerState.NORMAL) return;

        state = PlayerState.ATTACKING;
        attackAnimTimer = 0f;
        if (attackSkill != null) attackSkill.use();
    }

    // === Runner 스킬 ===
    public void useFogSkill() {
        if (role != PlayerRole.RUNNER) return;
        if (fogSkill != null && fogSkill.canUse()) {
            fogSkill.use();
        }
    }

    public void useDashSkill() {
        if (role != PlayerRole.RUNNER) return;
        if (dashSkill != null && dashSkill.canUse() && state == PlayerState.NORMAL) {
            dashSkill.use();
            state = PlayerState.DASHING;

            // 대시 이미지로 변경
            Animation<TextureRegion> anim = facingRight ? runnerDashRight : runnerDashLeft;
            if (anim != null) {
                TextureRegion frame = anim.getKeyFrame(0);

                // 현재 이미지 크기 저장 (0.1f)
                float currentW = image.getWidth();
                float currentH = image.getHeight();

                // 대시 이미지로 변경
                ((TextureRegionDrawable)image.getDrawable()).setRegion(frame);

                // 크기 유지 (0.1f 고정)
                image.setSize(currentW, currentH);
            }
        }
    }

    public void startUnfreezeTarget(Player target) {
        if (role != PlayerRole.RUNNER) return;
        if (target == null || !target.isFrozen()) return;
        if (state != PlayerState.NORMAL) return;

        state = PlayerState.UNFREEZING_TARGET;
        unfreezeTarget = target;
        unfreezeProgress = 0f;
    }

    public void cancelUnfreeze() {
        if (state == PlayerState.UNFREEZING_TARGET) {
            state = PlayerState.NORMAL;
            unfreezeTarget = null;
            unfreezeProgress = 0f;
        }
    }

    public float getUnfreezeProgress() {
        return unfreezeProgress / 2f; // 0~1
    }

    // === 충돌 박스 ===
    public Rectangle getBounds() {
        float pad = image.getWidth() * 0.12f;
        return new Rectangle(
            position.x + pad,
            position.y + pad,
            image.getWidth() - 2f * pad,
            image.getHeight() - 2f * pad
        );
    }

    public float distanceTo(Player other) {
        return position.dst(other.position);
    }
}
