/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SceneWrapper contains list of scenes info
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SceneWrapper {

	@JsonAlias("items")
	private List<Scene> scenes = new ArrayList<>();

	/**
	 * Retrieves {@code #scenes}}
	 *
	 * @return value of {@link #scenes}
	 */
	public List<Scene> getScenes() {
		return scenes;
	}

	/**
	 * Sets {@code scenes}
	 *
	 * @param scenes the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence.Scene>} field
	 */
	public void setScenes(List<Scene> scenes) {
		this.scenes = scenes;
	}
}
