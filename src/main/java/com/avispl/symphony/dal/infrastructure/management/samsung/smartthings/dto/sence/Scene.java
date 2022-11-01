/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Scene allowing user to define one or more actions that will occur by tapping scene trigger.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Scene {

	@JsonAlias("sceneId")
	private String sceneId;

	@JsonAlias("sceneName")
	private String sceneName;

	@JsonAlias("locationId")
	private String locationId;

	/**
	 * Retrieves {@link #sceneId}
	 *
	 * @return value of {@link #sceneId}
	 */
	public String getSceneId() {
		return sceneId;
	}

	/**
	 * Sets {@link #sceneId} value
	 *
	 * @param sceneId new value of {@link #sceneId}
	 */
	public void setSceneId(String sceneId) {
		this.sceneId = sceneId;
	}

	/**
	 * Retrieves {@link #sceneName}
	 *
	 * @return value of {@link #sceneName}
	 */
	public String getSceneName() {
		return sceneName;
	}

	/**
	 * Sets {@link #sceneName} value
	 *
	 * @param sceneName new value of {@link #sceneName}
	 */
	public void setSceneName(String sceneName) {
		this.sceneName = sceneName;
	}

	/**
	 * Retrieves {@link #locationId}
	 *
	 * @return value of {@link #locationId}
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * Sets {@link #locationId} value
	 *
	 * @param locationId new value of {@link #locationId}
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}
}
