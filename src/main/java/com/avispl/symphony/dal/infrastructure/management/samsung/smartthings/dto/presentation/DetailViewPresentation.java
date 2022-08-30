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
	 * Retrieves {@code {@link #capability}}
	 *
	 * @return value of {@link #capability}
	 */
	public String getCapability() {
		return capability;
	}

	/**
	 * Sets {@code capability}
	 *
	 * @param capability the {@code java.lang.String} field
	 */
	public void setCapability(String capability) {
		this.capability = capability;
	}

	/**
	 * Retrieves {@code {@link #label}}
	 *
	 * @return value of {@link #label}
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets {@code label}
	 *
	 * @param label the {@code java.lang.String} field
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Retrieves {@code {@link #displayType}}
	 *
	 * @return value of {@link #displayType}
	 */
	public String getDisplayType() {
		return displayType;
	}

	/**
	 * Sets {@code displayType}
	 *
	 * @param displayType the {@code java.lang.String} field
	 */
	public void setDisplayType(String displayType) {
		this.displayType = displayType;
	}

	/**
	 * Retrieves {@code {@link #standbyPowerSwitch}}
	 *
	 * @return value of {@link #standbyPowerSwitch}
	 */
	public Switch getStandbyPowerSwitch() {
		return standbyPowerSwitch;
	}

	/**
	 * Sets {@code standbyPowerSwitch}
	 *
	 * @param standbyPowerSwitch the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Switch} field
	 */
	public void setStandbyPowerSwitch(Switch standbyPowerSwitch) {
		this.standbyPowerSwitch = standbyPowerSwitch;
	}

	/**
	 * Retrieves {@code {@link #dropdownList}}
	 *
	 * @return value of {@link #dropdownList}
	 */
	public DropdownList getDropdownList() {
		return dropdownList;
	}

	/**
	 * Sets {@code dropdownList}
	 *
	 * @param dropdownList the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.DropdownList} field
	 */
	public void setDropdownList(DropdownList dropdownList) {
		this.dropdownList = dropdownList;
	}

	/**
	 * Retrieves {@code {@link #pushButton}}
	 *
	 * @return value of {@link #pushButton}
	 */
	public PushButton getPushButton() {
		return pushButton;
	}

	/**
	 * Sets {@code pushButton}
	 *
	 * @param pushButton the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.PushButton} field
	 */
	public void setPushButton(PushButton pushButton) {
		this.pushButton = pushButton;
	}

	/**
	 * Retrieves {@code {@link #slider}}
	 *
	 * @return value of {@link #slider}
	 */
	public Slider getSlider() {
		return slider;
	}

	/**
	 * Sets {@code slider}
	 *
	 * @param slider the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Slider} field
	 */
	public void setSlider(Slider slider) {
		this.slider = slider;
	}

	/**
	 * Retrieves {@code {@link #state}}
	 *
	 * @return value of {@link #state}
	 */
	public Slider getState() {
		return state;
	}

	/**
	 * Sets {@code state}
	 *
	 * @param state the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.Slider} field
	 */
	public void setState(Slider state) {
		this.state = state;
	}

	/**
	 * Retrieves {@code {@link #numberField}}
	 *
	 * @return value of {@link #numberField}
	 */
	public TextOrNumber getNumberField() {
		return numberField;
	}

	/**
	 * Sets {@code numberField}
	 *
	 * @param numberField the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.TextOrNumber} field
	 */
	public void setNumberField(TextOrNumber numberField) {
		this.numberField = numberField;
	}

	/**
	 * Retrieves {@code {@link #textField}}
	 *
	 * @return value of {@link #textField}
	 */
	public TextOrNumber getTextField() {
		return textField;
	}

	/**
	 * Sets {@code textField}
	 *
	 * @param textField the {@code com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.controllableproperties.type.TextOrNumber} field
	 */
	public void setTextField(TextOrNumber textField) {
		this.textField = textField;
	}
}
