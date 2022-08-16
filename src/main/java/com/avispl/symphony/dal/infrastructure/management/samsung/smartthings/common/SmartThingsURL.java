/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * SmartThingsURL
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/27/2022
 * @since 1.0.0
 */
public class SmartThingsURL {

	private SmartThingsURL() {
	}

	// URL
	public static final String BASE_URI = "v1/";
	public static final String HUB_DEVICE = "hubdevices/";
	public static final String DEVICE_HEALTH = "/health";
	public static final String LOCATIONS = "locations";
	public static final String ROOMS = "rooms";
	public static final String DEVICES = "devices";
	public static final String CAPABILITIES = "capabilities";
	public static final String PRESENTATION = "presentation";
	public static final String SCENE = "scenes";
	public static final String EXECUTE = "/execute";
	public static final String STATUS = "/status";
	public static final String COMMANDS = "/commands";

	// Parameter
	public static final String LOCATION_ID = "locationId=";
	public static final String PRESENTATION_ID = "presentationId=";
	public static final String MANUFACTURE_NAME = "manufacturerName=";
}
