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

	private SmartThingsConstant() {
	}

	public static final String HASH = "#";
	public static final char AT_SIGN = '@';
	public static final String COMMA = ",";
	public static final String QUESTION_MARK = "?";
	public static final String AMPERSAND = "&";
	public static final String EQUAL_SIGN = "=";
	public static final String DOUBLE_QUOTATION = "\"";
	public static final String SLASH = "/";
	public static final String NEXT_LINE = "\n";
	public static final String COLON = ":";
	public static final String DASH = "-";
	public static final String RIGHT_PARENTHESES = ")";
	public static final String LEFT_PARENTHESES = "(";
	public static final String HTTPS = "https://";
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
	public static final String MAIN = "main";
	public static final String ONLINE = "ONLINE";
	public static final String OFFLINE = "OFFLINE";
	public static final String NONE = "None";
	public static final String RATE_LIMIT_HEADER_KEY = "X-RateLimit-Reset";

	// Thread metric
	public static final int MAX_THREAD_QUANTITY = 8;
	public static final int MIN_THREAD_QUANTITY = 1;
	public static final int MAX_DEVICE_QUANTITY_PER_THREAD = 6;
	public static final int MIN_POOLING_INTERVAL = 1;

}
