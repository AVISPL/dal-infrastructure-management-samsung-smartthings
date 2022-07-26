/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Device Capabilities Wrapper contains list of device capabilities info
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/30/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceCapabilityWrapper {

	@JsonAlias("items")
	List<DeviceCapability> deviceCapabilities = new ArrayList<>();

	/**
	 * Retrieves {@link #deviceCapabilities}
	 *
	 * @return value of {@link #deviceCapabilities}
	 */
	public List<DeviceCapability> getDeviceCapabilities() {
		return deviceCapabilities;
	}

	/**
	 * Sets {@link #deviceCapabilities} value
	 *
	 * @param deviceCapabilities new value of {@link #deviceCapabilities}
	 */
	public void setDeviceCapabilities(List<DeviceCapability> deviceCapabilities) {
		this.deviceCapabilities = deviceCapabilities;
	}
}
