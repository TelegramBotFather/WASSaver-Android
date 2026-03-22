# WASSaver - WhatsApp Status Saver & Tools

A feature-rich Android app to view, save, and manage WhatsApp statuses — plus powerful tools for video splitting, deleted message recovery, direct messaging, and more.

📥 **[Download Latest Release](https://github.com/PBhadoo/WASSaver-Android/releases)**

## Features

### 📱 Home Menu Dashboard
- Beautiful card-based navigation with all features organized into sections
- Quick access to Status Tools, Sharing Tools, and App settings

### 📥 Status Viewer
- **WhatsApp & WA Business support** — Switch between WA and WAB status folders
- **Media filters** — Filter by All, Photos, or Videos
- **Grid view** — Thumbnail grid with photo/video type badges and save buttons
- **Full-screen viewer** — Swipe through statuses with video playback (ExoPlayer)
- **Save to gallery** — One-tap save to Pictures/WASSaver folder
- **Repost to WhatsApp** — Share directly back to WhatsApp
- **Share** — Share to any app via Android share sheet

### 🖼️ Media Browser
- Browse WhatsApp's private media folders (Images/Private & Video/Private)
- View once photos and videos — save them before they disappear
- Separate permission grants for image and video folders

### 💾 Saved Statuses
- Browse all previously saved statuses with media filters
- Multi-select mode for bulk deletion
- Individual delete with confirmation

### ✂️ Status Splitter
- **Split long videos into 90-second parts** perfect for WhatsApp Status
- Pick any video from your gallery or file manager
- Shows video preview, duration, and estimated number of parts
- Uses Android's built-in MediaExtractor + MediaMuxer (no FFmpeg needed)
- **Preserves original quality** — no re-encoding, just fast stream copying
- Share each part directly to WhatsApp Status with one tap
- Progress indicator during splitting

### 🗑️ Deleted Messages Recovery
- **Capture WhatsApp messages via notification listener**
- Even if the sender deletes a message, you still have a copy
- **Opt-in feature** — disabled by default, user must explicitly enable it
- Messages stored **locally only** — nothing sent to any server
- **Grouped by sender** — contact list view like WhatsApp chat list
- Tap into any contact to see chat-style message bubbles (oldest first)
- **Auto-refreshes every 3 seconds** when new messages arrive
- Timestamps with seconds precision for proper ordering
- **Search** across all messages and contacts
- **Clear per-chat** — delete all messages from one sender
- **Clear all** — wipe everything
- **Disable capture** anytime with one tap
- Supports both WhatsApp and WA Business
- Detects group messages and shows sender + group name

### 💬 Direct Message
- Send a message to any phone number without saving it as a contact
- Support for WhatsApp and WA Business
- Country code + phone number input with optional message

### 🔄 Check for Updates
- Checks for new releases from GitHub (`PBhadoo/WASSaver-Android/releases`)
- Shows current version vs latest version
- Release history with "INSTALLED" badge on current version
- Download APK or view release notes directly
- Uses GitHub API — no external dependencies

### ℹ️ About
- App info, version, and feature list
- Developer credits
- Links to GitHub source code and issue tracker

### 🎨 Design
- **Dark/Light theme** — WhatsApp-inspired green color palette, follows system theme
- Material 3 / Material You design language
- Smooth back navigation on all screens

## Tech Stack

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Coil** for image loading & video thumbnails
- **Media3/ExoPlayer** for video playback
- **MediaExtractor + MediaMuxer** for video splitting (zero external dependencies)
- **NotificationListenerService** for deleted message capture
- **Storage Access Framework (SAF)** for Android 11+ compatibility
- **MVVM architecture** with ViewModels and StateFlow
- No external networking library — uses built-in `HttpURLConnection` + `org.json`

## Permissions

| Permission | Purpose |
|---|---|
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | Access WhatsApp status files |
| `READ_EXTERNAL_STORAGE` (≤ API 32) | Legacy storage access |
| `INTERNET` | Check for app updates via GitHub API |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Deleted message capture (opt-in) |
| `REQUEST_INSTALL_PACKAGES` | Install APK updates |

## Requirements

- Android 8.0 (API 26) or higher
- WhatsApp / WhatsApp Business installed

## CI/CD — GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/build.yml`) that automatically builds a signed release APK.

### Setup GitHub Secrets

Go to your repo → **Settings → Secrets and variables → Actions** and add these 4 secrets:

| Secret Name | Description |
|---|---|
| `KEYSTORE_BASE64` | Your keystore file encoded as base64 |
| `KEYSTORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key password |

### How to encode your keystore as base64

```bash
# On Linux/Mac:
base64 -i your-keystore.jks | tr -d '\n'

# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks"))
```

Copy the output and paste it as the `KEYSTORE_BASE64` secret.

### How it works

- **On every push to `main`** — Builds signed APK, uploads as artifact
- **On tag push (`v*`)** — Builds signed APK + creates a GitHub Release with the APK attached
- **Manual trigger** — Use "Run workflow" button in Actions tab

### Creating a release

```bash
git tag v1.0.1
git push origin v1.0.1
```

This will trigger the workflow, build a signed `WASSaver-v1.0.1.apk`, and create a GitHub Release automatically.

## Changelog

### v1.0.1
- ✂️ Status Splitter — split videos into 90s parts for WhatsApp Status
- 🗑️ Deleted Messages Recovery — capture messages via notification listener
- 🔄 Update Checker — check GitHub releases for updates
- 🏠 Home Menu Dashboard — card-based navigation
- ℹ️ About Screen — app info and credits
- Auto-refresh messages, seconds-precision timestamps
- Removed bottom tab navigation in favor of menu dashboard

### v1.0.0
- Initial release
- Status viewer, media browser, saved statuses
- Direct message, dark/light theme

---

Built with ❤️ by Parveen Bhadoo

Credits: [Claude Opus 4](https://anthropic.com) by [Anthropic](https://anthropic.com)
