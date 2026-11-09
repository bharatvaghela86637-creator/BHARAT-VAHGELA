# FREE DOWNLOAD - Ad-Free Multi-Platform Video Downloader & Offline Player

A high-performance, 100% ad-free Android application that empowers users to paste links from major video and social platforms, select desired video resolutions (1080p, 720p, 480p, 360p, Audio MP3), download files at high speeds with real-time bandwidth telemetry, and manage offline playback using an advanced gesture-driven Media3 ExoPlayer with speed control and Picture-in-Picture.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction and UX decisions below reflect the user's responses during Phase 1 clarification:

- **Confirmed Decision 1 (Link Detection & Parsing Engine)**: Universal link parser supporting social URLs (YouTube, Instagram, Facebook, TikTok, WhatsApp direct/status) paired with a high-speed direct media downloader and rich sample test presets for immediate offline testing.
- **Confirmed Decision 2 (Visual Theme & Palette)**: Modern Dark Cyber/Neon aesthetic featuring an OLED pitch background (`#0B0E14`), deep slate container cards (`#141923`), with electric cyan (`#00F0FF`) and hyper-emerald (`#00E599`) accents.
- **Confirmed Decision 3 (Offline Media Player)**: Jetpack Media3 ExoPlayer integration featuring custom gesture controls (swipe brightness on left, swipe volume on right, double-tap seek $\pm 10$s), variable playback speed (0.5x to 2.0x), and system Picture-in-Picture (PiP).
- **Zero-Ad Commitment**: The app contains zero ad banners, interstitials, or tracking SDKs, fulfilling the strict 100% ad-free requirement.

---

## 1. Overview & Core Concept

- **What It Does**: "FREE DOWNLOAD" simplifies downloading web and social media video content. Users paste any URL (or tap preset demo media), the app extracts video metadata (thumbnail, title, duration, author, source platform), displays an interactive Quality Picker modal (1080p HD, 720p HD, 480p SD, 360p, or Audio MP3), and initiates concurrent high-speed chunked downloads with real-time speed metrics (MB/s, ETA, progress bar). Downloaded files are saved to private app storage (`context.filesDir` / scoped media files) and recorded into a local Room database with full offline playback.
- **Target Audience / Persona**: Users seeking a lightweight, respectful, lightning-fast downloader without spammy popups, predatory subscription walls, or ad clutter.
- **Key Value**: Instant link resolution, clear quality choices with file size estimates, pause/resume download engine, and a fluid built-in player that works completely offline.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Link Input & Extraction**:
   - The user opens the app to the **Downloader** tab. A prominent glowing neon search bar invites them to paste a link.
   - Quick platform badge chips (YouTube, Instagram, TikTok, Facebook, WhatsApp, Direct MP4) provide one-tap demo links to test download capabilities instantly.
   - An "Auto-detect from Clipboard" button allows 1-tap pasting when the user copies a link from another app.
2. **Quality & Format Selection**:
   - Tapping "Fetch Info" opens a sleek bottom sheet displaying the video title, preview thumbnail, creator, and available download streams:
     - `1080p FHD` (60fps / High Bitrate)
     - `720p HD` (Standard crisp)
     - `480p SD` (Data saver)
     - `360p Low` (Ultra fast)
     - `Audio Only (MP3/M4A)` (Music extraction)
   - Each stream displays estimated file size and video codec badge.
3. **High-Speed Download Execution**:
   - Upon quality selection, the download task is queued in the **Active Downloads** panel.
   - Real-time progress displays percent completed, active download speed (e.g., `4.8 MB/s`), downloaded vs total megabytes, and remaining time (ETA).
   - Controls allow Pause, Resume, or Cancel at any point.
4. **Download Manager & Library**:
   - The **Library** tab displays all downloaded media with thumbnail, platform badge, duration, file size, date, and storage usage summary.
   - Filter chips: All, Videos, Audio, Favorites.
   - Options to Play, Share via standard Android Intent, Rename, or Delete.
5. **Advanced Offline ExoPlayer**:
   - Tapping any downloaded video launches full-screen immersive playback.
   - Swipe gestures: vertical drag on left screen adjusts screen brightness; vertical drag on right adjusts volume.
   - Double tap left/right to rewind/fast-forward 10 seconds with ripple animations.
   - Top bar controls: Back, Title, Aspect Ratio fit/fill toggle, Playback Speed menu (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x), and PiP button.

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech Dark Cyberpunk / Neon Studio. Clean, dark, responsive, and clutter-free.
- **Color Palette**:
  - `Background`: Deep Midnight Slate (`#0B0E14`)
  - `Surface`: Dark Obsidian (`#131822`)
  - `Surface Variant`: Midnight Charcoal (`#1B2232`)
  - `Primary / Accent Cyan`: Electric Cyan (`#00F0FF`)
  - `Secondary / Accent Emerald`: Vivid Neon Green (`#00E599`)
  - `Warning / Amber`: Vivid Amber (`#FFB800`)
  - `Error / Coral`: Neon Coral (`#FF4565`)
  - `On-Surface Text`: Crisp White (`#F0F4FC`) & Muted Slate (`#8E9BAE`)
- **Typography & Hierarchy**:
  - Display / Title: SemiBold geometric headings with neon cyan glows.
  - Body: High-legibility sans-serif with generous line-height.
  - Monospace Data: Clean tabular numbers for speed counters (`MB/s`) and timestamps.
- **Component Styling & Layout**:
  - Pill-shaped tab navigation bar with glowing indicator.
  - Glassmorphic card surfaces with subtle 1dp border stroke (`#1E293B`).
  - Haptic feedback on button clicks and gesture thresholds.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Storage Strategy & Scoped Storage Compliance**:
  - *Chosen Approach*: Save downloaded media directly into app-specific external storage (`context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)` and internal fallback).
  - *Why*: Google Play Developer Policy prohibits legacy `WRITE_EXTERNAL_STORAGE` on Android 10+ (API 29+). App-specific storage requires **zero runtime permissions**, never triggers dangerous permission dialogs, guarantees instant file access for offline ExoPlayer, and supports file sharing via `FileProvider`.
  - *Alternatives Considered*: MediaStore insertion (requires extra SAF picker or scoped permissions and slower streaming).
- **Decision 2: Download Engine Architecture**:
  - *Chosen Approach*: Kotlin Coroutines + OkHttp streaming with buffer chunks and notification/progress state flow.
  - *Why*: Allows fine-grained control over chunked downloads, pause/resume capabilities via HTTP `Range` headers, accurate instant download speed calculation, and responsive UI updates without relying on system DownloadManager bugs on various vendor ROMs.
- **Decision 3: Video Player Implementation**:
  - *Chosen Approach*: Jetpack Media3 ExoPlayer (`androidx.media3:media3-exoplayer:1.5.1` and `androidx.media3:media3-ui:1.5.1`).
  - *Why*: Official modern standard for media playback on Android, seamless local MP4/MKV/WebM codec support, Picture-in-Picture lifecycle compatibility, and extensible gesture overlays.
- **Decision 4: Local Persistence**:
  - *Chosen Approach*: Room Database (`AppDatabase`, `DownloadDao`, `DownloadEntity`).
  - *Why*: Fully reactive `Flow<List<DownloadEntity>>`, persistent history across app restarts, instant sorting and querying by platform or status.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          FREE DOWNLOAD UI LAYER                         │
│                                                                        │
│   ┌─────────────────────┐   ┌───────────────────┐   ┌──────────────┐   │
│   │   DownloaderScreen  │   │   LibraryScreen   │   │ VideoPlayer  │   │
│   │  (URL, Chips, Demos)│   │(History, Manage)  │   │  (ExoPlayer) │   │
│   └──────────┬──────────┘   └─────────┬─────────┘   └───────▲──────┘   │
│              │                        │                     │          │
│              ▼                        ▼                     │          │
│   ┌─────────────────────────────────────────────┐           │          │
│   │            MainViewModel (StateFlow)        │───────────┘          │
│   └──────────────────────┬──────────────────────┘                      │
└──────────────────────────┼─────────────────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────────────────┐
│                         CORE DOMAIN & SERVICES                         │
│                                                                        │
│   ┌───────────────────────┐             ┌──────────────────────────┐   │
│   │  LinkExtractorEngine  │             │   DownloadManagerEngine  │   │
│   │- Social URL Parser    │             │- OkHttp Streaming Client │   │
│   │- Resolution Resolver  │             │- Chunked Progress/Speed  │   │
│   │- Demo Presets Bundle  │             │- Pause / Resume / Cancel │   │
│   └───────────────────────┘             └─────────────┬────────────┘   │
└───────────────────────────────────────────────────────┼────────────────┘
                                                        │
┌───────────────────────────────────────────────────────▼────────────────┐
│                         PERSISTENCE & STORAGE                          │
│                                                                        │
│   ┌────────────────────────────────┐    ┌──────────────────────────┐   │
│   │  Room DB (DownloadEntity)      │    │  Local App Media Storage │   │
│   │- Id, Title, SourcePlatform     │    │  /files/movies/*.mp4     │   │
│   │- Quality, FileSize, Progress   │    │  (Offline Video Files)   │   │
│   │- Status: Completed/Active/Pause│    │                          │   │
│   └────────────────────────────────┘    └──────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models & Entities

- **`DownloadEntity` (Room `@Entity`)**:
  - `id`: `String` (UUID primary key)
  - `url`: `String` (source link)
  - `title`: `String`
  - `platform`: `PlatformType` (YOUTUBE, INSTAGRAM, FACEBOOK, TIKTOK, WHATSAPP, DIRECT)
  - `thumbnailUrl`: `String?`
  - `quality`: `String` ("1080p", "720p", "480p", "360p", "Audio")
  - `localPath`: `String` (absolute path to cached media file)
  - `totalBytes`: `Long`
  - `downloadedBytes`: `Long`
  - `status`: `DownloadStatus` (`QUEUED`, `DOWNLOADING`, `PAUSED`, `COMPLETED`, `FAILED`)
  - `speedBytesPerSec`: `Long`
  - `durationSec`: `Int`
  - `createdAt`: `Long`

- **`VideoResolutionOption`**:
  - `label`: `String` ("1080p Full HD", "720p HD", "480p SD", "Audio MP3")
  - `qualityTag`: `String`
  - `estimatedSizeBytes`: `Long`
  - `downloadUrl`: `String`
  - `format`: `String` ("mp4", "mp3")

### Interactive State Transitions

1. **Extract Link**: `Idle` $\rightarrow$ `Parsing` $\rightarrow$ `QualityPickerSheetVisible(options)`
2. **Start Download**: `QualitySelected` $\rightarrow$ Insert queued row in Room $\rightarrow$ Trigger coroutine download task $\rightarrow$ UI switches to Live Progress Card.
3. **Download Loop**: Emits progress delta every 200ms with smooth speed smoothing $\rightarrow$ Room status updated to `COMPLETED` on finish.
4. **Playback**: Tapping completed item in Library launches `VideoPlayerScreen(filePath)` attached to `ExoPlayer.Builder(context).build()` with gesture overlays and PiP mode support.

---

## Implementation Steps & Verification Plan

1. **Dependencies & Version Catalog**: Add Media3 ExoPlayer (`media3-exoplayer`, `media3-ui`) to `libs.versions.toml` and `app/build.gradle.kts`. Enable coil and navigation dependencies if needed.
2. **Metadata & Manifest**: Update `metadata.json` ("FREE DOWNLOAD") and `strings.xml`. Ensure `INTERNET` and `FOREGROUND_SERVICE` are in `AndroidManifest.xml` with zero dangerous runtime storage permissions.
3. **Room Database & Models**: Create `DownloadEntity`, `DownloadDao`, and `AppDatabase`.
4. **Download & Extraction Engine**: Implement `DownloadEngine` with OkHttp streaming, byte counter, speed calculations, pause/resume, and platform link resolution with high-speed sample feeds.
5. **UI Layer & Jetpack Compose Components**:
   - `DownloaderScreen`: Glowing neon input, clipboard paste button, platform demo chips, resolution bottom sheet.
   - `LibraryScreen`: Downloaded items list, search/filter, storage gauge, share/delete actions.
   - `VideoPlayerScreen`: Modern ExoPlayer composable with custom gesture overlays (volume, brightness, seek), speed selector, and PiP handler.
6. **Theme & Branding**: Neon cyan/emerald palette, custom adaptive launcher icon for FREE DOWNLOAD.
7. **Verification**: Run `compile_applet` to ensure clean build with zero warnings or errors.
