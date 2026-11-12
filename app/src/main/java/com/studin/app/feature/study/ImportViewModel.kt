package com.studin.app.feature.study

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studin.app.core.data.repo.StudyRepository
import com.studin.domain.importer.CsvDeckParser
import com.studin.domain.importer.CsvImportException
import com.studin.domain.importer.CsvImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class ImportUiState(
    val text: String = "",
    val deckTitle: String = "",
    val fileName: String? = null,
    val preview: CsvImportResult? = null,
    val error: String? = null,
    val reading: Boolean = false,
    val importing: Boolean = false,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val study: StudyRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()
    private var parseJob: Job? = null

    fun onTextChange(text: String) {
        val capped = if (text.length > CsvDeckParser.MAX_BYTES) text.take(CsvDeckParser.MAX_BYTES) else text
        _state.update { it.copy(text = capped, error = if (capped.length < text.length) "Text trimmed to 1 MB" else null) }
        schedulePreview(debounce = true)
    }

    fun onTitleChange(title: String) = _state.update { it.copy(deckTitle = title.take(80)) }

    /** Reads a user-picked file with a hard byte cap; never trusts the reported size alone. */
    fun loadFile(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(reading = true, error = null) }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                var name: String? = null; var size = -1L
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        name = c.getString(0)
                        if (!c.isNull(1)) size = c.getLong(1)
                    }
                }
                if (size > CsvDeckParser.MAX_BYTES) throw CsvImportException("File is ${size / 1024} KB – the limit is 1 MB")
                val bytes = resolver.openInputStream(uri)?.use { input ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        total += n
                        if (total > CsvDeckParser.MAX_BYTES) throw CsvImportException("File is larger than 1 MB")
                        out.write(buf, 0, n)
                    }
                    out.toByteArray()
                } ?: throw CsvImportException("Couldn't open that file")
                if (bytes.take(512).any { it == 0.toByte() }) throw CsvImportException("That doesn't look like a text/CSV file")
                (name ?: "Imported deck") to bytes.toString(Charsets.UTF_8)
            }
        }
        result.onSuccess { (name, text) ->
            val cleanName = name.substringAfterLast('/').substringBeforeLast('.').replace('_', ' ').trim().take(60)
            _state.update {
                it.copy(text = text, fileName = name.take(80), reading = false,
                    deckTitle = it.deckTitle.ifBlank { cleanName.ifBlank { "Imported deck" } })
            }
            schedulePreview(debounce = false)
        }.onFailure { e ->
            _state.update { it.copy(reading = false, error = e.message ?: "Couldn't read the file") }
        }
    }

    private fun schedulePreview(debounce: Boolean) {
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            if (debounce) delay(350)
            val text = _state.value.text
            if (text.isBlank()) { _state.update { it.copy(preview = null) }; return@launch }
            val parsed = withContext(Dispatchers.Default) { runCatching { CsvDeckParser.parse(text) } }
            parsed.onSuccess { r -> _state.update { it.copy(preview = r, error = null) } }
                .onFailure { e -> _state.update { it.copy(preview = null, error = e.message ?: "Invalid CSV") } }
        }
    }

    fun import(onDone: (deckId: Long, count: Int) -> Unit) {
        val s = _state.value
        if (s.importing || s.text.isBlank()) return
        _state.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            runCatching { study.importCsv(s.deckTitle.ifBlank { "Imported deck" }, s.text) }
                .onSuccess { (id, result) ->
                    if (id <= 0) _state.update { it.copy(importing = false, error = "No valid cards found. Each row needs a front and a back.") }
                    else { _state.update { it.copy(importing = false) }; onDone(id, result.cards.size) }
                }
                .onFailure { e ->
                    _state.update { it.copy(importing = false, error = (e as? CsvImportException)?.message ?: "Import failed") }
                }
        }
    }
}
