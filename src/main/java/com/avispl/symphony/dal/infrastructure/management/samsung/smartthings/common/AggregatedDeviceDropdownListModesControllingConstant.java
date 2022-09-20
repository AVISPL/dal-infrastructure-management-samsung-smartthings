/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * AggregatedColorControllingMetric defined the constants for thermostat controlling in aggregated device.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public class AggregatedDeviceDropdownListModesControllingConstant {

	/**
	 * private constructor to prevent instance initialization
	 */
	private AggregatedDeviceDropdownListModesControllingConstant() {
	}

	// Thermostats
	public static final String THERMOSTAT_MODE = "thermostatMode";
	public static final String THERMOSTAT_MODE_OFF = "off";
	public static final String THERMOSTAT_MODE_HEAT = "heat";
	public static final String THERMOSTAT_MODE_AUTO = "auto";
	public static final String THERMOSTAT_MODE_COOL = "cool";
	public static final String THERMOSTAT_MODE_EMERGENCY = "emergency heat";

	public static final String THERMOSTAT_FAN_MODE = "thermostatFanMode";
	public static final String THERMOSTAT_FAN_MODE_AUTO = "auto";
	public static final String THERMOSTAT_FAN_MODE_ON = "on";
	public static final String THERMOSTAT_FAN_MODE_CIRCULATE = "circulate";

	//Window shade
	public static final String WINDOW_SHADE_MODE = "windowShade";
	public static final String WINDOW_SHADE_MODE_OPEN = "open";
	public static final String WINDOW_SHADE_MODE_CLOSE = "close";
	public static final String WINDOW_SHADE_PARTIALLY_OPEN = "partially open";

	// TV media playback
	public static final String TV_MEDIA_PLAYBACK_MODE = "mediaPlayback";
	public static final String TV_MEDIA_PLAYBACK_MODE_PLAY = "play";
	public static final String TV_MEDIA_PLAYBACK_MODE_STOP = "stop";
	public static final String TV_MEDIA_PLAYBACK_MODE_REWIND = "rewind";
	public static final String TV_MEDIA_PLAYBACK_MODE_FAST_FORWARD= "fastForward";
	public static final String TV_MEDIA_PLAYBACK_MODE_PAUSE = "pause";
	public static final String TV_MEDIA_PLAYBACK_STATUS_PLAY = "playing";
	public static final String TV_MEDIA_PLAYBACK_STATUS_STOP = "stopped";
	public static final String TV_MEDIA_PLAYBACK_STATUS_REWIND = "rewinding";
	public static final String TV_MEDIA_PLAYBACK_STATUS_FAST_FORWARD= "fast forwarding";
	public static final String TV_MEDIA_PLAYBACK_STATUS_PAUSE = "paused";
}
