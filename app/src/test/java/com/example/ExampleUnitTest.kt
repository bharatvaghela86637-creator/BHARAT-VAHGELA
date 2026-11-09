package com.example

import com.example.data.engine.LinkParserEngine
import com.example.data.model.PlatformType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPlatformDetection() {
        assertEquals(PlatformType.YOUTUBE, PlatformType.fromUrl("https://www.youtube.com/watch?v=12345"))
        assertEquals(PlatformType.YOUTUBE, PlatformType.fromUrl("https://youtu.be/12345"))
        assertEquals(PlatformType.INSTAGRAM, PlatformType.fromUrl("https://instagram.com/reel/abc123xyz"))
        assertEquals(PlatformType.TIKTOK, PlatformType.fromUrl("https://www.tiktok.com/@user/video/987654"))
        assertEquals(PlatformType.FACEBOOK, PlatformType.fromUrl("https://fb.watch/xyz123"))
        assertEquals(PlatformType.WHATSAPP, PlatformType.fromUrl("https://wa.me/status/123"))
        assertEquals(PlatformType.DIRECT, PlatformType.fromUrl("https://example.com/movie.mp4"))
    }

    @Test
    fun testLinkParserExtractsQualities() = runBlocking {
        val result = LinkParserEngine.parseUrl("https://example.com/test_video.mp4")
        assertTrue(result.isSuccess)
        val metadata = result.getOrNull()
        assertNotNull(metadata)
        assertEquals(5, metadata!!.availableResolutions.size)

        val tags = metadata.availableResolutions.map { it.qualityTag }
        assertTrue(tags.contains("1080p"))
        assertTrue(tags.contains("720p"))
        assertTrue(tags.contains("480p"))
        assertTrue(tags.contains("360p"))
        assertTrue(tags.contains("Audio"))
    }
}
