/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DeviceCapabilities contain capability ID and Version for getting Attributes and Commands. Attributes represent the state of information or properties of the Device, while Commands allow for the control of device functions.
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
	 * Retrieves {@link #id}
	 *
	 * @return value of {@link #id}
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets {@link #id} value
	 *
	 * @param id new value of {@link #id}
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Retrieves {@link #version}
	 *
	 * @return value of {@link #version}
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets {@link #version} value
	 *
	 * @param version new value of {@link #version}
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
