package com.studin.app.feature.focus

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.prefs.UserPreferences
import com.studin.app.core.data.repo.FocusRepository
import com.studin.app.platform.permissions.PermissionChecker
import com.studin.domain.focus.FocusPolicyEngine
import com.studin.domain.model.RuleMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val mode: RuleMode?,
    val emergency: Boolean,
)

data class RulesUi(
    val loading: Boolean = true,
    val apps: List<AppEntry> = emptyList(),
    val query: String = "",
    val allowKeywords: Set<String> = emptySet(),
    val blockKeywords: Set<String> = emptySet(),
    val guardEnabled: Boolean = false,
    val smartEnabled: Boolean = true,
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: FocusRepository,
    private val prefs: UserPreferences,
    private val permissions: PermissionChecker,
) : ViewModel() {
    private val policy = FocusPolicyEngine()
    private val installed = MutableStateFlow<List<Triple<String, String, ImageBitmap?>>?>(null)
    private val query = MutableStateFlow("")
    private val guard = MutableStateFlow(permissions.isAccessibilityEnabled())

    val ui: StateFlow<RulesUi> = combine(installed, repo.observeRules(), query, prefs.settings, guard) { apps, rules, q, s, g ->
        val modes = rules.associate { it.packageName to runCatching { RuleMode.valueOf(it.mode) }.getOrNull() }
        val list = apps.orEmpty()
            .filter { q.isBlank() || it.second.contains(q, true) || it.first.contains(q, true) }
            .map { (pkg, label, icon) -> AppEntry(pkg, label, icon, modes[pkg], policy.isEmergency(pkg)) }
            // Apps with a rule first, then alphabetical.
            .sortedWith(compareBy<AppEntry>({ it.mode == null || it.mode == RuleMode.ALLOW }, { it.label.lowercase() }))
        RulesUi(apps == null, list, q, s.allowKeywords, s.blockKeywords, g, s.smartFilterEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUi())

    init {
        viewModelScope.launch { installed.value = withContext(Dispatchers.IO) { loadLauncherApps() } }
    }

    fun refreshGuard() { guard.value = permissions.isAccessibilityEnabled() }
    fun setQuery(q: String) { query.value = q.take(40) }

    fun setMode(app: AppEntry, mode: RuleMode?) = viewModelScope.launch {
        if (app.emergency) return@launch
        if (mode == null || mode == RuleMode.ALLOW) repo.removeRule(app.packageName)
        else repo.setRule(app.packageName, app.label, mode)
    }

    /** Applies sensible defaults only to apps that are actually installed. */
    fun applySuggested() = viewModelScope.launch {
        val apps = installed.value.orEmpty().associate { it.first to it.second }
        var applied = 0
        SUGGESTED.forEach { (pkg, mode) ->
            apps[pkg]?.let { label -> repo.setRule(pkg, label, mode); applied++ }
        }
        // Also catch common game/social apps by package keyword.
        apps.forEach { (pkg, label) ->
            if (pkg !in SUGGESTED && GAME_HINTS.any { pkg.contains(it, true) } && !policy.isEmergency(pkg)) repo.setRule(pkg, label, RuleMode.BLOCK)
        }
    }

    fun setSmartEnabled(v: Boolean) = viewModelScope.launch { prefs.setSmartFilter(v) }

    fun addKeyword(allow: Boolean, word: String) = viewModelScope.launch {
        val w = word.trim().take(40)
        if (w.length < 2) return@launch
        val s = prefs.current()
        if (allow) prefs.setAllowKeywords(s.allowKeywords + w) else prefs.setBlockKeywords(s.blockKeywords + w)
    }

    fun removeKeyword(allow: Boolean, word: String) = viewModelScope.launch {
        val s = prefs.current()
        if (allow) prefs.setAllowKeywords(s.allowKeywords - word) else prefs.setBlockKeywords(s.blockKeywords - word)
    }

    private fun loadLauncherApps(): List<Triple<String, String, ImageBitmap?>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val sizePx = (40 * context.resources.displayMetrics.density).toInt().coerceAtLeast(32)
        return runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { ai ->
                val label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(ai.packageName)
                val icon = runCatching { ai.loadIcon(pm).toBitmap(sizePx).asImageBitmap() }.getOrNull()
                Triple(ai.packageName, label, icon)
            }
            .sortedBy { it.second.lowercase() }
            .toList()
    }

    private fun Drawable.toBitmap(size: Int): Bitmap {
        if (this is BitmapDrawable && bitmap != null) return Bitmap.createScaledBitmap(bitmap, size, size, true)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        setBounds(0, 0, size, size)
        draw(c)
        return bmp
    }

    companion object {
        val SUGGESTED: Map<String, RuleMode> = mapOf(
            "com.google.android.youtube" to RuleMode.SMART,
            "com.instagram.android" to RuleMode.BLOCK,
            "com.facebook.katana" to RuleMode.BLOCK,
            "com.facebook.lite" to RuleMode.BLOCK,
            "com.snapchat.android" to RuleMode.BLOCK,
            "com.twitter.android" to RuleMode.BLOCK,
            "in.mohalla.sharechat" to RuleMode.BLOCK,
            "in.mohalla.video" to RuleMode.BLOCK,
            "com.eterno.shortvideos" to RuleMode.BLOCK,
            "com.next.innovation.takatak" to RuleMode.BLOCK,
            "com.netflix.mediaclient" to RuleMode.BLOCK,
            "com.amazon.avod.thirdpartyclient" to RuleMode.BLOCK,
            "in.startv.hotstar" to RuleMode.BLOCK,
            "com.jio.media.ondemand" to RuleMode.BLOCK,
            "com.pubg.imobile" to RuleMode.BLOCK,
            "com.dts.freefireth" to RuleMode.BLOCK,
            "com.dts.freefiremax" to RuleMode.BLOCK,
            "com.supercell.clashofclans" to RuleMode.BLOCK,
            "com.miniclip.eightballpool" to RuleMode.BLOCK,
            "com.ludo.king" to RuleMode.BLOCK,
            "com.activision.callofduty.shooter" to RuleMode.BLOCK,
            "com.mojang.minecraftpe" to RuleMode.BLOCK,
            "com.roblox.client" to RuleMode.BLOCK,
            "com.king.candycrushsaga" to RuleMode.BLOCK,
            "com.reddit.frontpage" to RuleMode.BLOCK,
        )
        private val GAME_HINTS = listOf(".game", "games.", "supercell", "miniclip", "gameloft", "garena", "tencent.ig")
    }
}

/** Convenience used by RulesScreen headers. */
fun RulesUi.countFor(mode: RuleMode) = apps.count { it.mode == mode }
