package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.HubInfoMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsManagementGroupMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsURL;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.location.LocationManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.RoomManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.Hub;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.Device;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapability;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapabilityWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceHealth;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.Location;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.LocationWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.Room;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.RoomWrapper;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * SamSungSmartThingsAggregatorCommunicator
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/22/2022
 * @since 1.0.0
 */
public class SamSungSmartThingsAggregatorCommunicator extends RestCommunicator implements Aggregator, Monitorable, Controller {

	/**
	 * Process that is running constantly and triggers collecting data from Zoom API endpoints, based on the given timeouts and thresholds.
	 *
	 * @author Maksym.Rossiytsev
	 * @since 1.0.0
	 */
	class SamSungSmartThingsDeviceDataLoader implements Runnable {
		private volatile boolean inProgress;


		public SamSungSmartThingsDeviceDataLoader() {
			inProgress = true;
		}

		@Override
		public void run() {
			mainloop:
			while (inProgress) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					// Ignore for now
				}
				if (!inProgress) {
					break mainloop;
				}

				// next line will determine whether SamSungSmartThings monitoring was paused
				updateAggregatorStatus();
				if (devicePaused) {
					continue mainloop;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Fetching SmartThings devices list");
				}
				long currentTimestamp = System.currentTimeMillis();
				retrieveInfo(currentTimestamp);

				Set<String> presentationIds = new HashSet<>();

				if (logger.isDebugEnabled()) {
					logger.debug("Fetching other than SmartThings device list");
				}
				if (!devicesGlobal.isEmpty() && validDeviceMetaDataRetrievalPeriodTimestamp <= currentTimestamp) {
					validDeviceMetaDataRetrievalPeriodTimestamp = currentTimestamp + deviceMetaDataRetrievalTimeout;

					for (Device device : devicesGlobal.values()) {
						devicesExecutionPool.add(executorService.submit(()-> {
							try {
								retrieveDeviceHealth(device);

								if(!presentationIds.contains(device.getPresentationId())){
									retrieveDevicePresentation(device);
								}
							}catch (Exception e) {
								logger.error(String.format("Exception during retrieve '%s' data processing." + device.getName()), e);
							}
						}));
					}
					do {
						try {
							TimeUnit.MILLISECONDS.sleep(500);
						} catch (InterruptedException e) {
							if (!inProgress) {
								break;
							}
						}
						devicesExecutionPool.removeIf(Future::isDone);
					} while (!devicesExecutionPool.isEmpty());
				}
				if (!inProgress) {
					break mainloop;
				}
			}
		}

		/**
		 * Triggers main loop to stop
		 */
		public void stop() {
			inProgress = false;
		}
	}

	/**
	 * This parameter holds timestamp of when we need to stop performing API calls
	 * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
	 */
	private volatile long validRetrieveStatisticsTimestamp;

	/**
	 * Update the status of the device.
	 * The device is considered as paused if did not receive any retrieveMultipleStatistics()
	 * calls during {@link SamSungSmartThingsAggregatorCommunicator#validRetrieveStatisticsTimestamp}
	 */
	private synchronized void updateAggregatorStatus() {
		devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
	}

	/**
	 * Uptime time stamp to valid one
	 */
	private synchronized void updateValidRetrieveStatisticsTimestamp() {
		validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
		updateAggregatorStatus();
	}

	/**
	 * Indicates whether a device is considered as paused.
	 * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
	 * collection unless the {@link SamSungSmartThingsAggregatorCommunicator#retrieveMultipleStatistics()} method is called which will change it
	 * to a correct value
	 */
	private volatile boolean devicePaused = true;

	/**
	 * Aggregator inactivity timeout. If the {@link SamSungSmartThingsAggregatorCommunicator#retrieveMultipleStatistics()}  method is not
	 * called during this period of time - device is considered to be paused, thus the Cloud API
	 * is not supposed to be called
	 */
	private static final long retrieveStatisticsTimeOut = 3 * 60 * 1000;

	/**
	 * Device metadata retrieval timeout. The general devices list is retrieved once during this time period.
	 */
	private long deviceMetaDataRetrievalTimeout = 60 * 1000 / 2;

	/**
	 * If the {@link SamSungSmartThingsAggregatorCommunicator#deviceMetaDataRetrievalTimeout} is set to a value that is too small -
	 * devices list will be fetched too frequently. In order to avoid this - the minimal value is based on this value.
	 */
	private static final long defaultMetaDataTimeout = 60 * 1000 / 2;


	/**
	 * Time period within which the device metadata (basic devices information) cannot be refreshed.
	 * Ignored if device list is not yet retrieved or the cached device list is empty {@link SamSungSmartThingsAggregatorCommunicator#aggregatedDevices}
	 */
	private volatile long validDeviceMetaDataRetrievalPeriodTimestamp;

	// Adapter properties
	private String locationFilter;

	/**
	 * Store the list of hub info from SmartThings
	 */
	private Hub hubGlobal;

	/**
	 * Store the list of locations from SmartThings
	 */
	private LocationWrapper locationWrapperGlobal;

	/**
	 * Store the list of devices from SmartThings
	 */
	private ConcurrentHashMap<String, Device> devicesGlobal = new ConcurrentHashMap<>();

	/**
	 * Store the list of devices capabilities from SmartThings
	 */
	private List<DeviceCapability> capabilitiesGlobal = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Runner service responsible for collecting data
	 */
	private SamSungSmartThingsDeviceDataLoader deviceDataLoader;

	/**
	 * List of aggregated devices
	 */
	private List<AggregatedDevice> aggregatedDevices = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Executor that runs all the async operations, that {@link #deviceDataLoader} is posting and
	 * {@link #devicesExecutionPool} is keeping track of
	 */
	private static ExecutorService executorService;

	/**
	 * Pool for keeping all the async operations in, to track any operations in progress and cancel them if needed
	 */
	private List<Future> devicesExecutionPool = new ArrayList<>();

	/**
	 * Map of device id and status message
	 */
	private Map<String, String> deviceStatusMessageMap = new HashMap<>();

	/**
	 * Build an instance of ZoomRoomsAggregatorCommunicator
	 * Setup aggregated devices processor, initialize adapter properties
	 *
	 * @throws IOException if unable to locate mapping ymp file or properties file
	 */
	public SamSungSmartThingsAggregatorCommunicator() throws IOException {

	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {

		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		Map<String, String> stats = new HashMap<>();
		extendedStatistics.setStatistics(stats);


		return Collections.singletonList(extendedStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {

	}

	@Override
	public void controlProperties(List<ControllableProperty> list) throws Exception {

	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		return null;
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return null;
	}

	@Override
	protected void authenticate() throws Exception {

	}

	//region retrieve hub inventory, hub heath, locations, rooms, scene  info
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to retrieve hub detail info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}"
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubDetailInfo(Map<String, String> stats) {
		try {
			String request = buildDeviceFullPath(SmartThingsURL.HUB_DEVICE.concat(hubGlobal.getId()));

			Hub hub = doGet(request, Hub.class);

			if (hub != null) {
				stats.put(HubInfoMetric.NAME.getName(), hub.getName());
				stats.put(HubInfoMetric.FIRMWARE_VERSION.getName(), hub.getFirmwareVersion());
				stats.put(HubInfoMetric.SERIAL_NUMBER.getName(), hub.getSerialNumber());
			} else {
				throw new ResourceNotReachableException("Hub information is empty");
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieve hub info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to retrieve hub health info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}/health"
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubHealth(Map<String, String> stats) {
		try {
			String request = buildDeviceFullPath(SmartThingsURL.HUB_DEVICE
					.concat(hubGlobal.getId())
					.concat(SmartThingsURL.DEVICE_HEALTH));

			Hub hub = doGet(request, Hub.class);

			if (hub != null) {
				stats.put(HubInfoMetric.STATE.getName(), hub.getState());
			} else {
				throw new ResourceNotReachableException("Hub health info is empty");
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieve hub info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to retrieve locations info by send GET request to "https://api.smartthings.com/v1/locations"
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveLocations(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		try {
			String request = buildDeviceFullPath(SmartThingsURL.LOCATIONS);

			LocationWrapper locationWrapper = doGet(request, LocationWrapper.class);

			if (locationWrapper != null) {
				locationWrapperGlobal = locationWrapper;
				populateLocationsManagement(stats, advancedControllableProperties, locationWrapper);
			} else {
				throw new ResourceNotReachableException("Hub locations is empty");
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieve locations info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate locations management group
	 */
	public void populateLocationsManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, LocationWrapper locationWrapper) {
		List<Location> locations = locationWrapper.getLocations();
		for (int locationIndex = 1; locationIndex <= locations.size(); locationIndex++) {
			addAdvanceControlProperties(advancedControllableProperties,
					createText(stats, SmartThingsManagementGroupMetric.LOCATION_MANAGEMENT.getName() + LocationManagementMetric.LOCATION.getName() + locationIndex, locations.get(locationIndex).getName()));
		}
	}

	/**
	 * This method is used to retrieve rooms info by send GET request to "https://api.smartthings.com/v1/locations/{locationId}/rooms"
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveRooms(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		try {
			String locationId = locationWrapperGlobal.findByName(locationFilter);
			String request = buildDeviceFullPath(SmartThingsURL.LOCATIONS
					.concat(locationId)
					.concat(SmartThingsURL.ROOMS));

			RoomWrapper roomWrapper = doGet(request, RoomWrapper.class);

			if (roomWrapper != null) {
				populateRoomManagement(stats, advancedControllableProperties, roomWrapper);
			} else {
				throw new ResourceNotReachableException("rooms is empty");
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieve rooms info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate locations management group
	 */
	public void populateRoomManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, RoomWrapper roomWrapper) {
		List<Room> rooms = roomWrapper.getRooms();
		for (int roomIndex = 1; roomIndex <= rooms.size(); roomIndex++) {
			addAdvanceControlProperties(advancedControllableProperties,
					createText(stats, SmartThingsManagementGroupMetric.ROOM_MANAGEMENT.getName() + RoomManagementMetric.ROOM.getName() + roomIndex, rooms.get(roomIndex).getName()));
		}
	}

	/**
	 * This method is used to retrieve scenes info by send GET request to "https://api.smartthings.com/v1/scene?locationId={locationId}"
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveScenes(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		try {
			String locationId = locationWrapperGlobal.findByName(locationFilter);
			String request = buildDeviceFullPath(SmartThingsURL.LOCATIONS
					.concat(locationId)
					.concat(SmartThingsURL.ROOMS));

			RoomWrapper roomWrapper = doGet(request, RoomWrapper.class);

			if (roomWrapper != null) {
				populateRoomManagement(stats, advancedControllableProperties, roomWrapper);
			} else {
				throw new ResourceNotReachableException("rooms is empty");
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieve rooms info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate locations management group
	 */
	public void populateScenesManagement(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, RoomWrapper roomWrapper) {
		List<Room> rooms = roomWrapper.getRooms();
		for (int roomIndex = 1; roomIndex <= rooms.size(); roomIndex++) {
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(SmartThingsManagementGroupMetric.SCENE.getName().concat(SmartThingsConstant.RUN), SmartThingsConstant.RUN, SmartThingsConstant.RUNNING));
		}
	}
	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region retrieve aggregated device and device capabilities info
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Retrieve aggregated devices and system information data -
	 * and set next device/system collection iteration timestamp
	 */
	private void retrieveInfo(long currentTimestamp) {
		if (validDeviceMetaDataRetrievalPeriodTimestamp > currentTimestamp) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Aggregated devices data retrieval is in cool down. %s seconds left",
						(validDeviceMetaDataRetrievalPeriodTimestamp - currentTimestamp) / 1000));
				if (!aggregatedDevices.isEmpty()) {
					logger.debug(String.format("Old fetched devices list: %s", aggregatedDevices));
				}
				if (!capabilitiesGlobal.isEmpty()) {
					logger.debug(String.format("Old fetched capabilities list: %s", capabilitiesGlobal));
				}
			}
			return;
		}
		retrieveDevices();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched devices list: %s", aggregatedDevices));
		}
		retrieveCapabilities();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("New fetched capabilities list: %s", capabilitiesGlobal));
		}
	}

	/**
	 * This method is used to retrieve list of device every 30 seconds by send GET request to https://api.smartthings.com/v1/devices?locationId={locationId}
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveDevices() {
		try {
			String locationId = locationWrapperGlobal.findByName(locationFilter);
			String request = buildDeviceFullPath(SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationId));

			DeviceWrapper responseDeviceList = this.doGet(request, DeviceWrapper.class);
			devicesGlobal = (ConcurrentHashMap<String, Device>) responseDeviceList.getDevices().stream().collect(Collectors.toMap(Device::getDeviceId, Function.identity()));

		} catch (Exception e) {
			String errorMessage = String.format("Aggregated Device Data Retrieval-Error: %s with cause: %s", e.getMessage(), e.getCause().getMessage());
			throw new ResourceNotReachableException(errorMessage, e);
		}
	}

	/**
	 * This method is used to retrieve list of device capabilities every 30 seconds by send GET request to https://api.smartthings.com/v1/capabilities
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveCapabilities() {
		try {
			String request = buildDeviceFullPath(SmartThingsURL.CAPABILITIES);

			DeviceCapabilityWrapper deviceCapabilitiesWrapper = this.doGet(request, DeviceCapabilityWrapper.class);
			capabilitiesGlobal = deviceCapabilitiesWrapper.getDeviceCapabilities();

		} catch (Exception e) {
			String errorMessage = String.format("Device capabilities Data Retrieval-Error: %s with cause: %s", e.getMessage(), e.getCause().getMessage());
			throw new ResourceNotReachableException(errorMessage, e);
		}
	}
	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region retrieve device health, device presentation and device capabilities detail info
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to retrieve list of device health by send GET request to https://api.smartthings.com/v1/devices/{deviceId}/health
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveDeviceHealth(Device device) {
		try {
			String request = buildDeviceFullPath(SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.SLASH)
					.concat(device.getDeviceId())
					.concat(SmartThingsURL.DEVICE_HEALTH));

			DeviceHealth deviceHealth = doGet(request, DeviceHealth.class);

			if (deviceHealth != null) {
				devicesGlobal.get(device.getDeviceId()).setState(deviceHealth.getState());
			} else {
				throw new ResourceNotReachableException(String.format("%s health info is empty", device.getName()));
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while retrieve %s health info: " + e.getMessage(), device.getName()), e);
		}
	}

	/**
	 * This method is used to retrieve list of device health by send GET request to
	 * https://api.smartthings.com/v1/presentation?presentationId={presentationId}&manufacturerName={manufacturerName}
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	private void retrieveDevicePresentation(Device device) {
		try {
			String request = buildDeviceFullPath(SmartThingsURL.PRESENTATION
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.PRESENTATION_ID)
					.concat(device.getPresentationId())
					.concat(SmartThingsConstant.AMPERSAND)
					.concat(SmartThingsURL.MANUFACTURE_NAME)
					.concat(device.getManufacturerName()));

			DevicePresentation devicePresentation = doGet(request, DevicePresentation.class);

			if (devicePresentation != null) {
				for (Device dv : devicesGlobal.values()) {
					if (device.getPresentation().equals(dv.getPresentation())){
						devicesGlobal.get(dv.getDeviceId()).setPresentation(devicePresentation);
					}
				}
			} else {
				throw new ResourceNotReachableException(String.format("%s presentation info is empty", device.getName()));
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while retrieve %s presentation info: " + e.getMessage(), device.getName()), e);
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	//region populate advanced controllable properties
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Add advancedControllableProperties if advancedControllableProperties different empty
	 *
	 * @param advancedControllableProperties advancedControllableProperties is the list that store all controllable properties
	 * @param property the property is item advancedControllableProperties
	 */
	private void addAdvanceControlProperties(List<AdvancedControllableProperty> advancedControllableProperties, AdvancedControllableProperty property) {
		if (property != null) {
			for (AdvancedControllableProperty controllableProperty : advancedControllableProperties) {
				if (controllableProperty.getName().equals(property.getName())) {
					advancedControllableProperties.remove(controllableProperty);
					break;
				}
			}
			advancedControllableProperties.add(property);
		}
	}

	/**
	 * Create a switch controllable property
	 *
	 * @param name name of the switch
	 * @param status initial switch state (0|1)
	 * @return AdvancedControllableProperty button instance
	 */
	private AdvancedControllableProperty createSwitch(Map<String, String> stats, String name, int status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);
		stats.put(name, String.valueOf(status));
		return new AdvancedControllableProperty(name, new Date(), toggle, status);
	}

	/**
	 * Create a controllable property Text
	 *
	 * @param name the name of property
	 * @param stringValue character string
	 * @return AdvancedControllableProperty Text instance
	 */
	private AdvancedControllableProperty createText(Map<String, String> stats, String name, String stringValue) {
		if (stringValue == null) {
			stringValue = SmartThingsConstant.EMPTY;
		}
		stats.put(name, stringValue);
		AdvancedControllableProperty.Text text = new AdvancedControllableProperty.Text();
		return new AdvancedControllableProperty(name, new Date(), text, stringValue);
	}

	/**
	 * Instantiate Text controllable property
	 *
	 * @param name name of the property
	 * @param label default button label
	 * @return AdvancedControllableProperty button instance
	 */
	private AdvancedControllableProperty createButton(String name, String label, String labelPressed) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(0L);
		return new AdvancedControllableProperty(name, new Date(), button, "");
	}


	//--------------------------------------------------------------------------------------------------------------------------------
	//endregion

	/**
	 * @param path url of the request
	 * @return String full path of the device
	 */
	private String buildDeviceFullPath(String path) {
		Objects.requireNonNull(path);
		String host = getHost();
		if (StringUtils.isNotNullOrEmpty(host)) {
			host = SmartThingsURL.BASE_URI;
		}

		return SmartThingsConstant.HTTPS
				.concat(host)
				.concat(path);
	}
}
