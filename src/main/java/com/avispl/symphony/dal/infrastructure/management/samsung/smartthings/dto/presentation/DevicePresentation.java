/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.ColorDevicePresentation;

/**
 * Device Presentation define how the SmartThings app (or other clients) should present the Attributes and Commands of a Device in the user interface
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DevicePresentation {

	private ColorDevicePresentation color = new ColorDevicePresentation();

	@JsonAlias("mnmn")
	private String manufacturerName;

	@JsonAlias("vid")
	private String presentationId;

	@JsonAlias("dashboard")
	private DashboardPresentation dashboardPresentations;

	@JsonAlias("detailView")
	private List<DetailViewPresentation> detailViewPresentations = new ArrayList<>();

	@JsonAlias("language")
	private List<Language> languages = new ArrayList<>();

	/**
	 * Retrieves {@link #color}
	 *
	 * @return value of {@link #color}
	 */
	public ColorDevicePresentation getColor() {
		return color;
	}

	/**
	 * Sets {@link #color} value
	 *
	 * @param color new value of {@link #color}
	 */
	public void setColor(ColorDevicePresentation color) {
		this.color = color;
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
	 * Retrieves {@link #dashboardPresentations}
	 *
	 * @return value of {@link #dashboardPresentations}
	 */
	public DashboardPresentation getDashboardPresentations() {
		return dashboardPresentations;
	}

	/**
	 * Sets {@link #dashboardPresentations} value
	 *
	 * @param dashboardPresentations new value of {@link #dashboardPresentations}
	 */
	public void setDashboardPresentations(DashboardPresentation dashboardPresentations) {
		this.dashboardPresentations = dashboardPresentations;
	}

	/**
	 * Retrieves {@link #detailViewPresentations}
	 *
	 * @return value of {@link #detailViewPresentations}
	 */
	public List<DetailViewPresentation> getDetailViewPresentations() {
		return detailViewPresentations;
	}

	/**
	 * Sets {@link #detailViewPresentations} value
	 *
	 * @param detailViewPresentations new value of {@link #detailViewPresentations}
	 */
	public void setDetailViewPresentations(List<DetailViewPresentation> detailViewPresentations) {
		this.detailViewPresentations = detailViewPresentations;
	}

	/**
	 * Retrieves {@link #languages}
	 *
	 * @return value of {@link #languages}
	 */
	public List<Language> getLanguages() {
		return languages;
	}

	/**
	 * Sets {@link #languages} value
	 *
	 * @param languages new value of {@link #languages}
	 */
	public void setLanguages(List<Language> languages) {
		this.languages = languages;
	}
}
