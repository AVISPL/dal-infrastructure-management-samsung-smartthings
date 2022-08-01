/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
	/**
	 * @param name name of location
	 */
	public String findByName(String name) {
		Optional<Location> location = locations.stream().filter(l -> name.equals(l.getName())).findFirst();
		if (location.isPresent()){
			return location.get().getLocationId();
		}
		return locations.get(0).getLocationId();
	}

}
