package com.example.matematicaperbambini

fun shouldHighlightGuideCell(
    isInputCell: Boolean,
    isCurrentStepInput: Boolean,
    isChallengeMode: Boolean,
    isHomeworkMode: Boolean,
    highlightsEnabled: Boolean
): Boolean {
    if (isInputCell && !isCurrentStepInput) return false
    if (isInputCell) return true
    if (isChallengeMode) return false
    if (isHomeworkMode && !highlightsEnabled) return false
    return true
}

fun isChallengeMode(isHomeworkMode: Boolean, helps: HelpSettings?): Boolean {
    return !isHomeworkMode && helps?.highlightsEnabled == false
}
