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
import com.avispl.symphony.dal.util.StringUtils;

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

	@JsonAlias("deviceManufacturerCode")
	private String deviceManufacturerCode;

	/**
	 * non-parameter constructor
	 */
	public Device() {
	}

	/**
	 * parameter constructor for deep clone
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
		if (StringUtils.isNotNullOrEmpty(device.getDeviceManufacturerCode())) {
			this.deviceManufacturerCode = device.getDeviceManufacturerCode();
		}
	}

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
	 * Retrieves {@link #presentation}
	 *
	 * @return value of {@link #presentation}
	 */
	public DevicePresentation getPresentation() {
		return presentation;
	}

	/**
	 * Sets {@link #presentation} value
	 *
	 * @param presentation new value of {@link #presentation}
	 */
	public void setPresentation(DevicePresentation presentation) {
		this.presentation = presentation;
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

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets {@link #name} value
	 *
	 * @param name new value of {@link #name}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #presentationId}
	 *
	 * @return value of {@link #presentationId}
	 */
	public String getPresentationId() {
		return presentationId;
	}

	/**
	 * Sets {@link #presentationId} value
	 *
	 * @param presentationId new value of {@link #presentationId}
	 */
	public void setPresentationId(String presentationId) {
		this.presentationId = presentationId;
	}

	/**
	 * Retrieves {@link #manufacturerName}
	 *
	 * @return value of {@link #manufacturerName}
	 */
	public String getManufacturerName() {
		return manufacturerName;
	}

	/**
	 * Sets {@link #manufacturerName} value
	 *
	 * @param manufacturerName new value of {@link #manufacturerName}
	 */
	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	/**
	 * Retrieves {@link #locationId}
	 *
	 * @return value of {@link #locationId}
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * Sets {@link #locationId} value
	 *
	 * @param locationId new value of {@link #locationId}
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * Retrieves {@link #roomId}
	 *
	 * @return value of {@link #roomId}
	 */
	public String getRoomId() {
		return roomId;
	}

	/**
	 * Sets {@link #roomId} value
	 *
	 * @param roomId new value of {@link #roomId}
	 */
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	/**
	 * Retrieves {@link #components}
	 *
	 * @return value of {@link #components}
	 */
	public List<Component> getComponents() {
		return components;
	}

	/**
	 * Sets {@link #components} value
	 *
	 * @param components new value of {@link #components}
	 */
	public void setComponents(List<Component> components) {
		this.components = components;
	}

	/**
	 * Retrieves {@link #deviceManufacturerCode}
	 *
	 * @return value of {@link #deviceManufacturerCode}
	 */
	public String getDeviceManufacturerCode() {
		return deviceManufacturerCode;
	}

	/**
	 * Sets {@link #deviceManufacturerCode} value
	 *
	 * @param deviceManufacturerCode new value of {@link #deviceManufacturerCode}
	 */
	public void setDeviceManufacturerCode(String deviceManufacturerCode) {
		this.deviceManufacturerCode = deviceManufacturerCode;
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
