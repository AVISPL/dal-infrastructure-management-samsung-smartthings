/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Device Wrapper contains list of devices info
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/27/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceWrapper {

	@JsonAlias("items")
	private List<Device> devices = new ArrayList<>();

	/**
	 * Retrieves {@link #devices}
	 *
	 * @return value of {@link #devices}
	 */
	public List<Device> getDevices() {
		return devices;
	}

	/**
	 * Sets {@link #devices} value
	 *
	 * @param devices new value of {@link #devices}
	 */
	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}
}
