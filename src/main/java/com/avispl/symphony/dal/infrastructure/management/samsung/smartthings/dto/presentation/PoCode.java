/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Controllable property label by locale
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoCode {

	@JsonAlias("label")
	private String label;

	@JsonAlias("po")
	private String po;

	/**
	 * Retrieves {@link #label}
	 *
	 * @return value of {@link #label}
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets {@link #label} value
	 *
	 * @param label new value of {@link #label}
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Retrieves {@link #po}
	 *
	 * @return value of {@link #po}
	 */
	public String getPo() {
		return po;
	}

	/**
	 * Sets {@link #po} value
	 *
	 * @param po new value of {@link #po}
	 */
	public void setPo(String po) {
		this.po = po;
	}
}
