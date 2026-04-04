# Private Message Fade

Private Message Fade is a RuneLite plugin that automatically hides or fades split private messages after inactivity, while optionally showing unread-message notifications.

By default, Old School RuneScape keeps split private messages visible until something else replaces them. This plugin adds a configurable inactivity timer so split PMs can disappear automatically after a delay.

## Features

- Hides split private chat after a configurable period with no PM activity
- Supports either instant hide or gradual fade-out
- Resets visibility when private messages are received or sent
- Keeps split PMs visible while the private reply input is open
- Handles existing split PM text when the plugin is enabled mid-session
- Optional unread-message notifications after split PMs have faded away
- Optional notification on the `Private` chat tab
- Optional RuneLite-style movable notification widget above the chatbox
- Separate `Off`, `No count`, and `Count` display modes for both notification styles
- Optional `Private tab marks read` behavior to clear unread notifications when the user switches to the `Private` tab
- Optional `ESC closes PM window` behavior while typing a private reply

## Notification Modes

The plugin supports two independent unread-notification displays:

- `Widget notification`
  Shows a RuneLite overlay widget above the chatbox.
- `Private tab notification`
  Shows a `!` or count on the `Private` chat tab.

Each display mode can be set to:

- `Off`
- `No count`
- `Count`

This lets users enable either display, both, or neither.

## Configuration

### Split PM fading

- `Fade delay`
  Number of seconds to wait after the last PM activity before hiding or fading split PMs
- `Enable fade effect`
  Fades split PMs out gradually instead of hiding them instantly
- `Fade duration`
  Length of the fade animation in seconds
- `ESC closes PM window`
  Pressing `Esc` while typing a private message closes that reply input only

Setting `Fade delay` to `0` disables split-PM hiding.

### Widget notification

- `Widget notification`
  Controls whether the movable RuneLite widget is off, shown without a count, or shown with a count
- `Widget text`
  Text color for the widget
- `Widget bold`
  Makes the widget text bold
- `Widget text size`
  Adjusts the widget font size
- `Widget background`
  Background color and transparency for the widget

### Private tab notification

- `Private tab notification`
  Controls whether the `Private` tab notification is off, shown without a count, or shown with a count
- `Private tab text`
  Text color for the `Private` tab notification
- `Private tab marks read`
  Clears unread notifications when the `Private` tab is opened and suppresses notifications while that tab is active
- `Private ! size`
  Font size for the `!` on the `Private` tab
- `Private count size`
  Font size for the unread count on the `Private` tab
- `Private Offset X`
  Horizontal offset for the `Private` tab notification
- `Private Offset Y`
  Vertical offset for the `Private` tab notification

## Behavior

- Incoming private messages reset the split-PM inactivity timer
- Outgoing private messages reset the split-PM inactivity timer
- Opening the private reply input restores split PM visibility
- While the private reply input is open, split PMs remain visible
- If split PM text already exists when the plugin becomes active, the plugin can still fade that text without requiring a full RuneLite restart
- Unread notifications only appear after split PMs are fully hidden

## Scope

This plugin only affects the split private-message display above the chatbox and its unread indicators. It does not change normal chatbox history, PM storage, or RuneLite's native message logs.
