/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Alternative Command to set controllable properties
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alternative {

	@JsonAlias("key")
	private String key;

	@JsonAlias("value")
	private String value;

	/**
	 * Retrieves {@code {@link #key}}
	 *
	 * @return value of {@link #key}
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets {@code key}
	 *
	 * @param key the {@code java.lang.String} field
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Retrieves {@code {@link #value}}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets {@code value}
	 *
	 * @param value the {@code java.lang.String} field
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
