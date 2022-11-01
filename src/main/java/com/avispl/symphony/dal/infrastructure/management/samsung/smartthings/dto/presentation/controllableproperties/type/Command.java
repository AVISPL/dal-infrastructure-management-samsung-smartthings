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
	 * Retrieves {@link #on}
	 *
	 * @return value of {@link #on}
	 */
	public String getOn() {
		return on;
	}

	/**
	 * Sets {@link #on} value
	 *
	 * @param on new value of {@link #on}
	 */
	public void setOn(String on) {
		this.on = on;
	}

	/**
	 * Retrieves {@link #off}
	 *
	 * @return value of {@link #off}
	 */
	public String getOff() {
		return off;
	}

	/**
	 * Sets {@link #off} value
	 *
	 * @param off new value of {@link #off}
	 */
	public void setOff(String off) {
		this.off = off;
	}

	/**
	 * Retrieves {@link #controlCommand}
	 *
	 * @return value of {@link #controlCommand}
	 */
	public String getControlCommand() {
		return controlCommand;
	}

	/**
	 * Sets {@link #controlCommand} value
	 *
	 * @param controlCommand new value of {@link #controlCommand}
	 */
	public void setControlCommand(String controlCommand) {
		this.controlCommand = controlCommand;
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

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets {@link #name} value
	 *
	 * @param name new value of {@link #name}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #alternatives}
	 *
	 * @return value of {@link #alternatives}
	 */
	public List<Alternative> getAlternatives() {
		return alternatives;
	}

	/**
	 * Sets {@link #alternatives} value
	 *
	 * @param alternatives new value of {@link #alternatives}
	 */
	public void setAlternatives(List<Alternative> alternatives) {
		this.alternatives = alternatives;
	}
}
