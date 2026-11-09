package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PlatformType(
    val displayName: String,
    val brandColor: Color,
    val iconName: String
) {
    YOUTUBE("YouTube", Color(0xFFFF0033), "youtube"),
    INSTAGRAM("Instagram", Color(0xFFE1306C), "instagram"),
    FACEBOOK("Facebook", Color(0xFF1877F2), "facebook"),
    TIKTOK("TikTok", Color(0xFF00F2FE), "tiktok"),
    WHATSAPP("WhatsApp", Color(0xFF25D366), "whatsapp"),
    DIRECT("Direct URL", Color(0xFF00E599), "link");

    companion object {
        fun fromUrl(url: String): PlatformType {
            val lower = url.lowercase()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("instagram.com") || lower.contains("instagr.am") -> INSTAGRAM
                lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> FACEBOOK
                lower.contains("tiktok.com") -> TIKTOK
                lower.contains("whatsapp.com") || lower.contains("wa.me") -> WHATSAPP
                else -> DIRECT
            }
        }
    }
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class ResolutionOption(
    val id: String = UUID.randomUUID().toString(),
    val label: String,            // e.g. "1080p FHD"
    val qualityTag: String,       // "1080p", "720p", "480p", "360p", "Audio"
    val resolution: String,      // "1920x1080"
    val format: String,          // "MP4", "MP3"
    val estimatedSizeBytes: Long,
    val downloadUrl: String,
    val isAudioOnly: Boolean = false,
    val bitrate: String = "High Bitrate"
)

data class VideoMetadata(
    val url: String,
    val title: String,
    val author: String,
    val platform: PlatformType,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val availableResolutions: List<ResolutionOption>
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val author: String,
    val platformName: String,
    val thumbnailUrl: String?,
    val quality: String,
    val format: String,
    val localFilePath: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val statusName: String = DownloadStatus.QUEUED.name,
    val speedBytesPerSec: Long = 0L,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val status: DownloadStatus
        get() = try {
            DownloadStatus.valueOf(statusName)
        } catch (_: Exception) {
            DownloadStatus.FAILED
        }

    val platform: PlatformType
        get() = try {
            PlatformType.valueOf(platformName)
        } catch (_: Exception) {
            PlatformType.DIRECT
        }

    val progressPercent: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
