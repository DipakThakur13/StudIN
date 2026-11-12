package com.studin.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.studin.app.core.designsystem.GlassCard
import com.studin.app.core.designsystem.IconBubble
import com.studin.app.core.designsystem.Studin
import com.studin.app.core.designsystem.StudinColors

private data class PolicySection(val icon: ImageVector, val color: Color, val title: String, val points: List<String>)

private val sections = listOf(
    PolicySection(Icons.Rounded.CloudOff, StudinColors.Mint, "Your data stays on your device", listOf(
        "StudIN does not request the internet permission. It cannot send your data anywhere.",
        "Profile, syllabus, flashcards, focus sessions and insights are stored in a private database inside the app.",
        "There are no ads, no analytics SDKs and no accounts.",
    )),
    PolicySection(Icons.Rounded.Key, StudinColors.Cyan, "What each permission is for", listOf(
        "Notifications – the running timer and one optional daily reminder.",
        "Usage access – to know which app is in front during a focus session and to show screen-time insights.",
        "Display over other apps – to show the break screen above a blocked app.",
        "Unrestricted battery – so the timer isn't killed by aggressive battery savers.",
        "Every permission is optional and can be revoked from Permission health.",
    )),
    PolicySection(Icons.Rounded.Accessibility, StudinColors.Violet, "Focus Guard (Accessibility) disclosure", listOf(
        "Only if you enable it, Focus Guard reads the foreground app name and, for apps you set to Smart mode, the visible on-screen titles.",
        "It is active only during focus sessions you start.",
        "Titles are classified on-device and immediately discarded – never uploaded or saved.",
        "It never performs actions for you and never blocks calls, Settings or accessibility tools.",
    )),
    PolicySection(Icons.Rounded.Psychology, StudinColors.Star, "On-device AI", listOf(
        "The Lite AI and the optional language model run entirely on your phone.",
        "Model files are ones you import yourself; StudIN shows their SHA-256 checksum so you can verify them.",
        "Tutor chats are kept in memory only and disappear when you leave the screen.",
    )),
    PolicySection(Icons.Rounded.Timelapse, StudinColors.Teal, "Retention", listOf(
        "Focus Guard decisions keep only the verdict, confidence and matched keywords (never the title) and are deleted after 30 days.",
        "Everything else is kept until you delete it.",
    )),
    PolicySection(Icons.Rounded.DeleteSweep, StudinColors.Coral, "Your controls", listOf(
        "Export all your data as JSON from Settings → Export my data.",
        "Delete everything instantly from Settings → Delete all data, or by uninstalling the app.",
        "Android backups exclude imported AI models.",
    )),
    PolicySection(Icons.Rounded.ChildCare, StudinColors.Mint, "Young students", listOf(
        "StudIN collects no personal data from anyone, including students under 18.",
        "Parents and teachers cannot see a student's data unless the student exports and shares it.",
    )),
    PolicySection(Icons.Rounded.MailOutline, StudinColors.Cyan, "Contact", listOf(
        "Questions or concerns: support@studin.app (placeholder – replace before publishing).",
        "Policy version 1.0 · November 2026.",
    )),
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Privacy policy", "Plain language, no surprises", onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "StudIN is built to help you study, not to watch you. This page explains exactly what the app touches.",
                    style = MaterialTheme.typography.bodyLarge, color = Studin.extras.muted,
                )
            }
            items(sections, key = { it.title }) { sec ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(sec.icon, sec.color, 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(sec.title, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    sec.points.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Rounded.Storage, StudinColors.InkMuted, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Data location: this device only.", style = MaterialTheme.typography.labelMedium, color = Studin.extras.muted)
                }
            }
        }
    }
}
