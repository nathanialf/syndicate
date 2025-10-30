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
- **Smart Duplicate Detection**: HTTP/HTTPS URL normalization prevents duplicate feeds
- **Material 3 List-Detail Layout**: Selected feeds highlighted with proper Material colors
- **Swipe Gestures**: Swipe feeds to delete or toggle notifications with haptic feedback
- **Enhanced Error Reporting**: Detailed import error messages with specific failure reasons
- **Article Reading**: Full article detail view on both screen sizes
- **Read/Unread Status**: Mark articles as read/unread with swipe gestures and visual indicators
- **Article Detail Screen**: In-app article viewer with share and browser open actions
- **Background Sync**: Periodic article fetching with WorkManager for battery optimization
- **Push Notifications**: Per-feed notification system with mark-as-read actions and system permission prompts
- **Notification Channels**: Organized notifications with proper Android 8+ channel support
- **Enhanced UI/UX**: Feed descriptions with smart truncation, improved scrolling with spacers
- **Haptic Feedback**: Tactile feedback for swipe actions and article interactions
- **HTML Content Rendering**: Rich HTML content parsing with proper formatting and styling
- **Pull-to-Refresh**: Manual refresh to fetch new articles with scroll-to-top functionality
- **Feed Addition Confirmation**: Success notifications with feed titles when adding new feeds
- **Auto-dismissing Dialogs**: Improved UX with automatic dialog closure after successful actions
- **Smart Notification Dismissal**: Notifications automatically clear when articles are marked as read
- **Scroll-to-Top FAB**: Floating action button for quick navigation to top of article lists
- **Intelligent Notification Filtering**: Notifications only sent during background sync, not manual refresh or startup

### 🚧 Planned Features
- **Article Search**: Search through article titles and descriptions
- **Feed Availability Monitoring**: Track and display feed status
- **Article Filtering**: Filter by read/unread status, sort by date

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Database**: Room
- **DI**: Hilt
- **HTTP**: OkHttp + Retrofit
- **Image Loading**: Coil
- **RSS Parsing**: Rome Tools library
- **HTML Parsing**: Android HtmlCompat with custom span conversion

## Building from Source

1. Clone the repository
2. Open the project in Android Studio
3. Build and run: `./gradlew assembleDebug`

## Usage

1. **Adding Feeds**: Tap the + button to add RSS feeds by URL or import from OPML files (with confirmation notifications)
2. **Feed Organization**: Swipe feeds to delete or organize into custom groups
3. **Group Management**: Create, edit, and delete feed groups for better organization
4. **OPML Import**: Import feed subscriptions with intelligent duplicate detection
5. **Reading Articles**: Tap any feed to view its articles
6. **Export**: Use Settings > Export OPML to backup your feed list
7. **Theme**: Access theme settings through the gear icon