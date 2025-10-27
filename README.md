# Syndicate RSS Reader

A modern Android RSS reader built with Jetpack Compose and Material 3 design, featuring a responsive two-pane layout for tablets and wide screens.

## Features

### ✅ Implemented
- **Modern Material 3 Design**: Clean, intuitive interface following Material Design guidelines
- **Responsive Layout**: Two-pane layout for tablets, single-pane for phones
- **Feed Management**: Add, organize, and delete RSS feeds with swipe gestures
- **Feed Groups/Folders**: Organize feeds into custom groups with full CRUD operations
- **Favicon Support**: Automatic favicon loading for visual feed identification
- **OPML Export**: Export your feed subscriptions in OPML format
- **OPML Import**: Import feed subscriptions from OPML files with duplicate detection
- **Dark/Light Theme**: System-aware theming with manual override options
- **Scroll-Based UI Animation**: Top app bar and FAB animate away on scroll for immersive reading
- **Smart Duplicate Detection**: HTTP/HTTPS URL normalization prevents duplicate feeds
- **Material 3 List-Detail Layout**: Selected feeds highlighted with proper Material colors
- **Swipe Gestures**: Swipe feeds to delete or add to groups with animated feedback
- **Enhanced Error Reporting**: Detailed import error messages with specific failure reasons

### 🚧 Planned Features
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

1. **Adding Feeds**: Tap the + button to add RSS feeds by URL or import from OPML files
2. **Feed Organization**: Swipe feeds to delete or organize into custom groups
3. **Group Management**: Create, edit, and delete feed groups for better organization
4. **OPML Import**: Import feed subscriptions with intelligent duplicate detection
5. **Reading Articles**: Tap any feed to view its articles with immersive scroll animations
6. **Export**: Use Settings > Export OPML to backup your feed list
7. **Theme**: Access theme settings through the gear icon