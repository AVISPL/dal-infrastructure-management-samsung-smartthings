/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rom
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {

	@JsonAlias("roomId")
	private String roomId;

	@JsonAlias("locationId")
	private String locationId;

	@JsonAlias("name")
	private String name;

	/**
	 * Retrieves {@code {@link #roomId}}
	 *
	 * @return value of {@link #roomId}
	 */
	public String getRoomId() {
		return roomId;
	}

	/**
	 * Sets {@code roomId}
	 *
	 * @param roomId the {@code java.lang.String} field
	 */
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	/**
	 * Retrieves {@code {@link #locationId}}
	 *
	 * @return value of {@link #locationId}
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * Sets {@code locationId}
	 *
	 * @param locationId the {@code java.lang.String} field
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * Retrieves {@code {@link #name}}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets {@code name}
	 *
	 * @param name the {@code java.lang.String} field
	 */
	public void setName(String name) {
		this.name = name;
	}
}
