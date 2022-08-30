/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room;

/**
 * Room Management Metric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum RoomManagementMetric {

	ROOM("Room"),
	ACTIVE_ROOM("ActiveRoom"),
	DELETE_ROOM("Delete");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of room management metric
	 */
	RoomManagementMetric(String name) {
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
	 * This method is used to get room management controlling metric by name
	 *
	 * @param name is the name of metric that want to get
	 * @return RoomManagementMetric is the metric that want to get
	 */
	public static RoomManagementMetric getByName(String name, String roomIndex) {
		if (name.equals(RoomManagementMetric.ROOM.getName().concat(roomIndex))) {
			return ROOM;
		}
		if (name.equals(RoomManagementMetric.ROOM.getName().concat(roomIndex).concat(RoomManagementMetric.DELETE_ROOM.getName()))) {
			return DELETE_ROOM;
		}
		throw new IllegalStateException("Could not find the create room metric with name: " + name);
	}
}

