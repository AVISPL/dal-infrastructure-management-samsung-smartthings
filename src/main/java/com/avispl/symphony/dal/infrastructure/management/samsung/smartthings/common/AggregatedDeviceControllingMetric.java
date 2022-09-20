/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;
import java.util.Arrays;
import java.util.Optional;

/**
 * AggregatedDeviceControllingMetric defined the enum for unique controllable properties of aggregated device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/25/2022
 * @since 1.0.0
 */
public enum AggregatedDeviceControllingMetric {

	LOCATION_MANAGEMENT("Location#"),
	ROOM_MANAGEMENT("Room"),
	STATUS("Status"),
	AGGREGATED_DEVICE("AggregatedDevice"),
	COLOR_CONTROL("ColourControl"),
	HUE_CONTROL("ColourHue"),
	SATURATION_CONTROL("ColourSaturation"),
	CURRENT_COLOR_CONTROL("ColourCurrentColour"),
	CURRENT_VALUE("CurrentValue");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of Hub monitoring metric
	 */
	AggregatedDeviceControllingMetric(String name) {
		this.name = name;
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
	 * This method is used to get controlling metric group by name
	 *
	 * @param name is the name of metric that want to get
	 * @return AggregatedDeviceControllingMetric is the metric that want to get
	 */
	public static AggregatedDeviceControllingMetric getByName(String name) {
		Optional<AggregatedDeviceControllingMetric> managementGroupMetric = Arrays.stream(AggregatedDeviceControllingMetric.values()).filter(c -> name.equals(c.getName())).findFirst();
		return managementGroupMetric.orElse(AggregatedDeviceControllingMetric.AGGREGATED_DEVICE);
	}
}
