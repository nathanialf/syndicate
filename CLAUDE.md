# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Syndicate is a modern Android RSS reader built with Jetpack Compose and Material 3 design. The app features a responsive two-pane layout for tablets, comprehensive feed management with groups/folders, OPML import/export, and background synchronization with notifications.

## Development Commands

### Building
- `./gradlew assembleDebug` - Build debug APK (automatically runs clean first)
- `./gradlew assembleRelease` - Build release APK  
- `./gradlew installDebug` - Build and install debug APK to connected device

### Code Quality
- `./gradlew ktlintCheck` - Run ktlint code style checks
- `./gradlew ktlintFormat` - Auto-format code with ktlint

### Testing
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumented tests on device/emulator

### Database
The project uses Room with KSP. If you encounter cache corruption issues:
- `./gradlew clean` - Clear build cache (assembleDebug already depends on clean)

### Environment
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 15)  
- Kotlin 2.0.21 with Compose enabled
- Java 17 compatibility

## Architecture

### Core Structure
- **Presentation Layer**: Jetpack Compose UI with Material 3, ViewModels, Navigation
- **Domain Layer**: Use cases and business logic in `domain/` directory
- **Data Layer**: Repository pattern with Room database, RSS parsing, networking

### Key Components

#### Data Layer (`data/`)
- **Repository**: `RssRepository` - Single source of truth for data operations
- **Local**: Room database with DAOs for feeds, articles, groups, read status
- **Remote**: RSS fetching (`RssFetcher`), parsing (`RssParser`), OPML handling (`OpmlParser`)
- **Models**: Domain models (`Feed`, `Article`, `Group`) with entity conversion methods

#### UI Layer (`ui/`)
- **Navigation**: Single activity; `RssNavigation` picks an adaptive layout and drives it from `NavigationState` (`ui/navigation/`)
- **Screens**: Feature-based screen organization (feedmanagement, articlelist, etc.)
- **Components**: Reusable UI components in `components/`
- **Theme**: Material 3 theming with dark/light/system modes

#### Dependency Injection (`di/`)
- Hilt for DI with `DatabaseModule` providing Room database and preferences

#### Background Processing
- **Sync**: WorkManager for periodic RSS feed updates (`SyncWorker`, `SyncScheduler`)
- **Notifications**: Per-feed notification system with proper channel management

### Data Models
- `Feed`: RSS feed with metadata, availability status, notification preferences
- `Article`: RSS articles with read/unread status tracking
- `Group`: Feed organization with default group support and notifications
- Domain models have `toEntity()` methods for database conversion

### Key Features Architecture
- **OPML Import/Export**: Full feed subscription backup/restore with duplicate detection
- **Background Sync**: Battery-optimized periodic updates with new article notifications  
- **Read Status**: Separate tracking system preserving state across feed refreshes
- **Feed Groups**: Hierarchical organization with cross-reference tables
- **Adaptive Layout**: Layout chosen from the window width using Material 3 window size class breakpoints (`AppLayoutType`):
  - Compact (< 600dp): single pane + bottom navigation bar
  - Medium (600-839dp): single pane + navigation rail
  - Expanded (840-1199dp): feeds sidebar + content pane (`TwoPaneLayout`)
  - Large (>= 1200dp): feeds sidebar + article list + article detail (`ThreePaneLayout`)
  - A single `NavigationState` is hoisted in `RssNavigation` above the layout switch and saved with `rememberSaveable`, so selection survives rotation, fold/unfold and window resizing. The activity handles size-related config changes itself.

## Important Notes

### Build Configuration
- KSP incremental compilation is disabled due to cache corruption issues
- All warnings are treated as errors in Kotlin compilation
- Room compiler arguments: incremental=false, expandProjection=true

### Database
- Room database (version 3); schema changes need a `Migration` added in `RssDatabase` (`exportSchema = false`, so no auto-migrations)
- Article list queries filter read/unread state in SQL and select a truncated `description` preview; use `getArticleById` for full content
- Article inserts use `OnConflictStrategy.IGNORE` (never REPLACE, which would cascade-delete read status); a refresh is one transaction
- Separate read status tracking to preserve user reading progress
- Group management supports default group selection with transactions

### RSS Processing
- Uses Rome Tools library for RSS/Atom parsing
- Handles various feed formats with fallback mechanisms
- Favicon URL generation from feed metadata

### Theming
- Material 3 with dynamic theming support
- System/light/dark theme modes stored in DataStore preferences
- Edge-to-edge display with proper inset handling

### Testing
- Unit tests with JUnit and Coroutines Test
- Compose UI tests with test manifests for debug builds
- Android instrumented tests for Room database operations