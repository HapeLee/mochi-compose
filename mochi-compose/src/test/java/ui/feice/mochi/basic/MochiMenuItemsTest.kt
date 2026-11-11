// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.feice.mochi.basic.menu.menuLayer
import ui.feice.mochi.gesture.MochiController

/** 菜单项的新参数只加能力、不动几何：禁用项仍占一行，只是不高亮、不提交 */
class MochiMenuItemsTest {
    @Test
    fun itemDefaultsKeepOldBehaviour() {
        val item = MochiItem("最近")
        assertNull(item.id)
        assertTrue(item.enabled)
        assertNull(item.checked)
        assertFalse(item.dividerBefore)
        assertNull(item.groupTitleBefore)
    }

    @Test
    fun itemCarriesNewProperties() {
        val item = MochiItem(
            label = "置顶",
            id = "pin",
            enabled = false,
            checked = true,
            dividerBefore = true,
            groupTitleBefore = "协作",
        )
        assertEquals("pin", item.id)
        assertFalse(item.enabled)
        assertEquals(true, item.checked)
        assertTrue(item.dividerBefore)
        assertEquals("协作", item.groupTitleBefore)
    }

    @Test
    fun disabledRowKeepsItsSlotButNeverHovers() {
        val controller = controller(items = 3, enabled = { it != 1 })
        controller.track(Offset(10f, 0.5f * ItemHeightPx))
        assertEquals(0, controller.hover)
        assertFalse(controller.onDisabled(Offset(10f, 0.5f * ItemHeightPx)))

        // 第二行（禁用）落在原来的第 1 项上，但手势状态里没有悬停项
        controller.track(Offset(10f, 1.5f * ItemHeightPx))
        assertEquals(-1, controller.hover)
        assertTrue(controller.onDisabled(Offset(10f, 1.5f * ItemHeightPx)))

        controller.track(Offset(10f, 2.5f * ItemHeightPx))
        assertEquals(2, controller.hover)
    }

    @Test
    fun outsidePanelIsNotHoverAndNotDisabled() {
        val controller = controller(items = 3, enabled = { false })
        controller.track(Offset(10f, 4f * ItemHeightPx))
        assertEquals(-1, controller.hover)
        assertFalse(controller.onDisabled(Offset(10f, 4f * ItemHeightPx)))
    }

    @Test
    fun submenuLayerIsOneLevelDeep() {
        val grandChild = MochiItem("三级", id = "c-c")
        val child = MochiItem("复制", id = "c", children = persistentListOf(grandChild))
        val parent = MochiItem("分享", id = "p", children = persistentListOf(child))
        val plain = MochiItem("删除", id = "d")
        val items = persistentListOf(parent, plain)

        assertSame(items, menuLayer(items, -1))
        assertSame(parent.children, menuLayer(items, 0))
        // 子项自带的 children 不再展开（这里传的是顶层下标，子项里的下标没有意义）
        assertEquals(listOf(child), menuLayer(items, 0))
        assertSame(items, menuLayer(items, 5))
    }

    private fun controller(items: Int, enabled: (Int) -> Boolean) = MochiController().apply {
        itemCount = items
        itemHeightPx = ItemHeightPx
        paddingPx = 0f
        headerPx = 0f
        panelOnScreen = Rect(0f, 0f, 300f, items * ItemHeightPx)
        contentReady = { true }
        isEnabled = enabled
    }

    private companion object {
        const val ItemHeightPx = 52f
    }
}
