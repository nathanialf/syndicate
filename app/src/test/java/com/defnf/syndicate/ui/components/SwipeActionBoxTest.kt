package com.defnf.syndicate.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeActionBoxTest {

    private val threshold = 100f
    private val flingVelocity = 1000f

    private fun drag(
        offset: Float,
        delta: Float,
        canSwipeToEnd: Boolean = true,
        canSwipeToStart: Boolean = true
    ) = swipeDragOffset(
        offset = offset,
        delta = delta,
        threshold = threshold,
        maxOffset = 400f,
        canSwipeToEnd = canSwipeToEnd,
        canSwipeToStart = canSwipeToStart
    )

    @Test
    fun `the item follows the finger up to the threshold`() {
        assertEquals(60f, drag(offset = 20f, delta = 40f))
        assertEquals(-60f, drag(offset = -20f, delta = -40f))
    }

    @Test
    fun `past the threshold the item resists`() {
        assertEquals(100f + 50f * OverDragResistance, drag(offset = 100f, delta = 50f), 0.001f)
        assertEquals(-100f - 50f * OverDragResistance, drag(offset = -100f, delta = -50f), 0.001f)
    }

    @Test
    fun `dragging back past the threshold is not resisted`() {
        assertEquals(90f, drag(offset = 120f, delta = -30f))
    }

    @Test
    fun `dragging back across the start to the other side is not resisted`() {
        assertEquals(-180f, drag(offset = 120f, delta = -300f))
        assertEquals(180f, drag(offset = -120f, delta = 300f))
    }

    @Test
    fun `a direction without an action does not move`() {
        assertEquals(0f, drag(offset = 0f, delta = 30f, canSwipeToEnd = false))
        assertEquals(0f, drag(offset = 0f, delta = -30f, canSwipeToStart = false))
        // Dragging back through the start stops there
        assertEquals(0f, drag(offset = 10f, delta = -30f, canSwipeToStart = false))
    }

    @Test
    fun `the item never moves further than its width`() {
        assertEquals(400f, drag(offset = 390f, delta = 500f), 0.001f)
    }

    @Test
    fun `releasing past the threshold runs the action for that direction`() {
        assertEquals(SwipeRelease.ToEnd, swipeReleaseResult(threshold, 0f, threshold, flingVelocity))
        assertEquals(SwipeRelease.ToStart, swipeReleaseResult(-threshold, 0f, threshold, flingVelocity))
    }

    @Test
    fun `releasing before the threshold cancels`() {
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(threshold - 1f, 0f, threshold, flingVelocity))
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(0f, 5000f, threshold, flingVelocity))
    }

    @Test
    fun `a fast fling runs the action from part way`() {
        val partWay = threshold * FlingThresholdFraction
        assertEquals(SwipeRelease.ToEnd, swipeReleaseResult(partWay, flingVelocity, threshold, flingVelocity))
        assertEquals(SwipeRelease.ToStart, swipeReleaseResult(-partWay, -flingVelocity, threshold, flingVelocity))
    }

    @Test
    fun `a fling needs to be fast, in the swipe's direction and part way there`() {
        val partWay = threshold * FlingThresholdFraction
        // Too slow
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(partWay, flingVelocity / 2, threshold, flingVelocity))
        // Flung back the other way
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(partWay, -flingVelocity, threshold, flingVelocity))
        // Barely moved
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(partWay / 2, flingVelocity, threshold, flingVelocity))
    }

    @Test
    fun `flinging an armed swipe back cancels it`() {
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(threshold + 10f, -flingVelocity, threshold, flingVelocity))
        assertEquals(SwipeRelease.Cancel, swipeReleaseResult(-threshold - 10f, flingVelocity, threshold, flingVelocity))
        // A slow movement back doesn't count as a fling
        assertEquals(SwipeRelease.ToEnd, swipeReleaseResult(threshold + 10f, -flingVelocity / 2, threshold, flingVelocity))
    }
}
