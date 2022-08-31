/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room;
import java.util.Arrays;
import java.util.Optional;

/**
 * Create Room Metric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum CreateRoomMetric {

	LOCATION("Location"),
	ROOM_NAME("RoomName"),
	CREATE_ROOM("CreateRoom"),
	EDITED("Edited"),
	CANCEL("Cancel");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of create room metric
	 */
	CreateRoomMetric(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@code #name}}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * This method is used to get create room controlling metric by name
	 *
	 * @param name is the name of metric that want to get
	 * @return CreateRoomMetric is the metric that want to get
	 */
	public static CreateRoomMetric getByName(String name) {
		Optional<CreateRoomMetric> createRoomMetric = Arrays.stream(CreateRoomMetric.values()).filter(c -> name.contains(c.getName())).findFirst();
		if (createRoomMetric.isPresent()) {
			return createRoomMetric.get();
		}
		throw new IllegalStateException("Could not find the create room metric with name: " + name);
	}
}
