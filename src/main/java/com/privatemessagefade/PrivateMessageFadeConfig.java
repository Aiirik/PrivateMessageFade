package com.privatemessagefade;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("privatemessagefade")
public interface PrivateMessageFadeConfig extends Config
{
	@Range(min = 0, max = 600)
	@ConfigItem(
		position = 0,
		keyName = "fadeDelaySeconds",
		name = "Fade delay",
		description = "Seconds of split private chat inactivity before it hides. Set to 0 to disable."
	)
	default int fadeDelaySeconds()
	{
		return 10;
	}

	@ConfigItem(
		position = 1,
		keyName = "enableFadeEffect",
		name = "Enable fade effect",
		description = "Gradually fades split private chat instead of hiding it instantly."
	)
	default boolean enableFadeEffect()
	{
		return true;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		position = 2,
		keyName = "fadeDurationSeconds",
		name = "Fade duration",
		description = "How long the fade animation lasts after the idle delay."
	)
	default int fadeDurationSeconds()
	{
		return 1;
	}

	@ConfigItem(
		position = 3,
		keyName = "escClosesPrivateMessage",
		name = "ESC closes PM window",
		description = "Pressing Esc while typing a private message cancels and closes that message input only."
	)
	default boolean escClosesPrivateMessage()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = "movableWidgetDisplay",
		name = "Widget notification",
		description = "How to show the unread indicator as a RuneLite overlay widget above the chatbox."
	)
	default IndicatorDisplayOption movableWidgetDisplay()
	{
		return IndicatorDisplayOption.COUNT;
	}

	@Alpha
	@ConfigItem(
		position = 5,
		keyName = "widgetTextColor",
		name = "Widget text",
		description = "Text color used for the movable widget indicator."
	)
	default Color widgetTextColor()
	{
		return new Color(255, 235, 90);
	}

	@Alpha
	@ConfigItem(
		position = 6,
		keyName = "widgetBackgroundColor",
		name = "Widget background",
		description = "Background color used for the movable widget indicator. Lower alpha makes it more transparent."
	)
	default Color widgetBackgroundColor()
	{
		return new Color(35, 35, 35, 185);
	}

	@ConfigItem(
		position = 7,
		keyName = "privateTabDisplay",
		name = "Private tab notification",
		description = "How to show the unread indicator on the Private chat tab."
	)
	default IndicatorDisplayOption privateTabDisplay()
	{
		return IndicatorDisplayOption.OFF;
	}

	@Alpha
	@ConfigItem(
		position = 8,
		keyName = "privateTabTextColor",
		name = "Private tab text",
		description = "Text color used for the Private chat tab indicator."
	)
	default Color privateTabTextColor()
	{
		return new Color(255, 235, 90);
	}

	@Range(min = 8, max = 32)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 9,
		keyName = "indicatorBangSize",
		name = "Private ! size",
		description = "Font size used for the ! on the Private chat tab indicator."
	)
	default int indicatorBangSize()
	{
		return 15;
	}

	@Range(min = 8, max = 32)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 10,
		keyName = "indicatorCountSize",
		name = "Private count size",
		description = "Font size used for the unread count on the Private chat tab indicator."
	)
	default int indicatorCountSize()
	{
		return 13;
	}

	@Range(min = -200, max = 200)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 11,
		keyName = "indicatorOffsetX",
		name = "Private Offset X",
		description = "Horizontal offset for the unread indicator on the Private chat tab."
	)
	default int indicatorOffsetX()
	{
		return 0;
	}

	@Range(min = -200, max = 200)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 12,
		keyName = "indicatorOffsetY",
		name = "Private Offset Y",
		description = "Vertical offset for the unread indicator on the Private chat tab."
	)
	default int indicatorOffsetY()
	{
		return 0;
	}
}
