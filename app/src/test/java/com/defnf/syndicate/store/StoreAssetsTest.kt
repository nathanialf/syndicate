package com.defnf.syndicate.store

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.defnf.syndicate.R
import com.defnf.syndicate.data.local.RssDatabase
import com.defnf.syndicate.data.local.entities.ArticleEntity
import com.defnf.syndicate.data.local.entities.FeedEntity
import com.defnf.syndicate.data.local.entities.FeedGroupCrossRef
import com.defnf.syndicate.data.local.entities.GroupEntity
import com.defnf.syndicate.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import javax.inject.Inject

/**
 * Renders the Google Play listing assets (app icon, feature graphic, phone and tablet
 * screenshots) from the real resources and UI, seeded with sample feeds.
 *
 * Skipped unless run with `./gradlew :app:testDebugUnitTest -PstoreAssets`, which writes
 * the images to `store-assets/` at the repo root.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [35])
class StoreAssetsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var database: RssDatabase

    private val outputDir: File? = System.getProperty("syndicate.storeAssetsDir")?.let(::File)

    @Before
    fun setUp() {
        assumeTrue("Run with -PstoreAssets to render store assets", outputDir != null)
        // The database is a process-wide singleton; start every test from a fresh one
        RssDatabase::class.java.getDeclaredField("INSTANCE").apply {
            isAccessible = true
            set(null, null)
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext())
        hiltRule.inject()
        seedSampleData()
    }

    @Test
    @Config(qualifiers = "w512dp-h512dp-mdpi")
    fun appIcon() {
        // Play applies its own mask, so the icon is the full-bleed square of both adaptive layers
        val context = RuntimeEnvironment.getApplication()
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { id ->
            ContextCompat.getDrawable(context, id)!!.apply {
                setBounds(0, 0, 512, 512)
                draw(canvas)
            }
        }
        save(bitmap, "icon-512.png")
    }

    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic() {
        val context = RuntimeEnvironment.getApplication()
        val glyph =
            Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888).also { bitmap ->
                // Foreground glyph only; the tile behind it is a darker shade of the brand green
                ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)!!.apply {
                    setBounds(0, 0, 216, 216)
                    draw(Canvas(bitmap))
                }
            }
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Brand),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = glyph.asImageBitmap(),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(216.dp)
                                    .clip(RoundedCornerShape(56.dp))
                                    .background(BrandDark),
                        )
                        Spacer(Modifier.width(48.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Syndicate", color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Your feeds, beautifully organized",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 34.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        capture(scenario, "feature-graphic.png")
        scenario.close()
    }

    // Phone: 1080x1920 (9:16)
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phoneScreenshots() {
        val scenario = launchApp()
        capture(scenario, "phone-1-articles.png")
        composeRule.onNodeWithText(SAMPLE_ARTICLES[0].title).performClick()
        settle()
        capture(scenario, "phone-2-article.png")
        scenario.close()

        val feeds = launchApp()
        composeRule.onNodeWithText("Feeds").performClick()
        settle()
        capture(feeds, "phone-3-feeds.png")
        composeRule.onNodeWithText("Settings").performClick()
        settle()
        capture(feeds, "phone-4-settings.png")
        feeds.close()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-night-xxhdpi")
    fun phoneDarkScreenshot() {
        val scenario = launchApp()
        capture(scenario, "phone-5-articles-dark.png")
        scenario.close()
    }

    // 7" tablet: 1920x1080 (16:9), feeds sidebar + content
    @Test
    @Config(qualifiers = "w960dp-h540dp-land-xhdpi")
    fun sevenInchTabletScreenshot() {
        val scenario = launchApp()
        capture(scenario, "tablet7-1-articles.png")
        composeRule.onNodeWithText(SAMPLE_ARTICLES[1].title).performClick()
        settle()
        capture(scenario, "tablet7-2-article.png")
        scenario.close()
    }

    // 10" tablet: 2560x1440 (16:9), feeds sidebar + article list + article
    @Test
    @Config(qualifiers = "w1280dp-h720dp-land-xhdpi")
    fun tenInchTabletScreenshots() {
        val scenario = launchApp()
        composeRule.onNodeWithText(SAMPLE_ARTICLES[0].title).performClick()
        settle()
        capture(scenario, "tablet10-1-reading.png")
        scenario.close()

        val dark = launchApp(night = true)
        composeRule.onNodeWithText(SAMPLE_ARTICLES[2].title).performClick()
        settle()
        capture(dark, "tablet10-2-reading-dark.png")
        dark.close()
    }

    private fun launchApp(night: Boolean = false): ActivityScenario<MainActivity> {
        if (night) RuntimeEnvironment.setQualifiers("+night")
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        settle()
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().isNotEmpty() &&
                SAMPLE_ARTICLES.take(4).any { article ->
                    composeRule.onAllNodesWithText(article.title).fetchSemanticsNodes().isNotEmpty()
                }
        }
        return scenario
    }

    /** Lets Room deliver, the startup-sync indicator time out, and animations finish. */
    private fun settle() {
        repeat(3) {
            Thread.sleep(300)
            ShadowLooper.idleMainLooper(4, java.util.concurrent.TimeUnit.SECONDS)
            composeRule.mainClock.advanceTimeBy(2_000)
            composeRule.waitForIdle()
        }
    }

    /** Draws the activity window; Compose's captureToImage times out under Robolectric. */
    private fun capture(
        scenario: ActivityScenario<*>,
        name: String,
    ) {
        settle()
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            save(bitmap, name)
        }
    }

    private fun save(
        bitmap: Bitmap,
        name: String,
    ) {
        val dir = outputDir!!.apply { mkdirs() }
        // Play wants 24-bit PNGs (no alpha) for the feature graphic and screenshots
        if (name != "icon-512.png") bitmap.setHasAlpha(false)
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun seedSampleData() =
        runBlocking {
            val now = System.currentTimeMillis()
            val feedIds =
                SAMPLE_FEEDS.map { (title, url) ->
                    database.feedDao().insertFeed(
                        FeedEntity(
                            url = url,
                            title = title,
                            description = null,
                            siteUrl = null,
                            faviconUrl = null,
                            lastFetched = now,
                        ),
                    )
                }
            val tech = database.groupDao().insertGroup(GroupEntity(name = "Tech"))
            val outdoors = database.groupDao().insertGroup(GroupEntity(name = "Outdoors"))
            database.groupDao().insertFeedGroupCrossRefs(
                listOf(
                    FeedGroupCrossRef(feedIds[0], tech),
                    FeedGroupCrossRef(feedIds[1], tech),
                    FeedGroupCrossRef(feedIds[3], outdoors),
                ),
            )
            database.articleDao().insertArticles(
                SAMPLE_ARTICLES.mapIndexed { index, article ->
                    ArticleEntity(
                        id = "sample-$index",
                        feedId = feedIds[article.feed],
                        title = article.title,
                        description = article.body,
                        url = "https://example.com/articles/$index",
                        author = article.author,
                        publishedDate = now - article.minutesAgo * 60_000L,
                        thumbnailUrl = null,
                    )
                },
            )
        }

    private data class SampleArticle(
        val feed: Int,
        val title: String,
        val author: String,
        val minutesAgo: Long,
        val body: String,
    )

    private companion object {
        val Brand = Color(0xFF00B55C)
        val BrandDark = Color(0xFF009A4E)

        val SAMPLE_FEEDS =
            listOf(
                "Circuit Notes" to "https://circuitnotes.example.com/feed",
                "The Compile Times" to "https://compiletimes.example.com/rss",
                "Kitchen Table" to "https://kitchentable.example.com/feed.xml",
                "Trail Journal" to "https://trailjournal.example.com/atom",
                "Field & Frame" to "https://fieldandframe.example.com/rss",
            )

        val SAMPLE_ARTICLES =
            listOf(
                SampleArticle(
                    0,
                    "Why small, open protocols keep outliving the platforms built on them",
                    "Maya Okafor",
                    12,
                    "<p>Every few years a new platform promises to replace the humble feed. Every few years the feed is still here.</p>" +
                        "<p>The reason is not nostalgia. A feed is a plain file that any client can read, any server can publish, and no single company controls. That simplicity is a feature: it lets readers choose their own tools and keeps publishers from depending on an algorithm to reach the people who asked to hear from them.</p>" +
                        "<h3>Built to last</h3>" +
                        "<p>The formats have barely changed in two decades, which means a reader written today still understands feeds published in 2005. Few other parts of the web can say the same.</p>" +
                        "<p>If you follow a handful of writers you trust, a reader that checks their feeds and tells you when something new arrives is still the calmest way to keep up.</p>",
                ),
                SampleArticle(
                    1,
                    "A gentle guide to keeping your build fast as the project grows",
                    "Daniel Reyes",
                    47,
                    "<p>Slow builds rarely arrive all at once. They creep in one dependency and one code generator at a time.</p>" +
                        "<p>Start by measuring: profile a clean build and an incremental one, and write both numbers down. Then look for tasks that run on every change even though their inputs did not move.</p>" +
                        "<p>Caching, configuration avoidance and splitting large modules each help, but the biggest wins usually come from deleting work the build never needed to do.</p>",
                ),
                SampleArticle(
                    3,
                    "Five quiet trails for an early autumn morning",
                    "Priya Nandakumar",
                    95,
                    "<p>The crowds thin out once school starts, and the light gets better every week. These five loops are short enough for a morning and pretty enough to linger on.</p>" +
                        "<p>Bring a layer: the valley floor stays cool until the sun clears the ridge, and the creek crossings can be slick after a dewy night.</p>" +
                        "<p>Each trail below lists the distance, elevation gain and the best spot to stop for coffee on the way home.</p>",
                ),
                SampleArticle(
                    2,
                    "The one-pan weeknight pasta we keep coming back to",
                    "Sam Whitaker",
                    180,
                    "<p>Tomatoes, garlic, a splash of pasta water and ten minutes. That is the whole recipe, and it is better than it has any right to be.</p>",
                ),
                SampleArticle(
                    4,
                    "Shooting golden hour with only a phone",
                    "Lena Fischer",
                    260,
                    "<p>You do not need a bag of lenses to catch the best light of the day. Lock exposure on the brightest part of the sky, then let the shadows fall where they will.</p>",
                ),
                SampleArticle(
                    1,
                    "What changed in the latest language release, and what to try first",
                    "Daniel Reyes",
                    420,
                    "<p>The headline features get the attention, but the small quality-of-life fixes are what you will notice every day.</p>",
                ),
                SampleArticle(
                    0,
                    "Reading less, but reading better",
                    "Maya Okafor",
                    720,
                    "<p>Trimming a feed list to the sources you actually open each week turns an inbox into a magazine.</p>",
                ),
                SampleArticle(
                    3,
                    "Packing light for a two-night hut trip",
                    "Priya Nandakumar",
                    1500,
                    "<p>A short checklist that fits in a 30 litre pack, with room left for snacks.</p>",
                ),
            )
    }
}
