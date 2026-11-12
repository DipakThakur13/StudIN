package com.studin.app.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.studin.app.ai.ClassifierProvider
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.platform.focus.FocusController
import com.studin.app.platform.gate.GateLauncher
import com.studin.domain.focus.FocusPolicyEngine
import com.studin.domain.model.RuleMode
import com.studin.domain.model.Verdict
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Focus Guard. Disclosed, opt-in, and active ONLY while a focus session is running.
 * - BLOCK apps: show the break gate when they come to the foreground.
 * - SMART apps: bounded read of visible text (titles), classified on-device; only
 *   high-confidence off-syllabus content is gated. Raw text is never stored or sent.
 * Never automates input or interferes with Settings/emergency flows.
 */
@AndroidEntryPoint
class StudinAccessibilityService : AccessibilityService() {
    @Inject lateinit var controller: FocusController
    @Inject lateinit var focusRepo: FocusRepository
    @Inject lateinit var classifiers: ClassifierProvider
    @Inject lateinit var prefs: UserPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val policy = FocusPolicyEngine()
    private val mutex = Mutex()

    private val lastInspect = ConcurrentHashMap<String, Long>()
    private val lastTextHash = ConcurrentHashMap<String, Int>()
    private val lastGate = ConcurrentHashMap<String, Long>()
    @Volatile private var lastWarnAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!controller.isSessionActive) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in IGNORED || policy.isEmergency(pkg)) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val now = SystemClock.elapsedRealtime()
        // Content changes are frequent: throttle per package before any work.
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - (lastInspect[pkg] ?: 0L) < THROTTLE_MS) return

        scope.launch { handle(pkg, type) }
    }

    private suspend fun handle(pkg: String, type: Int) = mutex.withLock {
        val rule = focusRepo.ruleFor(pkg) ?: return@withLock
        if (rule == RuleMode.ALLOW) return@withLock
        val pass = controller.isPassActive()
        if (rule == RuleMode.BLOCK) {
            if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return@withLock
            val d = policy.decide(pkg, rule, sessionActive = true, passActive = pass)
            if (d.verdict == Verdict.BLOCK) gate(pkg, d.reason, -1L)
            return@withLock
        }
        // SMART
        if (pass || !prefs.current().smartFilterEnabled) return@withLock
        val now = SystemClock.elapsedRealtime()
        if (now - (lastInspect[pkg] ?: 0L) < THROTTLE_MS) return@withLock
        lastInspect[pkg] = now
        val text = readVisibleText() ?: return@withLock
        val hash = text.hashCode()
        if (lastTextHash[pkg] == hash) return@withLock
        lastTextHash[pkg] = hash

        val result = classifiers.classifier.classify(text)
        if (result.verdict == Verdict.ALLOW || result.verdict == Verdict.UNKNOWN) return@withLock
        val decisionId = focusRepo.logDecision(pkg, result)
        val d = policy.decide(pkg, rule, sessionActive = true, passActive = false, contentVerdict = result.verdict, contentReason = result.reason)
        when (d.verdict) {
            Verdict.BLOCK -> gate(pkg, d.reason, decisionId)
            Verdict.WARN -> if (now - lastWarnAt > WARN_INTERVAL_MS) {
                lastWarnAt = now
                GateLauncher.notifyGentle(this, GateLauncher.appLabel(this, pkg), d.reason)
            }
            else -> Unit
        }
    }

    private fun gate(pkg: String, reason: String, decisionId: Long) {
        val now = SystemClock.elapsedRealtime()
        if (now - (lastGate[pkg] ?: 0L) < GATE_COOLDOWN_MS) return
        lastGate[pkg] = now
        controller.recordBlockedAttempt()
        GateLauncher.launch(this, pkg, reason, decisionId, fromAccessibility = true)
    }

    /** Bounded BFS over the active window: depth ≤ 25, ≤ 80 nodes, ≤ 2000 chars. */
    private fun readVisibleText(): String? {
        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return null
        val sb = StringBuilder()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        var visited = 0
        try {
            while (queue.isNotEmpty() && visited < MAX_NODES && sb.length < MAX_CHARS) {
                val (node, depth) = queue.removeFirst()
                visited++
                if (node.isVisibleToUser && !node.isPassword) {
                    val t = node.text ?: node.contentDescription
                    if (!t.isNullOrBlank() && t.length in 3..300) sb.append(t).append(" · ")
                }
                if (depth < MAX_DEPTH) {
                    for (i in 0 until node.childCount) {
                        val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                        if (queue.size + visited < MAX_NODES) queue.add(child to depth + 1)
                    }
                }
            }
        } catch (_: Exception) {
            // Nodes can go stale mid-walk; treat as "no text".
        }
        val text = sb.toString().take(MAX_CHARS)
        return text.ifBlank { null }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val THROTTLE_MS = 1_500L
        private const val GATE_COOLDOWN_MS = 10_000L
        private const val WARN_INTERVAL_MS = 120_000L
        private const val MAX_DEPTH = 25
        private const val MAX_NODES = 80
        private const val MAX_CHARS = 2_000
        private val IGNORED = setOf(
            "com.android.systemui", "com.google.android.inputmethod.latin", "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey", "com.miui.securitycenter", "android",
        )
    }
}
