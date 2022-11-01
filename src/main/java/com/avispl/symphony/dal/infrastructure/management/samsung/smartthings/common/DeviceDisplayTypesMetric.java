/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common;

/**
 * DeviceTypeMetric defined the constant for display type of controllable properties ( switch, slider, button,...)
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public class DeviceDisplayTypesMetric {

	/**
	 * private constructor to prevent instance initialization
	 */
	private DeviceDisplayTypesMetric() {
	}

	public static final String SWITCH = "switch";
	public static final String STAND_BY_POWER_SWITCH = "standbyPowerSwitch";
	public static final String TOGGLE_SWITCH = "toggleSwitch";
	public static final String SLIDER = "slider";
	public static final String SWITCH_LEVEL = "switchLevel";
	public static final String PUSH_BUTTON = "pushButton";
	public static final String LIST = "list";
	public static final String NUMBER_FIELD = "numberField";
	public static final String TEXT_FIELD = "textField";
	public static final String STATE = "state";

}
