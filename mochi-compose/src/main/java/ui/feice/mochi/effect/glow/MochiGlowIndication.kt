// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.effect.glow

import android.os.SystemClock
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.gesture.TapPulseMillis

/**
 * 卡片和普通点击区域的高光，不缩放、平移或融合内容。
 * [spec] 与 mochiPressGlow 使用同一画法；点击 owner 的 InteractionSource 决定何时按下、取消，
 * 指针节点只观察移动、不消费事件，避免父卡片因子按钮按下而发光。
 * 高光覆盖内容，由宿主按真实 Shape 裁剪；键盘、焦点与悬停使用区域中心。
 * 按钮已有完整高光或形变时应显式关闭普通 indication，避免重复反馈。
 */
@Immutable
public data class MochiGlowIndication(public val spec: MochiGlowSpec) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = GlowIndicationNode(interactionSource, spec)
}

private class GlowIndicationNode(private val source: InteractionSource, private val spec: MochiGlowSpec) :
    Modifier.Node(),
    DrawModifierNode,
    LayoutAwareModifierNode,
    PointerInputModifierNode {
    private val presses = mutableSetOf<PressInteraction.Press>()
    private val hovers = mutableSetOf<HoverInteraction.Enter>()
    private val focuses = mutableSetOf<FocusInteraction.Focus>()
    private lateinit var glow: MochiGlowState
    private var bounds = IntSize.Zero
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    private var pressedAt = 0L

    override fun onAttach() {
        glow = MochiGlowState(spec, null)
        coroutineScope.launch { glow.runFollow() }
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        presses.add(interaction)
                        show(interaction.pressPosition)
                    }

                    is PressInteraction.Release -> {
                        presses.remove(interaction.press)
                        settle(cancelled = false)
                    }

                    is PressInteraction.Cancel -> {
                        presses.remove(interaction.press)
                        settle(cancelled = true)
                    }

                    is HoverInteraction.Enter -> {
                        hovers.add(interaction)
                        if (presses.isEmpty()) show(center())
                    }

                    is HoverInteraction.Exit -> {
                        hovers.remove(interaction.enter)
                        settle(cancelled = true)
                    }

                    is FocusInteraction.Focus -> {
                        focuses.add(interaction)
                        if (presses.isEmpty()) show(center())
                    }

                    is FocusInteraction.Unfocus -> {
                        focuses.remove(interaction.focus)
                        settle(cancelled = true)
                    }
                }
            }
        }
    }

    private fun center(): Offset = Offset(bounds.width / 2f, bounds.height / 2f)

    private fun show(position: Offset) {
        releaseJob?.cancel()
        pressJob?.cancel()
        pressedAt = SystemClock.uptimeMillis()
        val at = if (position.isSpecified) position else center()
        pressJob = coroutineScope.launch {
            glow.press(at, MochiGlowDefaults.radius(bounds.toSize(), requireDensity()))
        }
    }

    private fun settle(cancelled: Boolean) {
        if (presses.isNotEmpty()) return
        if (hovers.isNotEmpty() || focuses.isNotEmpty()) {
            glow.moveTo(center())
            return
        }
        releaseJob?.cancel()
        releaseJob = coroutineScope.launch {
            // 正常快点按至少亮一小会儿；滚动取消不补脉冲，立即交还滚动。
            if (!cancelled) delay((TapPulseMillis - (SystemClock.uptimeMillis() - pressedAt)).coerceAtLeast(0L))
            pressJob?.cancel()
            glow.release()
        }
    }

    override fun onRemeasured(size: IntSize) {
        bounds = size
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass == PointerEventPass.Final && presses.isNotEmpty()) {
            pointerEvent.changes.firstOrNull { it.pressed }?.let { glow.moveTo(it.position) }
        }
    }

    override fun onCancelPointerInput() {
        if (!isAttached) return
        presses.clear()
        settle(cancelled = true)
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        drawGlow(glow, Outline.Rectangle(Rect(Offset.Zero, size)), glow.center)
    }

    override fun onDetach() {
        pressJob?.cancel()
        releaseJob?.cancel()
        pressJob = null
        releaseJob = null
        presses.clear()
        hovers.clear()
        focuses.clear()
    }
}
