/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

import java.util.Arrays;
import java.util.Optional;

/**
 * SmartThingsManagementGroupMetric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum AggregatorManagementGroupMetric {

	LOCATION_MANAGEMENT("LocationManagement#"),
	DEVICES_DASHBOARD("Devices#"),
	SCENE("ScenesTrigger#"),
	ROOM_MANAGEMENT("RoomManagement#"),
	CREATE_ROOM("CreateRoom#"),
	AGGREGATED_DEVICE("AggregatedDevice#");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of Hub monitoring metric
	 */
	AggregatorManagementGroupMetric(String name) {
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

	/**
	 * This method is used to get controlling metric group by name
	 *
	 * @param name is the name of management metric that want to get
	 * @return AggregatorManagementGroupMetric is the management metric group that want to get
	 */
	public static AggregatorManagementGroupMetric getByName(String name) {
		Optional<AggregatorManagementGroupMetric> managementGroupMetric = Arrays.stream(AggregatorManagementGroupMetric.values()).filter(c -> name.contains(c.getName())).findFirst();
		if (managementGroupMetric.isPresent()) {
			return managementGroupMetric.get();
		}
		return AggregatorManagementGroupMetric.AGGREGATED_DEVICE;
	}
}
