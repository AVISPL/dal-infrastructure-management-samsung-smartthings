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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatorGroupControllingMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.HubInfoMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.location.LocationManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.CreateRoomMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.RoomManagementMetric;

/**
 * SamSungSmartThingsAggregatorCommunicatorTest
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/2/2022
 * @since 1.0.0
 */
class SamSungSmartThingsAggregatorCommunicatorTest {
	private SamSungSmartThingsAggregatorCommunicator communicator;

	@BeforeEach()
	public void setUp() throws Exception {
		communicator = new SamSungSmartThingsAggregatorCommunicator();
		communicator.setHost("api.smartthings.com");
		communicator.setTrustAllCertificates(false);
		communicator.setPort(443);
		communicator.setProtocol("https");
		communicator.setContentType("application/json");
		communicator.setPassword("***REMOVED***");
		communicator.init();
		communicator.authenticate();
	}

	/**
	 * Test getMultipleStatistics get all current system information
	 * Expect getMultipleStatistics successfully
	 */
	@Tag("RealDevice")
	@Test
	void testGetMultipleStatistics() throws Exception {
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);

		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertEquals("000.043.00004", stats.get(HubInfoMetric.FIRMWARE_VERSION.getName()));
		Assert.assertEquals("My home 1", stats.get(HubInfoMetric.CURRENT_LOCATION.getName()));
		Assert.assertNotEquals(SmartThingsConstant.NONE, stats.get(HubInfoMetric.STATE.getName()));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty location management : Change location name
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testUpdateLocationName() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.LOCATION_MANAGEMENT.getName() + LocationManagementMetric.LOCATION.getName() + "1";
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
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "1";
		String propertyValue = "Living Room 2";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty loom management : Delete room
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testDeleteRoomName() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "5" + RoomManagementMetric.DELETE_ROOM.getName();
		String propertyValue = "0";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty loom management : CreateRoom
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testCreateRoom() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.ROOM_NAME.getName();
		String propertyValue = "Living Room 3";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		propertyName = AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.CREATE_ROOM.getName();
		propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}
}
