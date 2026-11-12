package com.studin.app.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-device OCR (ML Kit, bundled model – works offline) for scanning notes. */
@Singleton
class OcrEngine @Inject constructor(@ApplicationContext private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    // Keep paragraph structure: one line per text block line.
                    val text = result.textBlocks.joinToString("\n") { b -> b.lines.joinToString(" ") { it.text } }
                    cont.resume(text.take(20_000))
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
