package com.studin.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studin.app.R
import com.studin.app.core.designsystem.Spacing
import com.studin.app.core.designsystem.StudINIllustration
import com.studin.app.core.designsystem.StudINScaffold
import com.studin.app.core.designsystem.StudINSectionHeader
import com.studin.app.core.designsystem.StudINTheme
import com.studin.app.core.designsystem.screenContentPadding
import com.studin.app.core.designsystem.studinContentWidth

internal data class PolicySection(val title: String, val points: List<String>)

internal val privacySections = listOf(
    PolicySection("Your data stays on your device", listOf(
        "StudIN does not request the internet permission. It cannot send your data anywhere.",
        "Profile, syllabus, flashcards, focus sessions and insights are stored in a private database inside the app.",
        "There are no ads, no analytics SDKs and no accounts.",
    )),
    PolicySection("What each permission is for", listOf(
        "Notifications – the running timer and one optional daily reminder.",
        "Usage access – to know which app is in front during a focus session and to show screen-time insights.",
        "Display over other apps – to show the break screen above a blocked app.",
        "Unrestricted battery – so the timer isn't stopped by aggressive battery savers.",
        "Every permission is optional and can be revoked from Permission health.",
    )),
    PolicySection("Focus Guard (Accessibility) disclosure", listOf(
        "Only if you enable it, Focus Guard reads the foreground app name and, for apps you set to Smart mode, the visible on-screen titles.",
        "It is active only during focus sessions you start.",
        "Titles are classified on-device and immediately discarded – never uploaded or saved.",
        "It never performs actions for you and never blocks calls, Settings or accessibility tools.",
    )),
    PolicySection("On-device AI", listOf(
        "Lite AI and the optional language model run entirely on your phone.",
        "Model files are ones you import yourself; StudIN shows their SHA-256 checksum so you can verify them.",
        "Tutor chats are kept in memory only and disappear when you leave AI Tutor.",
    )),
    PolicySection("Retention", listOf(
        "Focus Guard decisions keep only the verdict, confidence and matched keywords (never the title) and are deleted after 30 days.",
        "Everything else is kept until you delete it.",
    )),
    PolicySection("Your controls", listOf(
        "Export all your data as JSON from Settings → Export my data.",
        "Delete everything instantly from Settings → Delete all data, or by uninstalling the app.",
        "Android backups exclude imported AI models.",
    )),
    PolicySection("Young students", listOf(
        "StudIN collects no personal data from anyone, including students under 18.",
        "Parents and teachers cannot see a student's data unless the student exports and shares it.",
    )),
    PolicySection("Contact", listOf(
        "Questions or concerns: support@studin.app (placeholder – replace before publishing).",
        "Policy version 1.0 · November 2026.",
    )),
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    StudINScaffold(title = "Privacy policy", subtitle = "Plain language, no surprises", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().studinContentWidth(),
            contentPadding = screenContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "hero") {
                Column {
                    StudINIllustration(R.drawable.ill_privacy, contentDescription = null, height = 160.dp)
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "StudIN is built to help you study, not to watch you. This page explains exactly what the app touches.",
                        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.widthIn(max = 600.dp),
                    )
                }
            }
            items(privacySections, key = { it.title }) { sec ->
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = StudINTheme.colors.border, modifier = Modifier.padding(bottom = Spacing.md))
                    StudINSectionHeader(sec.title)
                    sec.points.forEach { point ->
                        Row(Modifier.padding(vertical = Spacing.xxs).widthIn(max = 600.dp)) {
                            Box(Modifier.padding(top = 9.dp).size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.width(Spacing.sm))
                            Text(point, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item(key = "location") {
                Text("Data location: this device only.", style = MaterialTheme.typography.labelMedium, color = StudINTheme.colors.textSecondary)
            }
        }
    }
}
