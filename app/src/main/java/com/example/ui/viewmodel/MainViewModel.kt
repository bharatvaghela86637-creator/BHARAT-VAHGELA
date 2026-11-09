package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.DemoPreset
import com.example.data.engine.DownloadManagerEngine
import com.example.data.engine.LinkParserEngine
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.ResolutionOption
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class LibraryFilter {
    ALL,
    VIDEOS,
    AUDIO
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val downloadDao = db.downloadDao()
    private val downloadEngine = DownloadManagerEngine(application, downloadDao)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeMetadata = MutableStateFlow<VideoMetadata?>(null)
    val activeMetadata: StateFlow<VideoMetadata?> = _activeMetadata.asStateFlow()

    // Active Downloads (Queued, Downloading, Paused)
    val activeDownloads: StateFlow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw Completed Downloads
    private val rawCompletedDownloads = downloadDao.getCompletedDownloads()

    // Search and filter in library
    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter.asStateFlow()

    // Filtered Completed Downloads
    val filteredCompletedDownloads: StateFlow<List<DownloadEntity>> = combine(
        rawCompletedDownloads,
        _librarySearchQuery,
        _libraryFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.platformName.contains(query, ignoreCase = true)
            val matchesType = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.VIDEOS -> item.format.equals("MP4", ignoreCase = true)
                LibraryFilter.AUDIO -> item.format.equals("MP3", ignoreCase = true)
            }
            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently playing media in player screen
    private val _activePlayingMedia = MutableStateFlow<DownloadEntity?>(null)
    val activePlayingMedia: StateFlow<DownloadEntity?> = _activePlayingMedia.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        _errorMessage.value = null
    }

    fun onClearUrl() {
        _urlInput.value = ""
        _errorMessage.value = null
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    _urlInput.value = text.trim()
                    parseCurrentUrl()
                } else {
                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun selectPreset(preset: DemoPreset) {
        _urlInput.value = preset.sourceUrl
        viewModelScope.launch {
            _isParsing.value = true
            _errorMessage.value = null
            val result = LinkParserEngine.parseUrl(preset.sourceUrl)
            _isParsing.value = false
            result.onSuccess { metadata ->
                _activeMetadata.value = metadata
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to resolve link"
            }
        }
    }

    fun parseCurrentUrl() {
        val input = _urlInput.value.trim()
        if (input.isEmpty()) {
            _errorMessage.value = "Please enter or paste a valid video URL"
            return
        }

        viewModelScope.launch {
            _isParsing.value = true
            _errorMessage.value = null
            val result = LinkParserEngine.parseUrl(input)
            _isParsing.value = false
            result.onSuccess { metadata ->
                _activeMetadata.value = metadata
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to resolve video link"
            }
        }
    }

    fun dismissQualityPicker() {
        _activeMetadata.value = null
    }

    fun startDownloadWithQuality(metadata: VideoMetadata, option: ResolutionOption) {
        viewModelScope.launch {
            _activeMetadata.value = null
            val downloadFolder = downloadEngine.getDownloadDir()
            val sanitizedTitle = metadata.title.take(30).replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val extension = if (option.isAudioOnly) "mp3" else "mp4"
            val targetFile = File(downloadFolder, "${sanitizedTitle}_${option.qualityTag}_${System.currentTimeMillis()}.$extension")

            val entity = DownloadEntity(
                id = UUID.randomUUID().toString(),
                url = option.downloadUrl,
                title = metadata.title,
                author = metadata.author,
                platformName = metadata.platform.name,
                thumbnailUrl = metadata.thumbnailUrl,
                quality = option.label,
                format = option.format,
                localFilePath = targetFile.absolutePath,
                downloadedBytes = 0L,
                totalBytes = option.estimatedSizeBytes,
                statusName = DownloadStatus.QUEUED.name,
                speedBytesPerSec = 0L,
                durationSeconds = metadata.durationSeconds,
                createdAt = System.currentTimeMillis()
            )

            downloadDao.insert(entity)
            downloadEngine.startDownload(entity)
            _urlInput.value = ""
        }
    }

    fun pauseDownload(id: String) {
        downloadEngine.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            val entity = downloadDao.getDownloadById(id)
            if (entity != null) {
                downloadEngine.resumeDownload(entity)
            }
        }
    }

    fun cancelDownload(id: String) {
        downloadEngine.cancelDownload(id)
    }

    fun deleteDownload(entity: DownloadEntity) {
        viewModelScope.launch {
            val file = File(entity.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            downloadDao.deleteById(entity.id)
            if (_activePlayingMedia.value?.id == entity.id) {
                _activePlayingMedia.value = null
            }
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            val completed = downloadDao.getDownloadById("") // or clearCompleted
            downloadDao.clearCompleted()
        }
    }

    fun onSearchQueryChanged(q: String) {
        _librarySearchQuery.value = q
    }

    fun onFilterChanged(f: LibraryFilter) {
        _libraryFilter.value = f
    }

    fun playMedia(entity: DownloadEntity) {
        _activePlayingMedia.value = entity
    }

    fun closePlayer() {
        _activePlayingMedia.value = null
    }

    fun shareMedia(context: Context, entity: DownloadEntity) {
        try {
            val file = File(entity.localFilePath)
            if (!file.exists()) {
                Toast.makeText(context, "Media file not found on device", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = if (entity.format.equals("MP3", ignoreCase = true)) "audio/*" else "video/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, entity.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share ${entity.title}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
