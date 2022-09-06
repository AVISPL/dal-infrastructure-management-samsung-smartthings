/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * DeviceCategoryMetric defined the enum for supported device categories
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public enum DeviceCategoriesMetric {

	HUB("Hub", true),
	LIGHT("Light", true),
	THERMOSTAT("Thermostat", true),
	PRESENCE_SENSOR("PresenceSensor", true),
	WINDOW_SHADE("Blind", true),
	TV("Television", true);

	private final String name;

	private final boolean isImplement;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of create room metric
	 * @param isImplement
	 */
	DeviceCategoriesMetric(String name, boolean isImplement) {
		this.name = name;
		this.isImplement = isImplement;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #isImplement}
	 *
	 * @return value of {@link #isImplement}
	 */
	public boolean isImplement() {
		return isImplement;
	}
}
