/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Device Presentation define how the SmartThings app (or other clients) should present the Attributes and Commands of a Device in the user interface
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DevicePresentation {

	@JsonAlias("mnmn")
	private String manufacturerName;

	@JsonAlias ("vid")
	private String presentationId;

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
}
