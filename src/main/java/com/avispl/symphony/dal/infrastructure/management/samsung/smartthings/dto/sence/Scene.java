/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Scene
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
	 * Retrieves {@code {@link #sceneId}}
	 *
	 * @return value of {@link #sceneId}
	 */
	public String getSceneId() {
		return sceneId;
	}

	/**
	 * Sets {@code sceneId}
	 *
	 * @param sceneId the {@code java.lang.String} field
	 */
	public void setSceneId(String sceneId) {
		this.sceneId = sceneId;
	}

	/**
	 * Retrieves {@code {@link #sceneName}}
	 *
	 * @return value of {@link #sceneName}
	 */
	public String getSceneName() {
		return sceneName;
	}

	/**
	 * Sets {@code sceneName}
	 *
	 * @param sceneName the {@code java.lang.String} field
	 */
	public void setSceneName(String sceneName) {
		this.sceneName = sceneName;
	}

	/**
	 * Retrieves {@code {@link #locationId}}
	 *
	 * @return value of {@link #locationId}
	 */
	public String getLocationId() {
		return locationId;
	}

	/**
	 * Sets {@code locationId}
	 *
	 * @param locationId the {@code java.lang.String} field
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}
}
