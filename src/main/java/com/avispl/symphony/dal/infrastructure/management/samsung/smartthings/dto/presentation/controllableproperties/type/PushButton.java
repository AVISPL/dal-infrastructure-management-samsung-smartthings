/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * PushButton presentation
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushButton {

	private String value;

	@JsonAlias("command")
	private String command;

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

	/**
	 * Retrieves {@code {@link #command}}
	 *
	 * @return value of {@link #command}
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Sets {@code command}
	 *
	 * @param command the {@code java.lang.String} field
	 */
	public void setCommand(String command) {
		this.command = command;
	}
}
