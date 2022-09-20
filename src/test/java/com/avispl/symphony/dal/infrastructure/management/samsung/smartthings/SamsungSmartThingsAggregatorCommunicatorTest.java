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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatedDeviceControllingMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatorGroupControllingMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.AggregatedDeviceColorControllingConstant;
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
@Tag ("RealDevice")
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
	 * Test getMultipleStatistics get all current hub information
	 * Expect getMultipleStatistics successfully
	 */
	@Test
	void testGetMultipleStatisticsRetrieveHubInfoSuccessfully() throws Exception {
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertEquals("000.043.00004", stats.get(HubInfoMetric.FIRMWARE_VERSION.getName()));
	}

	/**
	 * Test getMultipleStatistics get all current location information
	 * Expect getMultipleStatistics successfully
	 */
	@Test
	void testGetMultipleStatisticsRetrieveLocationSuccessfully() throws Exception {
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertEquals("HomeHCM", stats.get(HubInfoMetric.CURRENT_LOCATION.getName()));
	}

	/**
	 * Test getMultipleStatistics get all current hub state information
	 * Expect getMultipleStatistics successfully
	 */
	@Test
	void testGetMultipleStatisticsRetrieveHubStateSuccessfully() throws Exception {
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();

		Assert.assertNotEquals(SmartThingsConstant.NONE, stats.get(HubInfoMetric.STATE.getName()));
	}

	/**
	 * Test getMultipleStatistics with filter
	 * Expect getMultipleStatistics successfully
	 */
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
	@Test
	void testCreateRoom() throws Exception {
		communicator.getMultipleStatistics();
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatorGroupControllingMetric.CREATE_ROOM.getName() + CreateRoomMetric.ROOM_NAME.getName();
		String propertyValue = "Living Room 7";
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
	 * Test SamSungSmartThingsAggregator.controlProperty device : filtering location
	 *
	 * Expected: control successfully
	 */
	@Test
	void testLocationFilter() throws Exception {
		communicator.setConfigManagement("true");
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.setConfigManagement("true");
		communicator.setLocationFilter("HomeTMAA");
		communicator.getMultipleStatistics();
		communicator.getMultipleStatistics();

		Assertions.assertNull(AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + "Terrace");
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty device : filtering location and device
	 *
	 * Expected: control successfully
	 */
	@Test
	void testLocationAndDeviceFilter() throws Exception {
		communicator.setConfigManagement("true");
		communicator.setLocationFilter("HomeHCm");
		communicator.setDeviceNamesFilter("");
		communicator.setDeviceTypesFilter("light");
		communicator.setRoomsFilter("terrace, attic, media room");
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String propertyName = AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "ActiveRoom";
		String propertyValue = "Media room";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);

		Assertions.assertNotNull(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Light 10");
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty rooms : filter device and edit room name continuously
	 *
	 * Expected: control successfully
	 */
	@Test
	void testFilterAndEditRoomName() throws Exception {
		communicator.setConfigManagement("true");
		communicator.setLocationFilter("HomeHCm");
		communicator.setDeviceNamesFilter("VEdge Light 10");
		communicator.setDeviceTypesFilter("light");
		communicator.setRoomsFilter("terrace, attic, media room");
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		if (!stats.get(AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "00").equalsIgnoreCase("terrace")) {
			String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "00";
			String propertyValue = "Terrace";
			controllableProperty.setProperty(propertyName);
			controllableProperty.setValue(propertyValue);
			communicator.controlProperty(controllableProperty);
			communicator.getMultipleStatistics();
			Thread.sleep(30000);
			communicator.getMultipleStatistics();
		}

		if (!stats.get(AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "01").equalsIgnoreCase("attic")) {
			String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "00";
			String propertyValue = "Attic";
			controllableProperty.setProperty(propertyName);
			controllableProperty.setValue(propertyValue);
			communicator.controlProperty(controllableProperty);
			communicator.getMultipleStatistics();
			Thread.sleep(30000);
			communicator.getMultipleStatistics();
		}

		String propertyName = AggregatorGroupControllingMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + "00";
		String propertyValue = "terrace 2";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();

		Assertions.assertEquals(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + RoomManagementMetric.ACTIVE_ROOM.getName(), stats.get("Attic"));
	}

	/**
	 * Test SamSungSmartThingsAggregator.retrieveMultipleStatistics
	 *
	 * Expected: control successfully
	 */
	@Test
	void testRetrieveMultipleStatistics() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();

		Assertions.assertNotNull(AggregatorGroupControllingMetric.DEVICES_DASHBOARD.getName() + "vEdge Light 10");
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: switch control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceSwitchControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		communicator.getMultipleStatistics();
		Thread.sleep(30000);

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = "Power";
		String propertyValue = "1";
		String deviceId = "ed72472a-95d6-4a35-9e59-5853a52dd904";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);
		communicator.controlProperty(controllableProperty);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: slider control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceSliderControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		communicator.getMultipleStatistics();
		Thread.sleep(30000);

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = "Dimmer";
		String propertyValue = "51.5";
		String deviceId = "ed72472a-95d6-4a35-9e59-5853a52dd904";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: room control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceRoomControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		communicator.retrieveMultipleStatistics();
		communicator.getMultipleStatistics();
		Thread.sleep(30000);

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = "Room";
		String propertyValue = "Kiwide 09";
		String deviceId = "ed72472a-95d6-4a35-9e59-5853a52dd904";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: text control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceTextControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.retrieveMultipleStatistics();

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = "Channel";
		String propertyValue = "500";
		String deviceId = "9e7b9768-be3a-46fe-aeb4-6eb0a0fbeafb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: dropdown list control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceDropdownListControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics().get(0);
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.retrieveMultipleStatistics();

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = "Window shade";
		String propertyValue = "close";
		String deviceId = "5cbee5f3-f026-4060-9755-0c8fd72ede27";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: color control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceColorControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.retrieveMultipleStatistics();

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatedDeviceControllingMetric.COLOR_CONTROL.getName();
		String propertyValue = "Cyan";
		String deviceId = "2b0d90ff-6320-4a87-b4d1-3d7ec07ddbeb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);
		communicator.getMultipleStatistics();
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: custom color control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceCustomColorControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.retrieveMultipleStatistics();

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatedDeviceControllingMetric.COLOR_CONTROL.getName();
		String propertyValue = AggregatedDeviceColorControllingConstant.CUSTOM_COLOR;
		String deviceId = "2b0d90ff-6320-4a87-b4d1-3d7ec07ddbeb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);
		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: hue control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceHueControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.retrieveMultipleStatistics();

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatedDeviceControllingMetric.COLOR_CONTROL.getName();
		String propertyValue = AggregatedDeviceColorControllingConstant.CUSTOM_COLOR;
		String deviceId = "2b0d90ff-6320-4a87-b4d1-3d7ec07ddbeb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);

		propertyName = AggregatedDeviceControllingMetric.HUE_CONTROL.getName();
		propertyValue = "52";
		deviceId = "2b0d90ff-6320-4a87-b4d1-3d7ec07ddbeb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}

	/**
	 * Test SamSungSmartThingsAggregator.controlProperty aggregated device: saturation control
	 *
	 * Expected: control successfully
	 */
	@Test
	void testAggregatedDeviceSaturationControl() throws Exception {
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.getMultipleStatistics();
		Thread.sleep(30000);
		communicator.retrieveMultipleStatistics();

		ExtendedStatistics extendedStatistics = (ExtendedStatistics) communicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistics.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();

		String propertyName = AggregatedDeviceControllingMetric.COLOR_CONTROL.getName();
		String propertyValue = AggregatedDeviceColorControllingConstant.CUSTOM_COLOR;
		String deviceId = "2b0d90ff-6320-4a87-b4d1-3d7ec07ddbeb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);

		propertyName = AggregatedDeviceControllingMetric.SATURATION_CONTROL.getName();
		propertyValue = "52";
		deviceId = "2b0d90ff-6320-4a87-b4d1-3d7ec07ddbeb";
		controllableProperty.setProperty(propertyName);
		controllableProperty.setValue(propertyValue);
		controllableProperty.setDeviceId(deviceId);

		communicator.controlProperty(controllableProperty);

		Assertions.assertEquals(propertyValue, stats.get(propertyName));
	}
}
