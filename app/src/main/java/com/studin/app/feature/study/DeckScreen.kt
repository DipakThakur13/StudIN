package com.studin.app.feature.study

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DeckScreen(deckId: Long, onNavigate: (String) -> Unit, onBack: () -> Unit) { Text("Deck") }
