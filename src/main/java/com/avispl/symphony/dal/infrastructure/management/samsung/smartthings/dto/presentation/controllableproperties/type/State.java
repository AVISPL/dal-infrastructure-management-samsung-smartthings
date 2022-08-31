/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * State of Controllable property
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class State {

	@JsonAlias ("alternatives")
	private List<Alternative> alternatives = new ArrayList<>();

	/**
	 * Retrieves {@code #alternatives}}
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
