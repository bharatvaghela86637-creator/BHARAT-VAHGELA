package com.example.data.engine

import android.content.Context
import android.os.Environment
import com.example.data.local.DownloadDao
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManagerEngine(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun getDownloadDir(): File {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val target = if (moviesDir != null && moviesDir.exists()) moviesDir else context.filesDir
        val appFolder = File(target, "FreeDownload")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    fun startDownload(entity: DownloadEntity) {
        // Cancel existing job if running
        activeJobs[entity.id]?.cancel()

        val job = scope.launch {
            try {
                executeDownload(entity)
            } catch (e: CancellationException) {
                // User paused or cancelled
            } catch (e: Exception) {
                downloadDao.updateStatus(entity.id, DownloadStatus.FAILED.name)
            } finally {
                activeJobs.remove(entity.id)
            }
        }
        activeJobs[entity.id] = job
    }

    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            downloadDao.updateStatus(id, DownloadStatus.PAUSED.name)
        }
    }

    fun resumeDownload(entity: DownloadEntity) {
        startDownload(entity)
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            val entity = downloadDao.getDownloadById(id)
            if (entity != null) {
                val file = File(entity.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                downloadDao.deleteById(id)
            }
        }
    }

    private suspend fun executeDownload(initialEntity: DownloadEntity) = withContext(Dispatchers.IO) {
        val file = File(initialEntity.localFilePath)
        val existingBytes = if (file.exists()) file.length() else 0L

        downloadDao.updateStatus(initialEntity.id, DownloadStatus.DOWNLOADING.name)

        val requestBuilder = Request.Builder()
            .url(initialEntity.url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) FreeDownloadApp/1.0")

        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                downloadDao.updateStatus(initialEntity.id, DownloadStatus.FAILED.name)
                return@withContext
            }

            val body = response.body ?: run {
                downloadDao.updateStatus(initialEntity.id, DownloadStatus.FAILED.name)
                return@withContext
            }

            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) {
                existingBytes + contentLength
            } else {
                if (initialEntity.totalBytes > 0) initialEntity.totalBytes else 25_000_000L
            }

            var downloadedBytes = existingBytes
            val outputStream = FileOutputStream(file, existingBytes > 0)
            val inputStream: InputStream = body.byteStream()
            val buffer = ByteArray(64 * 1024) // 64 KB buffer for high speed
            var bytesRead: Int

            var lastReportTime = System.currentTimeMillis()
            var bytesSinceLastReport = 0L
            var smoothedSpeed = 0L

            outputStream.use { out ->
                inputStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) break

                        out.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        bytesSinceLastReport += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastReportTime
                        if (elapsed >= 300) { // report speed and progress every 300ms
                            val instantSpeed = (bytesSinceLastReport * 1000) / elapsed
                            smoothedSpeed = if (smoothedSpeed == 0L) {
                                instantSpeed
                            } else {
                                (smoothedSpeed * 0.7f + instantSpeed * 0.3f).toLong()
                            }

                            downloadDao.updateProgress(
                                id = initialEntity.id,
                                downloaded = downloadedBytes,
                                total = totalBytes,
                                speed = smoothedSpeed,
                                status = DownloadStatus.DOWNLOADING.name
                            )
                            bytesSinceLastReport = 0L
                            lastReportTime = now
                        }
                    }
                }
            }

            if (isActive) {
                // Completed!
                downloadDao.updateProgress(
                    id = initialEntity.id,
                    downloaded = totalBytes,
                    total = totalBytes,
                    speed = 0L,
                    status = DownloadStatus.COMPLETED.name
                )
            }
        }
    }
}
