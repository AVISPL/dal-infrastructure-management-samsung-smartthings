/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * HSB/ RGB color
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/24/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColorDevicePresentation {
	private float hue;
	private float saturation;
	private float brightness = 1;

	private int red;
	private int green;
	private int blue;

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
	 * Retrieves {@code {@link #brightness}}
	 *
	 * @return value of {@link #brightness}
	 */
	public float getBrightness() {
		return brightness;
	}

	/**
	 * Sets {@code brightness}
	 *
	 * @param brightness the {@code float} field
	 */
	public void setBrightness(float brightness) {
		this.brightness = brightness;
	}

	/**
	 * Retrieves {@code {@link #red}}
	 *
	 * @return value of {@link #red}
	 */
	public int getRed() {
		return red;
	}

	/**
	 * Sets {@code red}
	 *
	 * @param red the {@code int} field
	 */
	public void setRed(int red) {
		this.red = red;
	}

	/**
	 * Retrieves {@code {@link #green}}
	 *
	 * @return value of {@link #green}
	 */
	public int getGreen() {
		return green;
	}

	/**
	 * Sets {@code green}
	 *
	 * @param green the {@code int} field
	 */
	public void setGreen(int green) {
		this.green = green;
	}

	/**
	 * Retrieves {@code {@link #blue}}
	 *
	 * @return value of {@link #blue}
	 */
	public int getBlue() {
		return blue;
	}

	/**
	 * Sets {@code blue}
	 *
	 * @param blue the {@code int} field
	 */
	public void setBlue(int blue) {
		this.blue = blue;
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
