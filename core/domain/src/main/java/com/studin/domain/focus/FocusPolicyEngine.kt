package com.studin.domain.focus

import com.studin.domain.model.RuleMode
import com.studin.domain.model.Verdict

data class PolicyDecision(val verdict: Verdict, val reason: String, val layer: Layer) {
    enum class Layer { EMERGENCY, USER_ALLOW, SESSION_PASS, APP_BLOCK, FEED_RULE, ADVISORY, NONE }
}

/**
 * Rule precedence (roadmap §3):
 * emergency allowlist > explicit user allowlist > session exceptions > full app block
 * > feed-specific rule > advisory classification.
 */
class FocusPolicyEngine(
    private val emergencyPackages: Set<String> = DEFAULT_EMERGENCY,
) {
    fun decide(
        packageName: String,
        rule: RuleMode?,
        sessionActive: Boolean,
        passActive: Boolean,
        contentVerdict: Verdict? = null,
        contentReason: String? = null,
    ): PolicyDecision {
        if (isEmergency(packageName)) return PolicyDecision(Verdict.ALLOW, "Emergency & safety apps are always available", PolicyDecision.Layer.EMERGENCY)
        if (!sessionActive) return PolicyDecision(Verdict.ALLOW, "No focus session running", PolicyDecision.Layer.NONE)
        if (rule == RuleMode.ALLOW) return PolicyDecision(Verdict.ALLOW, "You allowed this app", PolicyDecision.Layer.USER_ALLOW)
        if (passActive) return PolicyDecision(Verdict.ALLOW, "Break pass active", PolicyDecision.Layer.SESSION_PASS)
        if (rule == RuleMode.BLOCK) return PolicyDecision(Verdict.BLOCK, "Blocked during focus", PolicyDecision.Layer.APP_BLOCK)
        if (rule == RuleMode.SMART) {
            return when (contentVerdict) {
                Verdict.BLOCK -> PolicyDecision(Verdict.BLOCK, contentReason ?: "Looks off-syllabus", PolicyDecision.Layer.FEED_RULE)
                Verdict.WARN -> PolicyDecision(Verdict.WARN, contentReason ?: "Might be a distraction", PolicyDecision.Layer.ADVISORY)
                Verdict.ALLOW -> PolicyDecision(Verdict.ALLOW, contentReason ?: "Study content", PolicyDecision.Layer.FEED_RULE)
                Verdict.UNKNOWN, null -> PolicyDecision(Verdict.UNKNOWN, "Not sure – allowed", PolicyDecision.Layer.ADVISORY)
            }
        }
        return PolicyDecision(Verdict.ALLOW, "No rule for this app", PolicyDecision.Layer.NONE)
    }

    fun isEmergency(pkg: String): Boolean =
        pkg in emergencyPackages || EMERGENCY_PREFIXES.any { pkg.startsWith(it) }

    companion object {
        val DEFAULT_EMERGENCY = setOf(
            "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
            "com.android.phone", "com.android.server.telecom", "com.android.emergency",
            "com.google.android.apps.safetyhub", "com.android.settings", "com.android.systemui",
            "com.google.android.contacts", "com.android.contacts", "com.samsung.android.app.contacts",
            "com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging",
            "com.google.android.marvin.talkback",
        )
        private val EMERGENCY_PREFIXES = listOf("com.android.launcher", "com.google.android.apps.nexuslauncher", "com.sec.android.app.launcher", "com.miui.home")
    }
}
