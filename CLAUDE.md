# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Syndicate is a modern Android RSS reader built with Jetpack Compose and Material 3 design. The app features a responsive two-pane layout for tablets, comprehensive feed management with groups/folders, OPML import/export, and background synchronization with notifications.

## Development Commands

### Building
- `./gradlew assembleDebug` - Build debug APK (automatically runs clean first)
- `./gradlew assembleRelease` - Build release APK  
- `./gradlew installDebug` - Build and install debug APK to connected device

### Release
- Version: bump `appVersionName` in `app/build.gradle.kts`; `versionCode` is derived (major*10000 + minor*100 + patch). Play never accepts a versionCode twice
- `.claude/skills/run-syndicate/driver.sh release` - signed release APK + AAB (needs `keystore.properties`), fails on any compiler warning
- `.githooks/pre-commit` bumps the patch and builds the signed AAB on commits touching the app; enable with `git config core.hooksPath .githooks`
- Play Store assets: `./gradlew :app:testDebugUnitTest -PstoreAssets --tests '*StoreAssetsTest'` renders the icon, feature graphic and phone/tablet screenshots into `store-assets/` (Robolectric, no emulator needed)

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
- Target SDK: 36 (Android 16), the minimum Play accepts  
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
- **Layout**: Adaptive layout in `ui/layout/` using the Material 3 adaptive libraries (see Adaptive Layout below)
- **Screens**: Feature-based screen organization (feedmanagement, articlelist, etc.)
- **Components**: Reusable UI components in `components/`
- **Theme**: Material 3 Expressive theming (`MaterialExpressiveTheme`) with dark/light/system modes

#### Dependency Injection (`di/`)
- Hilt for DI with `DatabaseModule` providing Room database and preferences

#### Background Processing
- **Sync**: WorkManager for periodic RSS feed updates (`SyncWorker`, `SyncScheduler`)
- **Notifications**: `NotificationManager` posts one notification per new article, bundled per feed under a summary, plus group notifications. Tap/action intents come from `NotificationIntents`, which gives each target a unique data URI (PendingIntents ignore extras when matching). `RssRepository` dismisses notifications when articles are marked read

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
- **Adaptive Layout** (`ui/layout/`), built on the official Material 3 adaptive libraries (`material3-adaptive-navigation-suite`, `adaptive-layout`, `adaptive-navigation`):
  - `RssNavigation` reads `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)` and maps its `WindowSizeClass` to an `AppLayoutType`:
    - Compact (< 600dp): single pane + short navigation bar (hidden while reading an article)
    - Medium (600-839dp): single pane + collapsed wide navigation rail
    - Expanded (840-1199dp): feeds sidebar + article list or article
    - Large (>= 1200dp): feeds sidebar + article list + article
  - Each `AppLayoutType` is described by capabilities (`navigationSuiteType`, `hasFeedsSidebar`, `hasArticleListBesideDetail`); nothing outside `fromWindowSizeClass` matches on the layout name
  - `AdaptiveLayout`: `NavigationSuiteScaffold` (bar / rail / none) around the feeds sidebar and the main content (`MainContent`: feeds tab, articles or settings)
  - Articles: `ArticleListDetail` uses `NavigableListDetailPaneScaffold` (standard pane motion + predictive back). `articlePaneDirective` sets one or two panes from the layout (not the window, since the sidebar takes space), keeps hinge avoidance, and never stacks panes vertically
  - The open article is owned by the scaffold's `ThreePaneScaffoldNavigator<String>` (content key = article id). `NavigationState` reaches it through the `ArticlePaneNavigator` interface (`ListDetailArticlePaneNavigator`), which keeps the history at most `[List, Detail(article)]`
  - Screens are layout-agnostic: no layout/mode flags. Optional actions are nullable callbacks (e.g. `onBackClick`, `onSettingsClick`) that `Panes.kt` supplies only when the layout needs them
  - Insets: screens use the default Material `Scaffold`/`TopAppBar` window insets; `NavigationSuiteScaffold` consumes the insets its bar or rail covers
  - Back: the article panes handle closing an article (predictive back, `BackNavigationBehavior.PopUntilContentChange` so it also works with the list beside the article); `RssNavigation`'s `BackHandler` handles destinations and selections via `NavigationState.canNavigateBack/navigateBack`
  - `NavigationState` is a plain, unit-tested state class saved with `NavigationState.saver`; `rememberNavigationState` (Compose/Hilt glue) also applies the default group on first launch. State survives rotation, fold/unfold and window resizing; the activity handles size-related config changes itself

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
- Material 3 Expressive: `SyndicateTheme` wraps `MaterialExpressiveTheme` with `MotionScheme.expressive()` and dynamic color (falls back to `expressiveLightColorScheme()` / `darkColorScheme()` before Android 12)
- Expressive components in use: `FloatingActionButtonMenu` (feeds), connected `ToggleButton` groups (article filter, theme), `HorizontalFloatingToolbar` (article actions), `LoadingIndicator` (also for pull to refresh), `LargeFlexibleTopAppBar` (settings), top app bar subtitles, shape-morphing `IconButton`s, `MaterialShapes` (empty states, group icons)
- Grouped lists (feeds sidebar, settings) use `SegmentedItem` / `SegmentedShapes`: large outer corners, tight inner corners, and a selected item morphs to fully rounded
- Expressive APIs need `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`; animate with `MaterialTheme.motionScheme` specs rather than fixed tweens
- System/light/dark theme modes stored in DataStore preferences
- Edge-to-edge display with proper inset handling

### Testing
- Unit tests with JUnit and Coroutines Test
- Compose UI tests with test manifests for debug builds
- Android instrumented tests for Room database operations