/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * List of controllable properties labels by locale
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/11/2022
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Language {

	@JsonAlias("locale")
	private String locale;

	@JsonAlias("poCodes")
	private List<PoCode> poCodes = new ArrayList<>();

	/**
	 * Retrieves {@code {@link #poCodes}}
	 *
	 * @return value of {@link #poCodes}
	 */
	public List<PoCode> getPoCodes() {
		return poCodes;
	}

	/**
	 * Sets {@code poCodes}
	 *
	 * @param poCodes the {@code java.util.List<com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.PoCode>} field
	 */
	public void setPoCodes(List<PoCode> poCodes) {
		this.poCodes = poCodes;
	}

	/**
	 * Retrieves {@code {@link #locale}}
	 *
	 * @return value of {@link #locale}
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * Sets {@code locale}
	 *
	 * @param locale the {@code java.lang.String} field
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}
}
