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
	private float hue;
	private float saturation;

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
	public ColorDevicePresentation(float hue, float saturation, String currentColor) {
		this.hue = hue;
		this.saturation = saturation;
		this.currentColor = currentColor;
	}

	/**
	 * Retrieves {@code {@link #hue}}
	 *
	 * @return value of {@link #hue}
	 */
	public float getHue() {
		return hue;
	}

	/**
	 * Sets {@code hue}
	 *
	 * @param hue the {@code float} field
	 */
	public void setHue(float hue) {
		this.hue = hue;
	}

	/**
	 * Retrieves {@code {@link #saturation}}
	 *
	 * @return value of {@link #saturation}
	 */
	public float getSaturation() {
		return saturation;
	}

	/**
	 * Sets {@code saturation}
	 *
	 * @param saturation the {@code float} field
	 */
	public void setSaturation(float saturation) {
		this.saturation = saturation;
	}

	/**
	 * Retrieves {@code {@link #currentColor}}
	 *
	 * @return value of {@link #currentColor}
	 */
	public String getCurrentColor() {
		return currentColor;
	}

	/**
	 * Sets {@code currentColor}
	 *
	 * @param currentColor the {@code java.lang.String} field
	 */
	public void setCurrentColor(String currentColor) {
		this.currentColor = currentColor;
	}


}
