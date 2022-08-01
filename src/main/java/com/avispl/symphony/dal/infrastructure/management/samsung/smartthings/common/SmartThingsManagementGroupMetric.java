/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * SmartThingsManagementGroupMetric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum SmartThingsManagementGroupMetric {

	LOCATION_MANAGEMENT("LocationManagement#"),
	DEVICES_DASHBOARD("Devices#"),
	SCENE("ScenesTrigger#"),
	ROOM_MANAGEMENT("RoomManagement#"),
	CREATE_ROOM("CreateRoom#");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of Hub monitoring metric
	 */
	SmartThingsManagementGroupMetric(String name) {
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
