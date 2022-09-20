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
@JsonIgnoreProperties(value = "value", ignoreUnknown = true)
public class Slider {

	private String value;
	private String unit;

	@JsonAlias("range")
	private List<Float> range = new ArrayList<>();

	@JsonAlias("command")
	private String command;

	@JsonAlias("argumentType")
	private String argumentType;

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
	 * Retrieves {@link #unit}
	 *
	 * @return value of {@link #unit}
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Sets {@link #unit} value
	 *
	 * @param unit new value of {@link #unit}
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Retrieves {@link #range}
	 *
	 * @return value of {@link #range}
	 */
	public List<Float> getRange() {
		return range;
	}

	/**
	 * Sets {@link #range} value
	 *
	 * @param range new value of {@link #range}
	 */
	public void setRange(List<Float> range) {
		this.range = range;
	}

	/**
	 * Retrieves {@link #command}
	 *
	 * @return value of {@link #command}
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Sets {@link #command} value
	 *
	 * @param command new value of {@link #command}
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Retrieves {@link #argumentType}
	 *
	 * @return value of {@link #argumentType}
	 */
	public String getArgumentType() {
		return argumentType;
	}

	/**
	 * Sets {@link #argumentType} value
	 *
	 * @param argumentType new value of {@link #argumentType}
	 */
	public void setArgumentType(String argumentType) {
		this.argumentType = argumentType;
	}
}
