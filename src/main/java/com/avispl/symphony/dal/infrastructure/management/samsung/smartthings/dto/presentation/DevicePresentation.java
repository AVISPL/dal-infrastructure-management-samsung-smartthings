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
	 * Retrieves {@code #color}}
	 *
	 * @return value of {@link #color}
	 */
	public ColorDevicePresentation getColor() {
		return color;
	}

	/**
	 * Sets {@code color}
	 *
	 * @param color the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.ColorDevicePresentation} field
	 */
	public void setColor(ColorDevicePresentation color) {
		this.color = color;
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
	 * Retrieves {@code #dashboardPresentations}}
	 *
	 * @return value of {@link #dashboardPresentations}
	 */
	public DashboardPresentation getDashboardPresentations() {
		return dashboardPresentations;
	}

	/**
	 * Sets {@code dashboardPresentations}
	 *
	 * @param dashboardPresentations the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DashboardPresentation} field
	 */
	public void setDashboardPresentations(DashboardPresentation dashboardPresentations) {
		this.dashboardPresentations = dashboardPresentations;
	}

	/**
	 * Retrieves {@code #detailViewPresentations}}
	 *
	 * @return value of {@link #detailViewPresentations}
	 */
	public List<DetailViewPresentation> getDetailViewPresentations() {
		return detailViewPresentations;
	}

	/**
	 * Sets {@code detailViewPresentations}
	 *
	 * @param detailViewPresentations the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DetailViewPresentation>} field
	 */
	public void setDetailViewPresentations(List<DetailViewPresentation> detailViewPresentations) {
		this.detailViewPresentations = detailViewPresentations;
	}

	/**
	 * Retrieves {@code #languages}}
	 *
	 * @return value of {@link #languages}
	 */
	public List<Language> getLanguages() {
		return languages;
	}

	/**
	 * Sets {@code languages}
	 *
	 * @param languages the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.Language>} field
	 */
	public void setLanguages(List<Language> languages) {
		this.languages = languages;
	}
}
