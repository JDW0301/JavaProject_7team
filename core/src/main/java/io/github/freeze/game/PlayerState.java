package io.github.freeze.game;

/**
 * 플레이어 상태 정의
 */
public enum PlayerState {
    NORMAL,      // 정상 상태
    FREEZING,    // 빙결 중 (5프레임 애니메이션)
    FROZEN,      // 완전히 얼린 상태
    UNFREEZING,  // 해빙 중 (5프레임 역순 애니메이션)
    ATTACKING,   // Chaser 공격 중
    DASHING,     // Runner 대시 중
    UNFREEZING_TARGET  // 해빙 시도 중 (F키 누르고 2초 대기)
}
