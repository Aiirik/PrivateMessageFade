# Private Fade

Private Fade is a RuneLite plugin that controls how long split private chat stays visible above the chatbox after private-message activity.

By default, Old School RuneScape keeps split private messages visible until something else replaces them. This plugin adds an inactivity timer so the split PM display can disappear automatically after a configurable delay.

## Features

- Hides split private chat after a user-defined period with no PM activity
- Resets the timer when a private message is received
- Resets the timer when a private message is sent
- Restores split private chat when the private reply input is opened
- Supports either an instant hide or a gradual fade-out effect

## Configuration

The plugin currently exposes these settings in RuneLite:

- `Fade delay`: Number of seconds to wait after the last private-message activity before hiding or fading the split PM display
- `Enable fade effect`: If enabled, the split PM display fades out over time instead of disappearing immediately
- `Fade duration`: Length of the fade animation in seconds

Setting `Fade delay` to `0` disables the inactivity hide behavior.

## Behavior

- Incoming private messages reset the inactivity timer and show the split PM display
- Outgoing private messages reset the inactivity timer and show the split PM display
- Opening the quick-reply/private-message input shows the split PM display again
- If the reply box is open, the plugin keeps the split PM display visible

## Development

New features will be added if I think of anything else that would fit this plugin.
Bug fixes will come with time.

## Status

This plugin is currently focused on split private chat visibility only. It does not modify normal chatbox message history or PM storage, only the split private-message widget shown above the chatbox.
