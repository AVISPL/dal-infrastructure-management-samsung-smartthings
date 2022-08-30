/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;

/**
 * Component include and group Capabilities in a Device Profile.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Component {

	@JsonAlias("id")
	private String id;

	@JsonAlias("label")
	private String name;

	@JsonAlias("capabilities")
	private List<DeviceCapability> capabilities = new ArrayList<>();

	@JsonAlias("categories")
	private List<DeviceCategories> categories = new ArrayList<>();

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
	 * Retrieves {@code {@link #capabilities}}
	 *
	 * @return value of {@link #capabilities}
	 */
	public List<DeviceCapability> getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets {@code capabilities}
	 *
	 * @param capabilities the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapability>} field
	 */
	public void setCapabilities(List<DeviceCapability> capabilities) {
		this.capabilities = capabilities;
	}

	/**
	 * Retrieves {@code {@link #categories}}
	 *
	 * @return value of {@link #categories}
	 */
	public List<DeviceCategories> getCategories() {
		return categories;
	}

	/**
	 * Sets {@code categories}
	 *
	 * @param categories the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCategories>} field
	 */
	public void setCategories(List<DeviceCategories> categories) {
		this.categories = categories;
	}

	/**
	 * Retrieves category
	 *
	 * @return String Category of device
	 */
	public String retrieveCategory() {
		if (!categories.isEmpty()){
			return categories.get(0).getName();
		}
		return SmartThingsConstant.EMPTY;
	}

}
