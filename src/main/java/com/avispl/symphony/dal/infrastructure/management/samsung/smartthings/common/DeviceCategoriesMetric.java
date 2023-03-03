/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

import java.util.Arrays;

/**
 * DeviceCategoryMetric defined the enum for supported device categories
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public enum DeviceCategoriesMetric {

	HUB("Hub", "Bridges", true),
	LIGHT("Light", "Lights", true),
	THERMOSTAT("Thermostat", "Sensor", true),
	PRESENCE_SENSOR("PresenceSensor", "Sensor", true),
	WINDOW_SHADE("Blind", "Shades", true),
	SWITCH("Switch", "Switch", true),
	TV("Television", "Monitors", true);

	private final String name;
	private final String uiName;

	private final boolean isImplement;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of create room metric
	 */
	DeviceCategoriesMetric(String name, String uiName, boolean isImplement) {
		this.name = name;
		this.uiName = uiName;
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
	 * Retrieves {@link #uiName}
	 *
	 * @return value of {@link #uiName}
	 */
	public String getUiName() {
		return uiName;
	}

	/**
	 * Retrieves {@link #isImplement}
	 *
	 * @return value of {@link #isImplement}
	 */
	public boolean isImplement() {
		return isImplement;
	}

	/**
	 * This method is used to get device category metric ui name by name
	 *
	 * @param name is the name of device category metric that want to get
	 * @return String ui name of device category metric that want to get
	 */
	public static String getUiNameByName(String name) {
		return Arrays.stream(DeviceCategoriesMetric.values()).filter(c -> name.equals(c.getName())).findFirst().map(DeviceCategoriesMetric::getUiName).orElse(SmartThingsConstant.EMPTY);
	}
}
