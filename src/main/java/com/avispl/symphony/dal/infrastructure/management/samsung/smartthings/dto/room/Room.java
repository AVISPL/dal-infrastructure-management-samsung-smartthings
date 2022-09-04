/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.avispl.symphony.dal.util.StringUtils;

/**
 * Room are a subgroup that exists inside a Location
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
	 * Retrieves {@code #roomId}}
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
	 * Retrieves {@code #locationId}}
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
	 * Retrieves {@code #name}}
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

	/**
	 * This method is used to create request body for room control:
	 *
	 * @return String JSON request body
	 */
	public String contributeRequestBody() {
		StringBuilder request = new StringBuilder();
		if (StringUtils.isNotNullOrEmpty(name)){
			request.append("{\"name\":\"").append(name).append("\"}");
		}
		return request.toString();
	}

	@Override
	public String toString() {
		return "Room{" +
				"roomId='" + roomId + '\'' +
				", locationId='" + locationId + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
