/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DashBoard Presentation define how the SmartThings app (or other clients) should present the Attributes and Commands of a Device in Dashboard
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardPresentation {

	@JsonAlias("actions")
	private List<DetailViewPresentation> actions = new ArrayList<>();

	/**
	 * Retrieves {@code {@link #actions}}
	 *
	 * @return value of {@link #actions}
	 */
	public List<DetailViewPresentation> getActions() {
		return actions;
	}

	/**
	 * Sets {@code actions}
	 *
	 * @param actions the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DetailViewPresentation>} field
	 */
	public void setActions(List<DetailViewPresentation> actions) {
		this.actions = actions;
	}
}
