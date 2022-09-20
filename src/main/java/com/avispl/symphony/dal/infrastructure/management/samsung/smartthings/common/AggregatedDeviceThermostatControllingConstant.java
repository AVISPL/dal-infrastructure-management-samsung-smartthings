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
public class AggregatedDeviceThermostatControllingConstant {

	/**
	 * private constructor to prevent instance initialization
	 */
	private AggregatedDeviceThermostatControllingConstant() {
	}

	public static final String THERMOSTAT_MODE = "thermostatMode";
	public static final String THERMOSTAT_MODE_OFF = "off";
	public static final String THERMOSTAT_MODE_HEAT = "heat";
	public static final String THERMOSTAT_MODE_AUTO = "auto";
	public static final String THERMOSTAT_MODE_COOL = "cool";
	public static final String THERMOSTAT_MODE_EMERGENCY = "emergencyheat";

	public static final String THERMOSTAT_FAN_MODE = "thermostatFanMode";
	public static final String THERMOSTAT_FAN_MODE_AUTO = "auto";
	public static final String THERMOSTAT_FAN_MODE_ON = "on";
	public static final String THERMOSTAT_FAN_MODE_CIRCULATE = "circulate";
}
