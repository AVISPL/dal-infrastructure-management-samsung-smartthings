/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatedDeviceColorControllingConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation;

/**
 * Aggregated device info
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {

	private String state;
	private DevicePresentation presentation;

	@JsonAlias("deviceId")
	private String deviceId;

	@JsonAlias("label")
	private String name;

	@JsonAlias("presentationId")
	private String presentationId;

	@JsonAlias("manufacturerName")
	private String manufacturerName;

	@JsonAlias("locationId")
	private String locationId;

	@JsonAlias("roomId")
	private String roomId;

	@JsonAlias("components")
	private List<Component> components = new ArrayList<>();

	/**
	 * non-parameter constructor
	 */
	public Device() {
	}

	/**
	 * parameter constructor for deep clone
	 * @param device
	 */
	public Device(Device device) {
		this.state = device.getState();
		this.presentation = device.getPresentation();
		this.deviceId = device.getDeviceId();
		this.name = device.getName();
		this.presentationId = device.getPresentationId();
		this.manufacturerName = device.getManufacturerName();
		this.locationId = device.getLocationId();
		this.roomId = device.getRoomId();
		this.components = device.getComponents();
	}

	/**
	 * Retrieves {@code #state}}
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
	 * Retrieves {@code #presentation}}
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
	 * Retrieves {@code #deviceId}}
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
	 * Retrieves {@code #presentationId}}
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
	 * Retrieves {@code #manufacturerName}}
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
	 * Retrieves {@code #components}}
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

	/**
	 * Retrieves device category
	 *
	 * @return String Category of device
	 */
	public String retrieveCategory() {
		for (Component component : components) {
			if (SmartThingsConstant.MAIN.equals(component.getId())) {
				return component.retrieveCategory();
			}
		}
		return SmartThingsConstant.EMPTY;
	}

	/**
	 * This method is used to create request body for device control of non-parameter command:
	 *
	 * @param capability device capability
	 * @param command control command
	 *
	 * @return String JSON request body
	 */
	public String contributeRequestBodyForNonParamCommand(String capability, String command) {
		StringBuilder request = new StringBuilder();
		request.append("{\"commands\":[{")
				.append("\"capability\":\"").append(capability).append("\",")
				.append("\"command\":\"").append(command).append("\"")
				.append("}]}");
		return request.toString();
	}

	/**
	 * This method is used to create request body for device control of parameter command:
	 *
	 * @param capability device capability
	 * @param command control command
	 * @param arguments control arguments
	 *
	 * @return String JSON request body
	 */
	public String contributeRequestBodyForParameterCommand(String capability, String command, String arguments) {
		StringBuilder request = new StringBuilder();
		request.append("{\"commands\":[{")
				.append("\"capability\":\"").append(capability).append("\",")
				.append("\"command\":\"").append(command).append("\",")
				.append("\"arguments\":[").append(arguments).append("]")
				.append("}]}");
		return request.toString();
	}

	/**
	 * This method is used to create request body for color device control
	 *
	 * @param hue hue control arguments
	 * @param saturation saturation control arguments
	 *
	 * @return String JSON request body
	 */
	public String contributeRequestBodyForColorCommand(String hue, String saturation) {
		StringBuilder request = new StringBuilder();
		request.append("{\"commands\":[{")
				.append("\"capability\":\"").append(AggregatedDeviceColorControllingConstant.COLOR_CONTROL).append("\",")
				.append("\"command\":\"").append(AggregatedDeviceColorControllingConstant.COLOR_CONTROL_SET_HUE).append("\",")
				.append("\"arguments\":[").append(hue).append("]},")
				.append("{")
				.append("\"capability\":\"").append(AggregatedDeviceColorControllingConstant.COLOR_CONTROL).append("\",")
				.append("\"command\":\"").append(AggregatedDeviceColorControllingConstant.COLOR_CONTROL_SET_SATURATION).append("\",")
				.append("\"arguments\":[").append(saturation).append("]")
				.append("}]}");
		return request.toString();
	}

	/**
	 * This method is used to create request body for updateDevice:
	 *
	 * @param roomId room ID
	 *
	 * @return String JSON request body
	 */
	public String contributeRequestBodyForUpdateDevice(String roomId) {
		StringBuilder request = new StringBuilder();
		request.append("{")
				.append("\"roomId\":\"").append(roomId).append("\"")
				.append("}");
		return request.toString();
	}

	@Override
	public String toString() {
		return "Device{" +
				"deviceId='" + deviceId + '\'' +
				", name='" + name + '\'' +
				", presentationId='" + presentationId + '\'' +
				", manufacturerName='" + manufacturerName + '\'' +
				", locationId='" + locationId + '\'' +
				", roomId='" + roomId + '\'' +
				", components=" + components +
				", state='" + state + '\'' +
				", presentation=" + presentation +
				'}';
	}
}
