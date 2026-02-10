package com.example.matematicaperbambini

enum class GameState {
    INIT,
    AWAITING_INPUT,
    VALIDATING,
    STEP_COMPLETED,
    GAME_COMPLETED
}

enum class ValidationFailure {
    EMPTY,
    NON_NUMERIC,
    OUT_OF_RANGE,
    NOT_AWAITING_INPUT,
    TOO_FAST,
    TOO_MANY_ATTEMPTS
}

data class ValidationResult(
    val isValid: Boolean,
    val failure: ValidationFailure? = null
) {
    companion object {
        fun valid() = ValidationResult(true)
        fun invalid(failure: ValidationFailure) = ValidationResult(false, failure)
    }
}

class StepInputGuard(
    private val minIntervalMs: Long = 400L,
    private val maxAttempts: Int = 3
) {
    private val lastInputAt = mutableMapOf<String, Long>()
    private val attempts = mutableMapOf<String, Int>()

    fun reset(stepId: String? = null) {
        if (stepId == null) {
            lastInputAt.clear()
            attempts.clear()
        } else {
            lastInputAt.remove(stepId)
            attempts.remove(stepId)
        }
    }

    fun allowInput(stepId: String, nowMs: Long = System.currentTimeMillis()): ValidationFailure? {
        val last = lastInputAt[stepId] ?: return run { lastInputAt[stepId] = nowMs; null }
        if (nowMs - last < minIntervalMs) return ValidationFailure.TOO_FAST
        lastInputAt[stepId] = nowMs
        return null
    }

    fun registerAttempt(stepId: String): Boolean {
        val newCount = (attempts[stepId] ?: 0) + 1
        attempts[stepId] = newCount
        if (newCount >= maxAttempts) {
            attempts[stepId] = 0
            return true
        }
        return false
    }
}

fun validateUserInput(
    stepId: String,
    value: String,
    expectedRange: IntRange,
    gameState: GameState = GameState.AWAITING_INPUT,
    guard: StepInputGuard? = null,
    onInit: (() -> Unit)? = null
): ValidationResult {
    val isInit = gameState == GameState.INIT
    if (isInit) {
        onInit?.invoke()
    }
    if (gameState != GameState.AWAITING_INPUT && !isInit) {
        return ValidationResult.invalid(ValidationFailure.NOT_AWAITING_INPUT)
    }
    guard?.allowInput(stepId)?.let { return ValidationResult.invalid(it) }
    if (value.isBlank()) {
        return ValidationResult.invalid(ValidationFailure.EMPTY)
    }
    val parsed = value.toIntOrNull() ?: return ValidationResult.invalid(ValidationFailure.NON_NUMERIC)
    if (parsed !in expectedRange) {
        return ValidationResult.invalid(ValidationFailure.OUT_OF_RANGE)
    }
    return ValidationResult.valid()
}
