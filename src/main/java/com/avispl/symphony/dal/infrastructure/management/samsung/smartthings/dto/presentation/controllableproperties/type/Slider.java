/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Slider presentation
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Slider {

	@JsonAlias("range")
	private List<Double> range = new ArrayList<>();

	@JsonAlias("unit")
	private String unit;

	@JsonAlias("command")
	private String command;

	@JsonAlias("argumentType")
	private String argumentType;

	/**
	 * Retrieves {@code {@link #range}}
	 *
	 * @return value of {@link #range}
	 */
	public List<Double> getRange() {
		return range;
	}

	/**
	 * Sets {@code range}
	 *
	 * @param range the {@code java.util.List<java.lang.Double>} field
	 */
	public void setRange(List<Double> range) {
		this.range = range;
	}

	/**
	 * Retrieves {@code {@link #unit}}
	 *
	 * @return value of {@link #unit}
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Sets {@code unit}
	 *
	 * @param unit the {@code java.lang.String} field
	 */
	public void setUnit(String unit) {
		this.unit = unit;
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

	/**
	 * Retrieves {@code {@link #argumentType}}
	 *
	 * @return value of {@link #argumentType}
	 */
	public String getArgumentType() {
		return argumentType;
	}

	/**
	 * Sets {@code argumentType}
	 *
	 * @param argumentType the {@code java.lang.String} field
	 */
	public void setArgumentType(String argumentType) {
		this.argumentType = argumentType;
	}
}
