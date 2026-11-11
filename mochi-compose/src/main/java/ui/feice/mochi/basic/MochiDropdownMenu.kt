// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.basic

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.collections.immutable.ImmutableList
import ui.feice.mochi.anim.MochiMotion
import ui.feice.mochi.basic.menu.Overlay
import ui.feice.mochi.basic.menu.onScreen
import ui.feice.mochi.basic.menu.rememberMochiEngine
import ui.feice.mochi.shape.continuityFor
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.utils.windowOriginOnScreen

/**
 * 只有锚点的菜单：按钮（或触发区域）完全归调用方，这里只把菜单浮层挂在 [anchorBounds] 上渲染。
 *
 * 和 [MochiDropdown] 的区别只在按钮：面板、选项、子菜单、动画、滚动、触感、返回键都是同一个引擎，
 * 所以外观和手感与从按钮里长出来的菜单一致。浮层里不画按钮的克隆，也不接管面板外那一下按压——
 * 开合由调用方的 `clickable` / `combinedClickable` 置位 [expanded] 即可，[behavior] 里的 `trigger`
 * 只作用于库自己的按钮，本入口不读它。
 *
 * - [anchorBounds] 是**窗口坐标**（`Modifier.onGloballyPositioned { it.boundsInWindow() }`），
 *   库自己换算到屏幕；没量到之前不传（或传 `IntRect.Zero`）就别展开，否则面板会从左上角长出来
 * - 面板默认在锚点下面长出来、放不下翻到上面；锚点宽度不参与面板宽度，面板按 [menuWidth] 排版
 * - 默认不连融球颈：锚点不归库画，库不知道它的底色。要「按钮流出面板」那种效果就传 [anchorColor]，
 *   颈和面板这一端会从它渐变过去
 * - 点面板外面、按返回键都会回调 `onExpandedChange(false)`；[expanded] 完全外部受控，
 *   库里没有按钮手势，也就没有「按下即开」和按住滑选
 * - [MochiItem.enabled] / [checked] / [dividerBefore] / [groupTitleBefore] / [children] /
 *   `commitAfterDismiss` 的语义与 [MochiDropdown] 完全一致
 *
 * @param anchorBounds 锚点在窗口坐标里的矩形（按钮所在的那一块）
 * @param anchorCornerRadius 锚点的圆角：展开的第一帧面板就是锚点大小，形状从它开始变形
 * @param anchorColor 锚点那一端的颜色（融球颈 / 面板开头的颜色），`Color.Unspecified` = 不连颈
 */
@Composable
public fun MochiDropdownMenu(
    anchorBounds: IntRect,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: ImmutableList<MochiItem>,
    onItemClick: (Int) -> Unit,
    selectedIndex: Int = -1,
    colors: MochiDropdownColors = MochiDropdownDefaults.dropdownColors(),
    behavior: MochiDropdownBehavior = MochiDropdownDefaults.behavior(),
    physics: MochiDropdownPhysics = MochiDropdownDefaults.physics(),
    motion: MochiMotion = MochiTheme.motionScheme.menu,
    menuWidth: Dp = MochiDropdownDefaults.MenuWidth,
    anchorCornerRadius: Dp = MochiDropdownDefaults.ButtonCornerRadius,
    anchorColor: Color = Color.Unspecified,
    commitAfterDismiss: Boolean = false,
    onChildClick: ((parent: Int, child: Int) -> Unit)? = null,
) {
    val view = LocalView.current
    // 多窗口 / 分屏里窗口原点会变，跟着 View 记一次；锚点本身由调用方在布局里上报
    val windowOrigin = remember(view) { view.windowOriginOnScreen() }
    val anchorOnScreen = remember(anchorBounds, windowOrigin) { anchorBounds.onScreen(windowOrigin) }
    // 弹出层之外只有选项列表，换玻璃菜单的排版和按钮版一致
    val liquid = liquidMenu()
    val necked = anchorColor.isSpecified
    val anchorColors = remember(colors, anchorColor) {
        if (necked) colors.copy(buttonColor = anchorColor) else colors
    }
    val style = remember(anchorColors, behavior, physics, menuWidth, anchorCornerRadius, liquid) {
        mochiStyle(anchorColors, behavior, physics, menuWidth)
            .copy(buttonCornerRadius = anchorCornerRadius)
            // 玻璃菜单的排版默认是「按钮整个变成面板」；锚点归调用方时只有调用方要包裹才包裹
            .let { if (liquid) it.liquid(cover = behavior.openMode == MochiOpenMode.Cover) else it }
    }
    val menuMotion = liquidMotion(liquid, motion)
    val engine = rememberMochiEngine(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onItemClick = onItemClick,
        items = items,
        style = style,
        motion = menuMotion,
        dismissDelayMillis = behavior.dismissDelayMillis,
        anchorSize = { anchorOnScreen.size },
        onChildClick = onChildClick,
        commitAfterDismiss = commitAfterDismiss,
    )
    val anchorShape = remember(style.buttonCornerRadius, style.g2Smoothing) {
        ContinuousRoundedRectangle(style.buttonCornerRadius, continuityFor(style.g2Smoothing))
    }
    engine.Overlay(
        anchorOnScreen = anchorOnScreen,
        // 没有库按钮，也就没有按钮手势；trigger 在浮层里只给克隆按钮用
        trigger = MochiTrigger.Press,
        items = items,
        selectedIndex = selectedIndex,
        motion = menuMotion,
        anchorShape = anchorShape,
        anchorPadding = PaddingValues(0.dp),
        singleBlob = !necked,
    )
}
