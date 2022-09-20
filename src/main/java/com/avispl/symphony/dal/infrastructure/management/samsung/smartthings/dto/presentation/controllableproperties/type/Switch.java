/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Switch presentation
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(value = "value", ignoreUnknown = true)
public class Switch {

	private String value;

	@JsonAlias("command")
	private Command command;

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets {@link #value} value
	 *
	 * @param value new value of {@link #value}
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Retrieves {@link #command}
	 *
	 * @return value of {@link #command}
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 * Sets {@link #command} value
	 *
	 * @param command new value of {@link #command}
	 */
	public void setCommand(Command command) {
		this.command = command;
	}
}
