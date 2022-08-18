/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LocationWrapper
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationWrapper {

	@JsonAlias("items")
	private List<Location> locations = new ArrayList<>();

	/**
	 * Retrieves {@code {@link #locations}}
	 *
	 * @return value of {@link #locations}
	 */
	public List<Location> getLocations() {
		return locations;
	}

	/**
	 * Sets {@code locations}
	 *
	 * @param locations the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.Location>} field
	 */
	public void setLocations(List<Location> locations) {
		this.locations = locations;
	}

}
