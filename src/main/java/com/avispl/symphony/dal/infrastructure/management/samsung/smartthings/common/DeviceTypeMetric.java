/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * Device Type Metric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public enum DeviceTypeMetric {

	HUB("Hub", true),
	TV("Television", true);

	private final String name;

	private final boolean isImplement;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of create room metric
	 * @param isImplement
	 */
	DeviceTypeMetric(String name, boolean isImplement) {
		this.name = name;
		this.isImplement = isImplement;
	}

	/**
	 * retrieve {@code {@link #name}}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Retrieves {@code {@link #isImplement}}
	 *
	 * @return value of {@link #isImplement}
	 */
	public boolean isImplement() {
		return isImplement;
	}
}
