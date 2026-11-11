// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glow

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.feice.mochi.effect.overscroll.MochiOverscrollEffect
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.lightColorScheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MochiThemeFeedbackTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun themeProvidesIndependentOverscrollEffectsAndAllowsLocalOptOut() {
        rule.setContent {
            MochiTheme(systemBarAppearance = false) {
                assertTrue(LocalIndication.current is MochiGlowIndication)
                val factory = checkNotNull(LocalOverscrollFactory.current)
                val first = factory.createOverscrollEffect()
                val second = factory.createOverscrollEffect()
                assertTrue(first is MochiOverscrollEffect)
                assertNotSame(first, second)
                CompositionLocalProvider(LocalOverscrollFactory provides null) {
                    assertNull(LocalOverscrollFactory.current)
                }
            }
        }
    }

    @Test
    fun glowIsIsolatedDoesNotDeformAndClearsOnCancellation() {
        val first = MutableInteractionSource()
        val second = MutableInteractionSource()
        rule.setContent {
            MochiTheme(colors = lightColorScheme(), systemBarAppearance = false) {
                Column {
                    listOf("first" to first, "second" to second).forEach { (tag, source) ->
                        Box(
                            Modifier.size(80.dp).testTag(tag).background(Color.DarkGray)
                                .clickable(source, LocalIndication.current, onClick = {}),
                        )
                    }
                }
            }
        }
        rule.mainClock.autoAdvance = false
        val node = rule.onNodeWithTag("first")
        val initialBounds = node.fetchSemanticsNode().boundsInRoot
        fun red(): Float {
            val center = node.fetchSemanticsNode().boundsInWindow.center
            var value = 0f
            rule.runOnUiThread {
                // Robolectric 的手动动画时钟下不走 PixelCopy/forceRedraw，直接绘制同一窗口。
                val view = rule.activity.window.decorView
                val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(image))
                value = android.graphics.Color.red(image.getPixel(center.x.toInt(), center.y.toInt())) / 255f
                image.recycle()
            }
            return value
        }
        val baseline = red()
        val otherPress = PressInteraction.Press(Offset(80f, 80f))
        rule.runOnIdle { assertTrue(second.tryEmit(otherPress)) }
        rule.mainClock.advanceTimeBy(200)
        assertEquals(baseline, red(), 0.005f)

        val ownPress = PressInteraction.Press(Offset(80f, 80f))
        rule.runOnIdle { assertTrue(first.tryEmit(ownPress)) }
        rule.mainClock.advanceTimeBy(200)
        assertTrue("按压应产生可见高光", red() > baseline + 0.01f)
        assertEquals(initialBounds, node.fetchSemanticsNode().boundsInRoot)

        rule.runOnIdle {
            assertTrue(first.tryEmit(PressInteraction.Cancel(ownPress)))
            assertTrue(second.tryEmit(PressInteraction.Cancel(otherPress)))
        }
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(baseline, red(), 0.005f)
    }
}
