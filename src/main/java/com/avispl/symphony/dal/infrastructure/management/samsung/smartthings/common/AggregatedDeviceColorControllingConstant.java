/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * AggregatedColorControllingMetric defined the constants for color controlling in aggregated device.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public class AggregatedDeviceColorControllingConstant {

	/**
	 * private constructor to prevent instance initialization
	 */
	private AggregatedDeviceColorControllingConstant() {
	}

	public static final String COLOR_CONTROL = "colorControl";

	// control command for color control
	public static final String COLOR_CONTROL_HUE = "hue";
	public static final String COLOR_CONTROL_SET_HUE = "setHue";
	public static final String COLOR_CONTROL_SATURATION = "saturation";
	public static final String COLOR_CONTROL_SET_SATURATION = "setSaturation";

	// color names
	public static final String BLUE = "Blue";
	public static final String CYAN = "Cyan";
	public static final String GREEN = "Green";
	public static final String ORANGE = "Orange";
	public static final String PINK = "Pink";
	public static final String RED = "Red";
	public static final String WHITE = "White";
	public static final String YELLOW = "Yellow";
	public static final String CUSTOM_COLOR = "CustomColour";

	// Color Sections
	public static final String RED_SECTION = "Red";
	public static final String ORANGE_SECTION = "Orange";
	public static final String YELLOW_SECTION = "Yellow";
	public static final String YELLOW_GREEN_SECTION = "Yellow Green";
	public static final String GREEN_SECTION = "Green";
	public static final String BLUE_GREEN_SECTION = "Blue Green";
	public static final String BLUE_SECTION = "Blue";
	public static final String BLUE_VIOLET_SECTION = "Blue Violet";
	public static final String VIOLET_SECTION = "Violet";
	public static final String MAUVE_SECTION = "Mauve";
	public static final String MAUVE_PINK_SECTION = "Mauve Pink";
	public static final String PINK_SECTION = "Pink";

	// Color section value ranges
	public static final float HUE_COORDINATE = 0;
	public static final float REDS_RANGE = 30;
	public static final float ORANGES_RANGE = 60;
	public static final float YELLOWS_RANGE = 90;
	public static final float YELLOW_GREENS_RANGE = 120;
	public static final float GREENS_RANGE = 150;
	public static final float BLUE_GREENS_RANGE = 180;
	public static final float BLUES_RANGE = 210;
	public static final float BLUE_VIOLETS_RANGE = 240;
	public static final float VIOLETS_RANGE = 270;
	public static final float MAUVES_RANGE = 300;
	public static final float MAUVE_PINKS_RANGE = 330;
	public static final float PINKS_RANGE = 360;

	// Color limitation values
	public static final float MAX_HUE = 360;
	public static final float MIN_HUE = 0;
	public static final float MAX_SATURATION = 100;
	public static final float MIN_SATURATION = 0;
	public static final float ONE_HUNDRED_PERCENT = 100;
	public static final float DEFAULT_BRIGHTNESS = 1;
}
