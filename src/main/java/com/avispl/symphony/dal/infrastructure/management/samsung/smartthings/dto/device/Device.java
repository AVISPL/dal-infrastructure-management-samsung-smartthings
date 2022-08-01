/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation;

/**
 * Aggregated device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

	@JsonAlias("deviceId")
	private String deviceId;

	@JsonAlias("label")
	private String name;

	@JsonAlias ("presentationId")
	private String presentationId;

	@JsonAlias ("manufacturerName")
	private String manufacturerName;

	@JsonAlias ("locationId")
	private String locationId;

	@JsonAlias ("roomId")
	private String roomId;

	@JsonAlias ("components")
	private List<Component> components = new ArrayList<>();

	private String state;

	private DevicePresentation presentation;

	/**
	 * Retrieves {@code {@link #deviceId}}
	 *
	 * @return value of {@link #deviceId}
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * Sets {@code deviceId}
	 *
	 * @param deviceId the {@code java.lang.String} field
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
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
	 * Retrieves {@code {@link #presentationId}}
	 *
	 * @return value of {@link #presentationId}
	 */
	public String getPresentationId() {
		return presentationId;
	}

	/**
	 * Sets {@code presentationId}
	 *
	 * @param presentationId the {@code java.lang.String} field
	 */
	public void setPresentationId(String presentationId) {
		this.presentationId = presentationId;
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
	 * Retrieves {@code {@link #state}}
	 *
	 * @return value of {@link #state}
	 */
	public String getState() {
		return state;
	}

	/**
	 * Sets {@code state}
	 *
	 * @param state the {@code java.lang.String} field
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * Retrieves {@code {@link #manufacturerName}}
	 *
	 * @return value of {@link #manufacturerName}
	 */
	public String getManufacturerName() {
		return manufacturerName;
	}

	/**
	 * Sets {@code manufacturerName}
	 *
	 * @param manufacturerName the {@code java.lang.String} field
	 */
	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	/**
	 * Retrieves {@code {@link #presentation}}
	 *
	 * @return value of {@link #presentation}
	 */
	public DevicePresentation getPresentation() {
		return presentation;
	}

	/**
	 * Sets {@code presentation}
	 *
	 * @param presentation the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation} field
	 */
	public void setPresentation(DevicePresentation presentation) {
		this.presentation = presentation;
	}

	/**
	 * Retrieves {@code {@link #components}}
	 *
	 * @return value of {@link #components}
	 */
	public List<Component> getComponents() {
		return components;
	}

	/**
	 * Sets {@code components}
	 *
	 * @param components the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.Component>} field
	 */
	public void setComponents(List<Component> components) {
		this.components = components;
	}
}
