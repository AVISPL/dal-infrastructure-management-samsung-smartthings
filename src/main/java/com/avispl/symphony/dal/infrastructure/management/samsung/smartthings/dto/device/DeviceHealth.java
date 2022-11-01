/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Device Health service tracks connected devices and hubs to SmartThings.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/31/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceHealth {

	@JsonAlias("state")
	private String state;

	@JsonAlias ("deviceId")
	private String deviceId;

	/**
	 * Retrieves {@link #state}
	 *
	 * @return value of {@link #state}
	 */
	public String getState() {
		return state;
	}

	/**
	 * Sets {@link #state} value
	 *
	 * @param state new value of {@link #state}
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * Retrieves {@link #deviceId}
	 *
	 * @return value of {@link #deviceId}
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * Sets {@link #deviceId} value
	 *
	 * @param deviceId new value of {@link #deviceId}
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
}
