/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * SmartThingsConstant
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/27/2022
 * @since 1.0.0
 */
public class SmartThingsConstant {

	/**
	 * private constructor to prevent instance initialization
	 */
	private SmartThingsConstant() {
	}

	public static final String HASH = "#";
	public static final String COMMA = ",";
	public static final String QUESTION_MARK = "?";
	public static final String AMPERSAND = "&";
	public static final String SLASH = "/";
	public static final String RIGHT_PARENTHESES = ")";
	public static final String LEFT_PARENTHESES = "(";
	public static final String PERCENT_UNIT = "(%)";
	public static final String EMPTY = "";
	public static final String ON = "On";
	public static final String OFF = "Off";
	public static final String RUN = "Run";
	public static final String RUNNING = "Running";
	public static final String SUCCESSFUL = "Successful";
	public static final String CREATE = "Create";
	public static final String CREATING = "Creating";
	public static final String DELETE = "Delete";
	public static final String DELETING = "Deleting";
	public static final String CANCEL = "Cancel";
	public static final String CANCELING = "Canceling";
	public static final String PUSH = "Push";
	public static final String PUSHING = "Pushing";
	public static final String MAIN = "main";
	public static final String ONLINE = "ONLINE";
	public static final String OFFLINE = "OFFLINE";
	public static final String NO_ROOM_ASSIGNED = "No room assigned";
	public static final String NO_LOCATION_FOUND = "No location found";
	public static final String NONE = "None";
	public static final String RATE_LIMIT_HEADER_KEY = "X-RateLimit-Reset";
	public static final String IS_VALID_CONFIG_MANAGEMENT = "true";
	public static final int MAX_ROOM_QUANTITY = 20;

	// Thread metric
	public static final int MAX_THREAD_QUANTITY = 8;
	public static final int MIN_THREAD_QUANTITY = 1;
	public static final int MAX_DEVICE_QUANTITY_PER_THREAD = 6;
	public static final int MIN_POLLING_INTERVAL = 1;
	public static final int CONVERT_POSITION_TO_INDEX = 1;
	public static final int FIRST_MONITORING_CYCLE_OF_POLLING_INTERVAL = 0;

	// presentations
	public static final String VALUE = "value";
	public static final String TV_PLAYBACK_STATUS = "playbackStatus";
	public static final String TV_AUDIO_MUTE_CAPABILITY = "audioMute";
	public static final String TV_AUDIO_MUTE = "muted";
	public static final String UNIT = "unit";
	public static final String ENG_LOCALE = "en";

	// Broken device capabilities
	public static final String TV_CHANNEL = "tvChannel";
}
