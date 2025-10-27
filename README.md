# Syndicate RSS Reader

A modern Android RSS reader built with Jetpack Compose and Material 3 design, featuring a responsive two-pane layout for tablets and wide screens.

## Features

### ✅ Implemented
- **Modern Material 3 Design**: Clean, intuitive interface following Material Design guidelines
- **Responsive Layout**: Two-pane layout for tablets, single-pane for phones
- **Feed Management**: Add, organize, and delete RSS feeds with swipe gestures
- **Favicon Support**: Automatic favicon loading for visual feed identification
- **OPML Export**: Export your feed subscriptions in OPML format
- **Dark/Light Theme**: System-aware theming with manual override options
- **Smooth Animations**: Polished transitions and micro-interactions
- **Material 3 List-Detail Layout**: Selected feeds highlighted with proper Material colors
- **Swipe Gestures**: Swipe feeds to delete or add to groups with animated feedback

### 🚧 Planned Features
- **Feed Groups/Folders**: Organize feeds into custom groups and folders
- **OPML Import**: Import feed subscriptions from OPML files
- **Article Reading**: Full article content display with WebView
- **Read/Unread Status**: Mark articles as read/unread with swipe gestures
- **Pull-to-Refresh**: Manual refresh to fetch new articles
- **Article Search**: Search through article titles and descriptions
- **Article Detail Screen**: In-app article viewer with browser fallback
- **Feed Availability Monitoring**: Track and display feed status
- **Article Filtering**: Filter by read/unread status, sort by date
- **Article Sharing**: Share articles via Android's native share system
- **Background Sync**: Periodic article fetching with per-feed notifications

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Database**: Room
- **DI**: Hilt
- **HTTP**: OkHttp + Retrofit
- **Image Loading**: Coil
- **RSS Parsing**: Custom XML parser

## Building from Source

1. Clone the repository
2. Open the project in Android Studio
3. Build and run: `./gradlew assembleDebug`

## Usage

1. **Adding Feeds**: Tap the + button to add RSS feeds by URL
2. **Feed Organization**: Swipe feeds to delete or organize into groups
3. **Reading Articles**: Tap any feed to view its articles
4. **Export**: Use Settings > Export OPML to backup your feed list
5. **Theme**: Access theme settings through the gear icon