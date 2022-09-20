/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

import java.util.Arrays;
import java.util.Optional;

/**
 * Aggregator Group Controlling Metric defined the enum for monitoring and controlling aggregator group
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum AggregatorGroupControllingMetric {

	LOCATION_MANAGEMENT("EditLocation#"),
	DEVICES_DASHBOARD("Devices#"),
	SCENE("ScenesTrigger#"),
	ROOM_MANAGEMENT("EditRoom#"),
	CREATE_ROOM("CreateRoom#"),
	AGGREGATED_DEVICE("AggregatedDevice#"),

	// message when control group is empty
	MESSAGE("Message");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of Hub monitoring metric
	 */
	AggregatorGroupControllingMetric(String name) {
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
	 * @param name is the name of management metric that want to get
	 * @return AggregatorManagementGroupMetric is the management metric group that want to get
	 */
	public static AggregatorGroupControllingMetric getByName(String name) {
		Optional<AggregatorGroupControllingMetric> managementGroupMetric = Arrays.stream(AggregatorGroupControllingMetric.values()).filter(c -> name.equals(c.getName())).findFirst();
		if (managementGroupMetric.isPresent()) {
			return managementGroupMetric.get();
		}
		return AggregatorGroupControllingMetric.AGGREGATED_DEVICE;
	}
}
