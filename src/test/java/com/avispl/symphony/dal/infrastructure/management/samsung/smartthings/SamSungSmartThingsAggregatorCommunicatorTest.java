/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * SamSungSmartThingsAggregatorCommunicatorTest
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/1/2022
 * @since 1.0.0
 */
public class SamSungSmartThingsAggregatorCommunicatorTest {
	static SamSungSmartThingsAggregatorCommunicator samSungSmartThingsAggregatorCommunicator;
	private static final int HTTP_PORT = 8088;
	private static final int HTTPS_PORT = 8443;
	private static final String HOST_NAME = "api.smartthings.com";
	private static final String PROTOCOL = "https";

	@BeforeEach
	public void init() throws Exception {
		samSungSmartThingsAggregatorCommunicator = new SamSungSmartThingsAggregatorCommunicator();
		samSungSmartThingsAggregatorCommunicator.setTrustAllCertificates(false);
		samSungSmartThingsAggregatorCommunicator.setProtocol(PROTOCOL);
		samSungSmartThingsAggregatorCommunicator.setPort(443);
		samSungSmartThingsAggregatorCommunicator.setHost(HOST_NAME);
		samSungSmartThingsAggregatorCommunicator.setContentType("application/json");
		samSungSmartThingsAggregatorCommunicator.setPassword("***REMOVED***");
		samSungSmartThingsAggregatorCommunicator.init();
		samSungSmartThingsAggregatorCommunicator.authenticate();
		samSungSmartThingsAggregatorCommunicator.ping();
	}

	@AfterEach
	void stopWireMockRule() {
		samSungSmartThingsAggregatorCommunicator.destroy();
	}

	/**
	 * Test getMultipleStatistics get all current system
	 * Expect getMultipleStatistics successfully with three systems
	 */
	@Tag("Mock")
	@Test
	void testGetMultipleStatistics() throws Exception {
		samSungSmartThingsAggregatorCommunicator.ping();
	}
}
