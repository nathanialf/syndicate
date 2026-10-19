package com.defnf.syndicate.ui.common

import com.defnf.syndicate.ui.common.SegmentedShapes.InnerCorner
import com.defnf.syndicate.ui.common.SegmentedShapes.OuterCorner
import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentedShapesTest {

    @Test
    fun `a single item is rounded all around`() {
        assertEquals(OuterCorner, SegmentedShapes.topCorner(index = 0))
        assertEquals(OuterCorner, SegmentedShapes.bottomCorner(index = 0, count = 1))
    }

    @Test
    fun `only the outer corners of a group are rounded`() {
        assertEquals(OuterCorner, SegmentedShapes.topCorner(index = 0))
        assertEquals(InnerCorner, SegmentedShapes.bottomCorner(index = 0, count = 3))

        assertEquals(InnerCorner, SegmentedShapes.topCorner(index = 1))
        assertEquals(InnerCorner, SegmentedShapes.bottomCorner(index = 1, count = 3))

        assertEquals(InnerCorner, SegmentedShapes.topCorner(index = 2))
        assertEquals(OuterCorner, SegmentedShapes.bottomCorner(index = 2, count = 3))
    }

    @Test
    fun `a selected item is rounded all around`() {
        assertEquals(OuterCorner, SegmentedShapes.topCorner(index = 1, selected = true))
        assertEquals(OuterCorner, SegmentedShapes.bottomCorner(index = 1, count = 3, selected = true))
    }
}
