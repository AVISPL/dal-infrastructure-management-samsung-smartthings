/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import java.util.ArrayList;
import java.util.List;

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

	@JsonAlias ("dashboard")
	private List<DashboardPresentation> dashboardPresentations = new ArrayList<>();

	@JsonAlias ("detailView")
	private List<DetailViewPresentation> detailViewPresentations = new ArrayList<>();

	@JsonAlias("language")
	private List<Language> languages = new ArrayList<>();
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

	/**
	 * Retrieves {@code {@link #dashboardPresentations}}
	 *
	 * @return value of {@link #dashboardPresentations}
	 */
	public List<DashboardPresentation> getDashboardPresentations() {
		return dashboardPresentations;
	}

	/**
	 * Sets {@code dashboardPresentations}
	 *
	 * @param dashboardPresentations the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DashboardPresentation>} field
	 */
	public void setDashboardPresentations(List<DashboardPresentation> dashboardPresentations) {
		this.dashboardPresentations = dashboardPresentations;
	}

	/**
	 * Retrieves {@code {@link #detailViewPresentations}}
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
	 * Retrieves {@code {@link #languages}}
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
