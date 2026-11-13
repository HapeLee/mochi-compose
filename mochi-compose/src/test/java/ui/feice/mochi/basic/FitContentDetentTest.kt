// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FitContentDetentTest {
    private fun fitOnly() = MochiSheetState(
        listOf(MochiSheetDetent.FitContent),
        MochiSheetDetent.FitContent,
    )

    @Test
    fun sitsAtContentHeightAndReachesFullPresence() {
        val state = fitOnly()
        state.height = 2000f
        state.largeTop = 0f
        state.fitHeight = 420f

        assertEquals(1580f, state.topOf(MochiSheetDetent.FitContent))

        runBlocking { state.top.snapTo(1580f) }
        assertEquals(1f, state.presence, 0.001f)
    }

    @Test
    fun clampsBetweenLargeTopAndTheWindowBottom() {
        val state = fitOnly()
        state.height = 2000f
        state.largeTop = 80f

        state.fitHeight = 0f
        assertEquals(2000f, state.topOf(MochiSheetDetent.FitContent))

        state.fitHeight = 3000f
        assertEquals(80f, state.topOf(MochiSheetDetent.FitContent))
    }

    @Test
    fun fitTopIsSafeBeforeTheWindowHeightArrives() {
        val state = fitOnly()
        state.largeTop = 12f
        state.height = 0f

        assertEquals(0f, state.topOf(MochiSheetDetent.FitContent))
    }

    @Test
    fun fitContentCanReachTheFullScreenTop() {
        val state = fitOnly()
        state.height = 2000f
        state.largeTop = 80f
        state.fitHeight = 2500f

        assertEquals(80f, state.topOf(MochiSheetDetent.FitContent))
    }

    @Test
    fun largenessStaysFiniteWhenFitContentFillsTheScreen() {
        val state = MochiSheetState(
            listOf(MochiSheetDetent.FitContent, MochiSheetDetent.Large),
            MochiSheetDetent.FitContent,
        )
        state.height = 2000f
        state.largeTop = 80f
        state.fitHeight = 1920f

        assertEquals(80f, state.topOf(MochiSheetDetent.FitContent))
        runBlocking { state.top.snapTo(80f) }
        assertEquals(1f, state.largeness, 0.001f)
    }

    @Test
    fun combinedWithLargeStillReportsLargeness() {
        val state = MochiSheetState(
            listOf(MochiSheetDetent.FitContent, MochiSheetDetent.Large),
            MochiSheetDetent.FitContent,
        )
        state.height = 2000f
        state.largeTop = 0f
        state.fitHeight = 600f

        runBlocking { state.top.snapTo(state.topOf(MochiSheetDetent.FitContent)) }
        assertEquals(0f, state.largeness, 0.001f)

        runBlocking { state.top.snapTo(0f) }
        assertEquals(1f, state.largeness, 0.001f)
    }
}
