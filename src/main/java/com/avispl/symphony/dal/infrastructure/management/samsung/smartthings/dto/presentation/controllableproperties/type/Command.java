/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Command to set controllable properties
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Command {

	@JsonAlias("on")
	private String on;

	@JsonAlias("off")
	private String off;

	@JsonAlias("command")
	private String controlCommand;

	@JsonAlias("argumentType")
	private String argumentType;

	@JsonAlias("name")
	private String name;

	@JsonAlias ("alternatives")
	private List<Alternative> alternatives = new ArrayList<>();

	/**
	 * Retrieves {@code {@link #on}}
	 *
	 * @return value of {@link #on}
	 */
	public String getOn() {
		return on;
	}

	/**
	 * Sets {@code on}
	 *
	 * @param on the {@code java.lang.String} field
	 */
	public void setOn(String on) {
		this.on = on;
	}

	/**
	 * Retrieves {@code {@link #off}}
	 *
	 * @return value of {@link #off}
	 */
	public String getOff() {
		return off;
	}

	/**
	 * Sets {@code off}
	 *
	 * @param off the {@code java.lang.String} field
	 */
	public void setOff(String off) {
		this.off = off;
	}

	/**
	 * Retrieves {@code {@link #controlCommand }}
	 *
	 * @return value of {@link #controlCommand}
	 */
	public String getControlCommand() {
		return controlCommand;
	}

	/**
	 * Sets {@code command}
	 *
	 * @param controlCommand the {@code java.lang.String} field
	 */
	public void setControlCommand(String controlCommand) {
		this.controlCommand = controlCommand;
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

	/**
	 * Retrieves {@code {@link #name}}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets {@code name}
	 *
	 * @param name the {@code java.lang.String} field
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@code {@link #alternatives}}
	 *
	 * @return value of {@link #alternatives}
	 */
	public List<Alternative> getAlternatives() {
		return alternatives;
	}

	/**
	 * Sets {@code alternatives}
	 *
	 * @param alternatives the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Alternative>} field
	 */
	public void setAlternatives(List<Alternative> alternatives) {
		this.alternatives = alternatives;
	}
}
