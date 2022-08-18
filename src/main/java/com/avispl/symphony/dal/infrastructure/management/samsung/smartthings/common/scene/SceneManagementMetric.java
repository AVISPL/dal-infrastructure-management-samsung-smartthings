/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.scene;

/**
 * Scene Management Metric
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
public enum SceneManagementMetric {
	SCENE("Scene");

	private final String name;

	/**
	 * Parameterized constructor
	 *
	 * @param name Name of scene management metric
	 */
	SceneManagementMetric(String name) {
		this.name = name;
	}

	/**
	 * retrieve {@code {@link #name}}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return this.name;
	}
}

