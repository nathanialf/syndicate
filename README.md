# Syndicate RSS Reader

A modern Android RSS reader built with Jetpack Compose and Material 3 design, featuring an adaptive layout for phones, foldables, tablets and resizable windows.

## Features

### ✅ Implemented
- **Material 3 Expressive Design**: Expressive motion, shapes and components (FAB menu, floating toolbar, connected button groups, loading indicators)
- **Adaptive Layout**: Bottom navigation on phones, navigation rail on medium-width windows, two-pane (feeds + content) on expanded widths and three-pane (feeds + list + article) on large screens; state is preserved across rotation, folding and window resizing
- **Feed Management**: Add, organize, and delete RSS feeds with swipe gestures
- **Feed Groups/Folders**: Organize feeds into custom groups with full CRUD operations
- **Favicon Support**: Automatic favicon loading for visual feed identification
- **OPML Export**: Export your feed subscriptions in OPML format
- **OPML Import**: Import feed subscriptions from OPML files with duplicate detection
- **Dark/Light Theme**: System-aware theming with manual override options
- **Smart Duplicate Detection**: HTTP/HTTPS URL normalization prevents duplicate feeds
- **Material 3 List-Detail Layout**: Selected feeds highlighted with proper Material colors
- **Swipe Gestures**: Swipe feeds and groups right to delete or left to toggle notifications (with Undo); the item springs back, and a haptic tick marks the point where the action will trigger
- **Enhanced Error Reporting**: Detailed import error messages with specific failure reasons
- **Article Reading**: Full article detail view on both screen sizes
- **Read/Unread Status**: Mark articles as read/unread with swipe gestures and visual indicators
- **Article Detail Screen**: In-app article viewer with share and browser open actions
- **Background Sync**: Periodic article fetching with WorkManager for battery optimization
- **Push Notifications**: One notification per new article (opens that article), bundled per feed under a summary that opens the feed; group notifications open the group. Mark-as-read actions and system permission prompts
- **Notification Channels**: Organized notifications with proper Android 8+ channel support
- **Enhanced UI/UX**: Feed descriptions with smart truncation, improved scrolling with spacers
- **Haptic Feedback**: Tactile feedback for swipe actions and article interactions
- **Enhanced HTML Content Parsing**: Rich HTML content rendering with support for:
  - Unordered lists with proper bullet formatting
  - Images displayed inline with fallback handling
  - Code blocks with monospace formatting and syntax highlighting
  - YouTube embeds with high-quality thumbnails and click-to-open functionality
  - Hyperlinks with modern clickable annotations
  - Proper content order preservation to prevent duplication
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
- **Design System**: Material 3 Expressive
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
2. **Feed Organization**: Long press a feed to set its notifications and groups (or create a group for it); swipe to delete or toggle notifications
3. **Group Management**: Long press a group to rename it, choose its feeds and set notifications or open-on-launch; changes save as you make them
4. **OPML Import**: Import feed subscriptions with intelligent duplicate detection
5. **Reading Articles**: Tap any feed to view its articles
6. **Export**: Use Settings > Export OPML to backup your feed list
7. **Theme**: Access theme settings through the gear icon