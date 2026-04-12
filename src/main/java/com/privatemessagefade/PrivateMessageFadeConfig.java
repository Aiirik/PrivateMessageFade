package com.privatemessagefade;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("privatemessagefade")
public interface PrivateMessageFadeConfig extends Config
{
	/* Indicator display options for widgets and the private tab. Moved here to keep the small enum next to the config using it. */
	enum IndicatorDisplayOption
	{
		OFF,
		NO_COUNT,
		COUNT
	}

	@ConfigSection(
		name = "Fade settings",
		description = "Settings for split private message fading.",
		position = 0
	)
	String fadeSettings = "fadeSettings";

	@ConfigSection(
		name = "Widget notification",
		description = "Settings for the RuneLite-style widget notification.",
		position = 1
	)
	String widgetNotificationSection = "widgetNotificationSection";

	@ConfigSection(
		name = "Private tab notification",
		description = "Settings for the Private chat tab notification.",
		position = 2
	)
	String privateTabNotificationSection = "privateTabNotificationSection";

	@Range(min = 0, max = 600)
	@ConfigItem(
		position = 0,
		keyName = "fadeDelaySeconds",
		name = "Display Duration",
		description = "How long (seconds) split private chat stays at full opacity before fading. Set to 0 to disable.",
		section = fadeSettings
	)
	default int fadeDelaySeconds()
	{
		return 10;
	}

	@ConfigItem(
		position = 1,
		keyName = "enableFadeEffect",
		name = "Enable fade effect",
		description = "Gradually fades split private chat instead of hiding it instantly.",
		section = fadeSettings
	)
	default boolean enableFadeEffect()
	{
		return true;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		position = 2,
		keyName = "fadeDurationSeconds",
		name = "Fade Duration",
		description = "How long (seconds) the fade-out animation takes after the display duration.",
		section = fadeSettings
	)
	default int fadeDurationSeconds()
	{
		return 1;
	}

	@ConfigItem(
		position = 3,
		keyName = "escClosesPrivateMessage",
		name = "ESC closes PM window",
		description = "Pressing Esc while typing a private message cancels and closes that message input only.",
		section = fadeSettings
	)
	default boolean escClosesPrivateMessage()
	{
		return false;
	}

	@ConfigItem(
		position = 4,
		keyName = "movableWidgetDisplay",
		name = "Widget notification",
		description = "How to show the unread indicator as a RuneLite overlay widget above the chatbox.",
		section = widgetNotificationSection
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
		description = "Text color used for the movable widget indicator.",
		section = widgetNotificationSection
	)
	default Color widgetTextColor()
	{
		return new Color(255, 235, 90);
	}

	@ConfigItem(
		position = 6,
		keyName = "widgetBoldText",
		name = "Widget bold",
		description = "Uses a bold version of RuneLite's infobox font for the widget notification.",
		section = widgetNotificationSection
	)
	default boolean widgetBoldText()
	{
		return false;
	}

	@Range(min = 0, max = 64)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 7,
		keyName = "widgetSize",
		name = "Widget size",
		description = "Configures the size of widget notifications in pixels. Set to 0 to use RuneLite's infobox size.",
		section = widgetNotificationSection
	)
	default int widgetSize()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		position = 8,
		keyName = "widgetBackgroundColor",
		name = "Widget background",
		description = "Background color used for the movable widget indicator. Lower alpha makes it more transparent.",
		section = widgetNotificationSection
	)
	default Color widgetBackgroundColor()
	{
		return new Color(70, 61, 50, 156);
	}

	@Range(min = 0, max = 50)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 9,
		keyName = "widgetCountSpacing",
		name = "Widget !# spacing",
		description = "Number of spaces between ! and count. (0 = no space, 50 = maximum)",
		section = widgetNotificationSection
	)
	default int widgetCountSpacing()
	{
		return 0;
	}

	@ConfigItem(
		position = 10,
		keyName = "privateTabDisplay",
		name = "Private tab notification",
		description = "How to show the unread indicator on the Private chat tab.",
		section = privateTabNotificationSection
	)
	default IndicatorDisplayOption privateTabDisplay()
	{
		return IndicatorDisplayOption.OFF;
	}

	@Alpha
	@ConfigItem(
		position = 11,
		keyName = "privateTabTextColor",
		name = "Private tab text",
		description = "Text color used for the Private chat tab indicator.",
		section = privateTabNotificationSection
	)
	default Color privateTabTextColor()
	{
		return new Color(255, 235, 90);
	}

	@ConfigItem(
		position = 12,
		keyName = "privateTabClickMarksRead",
		name = "Private tab marks read",
		description = "Switching to the Private tab clears unread notifications and suppresses them while that tab is selected.",
		section = privateTabNotificationSection
	)
	default boolean privateTabClickMarksRead()
	{
		return false;
	}

	@ConfigItem(
		position = 13,
		keyName = "openSplitChatOnPrivateTab",
		name = "Private tab opens split chat",
		description = "Switching to the Private tab restores split private chat visibility.",
		section = privateTabNotificationSection
	)
	default boolean openSplitChatOnPrivateTab()
	{
		return true;
	}

	@ConfigItem(
		position = 14,
		keyName = "keepSplitChatOpenOnPrivateTab",
		name = "Keep split chat open",
		description = "While the Private tab is selected, keep split private chat visible and prevent it from fading.",
		section = privateTabNotificationSection
	)
	default boolean keepSplitChatOpenOnPrivateTab()
	{
		return false;
	}

	@Range(min = 8, max = 32)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 15,
		keyName = "indicatorBangSize",
		name = "Private ! size",
		description = "Font size used for the ! on the Private chat tab indicator.",
		section = privateTabNotificationSection
	)
	default int indicatorBangSize()
	{
		return 15;
	}

	@Range(min = 0, max = 50)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 16,
		keyName = "privateTabCountSpacing",
		name = "Private !# spacing",
		description = "Number of spaces between ! and count on the Private tab. (0 = no space, 50 = maximum)",
		section = privateTabNotificationSection
	)
	default int privateTabCountSpacing()
	{
		return 0;
	}

	@Range(min = 8, max = 32)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 17,
		keyName = "indicatorCountSize",
		name = "Private count size",
		description = "Font size used for the unread count on the Private chat tab indicator.",
		section = privateTabNotificationSection
	)
	default int indicatorCountSize()
	{
		return 13;
	}

	@Range(min = -200, max = 200)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 18,
		keyName = "indicatorOffsetX",
		name = "Private Offset X",
		description = "Horizontal offset for the unread indicator on the Private chat tab.",
		section = privateTabNotificationSection
	)
	default int indicatorOffsetX()
	{
		return 0;
	}

	@Range(min = -200, max = 200)
	@Units(Units.PIXELS)
	@ConfigItem(
		position = 19,
		keyName = "indicatorOffsetY",
		name = "Private Offset Y",
		description = "Vertical offset for the unread indicator on the Private chat tab.",
		section = privateTabNotificationSection
	)
	default int indicatorOffsetY()
	{
		return 0;
	}
}
