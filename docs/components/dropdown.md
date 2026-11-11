# MochiDropdown / MochiIconDropdown

A dropdown that grows out of its button, joined by a metaball neck. Press (or long-press) to open, keep your finger down and slide onto an item, release to select. Dragging outside the panel pulls it along like a rubber band.

- `MochiDropdown`: text capsule button, detached by default (the panel grows out next to the button with a metaball neck)
- `MochiIconDropdown`: circular icon button (⋯, +), cover mode by default (the button itself morphs into the panel), end-aligned
- `MochiDropdownMenu`: anchor only, the button is drawn by the caller (see "Anchor only: draw your own button")

## Import

```kotlin
import ui.feice.mochi.basic.MochiDropdown
import ui.feice.mochi.basic.MochiIconDropdown
import ui.feice.mochi.basic.MochiDropdownMenu
import ui.feice.mochi.basic.MochiItem
import ui.feice.mochi.basic.MochiDropdownDefaults
```

## Basic usage

Single-choice menu:

```kotlin
var expanded by remember { mutableStateOf(false) }
var selected by remember { mutableIntStateOf(0) }
val items = remember { persistentListOf(MochiItem("Recent"), MochiItem("Name"), MochiItem("Size")) }

MochiDropdown(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    items = items,
    onItemClick = { selected = it },
    selectedIndex = selected,
) {
    Text("Sort: ${items[selected].label}")
}
```

Action menu with icons and a destructive item. Leave `selectedIndex` out for no selection state:

```kotlin
MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem("Share", icon = { tint -> Icon(shareIcon, null, tint = tint) }),
        MochiItem("Delete", destructive = true, icon = { tint -> Icon(deleteIcon, null, tint = tint) }),
    ),
    onItemClick = { /* … */ },
) {
    Icon(moreIcon, null)
}
```

## Disabled items, independent checks, grouping

Items don't have to be rebuilt for every menu: `id` / `enabled` / `checked` / `dividerBefore` / `groupTitleBefore` let the same set of actions live outside composition and only change state. Row height, panel height and scroll range stay the same, and `onItemClick` is still the index of the tapped item.

```kotlin
MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem("Copy link", id = "copy"),
        MochiItem("Pin", id = "pin", checked = pinned),               // independent toggle: a check circle, no selected fill
        MochiItem("Rename", id = "rename", dividerBefore = true),     // 1dp line at the row top, no extra height
        MochiItem("Share", id = "share", groupTitleBefore = "Collaborate"), // small title inside the row, still one row
        MochiItem("Export PDF", id = "pdf", enabled = false),          // disabled: one row, no highlight, no callback
        MochiItem("Delete", id = "delete", destructive = true),
    ),
    onItemClick = { /* … */ },
) { MoreDots() }
```

- A stable `id` makes rows reuse by identity: reordering items doesn't carry the staggered reveal animation or per-row state over to another item
- `enabled = false` rows are `disabled` in the semantics tree, and a semantic click won't call `onItemClick`
- `checked` and `selectedIndex` can coexist: the check column is reserved once, and the selected fill still comes only from `selectedIndex`
- `commitAfterDismiss = true` closes the menu first and calls `onItemClick` after the close animation finishes. Use it for work that must not run while the overlay is still attached (switching the root theme, rebuilding the whole tree). The default `false` still calls back first and closes 150ms later

## One level of submenus

`MochiItem.children` gives one level of submenu; child taps go to `onChildClick(parent, child)`. A parent is never "selected": tapping it switches layer.

```kotlin
MochiIconDropdown(
    expanded = moreOpen,
    onExpandedChange = { moreOpen = it },
    items = persistentListOf(
        MochiItem("Copy link", id = "copy"),
        MochiItem(
            "Share", id = "share",
            children = persistentListOf(
                MochiItem("Copy share link", id = "share-copy"),
                MochiItem("Export PDF", id = "share-pdf"),
            ),
        ),
    ),
    onItemClick = { /* top-level items */ },
    onChildClick = { parent, child -> /* parent = 0, child = 0 / 1 */ },
) { MoreDots() }
```

- Two levels only: `children` inside a submenu are not expanded
- The submenu has a back row (‹ + parent label) at the top; tap it or press back to return to the top layer, and the panel height is recomputed from the child count
- Entering a submenu scrolls from the top, going back restores the previous position, and the content reveals once more
- Without `onChildClick` an item with `children` behaves like a plain item (the tap goes to `onItemClick`), so you never get an entry that does nothing
- `MochiContextMenu` has no `onChildClick`, so `children` there is not expanded

## Anchor only: draw your own button

When the button doesn't belong to the library (your own top bar button, an icon button, any custom control) use `MochiDropdownMenu`: it renders no button, it only puts the same menu overlay on the anchor rectangle. Tap, long press and the pressed feedback stay with the caller; `expanded` is fully externally controlled.

```kotlin
var expanded by remember { mutableStateOf(false) }
var bounds by remember { mutableStateOf(IntRect.Zero) }

Box(Modifier.onGloballyPositioned { bounds = it.boundsInWindow().roundToIntRect() }) {
    TopBarActionButton(onClick = { expanded = true }, imageVector = Icons.MoreVert, contentDescription = "More")
    MochiDropdownMenu(
        anchorBounds = bounds,                 // window coordinates, converted to the screen by the library
        expanded = expanded,
        onExpandedChange = { expanded = it },
        items = items,
        onItemClick = { /* … */ },
    )
}
```

- Panel, items, submenus, animation, scrolling, haptics and the back key run on the same engine as `MochiDropdown`, so they look and feel the same
- No button clone is drawn in the overlay, and there is no press-to-open or press-and-slide: the button owns that gesture now
- No metaball neck by default (the anchor isn't drawn by the library, so it can't know its colour). Pass `anchorColor = the button colour` to get the "panel flows out of the button" neck, blended into the panel colour
- The panel is laid out at `menuWidth` and is not as wide as the anchor; with `behavior.openMode = Cover` the first frame is the anchor's size (rounded by `anchorCornerRadius`) and then it spreads
- Don't expand before `anchorBounds` is measured (`IntRect.Zero`), or the panel grows from the screen's top-left corner

## Properties

### MochiDropdown

| Parameter | Type | Description | Default |
|---|---|---|---|
| expanded | `Boolean` | Whether the menu is open | required |
| onExpandedChange | `(Boolean) -> Unit` | Open / close request | required |
| items | `ImmutableList<MochiItem>` | Menu items | required |
| onItemClick | `(Int) -> Unit` | Item selected (the menu closes itself afterwards) | required |
| onChildClick | `((Int, Int) -> Unit)?` | Submenu item tap (parent index, child index); required for `children` to expand | `null` |
| modifier | `Modifier` | Applied to the button | `Modifier` |
| enabled | `Boolean` | When false the button is dimmed and ignores input | `true` |
| selectedIndex | `Int` | Selected index, -1 = action menu | `-1` |
| colors | `MochiDropdownColors` | Colors | `MochiDropdownDefaults.dropdownColors()` |
| behavior | `MochiDropdownBehavior` | Interaction behavior | `MochiDropdownDefaults.behavior()` |
| physics | `MochiDropdownPhysics` | Metaball / jelly / rubber band | `MochiDropdownDefaults.physics()` |
| motion | `MochiMotion` | Open / close animation | `MochiTheme.motionScheme.menu` |
| menuWidth | `Dp` | Panel width (never narrower than the button) | `MochiDropdownDefaults.MenuWidth` |
| buttonPadding | `PaddingValues` | Button padding | `MochiDropdownDefaults.ButtonPadding` |
| buttonMinWidth | `Dp` | Minimum button width | `0.dp` |
| buttonMinHeight | `Dp` | Minimum button height | `MochiDropdownDefaults.ButtonMinHeight` |
| commitAfterDismiss | `Boolean` | Close the menu first, then call `onItemClick` once the close animation finishes | `false` |
| buttonContent | `@Composable RowScope.() -> Unit` | Button content; `LocalContentColor` is set to the button content color | required |

### MochiIconDropdown

Same as `MochiDropdown`, except:

| Parameter | Type | Description | Default |
|---|---|---|---|
| colors | `MochiDropdownColors` | Button and panel share one color for a seamless morph | `MochiDropdownDefaults.iconDropdownColors()` |
| behavior | `MochiDropdownBehavior` | Cover mode, end-aligned | `MochiDropdownDefaults.iconBehavior()` |
| menuWidth | `Dp` | Panel width | `MochiDropdownDefaults.IconMenuWidth` |
| size | `Dp` | Button diameter | `MochiDropdownDefaults.IconButtonSize` |
| icon | `@Composable () -> Unit` | Icon | required |

### MochiDropdownMenu

Items, colors, behavior, physics, motion, `commitAfterDismiss` and `onChildClick` are the same as `MochiDropdown`. Differences:

| Parameter | Type | Description | Default |
|---|---|---|---|
| anchorBounds | `IntRect` | Anchor rectangle (**window coordinates**, converted to the screen by the library) | required |
| menuWidth | `Dp` | Panel width, not matched to the anchor | `MochiDropdownDefaults.MenuWidth` |
| anchorCornerRadius | `Dp` | Anchor corner radius, the shape the panel starts from | `MochiDropdownDefaults.ButtonCornerRadius` |
| anchorColor | `Color` | Colour of the anchor end (metaball neck); `Color.Unspecified` = no neck | `Color.Unspecified` |

No `modifier` / `enabled` / `buttonPadding` / `buttonMinWidth` / `buttonMinHeight` / `onClick` / `buttonContent`: the button isn't in the library, and the caller's button owns those (`expanded = expanded && enabled`).

### MochiItem

| Property | Type | Description | Default |
|---|---|---|---|
| label | `String` | Text | required |
| destructive | `Boolean` | Destructive item (uses the error color) | `false` |
| icon | `(@Composable (tint: Color) -> Unit)?` | Icon slot, receives the current text color | `null` |
| id | `String?` | Stable ID: rows reuse by identity, so reordering doesn't carry state over | `null` |
| enabled | `Boolean` | Per-item disable (still one row, no highlight, no commit) | `true` |
| checked | `Boolean?` | Independent toggle state (null = plain item), shown as a check circle | `null` |
| dividerBefore | `Boolean` | 1dp divider at the row top, no extra height | `false` |
| groupTitleBefore | `String?` | Small title inside the row, doesn't take an extra row | `null` |
| children | `ImmutableList<MochiItem>` | One level of submenu (expands only with `onChildClick`) | `persistentListOf()` |

### MochiDropdownBehavior

| Property | Type | Description | Default |
|---|---|---|---|
| openMode | `MochiOpenMode` | `Detach` (metaball neck) / `Cover` (button morphs into panel) | `Detach` |
| trigger | `MochiTrigger` | `Press` opens on touch down / `LongPress` (a tap still opens) | `Press` |
| pressStyle | `MochiPressStyle` | `Grow` scale up + glow from the finger (iOS) / `Shrink` (MD3) | `Grow` |
| itemLayout | `MochiItemLayout` | `Material` icon start, check end / `Ios` check start, icon end | `Material` |
| hapticProfile | `MochiHapticProfile` | `IosLike` / `Rich` / `Off` | `IosLike` |
| align | `MochiAlign` | Panel alignment `Start` / `Center` / `End` | `Start` |
| dismissDelayMillis | `Long` | Delay before closing after a selection | `150` |

### MochiDropdownPhysics

| Property | Type | Description | Default |
|---|---|---|---|
| fuseRadius | `Dp` | Metaball radius; larger = thicker, stickier neck | `28.dp` |
| g2Smoothing | `Float` | 0 = plain rounded corners, >0 = continuous curvature | `1f` |
| gel | `Float` | Jelly stretch driven by opening velocity, 0 = off | `0.6f` |
| squash | `Float` | How much the button swells while opening | `0.04f` |
| rubberMax | `Dp` | Maximum rubber-band pull when dragging outside | `72.dp` |

## MochiDropdownDefaults

### Constants

| Name | Value | Description |
|---|---|---|
| MenuWidth | `232.dp` | Default panel width |
| IconMenuWidth | `220.dp` | Panel width for icon buttons |
| ItemHeight | `52.dp` | Item height |
| PanelPadding | `8.dp` | Panel padding |
| PanelCornerRadius | `20.dp` | Panel corner (MD3E Large Increased) |
| ButtonCornerRadius | `10_000.dp` | Button corner (capsule / circle) |
| ButtonMinHeight | `48.dp` | Minimum button height |
| IconButtonSize | `48.dp` | Icon button diameter |
| ButtonPadding | `start 20.dp, end 18.dp` | Capsule button padding |
| Gap | `8.dp` | Button-to-panel gap in detach mode |
| ScreenMargin | `12.dp` | Minimum distance from the screen edge |

### Functions

| Function | Description |
|---|---|
| `dropdownColors(...)` | Button secondaryContainer, panel surfaceContainerHigh, selection tertiaryContainer; every color can be overridden |
| `iconDropdownColors(...)` | Button and panel both surfaceContainerHighest |
| `behavior(...)` | Builds a `MochiDropdownBehavior` |
| `iconBehavior(...)` | Cover mode, end-aligned |
| `physics(...)` | Builds a `MochiDropdownPhysics` |

## Long lists

The panel height is capped (it stays on screen). When the items don't fit, the list scrolls with iOS feel:

- Dragging vertically inside the panel scrolls (after the touch slop), with a rubber band past either end; releasing flings, and hitting an end overshoots and bounces back
- Touching a list that is still flinging only catches it and doesn't select anything
- While press-dragging from the button, holding the finger near the top or bottom edge auto-scrolls, faster the closer to the edge
- The selected item is scrolled to the middle on open; a thin scroll indicator shows while scrolling and fades out afterwards

## With liquid glass

When the theme has menu glass (`MochiMaterials.menu`) and the page is recorded as a backdrop, the panel is drawn as glass and the layout and motion switch to the iOS 27 glass menu: corner 32, rows 42 tall, width 250, checkmark and icon on the leading side, and the button morphs straight into the panel with no metaball neck. Without a `motion` argument it uses `MochiMotions.Ios27Menu`. `MochiContextMenu` switches layout and motion the same way, and its menu still opens outside the card. See [iOS theme](../guide/ios-theme.md) for where the values come from.

Opening and closing: it is glass from the button onward and reshapes live to the current panel's position and size (no scaling). Position, size and content share one spring (0.8 / 0.3 to open, 0.9 / 0.3 to close, iOS native values); the corner radius goes from a capsule (half the short side) to the panel radius with progress (approximation: iOS recordings show a capsule midway; the native radius interpolation was not recovered). Closing shrinks back into the button, and the button's color returns at the end. While it moves, the content passes through a lens, with values from iOS AnimationKit's LiquidMorphAnimation: strength rises to 1 from 0 s (damping 1, response 0.2) and falls back to 0 after 0.12 s opening / 0.1 s closing (damping 1, response 0.6 / 0.4); edge displacement 75pt × strength × min(short side / 250pt, 1) (Android 13+); the content is also blurred by 4 and magnified 1.65× (panel height ≤ 215pt) to 1.2× (≥ 520pt), entering with blurInSpring 1.0 / 0.3 and leaving after the delay with blurOutSpring 0.8 / 0.45 (blur on Android 12+). Approximations: which delay and spring belong to opening vs closing is inferred; the displacement profile reuses the glass refraction profile; the blur of 4 is treated as a sigma; natively the lens acts on portals of both morph ends, here only on the menu content. It also kicks at the start: the menu is pushed vertically by 80pt × clamp((menu height − 60pt) / 140pt, 0.5, 1) × (1 − 2u) (u is the button center's relative height within the menu, so it sinks when the button is above), kicked out with damping 1 / response 0.2 and returned after 0.1 s (the return uses the kick channel spring 0.8 / 0.35, an approximation). Inside a button group, use `MochiBarGroupDropdownItem` for the item that opens a menu: with menu glass on, the whole group morphs into the menu (the group hides, the panel grows from its capsule and shrinks back into it on close), and the group's icons fade out at the start and back in at the end of closing. The menu does not grow from the single item, which would leave the group behind as a separate piece of glass. Pressing the item dims its icon to about 0.23 (measured from a recording, an approximation), without growing or a press glow. 

## Requirements

- Metaball neck, shadows and color blending need API 33+; older versions fall back to two solid shapes, gestures and animations still work
- When system animations are off, jelly / metaball / rubber band are disabled
- The overlay is attached to the current Activity's DecorView; the host must be a `ComponentActivity` (back is handled with `BackHandler`)

## Tap for an action, long press for the menu

`onClick` + `behavior(trigger = MochiTrigger.LongPress)`: a tap runs `onClick`, only a long press opens the menu. The back button's history menu works this way (tap to go back, long press for history).

Long lists are capped so the panel stays between the status bar and the navigation bar (with the usual screen margin); beyond that the list scrolls inside the panel.

Menu content is clipped to the current panel outline during opening, closing, overshoot and rubber-band motion. The clipping layer spans the overlay, rather than the final menu rectangle: animated content can cross those final bounds, which would otherwise cut off the last row. Content retains its final measured size; its translation and clip read the same frame geometry without remeasuring every animation frame.

## Implementation notes: matching iOS 26

The library started with the dropdown. This is how it matches iOS 26.

| Goal | Status | How |
|---|---|---|
| Grows out of the button with a metaball neck | ✅ | The SDFs of the button and panel rounded rectangles are merged with smooth-min, and only the extra "neck" outside both bodies is drawn. The fuse radius fades in over the first 15% of opening and returns to 0 between 50% and 95%, so the neck thins and snaps |
| Wrap mode (⋯ becomes the menu) | ✅ | The button shape equals the panel, so only one thing morphs: it widens first, then drops. Press scaling applies only while it is still a button, so the open panel never bulges |
| MD3E material | ✅ | materialkolor generates tonal colors from a seed using the 2025 spec, or follows the system palette on Android 12+. Button secondaryContainer / panel surfaceContainerHigh / selection tertiaryContainer |
| G2 corners | ✅ | Body, selection, highlight and content clip all use Kyant's G2 continuous-curvature corners, becoming a capsule at half the side length. Shader shadows and the neck use a same-size superellipse SDF |
| Shadow | ✅ | Two layers: a faint ambient shadow that is inset, widely blurred and offset downward, plus a very soft, faint contact layer. Only a trace of shadow sits at the edge and it fades out over a long distance, with no gray ring hugging the edge. Computed as the coverage of a Gaussian-blurred shape, with ±0.5/255 dithering against banding. Bigger panels cast wider, softer shadows |
| Color blend | ✅ | The shader also computes the body color, blending from button to panel color by distance. Body and neck share the formula, so the seam is continuous |
| Press glow | ✅ | Soft light rising from the finger with smooth falloff and no bright rim; its spread follows the element size |
| Press-and-slide + deformation | ✅ | The highlight's top and bottom edges are a fast and a slow spring, stretching and narrowing while sliding. Dragging out of the panel rubber-bands (iOS scroll formula plus a per-frame spring, so it follows a moving finger continuously) |
| No clipping at the bottom | ✅ | The overlay lives on the Activity's DecorView in the same window, not a Popup sub-window, so it can draw over the navigation bar |
| Sharpness | ✅ | AGSL works in pixel coordinates; body edges use path anti-aliasing |
| Not yet | ⏳ | Glass refraction / lensing (deliberately replaced by MD3E), shadows adapting to the backdrop, arrow-key navigation inside the menu, a top row of small icons |

Apple has not published the menu's spring parameters or haptic timing; both are tuned by feel.
