/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.DropdownList;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.PushButton;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Slider;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Switch;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.TextOrNumber;

/**
 * Detail View Presentation define how the SmartThings app (or other clients) should present the Attributes and Commands of a Device in detail view/ Aggregated Device view
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/26/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailViewPresentation {

	@JsonAlias("capability")
	private String capability;

	@JsonAlias("label")
	private String label;

	@JsonAlias("displayType")
	private String displayType;

	@JsonAlias({"standbyPowerSwitch", "toggleSwitch"})
	private Switch standbyPowerSwitch;

	@JsonAlias("list")
	private DropdownList dropdownList;

	@JsonAlias("pushButton")
	private PushButton pushButton;

	@JsonAlias("slider")
	private Slider slider;

	@JsonAlias("state")
	private Slider state;

	@JsonAlias("numberField")
	private TextOrNumber numberField;

	@JsonAlias("textField")
	private TextOrNumber textField;

	/**
	 * Retrieves {@link #capability}
	 *
	 * @return value of {@link #capability}
	 */
	public String getCapability() {
		return capability;
	}

	/**
	 * Sets {@link #capability} value
	 *
	 * @param capability new value of {@link #capability}
	 */
	public void setCapability(String capability) {
		this.capability = capability;
	}

	/**
	 * Retrieves {@link #label}
	 *
	 * @return value of {@link #label}
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets {@link #label} value
	 *
	 * @param label new value of {@link #label}
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Retrieves {@link #displayType}
	 *
	 * @return value of {@link #displayType}
	 */
	public String getDisplayType() {
		return displayType;
	}

	/**
	 * Sets {@link #displayType} value
	 *
	 * @param displayType new value of {@link #displayType}
	 */
	public void setDisplayType(String displayType) {
		this.displayType = displayType;
	}

	/**
	 * Retrieves {@link #standbyPowerSwitch}
	 *
	 * @return value of {@link #standbyPowerSwitch}
	 */
	public Switch getStandbyPowerSwitch() {
		return standbyPowerSwitch;
	}

	/**
	 * Sets {@link #standbyPowerSwitch} value
	 *
	 * @param standbyPowerSwitch new value of {@link #standbyPowerSwitch}
	 */
	public void setStandbyPowerSwitch(Switch standbyPowerSwitch) {
		this.standbyPowerSwitch = standbyPowerSwitch;
	}

	/**
	 * Retrieves {@link #dropdownList}
	 *
	 * @return value of {@link #dropdownList}
	 */
	public DropdownList getDropdownList() {
		return dropdownList;
	}

	/**
	 * Sets {@link #dropdownList} value
	 *
	 * @param dropdownList new value of {@link #dropdownList}
	 */
	public void setDropdownList(DropdownList dropdownList) {
		this.dropdownList = dropdownList;
	}

	/**
	 * Retrieves {@link #pushButton}
	 *
	 * @return value of {@link #pushButton}
	 */
	public PushButton getPushButton() {
		return pushButton;
	}

	/**
	 * Sets {@link #pushButton} value
	 *
	 * @param pushButton new value of {@link #pushButton}
	 */
	public void setPushButton(PushButton pushButton) {
		this.pushButton = pushButton;
	}

	/**
	 * Retrieves {@link #slider}
	 *
	 * @return value of {@link #slider}
	 */
	public Slider getSlider() {
		return slider;
	}

	/**
	 * Sets {@link #slider} value
	 *
	 * @param slider new value of {@link #slider}
	 */
	public void setSlider(Slider slider) {
		this.slider = slider;
	}

	/**
	 * Retrieves {@link #state}
	 *
	 * @return value of {@link #state}
	 */
	public Slider getState() {
		return state;
	}

	/**
	 * Sets {@link #state} value
	 *
	 * @param state new value of {@link #state}
	 */
	public void setState(Slider state) {
		this.state = state;
	}

	/**
	 * Retrieves {@link #numberField}
	 *
	 * @return value of {@link #numberField}
	 */
	public TextOrNumber getNumberField() {
		return numberField;
	}

	/**
	 * Sets {@link #numberField} value
	 *
	 * @param numberField new value of {@link #numberField}
	 */
	public void setNumberField(TextOrNumber numberField) {
		this.numberField = numberField;
	}

	/**
	 * Retrieves {@link #textField}
	 *
	 * @return value of {@link #textField}
	 */
	public TextOrNumber getTextField() {
		return textField;
	}

	/**
	 * Sets {@link #textField} value
	 *
	 * @param textField new value of {@link #textField}
	 */
	public void setTextField(TextOrNumber textField) {
		this.textField = textField;
	}
}
