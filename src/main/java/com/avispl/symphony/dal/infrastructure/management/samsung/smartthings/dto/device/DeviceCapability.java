/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Device Capabilities
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceCapability {

	@JsonAlias("id")
	private String id;

	@JsonAlias("version")
	private String version;

	/**
	 * Retrieves {@code {@link #id}}
	 *
	 * @return value of {@link #id}
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets {@code id}
	 *
	 * @param id the {@code java.lang.String} field
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Retrieves {@code {@link #version}}
	 *
	 * @return value of {@link #version}
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets {@code version}
	 *
	 * @param version the {@code java.lang.String} field
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "DeviceCapability{" +
				"id='" + id + '\'' +
				", version='" + version + '\'' +
				'}';
	}
}
