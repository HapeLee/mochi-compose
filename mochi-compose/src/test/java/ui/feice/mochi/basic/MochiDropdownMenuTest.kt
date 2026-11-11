// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ui.feice.mochi.basic.menu.onScreen
import ui.feice.mochi.theme.MochiTheme

/**
 * 只有锚点的菜单（[MochiDropdownMenu]）：按钮不归库，浮层仍要和按钮版一样列出选项、按项下标回调。
 * 锚点坐标从窗口换到屏幕这一步单独测（[onScreen]），其余只验证组合与回调。
 */
@RunWith(org.robolectric.RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
class MochiDropdownMenuTest {
    @get:Rule val rule = createComposeRule()

    private val anchor = IntRect(IntOffset(40, 40), IntSize(120, 48))

    private fun show(
        expanded: Boolean = true,
        items: ImmutableList<MochiItem> = persistentListOf(MochiItem("最近"), MochiItem("名称")),
        selectedIndex: Int = -1,
        onItemClick: (Int) -> Unit = {},
        onExpandedChange: (Boolean) -> Unit = {},
    ) {
        rule.setContent {
            MochiTheme(systemBarAppearance = false) {
                Box(Modifier.fillMaxSize()) {
                    MochiDropdownMenu(
                        anchorBounds = anchor,
                        expanded = expanded,
                        onExpandedChange = onExpandedChange,
                        items = items,
                        onItemClick = onItemClick,
                        selectedIndex = selectedIndex,
                    )
                }
            }
        }
    }

    @Test
    fun windowAnchorMovesToScreenCoordinates() {
        val origin = Offset(12f, 30f)
        val screen = anchor.onScreen(origin)
        assertEquals(52f, screen.left)
        assertEquals(70f, screen.top)
        assertEquals(172f, screen.right)
        assertEquals(118f, screen.bottom)
    }

    @Test
    fun anchoredMenuListsItems() {
        show()
        rule.onNodeWithText("最近").assertExists()
        rule.onNodeWithText("名称").assertExists()
    }

    @Test
    fun collapsedMenuDrawsNothing() {
        show(expanded = false)
        rule.onNodeWithText("最近").assertDoesNotExist()
    }

    @Test
    fun tappingItemCommitsItsIndex() {
        var clicked = -1
        show(onItemClick = { clicked = it })
        // 不在动画中途点：等开合弹簧停稳
        rule.waitForIdle()
        rule.onNodeWithText("名称").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun disabledItemStaysOnItsRowButCannotBeSelected() {
        var clicked = -1
        show(
            items = persistentListOf(MochiItem("最近"), MochiItem("名称", enabled = false)),
            onItemClick = { clicked = it },
        )
        rule.waitForIdle()
        // 仍占一行（面板高度不变），但语义上禁用、点了不回调
        rule.onNodeWithText("名称").assertIsNotEnabled()
        rule.onNodeWithText("名称").performClick()
        assertEquals(-1, clicked)
    }

    @Test
    fun checkedItemReportsCheckboxRole() {
        show(items = persistentListOf(MochiItem("最近"), MochiItem("置顶", checked = false)))
        rule.waitForIdle()
        // 独立勾选的项是开关；单选（selectedIndex）菜单里的行仍是按钮
        rule.onNodeWithText("置顶").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
        )
        rule.onNodeWithText("最近").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        )
    }
}
