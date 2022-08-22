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
 * SamsungSmartThingsAggregatorCommunicatorTest
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/2/2022
 * @since 1.0.0
 *
 */
class SamsungSmartThingsAggregatorCommunicatorTest {
	private SamsungSmartThingsAggregatorCommunicator communicator;

	@BeforeEach()
	public void setUp() throws Exception {
		communicator = new SamsungSmartThingsAggregatorCommunicator();
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
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertEquals("000.043.00004", stats.get(HubInfoMetric.FIRMWARE_VERSION.getName()));
		Assert.assertEquals("HomeHCM", stats.get(HubInfoMetric.CURRENT_LOCATION.getName()));
		Assert.assertNotEquals(SmartThingsConstant.NONE, stats.get(HubInfoMetric.STATE.getName()));
	}

	/**
	 * Test getMultipleStatistics with filter
	 * Expect getMultipleStatistics successfully
	 */
	@Tag("RealDevice")
	@Test
	void testFilter() throws Exception {
		communicator.setRoomsFilter("Dining");
		communicator.setDeviceTypesFilter("Light");
		communicator.setDeviceNamesFilter("light 1");
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertNotNull(stats.get(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "light 1"));
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

		String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "13";
		String propertyValue = "Living Room 5";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();

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

		String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "16" + RoomManagementMetric.DELETE_ROOM.getName();
		String propertyValue = "0";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty loom management : Delete and edit room continuously
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testDeleteAndEditRoomNameContinuously() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "15" + RoomManagementMetric.DELETE_ROOM.getName();
		String propertyValue = "0";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "14";
		propertyValue = "Living Room 2";

		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		communicator.getMultipleStatistics();
		communicator.getMultipleStatistics();
		Thread.sleep(30000);

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


	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : Open window shade
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testOpenWindowShade() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Shade 1";
		String propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);
		Map<String, String> stats = extendedStatistics.getStatistics();
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}


	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : Open garage door
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testOpenGarageDoor() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Garagedoor 1";
		String propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : Power on presence sensor
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testPowerOnPresenceSensor() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		;

		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Presence 1";
		String propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : Power on TV
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testPowerOnTV() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		;

		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "TVD";
		String propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : Power on light
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testPowerOnLight() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Light 9";
		String propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : room dropdown
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testDeviceDashBoardRoomDropDown() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "ActiveRoom";
		String propertyValue = "Media room";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Light 10";
		propertyValue = "1";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		communicator.getMultipleStatistics();
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : room dropdown
	 *
	 * Expected: control successfully
	 */
	@Tag("RealDevice")
	@Test
	void testLocationFilter() throws Exception {
		communicator.setConfigManagement("true");
		communicator.setLocationFilter("HomeHCM");
		communicator.setDeviceNamesFilter("light 1");
		communicator.setDeviceTypesFilter("Light");
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "ActiveRoom";
		String propertyValue = "Media room";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertNotNull(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Light 10");
	}
}
