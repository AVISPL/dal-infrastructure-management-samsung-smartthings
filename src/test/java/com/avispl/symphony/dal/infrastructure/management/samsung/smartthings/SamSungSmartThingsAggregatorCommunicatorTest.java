/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatorManagementGroupMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.location.LocationManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.RoomManagementMetric;

/**
 * SamSungSmartThingsAggregatorCommunicatorTest
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/2/2022
 * @since 1.0.0
 */
public class SamSungSmartThingsAggregatorCommunicatorTest {
	private SamSungSmartThingsAggregatorCommunicator communicator;

	@BeforeEach()
	public void setUp() throws Exception {
		communicator = new SamSungSmartThingsAggregatorCommunicator();
		communicator.setHost("api.smartthings.com");
		communicator.setTrustAllCertificates(false);
		communicator.setPort(443);
		communicator.setProtocol("https");
		communicator.setContentType("application/json");
		communicator.setPassword("66ae2a6f-9911-4483-91ea-168d41af62d8");
		communicator.init();
		communicator.authenticate();
	}

	/**
	 * Test getMultipleStatistics get all current system
	 * Expect getMultipleStatistics successfully with three systems
	 */
	@Tag("Mock")
	@Test
	void testGetMultipleStatistics() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertNotEquals("9468", stats.get("AVISPL Test Core110f" + "#" + "SystemId"));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty location management : Change location name
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testUpdateLocationName() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorManagementGroupMetric.LOCATION_MANAGEMENT.getName() + LocationManagementMetric.LOCATION.getName() + "1";
		String propertyValue = "HomeHarry";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty loom management : Change room name
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testUpdateRoomName() throws Exception {
		communicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorManagementGroupMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "1";
		String propertyValue = "Living Room 2";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

}
