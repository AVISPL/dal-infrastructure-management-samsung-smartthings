/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * Specific color control
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public class ColorControlMetric {
	private ColorControlMetric() {
	}

	public static final String COLOR_CONTROL = "colorControl";

	// control command for specific control
	public static final String COLOR_CONTROL_HUE = "hue";
	public static final String COLOR_CONTROL_SET_HUE = "setHue";
	public static final String COLOR_CONTROL_SATURATION = "saturation";
	public static final String COLOR_CONTROL_SET_SATURATION = "setSaturation";

	// color name
	public static final String BLUE = "Blue";
	public static final String CYAN = "Cyan";
	public static final String GREEN = "Green";
	public static final String ORANGE = "Orange";
	public static final String PINK = "Pink";
	public static final String RED = "Red";
	public static final String WHITE = "White";
	public static final String YELLOW = "Yellow";
	public static final String CUSTOM_COLOR = "CustomColor";

	// Color Section
	public static final String REDS = "Reds";
	public static final String ORANGES = "Oranges";
	public static final String YELLOWS = "Yellows";
	public static final String YELLOW_GREENS = "Yellow Greens";
	public static final String GREENS = "Greens";
	public static final String BLUE_GREENS = "Blue Greens";
	public static final String BLUES = "Blues";
	public static final String BLUE_VIOLETS = "Blue Violets";
	public static final String VIOLETS = "Violets";
	public static final String MAUVES = "Mauves";
	public static final String MAUVE_PINKS = "Mauve Pinks";
	public static final String PINKS = "Pinks";

	// Color section value range
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

	public static final float MAX_HUE = 360;
	public static final float MIN_HUE = 0;
	public static final float MAX_SATURATION = 100;
	public static final float MIN_SATURATION = 0;
	public static final float ONE_HUNDRED_PERCENT = 100;
	public static final float DEFAULT_BRIGHTNESS = 1;
}
