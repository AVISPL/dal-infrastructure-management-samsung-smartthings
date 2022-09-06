/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * HSB color data
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/24/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColorDevicePresentation {
	private Float hue;
	private Float saturation;

	private String currentColor;

	/**
	 * non-parameter constructor
	 */
	public ColorDevicePresentation() {
	}

	/**
	 * parameterized constructor
	 *
	 * @param hue hue value
	 * @param saturation saturation value
	 * @param currentColor current color name
	 */
	public ColorDevicePresentation(Float hue, Float saturation, String currentColor) {
		this.hue = hue;
		this.saturation = saturation;
		this.currentColor = currentColor;
	}

	/**
	 * Retrieves {@link #hue}
	 *
	 * @return value of {@link #hue}
	 */
	public Float getHue() {
		return hue;
	}

	/**
	 * Sets {@link #hue} value
	 *
	 * @param hue new value of {@link #hue}
	 */
	public void setHue(Float hue) {
		this.hue = hue;
	}

	/**
	 * Retrieves {@link #saturation}
	 *
	 * @return value of {@link #saturation}
	 */
	public Float getSaturation() {
		return saturation;
	}

	/**
	 * Sets {@link #saturation} value
	 *
	 * @param saturation new value of {@link #saturation}
	 */
	public void setSaturation(Float saturation) {
		this.saturation = saturation;
	}

	/**
	 * Retrieves {@link #currentColor}
	 *
	 * @return value of {@link #currentColor}
	 */
	public String getCurrentColor() {
		return currentColor;
	}

	/**
	 * Sets {@link #currentColor} value
	 *
	 * @param currentColor new value of {@link #currentColor}
	 */
	public void setCurrentColor(String currentColor) {
		this.currentColor = currentColor;
	}
}
