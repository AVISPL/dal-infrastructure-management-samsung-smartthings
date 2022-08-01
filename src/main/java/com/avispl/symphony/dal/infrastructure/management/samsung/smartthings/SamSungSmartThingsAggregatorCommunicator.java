package com.avispl.symphony.dal.infrastructure.management.samsung.smartthings;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

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
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.DeviceTypeMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.HubInfoMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsConstant;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsManagementGroupMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.SmartThingsURL;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.location.LocationManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.common.room.RoomManagementMetric;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.Hub;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.Component;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.Device;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapability;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceCapabilityWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceHealth;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.device.DeviceWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.Location;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.location.LocationWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.presentation.DevicePresentation;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.Room;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.room.RoomWrapper;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence.Scene;
import com.avispl.symphony.dal.infrastructure.management.samsung.smartthings.dto.sence.SceneWrapper;
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
						devicesExecutionPool.add(executorService.submit(() -> {
							try {
								retrieveDeviceHealth(device.getDeviceId());
								retrieveDevicePresentation(device);

								if (!presentationIds.contains(device.getPresentationId())) {
									retrieveDevicePresentation(device);
								}
							} catch (Exception e) {
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
	 * Time period within which the device metadata (basic devices information) cannot be refreshed.
	 * Ignored if device list is not yet retrieved or the cached device list is empty {@link SamSungSmartThingsAggregatorCommunicator#aggregatedDevices}
	 */
	private volatile long validDeviceMetaDataRetrievalPeriodTimestamp;

	// Adapter properties
	private String locationFilter;

	/**
	 * SmartThings personal access token
	 */
	private String apiToken;

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
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		apiToken = this.getPassword();
		this.setBaseUri(SmartThingsURL.BASE_URI);
		if (checkValidApiToken()) {
			executorService = Executors.newFixedThreadPool(8);
			executorService.submit(deviceDataLoader = new SamSungSmartThingsDeviceDataLoader());
			validDeviceMetaDataRetrievalPeriodTimestamp = System.currentTimeMillis();
		}
		super.internalInit();
	}

	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		if (!checkValidApiToken()) {
			throw new ResourceNotReachableException("Personal access token cannot be null or empty, please enter valid token in the password field.");
		}
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		Map<String, String> stats = new HashMap<>();
		List<AdvancedControllableProperty> controllablePropertyList = new ArrayList<>();
		extendedStatistics.setStatistics(stats);
		if (!devicesGlobal.isEmpty()) {
			retrieveLocations(stats, controllablePropertyList);
			retrieveRooms(stats, controllablePropertyList);
			retrieveScenes(stats, controllablePropertyList);

			String hubId = findDeviceIdByType(DeviceTypeMetric.HUB.getName());
			if (!hubId.isEmpty()) {
				retrieveHubDetailInfo(stats, hubId);
				retrieveHubHealth(stats, hubId);
			}

		}
		return Collections.singletonList(extendedStatistics);
	}

	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
  //ToDo:
	}

	@Override
	public void controlProperties(List<ControllableProperty> list) throws Exception {
		//ToDo:
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		updateValidRetrieveStatisticsTimestamp();
		return null;
	}

	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}

		if (deviceDataLoader != null) {
			deviceDataLoader.stop();
			deviceDataLoader = null;
		}

		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}

		devicesExecutionPool.forEach(future -> future.cancel(true));
		devicesExecutionPool.clear();

		aggregatedDevices.clear();
		devicesGlobal.clear();
		super.internalDestroy();
	}

	@Override
	protected void authenticate() throws Exception {
  // The aggregator have its own authorization method
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) {
		headers.setBearerAuth(apiToken);
		return headers;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * SmartThings api endpoint does not have ICMP available, so this workaround is needed to provide
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				try (Socket puSocketConnection = new Socket(this.getHost(), this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());

					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, this.getHost(), this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", this.getHost(), this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException tex) {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", this.getHost(), this.getPingTimeout()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	//region retrieve hub inventory, hub heath, locations, rooms, scene  info
	//--------------------------------------------------------------------------------------------------------------------------------

	/**
	 * This method is used to retrieve hub detail info by send GET request to "https://api.smartthings.com/v1/hubdevices/{hubId}"
	 *
	 * @throws ResourceNotReachableException When getting the empty response
	 * @throws ResourceNotReachableException If any error occurs
	 */
	public void retrieveHubDetailInfo(Map<String, String> stats, String hubId) {
		try {
			String request = SmartThingsURL.HUB_DEVICE.concat(hubId);

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
	public void retrieveHubHealth(Map<String, String> stats, String hubId) {
		try {
			String request = SmartThingsURL.HUB_DEVICE
					.concat(hubId)
					.concat(SmartThingsURL.DEVICE_HEALTH);

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
			String request = SmartThingsURL.LOCATIONS;

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
			String request = SmartThingsURL.LOCATIONS
					.concat(locationId)
					.concat(SmartThingsConstant.SLASH)
					.concat(SmartThingsURL.ROOMS);

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
			String request = SmartThingsURL.SCENE
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationId);

			SceneWrapper sceneWrapper = doGet(request, SceneWrapper.class);

			if (sceneWrapper != null) {
				populateScenesManagement(advancedControllableProperties, sceneWrapper);
			} else {
				throw new ResourceNotReachableException("rooms is empty");
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieve rooms info: " + e.getMessage(), e);
		}
	}

	/**
	 * This method is used to populate scenes trigger group
	 */
	public void populateScenesManagement( List<AdvancedControllableProperty> advancedControllableProperties, SceneWrapper sceneWrapper) {
		List<Scene> scenes = sceneWrapper.getScenes();
		for (Scene scene : scenes) {
			addAdvanceControlProperties(advancedControllableProperties,
					createButton(SmartThingsManagementGroupMetric.SCENE.getName().concat(scene.getSceneName()), SmartThingsConstant.RUN, SmartThingsConstant.RUNNING));
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
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.LOCATION_ID)
					.concat(locationId);

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
			String request = SmartThingsURL.CAPABILITIES;

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
	private void retrieveDeviceHealth(String deviceID) {
		Objects.requireNonNull(deviceID);
		try {
			String request = SmartThingsURL.DEVICES
					.concat(SmartThingsConstant.SLASH)
					.concat(deviceID)
					.concat(SmartThingsURL.DEVICE_HEALTH);

			DeviceHealth deviceHealth = doGet(request, DeviceHealth.class);

			if (deviceHealth != null) {
				devicesGlobal.get(deviceID).setState(deviceHealth.getState());
			} else {
				throw new ResourceNotReachableException(String.format("%s health info is empty", devicesGlobal.get(deviceID).getName()));
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException(String.format("Error while retrieve %s health info: " + e.getMessage(), devicesGlobal.get(deviceID).getName()), e);
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
			String request = SmartThingsURL.PRESENTATION
					.concat(SmartThingsConstant.QUESTION_MARK)
					.concat(SmartThingsURL.PRESENTATION_ID)
					.concat(device.getPresentationId())
					.concat(SmartThingsConstant.AMPERSAND)
					.concat(SmartThingsURL.MANUFACTURE_NAME)
					.concat(device.getManufacturerName());

			DevicePresentation devicePresentation = doGet(request, DevicePresentation.class);

			if (devicePresentation != null) {
				for (Device dv : devicesGlobal.values()) {
					if (device.getPresentation().equals(dv.getPresentation())) {
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
	 * Check API token validation
	 *
	 * @return boolean
	 */
	private boolean checkValidApiToken() {
		return !StringUtils.isNullOrEmpty(apiToken);
	}

	/**
	 * Check API token validation
	 *
	 * @return boolean
	 */
	private String findDeviceIdByType(String type) {
		for (Device device : devicesGlobal.values()) {
			List<Component> components = device.getComponents();
			for (Component component : components) {
				if (SmartThingsConstant.MAIN.equals(component.getName()) && type.equals(component.getCategory())) {
					return device.getDeviceId();
				}
			}
		}
		return SmartThingsConstant.EMPTY;
	}
}
