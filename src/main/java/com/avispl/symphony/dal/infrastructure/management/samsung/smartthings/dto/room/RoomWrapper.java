/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * RoomWrapper
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomWrapper {

	@JsonAlias("items")
	private List<Room> rooms = new ArrayList<>();

	/**
	 * Retrieves {@code {@link #rooms }}
	 *
	 * @return value of {@link #rooms}
	 */
	public List<Room> getRooms() {
		return rooms;
	}

	/**
	 * Sets {@code rooms}
	 *
	 * @param rooms the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.Room>} field
	 */
	public void setRooms(List<Room> rooms) {
		this.rooms = rooms;
	}
}
