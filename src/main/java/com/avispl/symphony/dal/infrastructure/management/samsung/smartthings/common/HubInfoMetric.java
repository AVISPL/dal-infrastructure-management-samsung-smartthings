/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * Hub Info Metric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum HubInfoMetric {

	NAME("HubName"),
	CURRENT_LOCATION("CurrentLocation"),
	FIRMWARE_VERSION("FirmwareVersion"),
	MIN_POLLING_INTERVAL("MinPollingInterval"),
	NEXT_POLLING_INTERVAL("NextPollingInterval"),
	STATE("Status");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of Hub monitoring metric
	 */
	HubInfoMetric(String name) {
		this.name = name;
	}

	/**
	 * retrieve {@code {@link #name}}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return this.name;
	}
}
