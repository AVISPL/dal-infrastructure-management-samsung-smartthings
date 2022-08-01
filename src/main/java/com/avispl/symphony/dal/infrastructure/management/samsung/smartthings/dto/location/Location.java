/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Location
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {

	@JsonAlias("locationId")
	private String locationId;

	@JsonAlias("name")
	private String name;

	@JsonAlias("parent")
	private LocationParent locationParent;

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

	/**
	 * Retrieves {@code {@link #locationParent}}
	 *
	 * @return value of {@link #locationParent}
	 */
	public LocationParent getLocationParent() {
		return locationParent;
	}

	/**
	 * Sets {@code locationParent}
	 *
	 * @param locationParent the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.LocationParent} field
	 */
	public void setLocationParent(LocationParent locationParent) {
		this.locationParent = locationParent;
	}
}